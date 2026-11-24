import React, { createContext, useCallback, useContext, useEffect, useRef, useState } from 'react';
import { usePlan } from './PlanContext';
import { planApi } from '../services/api';
import { ExplainThread, FinancialPlan } from '../types';

interface ExplainContextType {
  isOpen: boolean;
  open: () => void;
  close: () => void;
  thread: ExplainThread | null;
  loading: boolean;
  error: string | null;
  notice: string | null;
  ask: (question: string) => Promise<void>;
}

const ExplainContext = createContext<ExplainContextType | undefined>(undefined);

const POLL_MS = 2500;
const RELOAD_DEBOUNCE_MS = 600;

/**
 * The conversation about the plan. The server keys it by the plan's contents,
 * so an edit on any tab starts a fresh thread; the panel reloads when the plan
 * changes while it is open, and keeps polling a running question even after
 * the panel is closed so the answer is there when it reopens.
 */
export const ExplainProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const { persons, entries, rates, statesByYear } = usePlan();
  const [isOpen, setOpen] = useState(false);
  const [thread, setThread] = useState<ExplainThread | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);
  const planRef = useRef<FinancialPlan>({ persons, entries, rates, statesByYear });
  planRef.current = { persons, entries, rates, statesByYear };
  const suggestionsRef = useRef<string[]>([]);
  const threadRef = useRef<ExplainThread | null>(null);

  // A status poll carries no suggestions; keep the ones the last full load gave.
  const merge = useCallback((t: ExplainThread) => {
    if (t.suggestions) suggestionsRef.current = t.suggestions;
    const next = { ...t, suggestions: t.suggestions ?? suggestionsRef.current };
    threadRef.current = next;
    setThread(next);
  }, []);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      merge(await planApi.explainThread(planRef.current));
    } catch (err: any) {
      setError(err.response?.data?.error || err.message || 'Could not load the conversation');
    } finally {
      setLoading(false);
    }
  }, [merge]);

  useEffect(() => {
    if (!isOpen) return;
    const delay = threadRef.current ? RELOAD_DEBOUNCE_MS : 0;
    const t = window.setTimeout(load, delay);
    return () => window.clearTimeout(t);
  }, [isOpen, persons, entries, rates, statesByYear, load]);

  const running = thread?.running ?? false;
  const planHash = thread?.planHash ?? null;
  useEffect(() => {
    if (!running || !planHash) return;
    const id = window.setInterval(async () => {
      try {
        merge(await planApi.explainStatus(planHash));
      } catch {
        // the next tick tries again
      }
    }, POLL_MS);
    return () => window.clearInterval(id);
  }, [running, planHash, merge]);

  const ask = useCallback(async (question: string) => {
    setError(null);
    setNotice(null);
    const before = threadRef.current?.messages.length ?? 0;
    try {
      const t = await planApi.explainAsk(planRef.current, question);
      merge(t);
      if (!t.running && t.messages.length === before) setNotice('That question is already answered above.');
    } catch (err: any) {
      setError(err.response?.data?.error || err.message || 'Could not send the question');
    }
  }, [merge]);

  const open = useCallback(() => { setNotice(null); setOpen(true); }, []);
  const close = useCallback(() => setOpen(false), []);

  return (
    <ExplainContext.Provider value={{ isOpen, open, close, thread, loading, error, notice, ask }}>
      {children}
    </ExplainContext.Provider>
  );
};

export const useExplain = (): ExplainContextType => {
  const ctx = useContext(ExplainContext);
  if (!ctx) throw new Error('useExplain must be used within an ExplainProvider');
  return ctx;
};
