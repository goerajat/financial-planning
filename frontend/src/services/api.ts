import axios from 'axios';
import { ExplainThread, FinancialPlan, HealthPremiumEstimate, HealthPremiumEstimateRequest, RateConfig, RefreshBalancesResponse, RefreshRun, YearSummary, SensitivityAnalysisRequest, SensitivityAnalysisResponse, RothComparisonRequest, RothComparisonResponse, StateScenarioRequest, StateScenarioResponse, ScenarioPdfRequest } from '../types';

const API_BASE = process.env.REACT_APP_API_URL || '/api';

const api = axios.create({
  baseURL: API_BASE,
  headers: {
    'Content-Type': 'application/json',
  },
});

export const planApi = {
  getPlan: async (): Promise<FinancialPlan> => {
    const response = await api.get('/plan');
    return response.data;
  },

  savePlan: async (planData: FinancialPlan): Promise<FinancialPlan> => {
    const response = await api.put('/plan', planData);
    return response.data;
  },

  getHistory: async (): Promise<RefreshRun[]> => {
    const response = await api.get('/plan/history');
    return response.data;
  },

  getHistoryRun: async (id: number): Promise<RefreshRun> => {
    const response = await api.get(`/plan/history/${id}`);
    return response.data;
  },

  generatePdf: async (planData: FinancialPlan): Promise<Blob> => {
    const response = await api.post('/plan/generate', planData, {
      responseType: 'blob',
    });
    return response.data;
  },

  exportToJson: async (planData: FinancialPlan): Promise<string> => {
    const response = await api.post('/plan/export', planData);
    return response.data;
  },

  importFromJson: async (json: string): Promise<FinancialPlan> => {
    const response = await api.post('/plan/import', json, {
      headers: { 'Content-Type': 'text/plain' },
    });
    return response.data;
  },

  uploadFile: async (file: File): Promise<FinancialPlan> => {
    const formData = new FormData();
    formData.append('file', file);
    const response = await api.post('/plan/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    });
    return response.data;
  },

  getSummary: async (planData: FinancialPlan): Promise<YearSummary[]> => {
    const response = await api.post('/plan/summary', planData);
    return response.data;
  },

  refreshBalances: async (planData: FinancialPlan): Promise<RefreshBalancesResponse> => {
    const response = await api.post('/plan/refresh-balances', planData);
    return response.data;
  },

  getDefaultRates: async (): Promise<RateConfig[]> => {
    const response = await api.get('/plan/default-rates');
    return response.data;
  },

  runSensitivityAnalysis: async (request: SensitivityAnalysisRequest): Promise<SensitivityAnalysisResponse> => {
    const response = await api.post('/plan/sensitivity-analysis', request);
    return response.data;
  },

  runRothComparison: async (request: RothComparisonRequest): Promise<RothComparisonResponse> => {
    const response = await api.post('/plan/roth-comparison', request);
    return response.data;
  },

  runStateScenarioAnalysis: async (request: StateScenarioRequest): Promise<StateScenarioResponse> => {
    const response = await api.post('/plan/state-scenario', request);
    return response.data;
  },

  getHealthPremiumEstimate: async (personName: string): Promise<HealthPremiumEstimate> => {
    const response = await api.get('/plan/health-premiums', { params: { person: personName } });
    return response.data;
  },

  requestHealthPremiumEstimate: async (request: HealthPremiumEstimateRequest): Promise<HealthPremiumEstimate> => {
    const response = await api.post('/plan/health-premiums/estimate', request);
    return response.data;
  },

  explainThread: async (planData: FinancialPlan): Promise<ExplainThread> => {
    const response = await api.post('/plan/explain/thread', planData);
    return response.data;
  },

  explainAsk: async (planData: FinancialPlan, question: string): Promise<ExplainThread> => {
    const response = await api.post('/plan/explain/ask', { plan: planData, question });
    return response.data;
  },

  explainStatus: async (planHash: string): Promise<ExplainThread> => {
    const response = await api.get('/plan/explain/status', { params: { plan: planHash } });
    return response.data;
  },

  generateScenarioPdf: async (request: ScenarioPdfRequest): Promise<Blob> => {
    const response = await api.post('/plan/generate-scenario', request, {
      responseType: 'blob',
    });
    return response.data;
  },
};

export default api;
