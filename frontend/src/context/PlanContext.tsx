import React, { createContext, useContext, useState, useCallback, useEffect, useRef } from 'react';
import { Person, FinancialEntry, FinancialPlan, RateConfig, StateByYear, RefreshReport, YearSummary } from '../types';
import { planApi } from '../services/api';

export type SaveStatus = 'loading' | 'saved' | 'saving' | 'error';

interface PlanContextType {
  persons: Person[];
  entries: FinancialEntry[];
  rates: { [key: string]: number };
  rateConfigs: RateConfig[];
  statesByYear: StateByYear[];
  pdfBlob: Blob | null;
  summary: YearSummary[] | null;
  isLoading: boolean;
  error: string | null;
  saveStatus: SaveStatus;
  saveError: string | null;

  addPerson: (person: Person) => void;
  updatePerson: (index: number, person: Person) => void;
  deletePerson: (index: number) => void;

  addEntry: (entry: FinancialEntry) => void;
  updateEntry: (id: number, entry: FinancialEntry) => void;
  deleteEntry: (id: number) => void;
  refreshBalances: () => Promise<RefreshReport | null>;

  updateRate: (itemType: string, rate: number) => void;

  addStateByYear: (state: StateByYear) => void;
  updateStateByYear: (index: number, state: StateByYear) => void;
  deleteStateByYear: (index: number) => void;

  generatePdf: () => Promise<void>;
  exportPlan: () => void;
  importPlan: (file: File) => Promise<void>;
  clearError: () => void;
}

const PlanContext = createContext<PlanContextType | undefined>(undefined);

const FALLBACK_RATES: { [key: string]: number } = {
  INCOME: 3.0,
  EXPENSE: 3.0,
  NON_QUALIFIED: 6.0,
  QUALIFIED: 6.0,
  ROTH: 6.0,
  CASH: 2.0,
  REAL_ESTATE: 3.0,
  LIFE_INSURANCE_BENEFIT: 0.0,
  LIFE_INSURANCE_CASH: 4.0,
  SOCIAL_SECURITY_BENEFITS: 2.0,
  ROTH_CONTRIBUTION: 0.0,
  QUALIFIED_CONTRIBUTION: 0.0,
  LIFE_INSURANCE_CONTRIBUTION: 0.0,
  MORTGAGE: 6.5,
  MORTGAGE_REPAYMENT: 0.0,
  ONE_OFF_PROCEEDS: 0.0,
  HEALTH_PREMIUM: 5.5,
};

const SAVE_DELAY_MS = 500;

export const PlanProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [persons, setPersons] = useState<Person[]>([]);
  const [entries, setEntries] = useState<FinancialEntry[]>([]);
  const [rates, setRates] = useState<{ [key: string]: number }>({});
  const [rateConfigs, setRateConfigs] = useState<RateConfig[]>([]);
  const [statesByYear, setStatesByYear] = useState<StateByYear[]>([]);
  const [pdfBlob, setPdfBlob] = useState<Blob | null>(null);
  const [summary, setSummary] = useState<YearSummary[] | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [saveStatus, setSaveStatus] = useState<SaveStatus>('loading');
  const [saveError, setSaveError] = useState<string | null>(null);
  const [nextEntryId, setNextEntryId] = useState(1);

  // The plan is stored on the server; every edit is written back after a short
  // pause. State that came FROM the server must not be written straight back,
  // so loads flip this flag and the save effect skips that one render.
  const fromServerRef = useRef(false);
  const loadedRef = useRef(false);
  const defaultsRef = useRef<{ [key: string]: number }>(FALLBACK_RATES);
  const saveTimer = useRef<number | null>(null);
  const pendingRef = useRef(false);

  const applyPlan = useCallback((plan: FinancialPlan, defaultRates?: { [key: string]: number }) => {
    fromServerRef.current = true;
    setPersons(plan.persons || []);
    setEntries(plan.entries || []);
    if (defaultRates) defaultsRef.current = defaultRates;
    const hasRates = plan.rates && Object.keys(plan.rates).length > 0;
    // The engine fills a missing key with its default, so show that default rather than 0.
    setRates(hasRates ? { ...defaultsRef.current, ...plan.rates } : defaultsRef.current);
    setStatesByYear(plan.statesByYear || []);
    const maxId = Math.max(0, ...(plan.entries || []).map(e => e.id || 0));
    setNextEntryId(maxId + 1);
  }, []);

  // First load: default rates and the stored plan together, so an empty store
  // starts from the defaults without writing them back as a plan.
  useEffect(() => {
    let cancelled = false;
    const load = async () => {
      let defaults = FALLBACK_RATES;
      try {
        const defaultRates = await planApi.getDefaultRates();
        setRateConfigs(defaultRates);
        const rateMap: { [key: string]: number } = {};
        defaultRates.forEach(r => { rateMap[r.itemType] = r.rate; });
        defaults = rateMap;
      } catch (err) {
        console.error('Failed to load default rates:', err);
      }
      try {
        const plan = await planApi.getPlan();
        if (cancelled) return;
        applyPlan(plan, defaults);
        setSaveStatus('saved');
      } catch (err: any) {
        if (cancelled) return;
        fromServerRef.current = true;
        setRates(defaults);
        setSaveStatus('error');
        setSaveError(err.message || 'Could not load the stored plan');
      } finally {
        loadedRef.current = true;
      }
    };
    load();
    return () => { cancelled = true; };
  }, [applyPlan]);

  // Autosave.
  useEffect(() => {
    if (!loadedRef.current) return;
    if (fromServerRef.current) {
      fromServerRef.current = false;
      return;
    }
    pendingRef.current = true;
    setSaveStatus('saving');
    if (saveTimer.current !== null) window.clearTimeout(saveTimer.current);
    const planData: FinancialPlan = { persons, entries, rates, statesByYear };
    saveTimer.current = window.setTimeout(async () => {
      saveTimer.current = null;
      try {
        await planApi.savePlan(planData);
        pendingRef.current = false;
        setSaveStatus('saved');
        setSaveError(null);
      } catch (err: any) {
        setSaveStatus('error');
        setSaveError(err.response?.data?.error || err.message || 'Save failed');
      }
    }, SAVE_DELAY_MS);
  }, [persons, entries, rates, statesByYear]);

  // Another device may have changed the plan while this tab was in the
  // background; pick it up when the tab comes back, unless a save is pending.
  useEffect(() => {
    const onVisible = async () => {
      if (document.visibilityState !== 'visible' || !loadedRef.current || pendingRef.current) return;
      try {
        const plan = await planApi.getPlan();
        if (pendingRef.current) return;
        applyPlan(plan);
      } catch (err) {
        console.error('Failed to reload the plan:', err);
      }
    };
    document.addEventListener('visibilitychange', onVisible);
    return () => document.removeEventListener('visibilitychange', onVisible);
  }, [applyPlan]);

  const addPerson = useCallback((person: Person) => {
    const currentYear = new Date().getFullYear();
    setPersons(prev => [...prev, { ...person, currentAge: currentYear - person.yearOfBirth }]);
  }, []);

  const updatePerson = useCallback((index: number, person: Person) => {
    const currentYear = new Date().getFullYear();
    setPersons(prev => {
      const updated = [...prev];
      updated[index] = { ...person, currentAge: currentYear - person.yearOfBirth };
      return updated;
    });
  }, []);

  const deletePerson = useCallback((index: number) => {
    setPersons(prev => prev.filter((_, i) => i !== index));
  }, []);

  const addEntry = useCallback((entry: FinancialEntry) => {
    setEntries(prev => [...prev, { ...entry, id: nextEntryId }]);
    setNextEntryId(prev => prev + 1);
  }, [nextEntryId]);

  const updateEntry = useCallback((id: number, entry: FinancialEntry) => {
    setEntries(prev => prev.map(e => e.id === id ? { ...entry, id } : e));
  }, []);

  const deleteEntry = useCallback((id: number) => {
    setEntries(prev => prev.filter(e => e.id !== id));
  }, []);

  const refreshBalances = useCallback(async (): Promise<RefreshReport | null> => {
    setIsLoading(true);
    setError(null);
    try {
      const planData: FinancialPlan = { persons, entries, rates, statesByYear };
      const result = await planApi.refreshBalances(planData);
      // The server stored the refreshed plan before answering.
      applyPlan(result.plan);
      setSaveStatus('saved');
      return result.report;
    } catch (err: any) {
      setError(err.response?.data?.error || err.message || 'Failed to refresh balances');
      return null;
    } finally {
      setIsLoading(false);
    }
  }, [persons, entries, rates, statesByYear, applyPlan]);

  const updateRate = useCallback((itemType: string, rate: number) => {
    setRates(prev => ({ ...prev, [itemType]: rate }));
  }, []);

  const addStateByYear = useCallback((state: StateByYear) => {
    setStatesByYear(prev => [...prev, state]);
  }, []);

  const updateStateByYear = useCallback((index: number, state: StateByYear) => {
    setStatesByYear(prev => {
      const updated = [...prev];
      updated[index] = state;
      return updated;
    });
  }, []);

  const deleteStateByYear = useCallback((index: number) => {
    setStatesByYear(prev => prev.filter((_, i) => i !== index));
  }, []);

  const generatePdf = useCallback(async () => {
    setIsLoading(true);
    setError(null);
    try {
      const planData: FinancialPlan = { persons, entries, rates, statesByYear };
      const [blob, rows] = await Promise.all([planApi.generatePdf(planData), planApi.getSummary(planData)]);
      setPdfBlob(blob);
      setSummary(rows);
    } catch (err: any) {
      setError(err.message || 'Failed to generate PDF');
    } finally {
      setIsLoading(false);
    }
  }, [persons, entries, rates, statesByYear]);

  const exportPlan = useCallback(() => {
    const planData: FinancialPlan = { persons, entries, rates, statesByYear };
    const json = JSON.stringify(planData, null, 2);
    const blob = new Blob([json], { type: 'application/json' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = 'financial_plan.json';
    a.click();
    URL.revokeObjectURL(url);
  }, [persons, entries, rates, statesByYear]);

  const importPlan = useCallback(async (file: File) => {
    setIsLoading(true);
    setError(null);
    try {
      // The server stores the imported plan before answering.
      const planData = await planApi.uploadFile(file);
      applyPlan(planData);
      setSaveStatus('saved');
    } catch (err: any) {
      setError(err.message || 'Failed to import plan');
    } finally {
      setIsLoading(false);
    }
  }, [applyPlan]);

  const clearError = useCallback(() => setError(null), []);

  return (
    <PlanContext.Provider value={{
      persons,
      entries,
      rates,
      rateConfigs,
      statesByYear,
      pdfBlob,
      summary,
      isLoading,
      error,
      saveStatus,
      saveError,
      addPerson,
      updatePerson,
      deletePerson,
      addEntry,
      updateEntry,
      deleteEntry,
      refreshBalances,
      updateRate,
      addStateByYear,
      updateStateByYear,
      deleteStateByYear,
      generatePdf,
      exportPlan,
      importPlan,
      clearError,
    }}>
      {children}
    </PlanContext.Provider>
  );
};

export const usePlan = () => {
  const context = useContext(PlanContext);
  if (!context) {
    throw new Error('usePlan must be used within a PlanProvider');
  }
  return context;
};
