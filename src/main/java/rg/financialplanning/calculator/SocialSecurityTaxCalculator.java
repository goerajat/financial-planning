package rg.financialplanning.calculator;

import rg.financialplanning.model.FilingStatus;
import rg.financialplanning.model.IndividualYearlySummary;
import rg.financialplanning.model.Person;
import rg.financialplanning.model.YearlySummary;

/**
 * Calculates Social Security taxes (OASDI - Old Age, Survivors, and Disability Insurance).
 *
 * Social Security Tax:
 * - Employee rate: 6.2% of wages up to the wage base limit
 * - Employer rate: 6.2% (matched)
 * - Self-employed rate: 12.4% (both portions)
 * - Wage base limit for 2024: $168,600 - only wages up to this amount are subject to Social Security tax
 */
public class SocialSecurityTaxCalculator implements TaxCalculator {

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
     * Calculates total Social Security tax for all individuals in the yearly summary.
     * Uses each individual's isSelfEmployed status from their Person record.
     *
     * @param summary the yearly financial summary containing individual summaries
     * @param filingStatus the filing status (not used for SS tax, but required by interface)
     * @return the total Social Security tax for all individuals
     */
    @Override
    public double calculateTax(YearlySummary summary, FilingStatus filingStatus) {
        if (summary == null) {
            return 0.0;
        }

        double totalSocialSecurityTax = 0.0;

        // Calculate Social Security tax for each individual based on their self-employment status
        for (IndividualYearlySummary individual : summary.individualSummaries().values()) {
            double income = individual.income();
            boolean isSelfEmployed = isSelfEmployed(individual);

            totalSocialSecurityTax += calculateSocialSecurityTax(income, isSelfEmployed);
        }

        return totalSocialSecurityTax;
    }

    /**
     * Calculates the ordinary income from a yearly summary.
     * For Social Security tax, this is the total earned income (wages/self-employment income),
     * capped at the wage base.
     *
     * @param summary the yearly financial summary
     * @return the total earned income subject to Social Security tax
     */
    @Override
    public double calculateOrdinaryIncome(YearlySummary summary) {
        if (summary == null) {
            return 0.0;
        }
        return Math.min(summary.totalIncome(), socialSecurityWageBase);
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

    public double getSocialSecurityWageBase() {
        return socialSecurityWageBase;
    }

    public double getSocialSecurityRateEmployee() {
        return SOCIAL_SECURITY_RATE_EMPLOYEE;
    }

    public double getSocialSecurityRateSelfEmployed() {
        return SOCIAL_SECURITY_RATE_SELF_EMPLOYED;
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
