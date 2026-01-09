export interface Person {
  name: string;
  yearOfBirth: number;
  currentAge?: number;
}

export interface FinancialEntry {
  id?: number;
  personName: string;
  itemType: string;
  description: string;
  value: number;
  startYear: number;
  endYear: number;
}

export interface RateConfig {
  itemType: string;
  rate: number;
  displayName: string;
  description: string;
}

export interface StateByYear {
  state: State;
  startYear: number;
  endYear: number;
}

export type State = 'NJ' | 'NY' | 'FL';

export const STATES: { value: State; label: string }[] = [
  { value: 'NJ', label: 'New Jersey' },
  { value: 'NY', label: 'New York' },
  { value: 'FL', label: 'Florida' },
];

export interface FinancialPlan {
  persons: Person[];
  entries: FinancialEntry[];
  rates: { [key: string]: number };
  statesByYear?: StateByYear[];
}

export const ITEM_TYPES = [
  { value: 'INCOME', label: 'Income' },
  { value: 'EXPENSE', label: 'Expense' },
  { value: 'NON_QUALIFIED', label: 'Non-Qualified Assets' },
  { value: 'QUALIFIED', label: 'Qualified Assets (401K/IRA)' },
  { value: 'ROTH', label: 'Roth IRA' },
  { value: 'CASH', label: 'Cash' },
  { value: 'REAL_ESTATE', label: 'Real Estate' },
  { value: 'LIFE_INSURANCE_BENEFIT', label: 'Life Insurance Benefit' },
  { value: 'SOCIAL_SECURITY_BENEFITS', label: 'Social Security Benefits' },
  { value: 'ROTH_CONTRIBUTION', label: 'Roth Contribution' },
  { value: 'QUALIFIED_CONTRIBUTION', label: 'Qualified Contribution' },
  { value: 'LIFE_INSURANCE_CONTRIBUTION', label: 'Life Insurance Premium' },
  { value: 'MORTGAGE', label: 'Mortgage' },
  { value: 'MORTGAGE_REPAYMENT', label: 'Mortgage Extra Payment' },
] as const;

export type ItemType = typeof ITEM_TYPES[number]['value'];

// Sensitivity Analysis Types
export type SensitivityRateType = 'EXPENSE' | 'ALL_ASSETS' | 'QUALIFIED' | 'NON_QUALIFIED' | 'ROTH';

export const SENSITIVITY_RATE_TYPES: { value: SensitivityRateType; label: string; defaultMin: number; defaultMax: number }[] = [
  { value: 'EXPENSE', label: 'Expense Growth', defaultMin: 2, defaultMax: 5 },
  { value: 'ALL_ASSETS', label: 'All Asset Growth (Qualified + Non-Qualified + Roth)', defaultMin: 4, defaultMax: 8 },
  { value: 'QUALIFIED', label: 'Qualified Asset Growth', defaultMin: 4, defaultMax: 8 },
  { value: 'NON_QUALIFIED', label: 'Non-Qualified Asset Growth', defaultMin: 4, defaultMax: 8 },
  { value: 'ROTH', label: 'Roth Asset Growth', defaultMin: 4, defaultMax: 8 },
];

export interface SensitivityAnalysisRequest {
  planData: FinancialPlan;
  rateType: SensitivityRateType;
  minRate: number;
  maxRate: number;
  rateIncrement?: number;
  yearIncrement?: number;
}

export interface SensitivityCell {
  year: number;
  netWorth: number | null;
  hasShortfall: boolean;
  shortfallYear: number | null;
}

export interface SensitivityRow {
  rate: number;
  cells: SensitivityCell[];
}

export interface SensitivityAnalysisResponse {
  rateType: string;
  rateTypeLabel: string;
  firstDataYear: number;
  milestoneYears: number[];
  rows: SensitivityRow[];
}

// Roth Conversion Comparison Types
export type FilingStatus = 'SINGLE' | 'MARRIED_FILING_JOINTLY' | 'MARRIED_FILING_SEPARATELY' | 'HEAD_OF_HOUSEHOLD';

export const FILING_STATUSES: { value: FilingStatus; label: string }[] = [
  { value: 'MARRIED_FILING_JOINTLY', label: 'Married Filing Jointly' },
  { value: 'SINGLE', label: 'Single' },
  { value: 'MARRIED_FILING_SEPARATELY', label: 'Married Filing Separately' },
  { value: 'HEAD_OF_HOUSEHOLD', label: 'Head of Household' },
];

export interface RothComparisonRequest {
  planData: FinancialPlan;
  yearIncrement?: number;
  filingStatus?: FilingStatus;
}

export interface RothComparisonCell {
  year: number;
  netWorth: number;
  netWorthDelta: number;
  cumulativeTaxes: number;
  rothBalance: number;
  hasShortfall: boolean;
  shortfallYear: number | null;
}

export interface RothComparisonRow {
  scenarioName: string;
  bracketThreshold: number | null;
  cells: RothComparisonCell[];
}

export interface RothComparisonResponse {
  firstDataYear: number;
  milestoneYears: number[];
  rows: RothComparisonRow[];
}

// State Scenario Analysis Types
export interface StateScenario {
  name: string;
  statesByYear: StateByYear[];
}

export interface StateScenarioRequest {
  planData: FinancialPlan;
  scenarios: StateScenario[];
  yearIncrement?: number;
  filingStatus?: FilingStatus;
}

export interface StateScenarioCell {
  year: number;
  netWorth: number;
  netWorthDelta: number;
  cumulativeTaxes: number;
  cumulativeStateTaxes: number;
  stateTaxSavings: number;
  activeState: string;
  hasShortfall: boolean;
  shortfallYear: number | null;
}

export interface StateScenarioRow {
  scenarioName: string;
  scenarioDescription: string;
  cells: StateScenarioCell[];
}

export interface StateScenarioResponse {
  firstDataYear: number;
  milestoneYears: number[];
  rows: StateScenarioRow[];
}

// Scenario PDF Generation Request
export interface ScenarioPdfRequest {
  planData: FinancialPlan;
  filingStatus?: FilingStatus;
  rothConversionThreshold?: number;
  rateType?: string;
  rateValue?: number;
  scenarioStatesByYear?: StateByYear[];
  scenarioName?: string;
}
