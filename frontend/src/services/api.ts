import axios from 'axios';
import { FinancialPlan, RateConfig } from '../types';

const API_BASE = process.env.REACT_APP_API_URL || 'http://localhost:8080/api';

const api = axios.create({
  baseURL: API_BASE,
  headers: {
    'Content-Type': 'application/json',
  },
});

export const planApi = {
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

  getDefaultRates: async (): Promise<RateConfig[]> => {
    const response = await api.get('/plan/default-rates');
    return response.data;
  },
};

export default api;
