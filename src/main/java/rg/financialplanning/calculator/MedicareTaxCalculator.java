package rg.financialplanning.calculator;

import rg.financialplanning.model.FilingStatus;
import rg.financialplanning.model.IndividualYearlySummary;
import rg.financialplanning.model.Person;
import rg.financialplanning.model.YearlySummary;

/**
 * Calculates Medicare taxes (HI - Hospital Insurance).
 *
 * Medicare Tax:
 * - Employee rate: 1.45% of all wages (no wage base limit)
 * - Employer rate: 1.45% (matched)
 * - Self-employed rate: 2.9% (both portions)
 *
 * Additional Medicare Tax:
 * - Rate: 0.9% on wages exceeding the threshold
 * - Thresholds: $200,000 (Single/Head of Household), $250,000 (Married Filing Jointly),
 *   $125,000 (Married Filing Separately)
 * - Only paid by employee (not matched by employer)
 */
public class MedicareTaxCalculator implements TaxCalculator {

    /**
     * Medicare tax rate for employees (1.45%).
     * Employers pay an additional matching 1.45%.
     */
    private static final double MEDICARE_RATE_EMPLOYEE = 0.0145;

    /**
     * Medicare tax rate for self-employed individuals (2.9%).
     * This combines both the employee and employer portions.
     */
    private static final double MEDICARE_RATE_SELF_EMPLOYED = 0.029;

    /**
     * Additional Medicare Tax rate (0.9%).
     * Applied to wages exceeding the threshold based on filing status.
     * This is only paid by employees, not matched by employers.
     */
    private static final double ADDITIONAL_MEDICARE_RATE = 0.009;

    /**
     * Additional Medicare Tax threshold for Single and Head of Household filers ($200,000).
     */
    private static final double ADDITIONAL_MEDICARE_THRESHOLD_SINGLE = 200_000.0;

    /**
     * Additional Medicare Tax threshold for Married Filing Jointly ($250,000).
     */
    private static final double ADDITIONAL_MEDICARE_THRESHOLD_MFJ = 250_000.0;

    /**
     * Additional Medicare Tax threshold for Married Filing Separately ($125,000).
     */
    private static final double ADDITIONAL_MEDICARE_THRESHOLD_MFS = 125_000.0;

    /**
     * Calculates total Medicare tax for all individuals in the yearly summary.
     * Uses each individual's isSelfEmployed status from their Person record.
     *
     * @param summary the yearly financial summary containing individual summaries
     * @param filingStatus the filing status for Additional Medicare Tax threshold
     * @return the total Medicare tax for all individuals
     */
    @Override
    public double calculateTax(YearlySummary summary, FilingStatus filingStatus) {
        if (summary == null) {
            return 0.0;
        }

        double totalMedicareTax = 0.0;
        double totalEarnedIncome = 0.0;

        // Calculate Medicare tax for each individual based on their self-employment status
        for (IndividualYearlySummary individual : summary.individualSummaries().values()) {
            double income = individual.income();
            boolean isSelfEmployed = isSelfEmployed(individual);

            totalMedicareTax += calculateMedicareTax(income, isSelfEmployed);
            totalEarnedIncome += income;
        }

        // Additional Medicare Tax is calculated on combined earned income
        totalMedicareTax += calculateAdditionalMedicareTax(totalEarnedIncome, filingStatus);

        return totalMedicareTax;
    }

    /**
     * Calculates the ordinary income from a yearly summary.
     * For Medicare tax, this is the total earned income (wages/self-employment income).
     *
     * @param summary the yearly financial summary
     * @return the total earned income
     */
    @Override
    public double calculateOrdinaryIncome(YearlySummary summary) {
        if (summary == null) {
            return 0.0;
        }
        return summary.totalIncome();
    }

    /**
     * Calculates Medicare tax (excluding additional Medicare tax).
     *
     * @param wages the total wages or self-employment income
     * @param isSelfEmployed true if calculating for self-employed individual
     * @return the Medicare tax amount
     */
    public double calculateMedicareTax(double wages, boolean isSelfEmployed) {
        if (wages < 0) {
            throw new IllegalArgumentException("Wages cannot be negative");
        }

        double rate = isSelfEmployed ? MEDICARE_RATE_SELF_EMPLOYED : MEDICARE_RATE_EMPLOYEE;
        return wages * rate;
    }

    /**
     * Calculates Medicare tax for an employee (non-self-employed).
     *
     * @param wages the total wages
     * @return the Medicare tax amount
     */
    public double calculateMedicareTax(double wages) {
        return calculateMedicareTax(wages, false);
    }

    /**
     * Calculates the Additional Medicare Tax for high earners.
     *
     * @param wages the total wages
     * @param filingStatus the filing status
     * @return the Additional Medicare Tax amount
     */
    public double calculateAdditionalMedicareTax(double wages, FilingStatus filingStatus) {
        if (wages < 0) {
            throw new IllegalArgumentException("Wages cannot be negative");
        }
        if (filingStatus == null) {
            throw new IllegalArgumentException("Filing status cannot be null");
        }

        double threshold = getAdditionalMedicareThreshold(filingStatus);
        if (wages <= threshold) {
            return 0;
        }

        return (wages - threshold) * ADDITIONAL_MEDICARE_RATE;
    }

    /**
     * Gets the Additional Medicare Tax threshold based on filing status.
     *
     * @param filingStatus the filing status
     * @return the threshold amount
     */
    public double getAdditionalMedicareThreshold(FilingStatus filingStatus) {
        return switch (filingStatus) {
            case SINGLE, HEAD_OF_HOUSEHOLD -> ADDITIONAL_MEDICARE_THRESHOLD_SINGLE;
            case MARRIED_FILING_JOINTLY -> ADDITIONAL_MEDICARE_THRESHOLD_MFJ;
            case MARRIED_FILING_SEPARATELY -> ADDITIONAL_MEDICARE_THRESHOLD_MFS;
        };
    }

    public double getMedicareRateEmployee() {
        return MEDICARE_RATE_EMPLOYEE;
    }

    public double getMedicareRateSelfEmployed() {
        return MEDICARE_RATE_SELF_EMPLOYED;
    }

    public double getAdditionalMedicareRate() {
        return ADDITIONAL_MEDICARE_RATE;
    }

    /**
     * Determines if an individual is self-employed based on their Person record.
     *
     * @param individual the individual yearly summary
     * @return true if self-employed, false otherwise
     */
    private boolean isSelfEmployed(IndividualYearlySummary individual) {
        Person person = individual.person();
        return person != null && person.isSelfEmployed();
    }
}
