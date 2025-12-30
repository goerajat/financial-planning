package rg.financialplanning.calculator;

import rg.financialplanning.model.FilingStatus;

/**
 * Calculates Social Security and Medicare taxes (FICA taxes).
 *
 * Social Security Tax (OASDI - Old Age, Survivors, and Disability Insurance):
 * - Employee rate: 6.2% of wages up to the wage base limit
 * - Employer rate: 6.2% (matched)
 * - Self-employed rate: 12.4% (both portions)
 * - Wage base limit for 2024: $168,600 - only wages up to this amount are subject to Social Security tax
 *
 * Medicare Tax (HI - Hospital Insurance):
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
public class SocialSecurityTaxCalculator {

    /**
     * Social Security tax rate for employees (6.2%).
     * Employers pay an additional matching 6.2%.
     */
    private static final double SOCIAL_SECURITY_RATE_EMPLOYEE = 0.062;

    /**
     * Social Security tax rate for self-employed individuals (12.4%).
     * This combines both the employee and employer portions.
     */
    private static final double SOCIAL_SECURITY_RATE_SELF_EMPLOYED = 0.124;

    /**
     * Social Security wage base limit for 2024 ($168,600).
     * Only wages up to this amount are subject to Social Security tax.
     * This limit is adjusted annually for inflation.
     */
    private static final double SOCIAL_SECURITY_WAGE_BASE_2024 = 168_600.0;

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

    private final double socialSecurityWageBase;

    public SocialSecurityTaxCalculator() {
        this(SOCIAL_SECURITY_WAGE_BASE_2024);
    }

    public SocialSecurityTaxCalculator(double socialSecurityWageBase) {
        if (socialSecurityWageBase < 0) {
            throw new IllegalArgumentException("Social Security wage base cannot be negative");
        }
        this.socialSecurityWageBase = socialSecurityWageBase;
    }

    /**
     * Calculates the employee's Social Security tax.
     *
     * @param wages the total wages
     * @return the Social Security tax amount
     */
    public double calculateSocialSecurityTax(double wages) {
        return calculateSocialSecurityTax(wages, false);
    }

    /**
     * Calculates Social Security tax.
     *
     * @param wages the total wages or self-employment income
     * @param isSelfEmployed true if calculating for self-employed individual
     * @return the Social Security tax amount
     */
    public double calculateSocialSecurityTax(double wages, boolean isSelfEmployed) {
        if (wages < 0) {
            throw new IllegalArgumentException("Wages cannot be negative");
        }

        double taxableWages = Math.min(wages, socialSecurityWageBase);
        double rate = isSelfEmployed ? SOCIAL_SECURITY_RATE_SELF_EMPLOYED : SOCIAL_SECURITY_RATE_EMPLOYEE;
        return taxableWages * rate;
    }

    /**
     * Calculates the employee's Medicare tax (excluding additional Medicare tax).
     *
     * @param wages the total wages
     * @return the Medicare tax amount
     */
    public double calculateMedicareTax(double wages) {
        return calculateMedicareTax(wages, false);
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

    public double getSocialSecurityWageBase() {
        return socialSecurityWageBase;
    }

    public double getSocialSecurityRateEmployee() {
        return SOCIAL_SECURITY_RATE_EMPLOYEE;
    }

    public double getSocialSecurityRateSelfEmployed() {
        return SOCIAL_SECURITY_RATE_SELF_EMPLOYED;
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
}
