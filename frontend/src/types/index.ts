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

export interface FinancialPlan {
  persons: Person[];
  entries: FinancialEntry[];
  rates: { [key: string]: number };
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
