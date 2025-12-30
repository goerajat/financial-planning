package rg.financialplanning.calculator;

import rg.financialplanning.model.FilingStatus;

/**
 * Calculates New Jersey state income tax for tax year 2026.
 * Uses projected 2026 tax brackets based on current NJ tax structure.
 */
public class NJStateTaxCalculator {

    // 2026 projected tax brackets for Single/Married Filing Separately
    private static final double[] SINGLE_BRACKETS = {20_000, 35_000, 40_000, 75_000, 500_000, 1_000_000};
    private static final double[] SINGLE_RATES = {0.014, 0.0175, 0.035, 0.05525, 0.0637, 0.0897, 0.1075};

    // 2026 projected tax brackets for Married Filing Jointly/Head of Household
    private static final double[] MFJ_BRACKETS = {20_000, 50_000, 70_000, 80_000, 150_000, 500_000, 1_000_000};
    private static final double[] MFJ_RATES = {0.014, 0.0175, 0.0245, 0.035, 0.05525, 0.0637, 0.0897, 0.1075};

    /**
     * Calculates the New Jersey state income tax for the given taxable income and filing status.
     *
     * @param taxableIncome the NJ taxable income
     * @param filingStatus the filing status
     * @return the calculated NJ state income tax
     */
    public double calculateTax(double taxableIncome, FilingStatus filingStatus) {
        if (taxableIncome < 0) {
            throw new IllegalArgumentException("Taxable income cannot be negative");
        }
        if (filingStatus == null) {
            throw new IllegalArgumentException("Filing status cannot be null");
        }

        if (taxableIncome == 0) {
            return 0;
        }

        double[] brackets = getBrackets(filingStatus);
        double[] rates = getRates(filingStatus);
        return calculateTaxWithBrackets(taxableIncome, brackets, rates);
    }

    private double[] getBrackets(FilingStatus filingStatus) {
        return switch (filingStatus) {
            case SINGLE, MARRIED_FILING_SEPARATELY -> SINGLE_BRACKETS;
            case MARRIED_FILING_JOINTLY, HEAD_OF_HOUSEHOLD -> MFJ_BRACKETS;
        };
    }

    private double[] getRates(FilingStatus filingStatus) {
        return switch (filingStatus) {
            case SINGLE, MARRIED_FILING_SEPARATELY -> SINGLE_RATES;
            case MARRIED_FILING_JOINTLY, HEAD_OF_HOUSEHOLD -> MFJ_RATES;
        };
    }

    private double calculateTaxWithBrackets(double taxableIncome, double[] brackets, double[] rates) {
        double tax = 0;
        double previousBracket = 0;

        for (int i = 0; i < brackets.length; i++) {
            if (taxableIncome <= brackets[i]) {
                tax += (taxableIncome - previousBracket) * rates[i];
                return tax;
            }
            tax += (brackets[i] - previousBracket) * rates[i];
            previousBracket = brackets[i];
        }

        // Income exceeds highest bracket - apply top rate
        tax += (taxableIncome - previousBracket) * rates[rates.length - 1];
        return tax;
    }

    /**
     * Calculates the effective tax rate for the given taxable income and filing status.
     *
     * @param taxableIncome the NJ taxable income
     * @param filingStatus the filing status
     * @return the effective tax rate as a decimal
     */
    public double getEffectiveTaxRate(double taxableIncome, FilingStatus filingStatus) {
        if (taxableIncome <= 0) {
            return 0;
        }
        return calculateTax(taxableIncome, filingStatus) / taxableIncome;
    }

    /**
     * Returns the marginal tax rate for the given taxable income and filing status.
     *
     * @param taxableIncome the NJ taxable income
     * @param filingStatus the filing status
     * @return the marginal tax rate as a decimal
     */
    public double getMarginalTaxRate(double taxableIncome, FilingStatus filingStatus) {
        if (taxableIncome <= 0) {
            return getRates(filingStatus)[0];
        }

        double[] brackets = getBrackets(filingStatus);
        double[] rates = getRates(filingStatus);

        for (int i = 0; i < brackets.length; i++) {
            if (taxableIncome <= brackets[i]) {
                return rates[i];
            }
        }

        return rates[rates.length - 1];
    }

    /**
     * Calculates the pre-tax (gross) income required to achieve a given post-tax (net) amount.
     * This is the inverse of the tax calculation.
     *
     * @param postTaxAmount the desired after-tax amount
     * @param filingStatus the filing status
     * @return the pre-tax income needed to achieve the post-tax amount
     */
    public double calculatePreTaxAmount(double postTaxAmount, FilingStatus filingStatus) {
        if (postTaxAmount < 0) {
            throw new IllegalArgumentException("Post-tax amount cannot be negative");
        }
        if (filingStatus == null) {
            throw new IllegalArgumentException("Filing status cannot be null");
        }

        if (postTaxAmount == 0) {
            return 0;
        }

        double[] brackets = getBrackets(filingStatus);
        double[] rates = getRates(filingStatus);
        return calculatePreTaxWithBrackets(postTaxAmount, brackets, rates);
    }

    private double calculatePreTaxWithBrackets(double postTaxAmount, double[] brackets, double[] rates) {
        double cumulativeTax = 0;
        double previousBracket = 0;

        for (int i = 0; i < brackets.length; i++) {
            double bracketTax = (brackets[i] - previousBracket) * rates[i];
            double postTaxAtBracketEnd = brackets[i] - (cumulativeTax + bracketTax);

            if (postTaxAmount <= postTaxAtBracketEnd) {
                // Post-tax amount falls within this bracket
                return (postTaxAmount + cumulativeTax - previousBracket * rates[i]) / (1 - rates[i]);
            }

            cumulativeTax += bracketTax;
            previousBracket = brackets[i];
        }

        // Post-tax amount exceeds all brackets - use top rate
        double topRate = rates[rates.length - 1];
        return (postTaxAmount + cumulativeTax - previousBracket * topRate) / (1 - topRate);
    }
}
