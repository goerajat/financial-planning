import React, { createContext, useContext, useState, useCallback, useEffect } from 'react';
import { Person, FinancialEntry, FinancialPlan, RateConfig } from '../types';
import { planApi } from '../services/api';

interface PlanContextType {
  persons: Person[];
  entries: FinancialEntry[];
  rates: { [key: string]: number };
  rateConfigs: RateConfig[];
  pdfBlob: Blob | null;
  isLoading: boolean;
  error: string | null;

  addPerson: (person: Person) => void;
  updatePerson: (index: number, person: Person) => void;
  deletePerson: (index: number) => void;

  addEntry: (entry: FinancialEntry) => void;
  updateEntry: (id: number, entry: FinancialEntry) => void;
  deleteEntry: (id: number) => void;

  updateRate: (itemType: string, rate: number) => void;

  generatePdf: () => Promise<void>;
  savePlan: () => void;
  loadPlan: (file: File) => Promise<void>;
  clearError: () => void;
}

const PlanContext = createContext<PlanContextType | undefined>(undefined);

export const PlanProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [persons, setPersons] = useState<Person[]>([]);
  const [entries, setEntries] = useState<FinancialEntry[]>([]);
  const [rates, setRates] = useState<{ [key: string]: number }>({});
  const [rateConfigs, setRateConfigs] = useState<RateConfig[]>([]);
  const [pdfBlob, setPdfBlob] = useState<Blob | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [nextEntryId, setNextEntryId] = useState(1);

  // Load default rates on mount
  useEffect(() => {
    const loadDefaultRates = async () => {
      try {
        const defaultRates = await planApi.getDefaultRates();
        setRateConfigs(defaultRates);
        const rateMap: { [key: string]: number } = {};
        defaultRates.forEach(r => {
          rateMap[r.itemType] = r.rate;
        });
        setRates(rateMap);
      } catch (err) {
        console.error('Failed to load default rates:', err);
        // Set fallback defaults
        setRates({
          INCOME: 3.0,
          EXPENSE: 3.0,
          NON_QUALIFIED: 6.0,
          QUALIFIED: 6.0,
          ROTH: 6.0,
          CASH: 2.0,
          REAL_ESTATE: 3.0,
          LIFE_INSURANCE_BENEFIT: 0.0,
          SOCIAL_SECURITY_BENEFITS: 2.0,
          ROTH_CONTRIBUTION: 0.0,
          QUALIFIED_CONTRIBUTION: 0.0,
          LIFE_INSURANCE_CONTRIBUTION: 0.0,
          MORTGAGE: 6.5,
          MORTGAGE_REPAYMENT: 0.0,
        });
      }
    };
    loadDefaultRates();
  }, []);

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

  const updateRate = useCallback((itemType: string, rate: number) => {
    setRates(prev => ({ ...prev, [itemType]: rate }));
  }, []);

  const generatePdf = useCallback(async () => {
    setIsLoading(true);
    setError(null);
    try {
      const planData: FinancialPlan = { persons, entries, rates };
      const blob = await planApi.generatePdf(planData);
      setPdfBlob(blob);
    } catch (err: any) {
      setError(err.message || 'Failed to generate PDF');
    } finally {
      setIsLoading(false);
    }
  }, [persons, entries, rates]);

  const savePlan = useCallback(() => {
    const planData: FinancialPlan = { persons, entries, rates };
    const json = JSON.stringify(planData, null, 2);
    const blob = new Blob([json], { type: 'application/json' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = 'financial_plan.json';
    a.click();
    URL.revokeObjectURL(url);
  }, [persons, entries, rates]);

  const loadPlan = useCallback(async (file: File) => {
    setIsLoading(true);
    setError(null);
    try {
      const planData = await planApi.uploadFile(file);
      setPersons(planData.persons || []);
      setEntries(planData.entries || []);
      if (planData.rates) {
        setRates(planData.rates);
      }
      // Update next entry ID
      const maxId = Math.max(0, ...planData.entries.map(e => e.id || 0));
      setNextEntryId(maxId + 1);
    } catch (err: any) {
      setError(err.message || 'Failed to load plan');
    } finally {
      setIsLoading(false);
    }
  }, []);

  const clearError = useCallback(() => setError(null), []);

  return (
    <PlanContext.Provider value={{
      persons,
      entries,
      rates,
      rateConfigs,
      pdfBlob,
      isLoading,
      error,
      addPerson,
      updatePerson,
      deletePerson,
      addEntry,
      updateEntry,
      deleteEntry,
      updateRate,
      generatePdf,
      savePlan,
      loadPlan,
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
