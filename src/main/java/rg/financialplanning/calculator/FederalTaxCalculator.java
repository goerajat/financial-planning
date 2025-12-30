package rg.financialplanning.calculator;

import rg.financialplanning.model.FilingStatus;

/**
 * Calculates federal income tax for tax year 2026.
 * Uses projected 2026 tax brackets based on inflation adjustments.
 */
public class FederalTaxCalculator {

    // 2026 projected tax brackets for Single filers
    private static final double[] SINGLE_BRACKETS = {11_925, 48_475, 103_350, 197_300, 250_525, 626_350};

    // 2026 projected tax brackets for Married Filing Jointly
    private static final double[] MFJ_BRACKETS = {23_850, 96_950, 206_700, 394_600, 501_050, 751_600};

    // 2026 projected tax brackets for Married Filing Separately (same as Single)
    private static final double[] MFS_BRACKETS = {11_925, 48_475, 103_350, 197_300, 250_525, 375_800};

    // 2026 projected tax brackets for Head of Household
    private static final double[] HOH_BRACKETS = {17_000, 64_850, 103_350, 197_300, 250_500, 626_350};

    // Tax rates corresponding to brackets
    private static final double[] TAX_RATES = {0.10, 0.12, 0.22, 0.24, 0.32, 0.35, 0.37};

    /**
     * Calculates the federal income tax for the given taxable income and filing status.
     *
     * @param taxableIncome the federal taxable income
     * @param filingStatus the filing status
     * @return the calculated federal income tax
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
        return calculateTaxWithBrackets(taxableIncome, brackets);
    }

    private double[] getBrackets(FilingStatus filingStatus) {
        return switch (filingStatus) {
            case SINGLE -> SINGLE_BRACKETS;
            case MARRIED_FILING_JOINTLY -> MFJ_BRACKETS;
            case MARRIED_FILING_SEPARATELY -> MFS_BRACKETS;
            case HEAD_OF_HOUSEHOLD -> HOH_BRACKETS;
        };
    }

    private double calculateTaxWithBrackets(double taxableIncome, double[] brackets) {
        double tax = 0;
        double previousBracket = 0;

        for (int i = 0; i < brackets.length; i++) {
            if (taxableIncome <= brackets[i]) {
                tax += (taxableIncome - previousBracket) * TAX_RATES[i];
                return tax;
            }
            tax += (brackets[i] - previousBracket) * TAX_RATES[i];
            previousBracket = brackets[i];
        }

        // Income exceeds highest bracket - apply top rate
        tax += (taxableIncome - previousBracket) * TAX_RATES[TAX_RATES.length - 1];
        return tax;
    }

    /**
     * Calculates the effective tax rate for the given taxable income and filing status.
     *
     * @param taxableIncome the federal taxable income
     * @param filingStatus the filing status
     * @return the effective tax rate as a decimal (e.g., 0.22 for 22%)
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
     * @param taxableIncome the federal taxable income
     * @param filingStatus the filing status
     * @return the marginal tax rate as a decimal (e.g., 0.22 for 22%)
     */
    public double getMarginalTaxRate(double taxableIncome, FilingStatus filingStatus) {
        if (taxableIncome <= 0) {
            return TAX_RATES[0];
        }

        double[] brackets = getBrackets(filingStatus);

        for (int i = 0; i < brackets.length; i++) {
            if (taxableIncome <= brackets[i]) {
                return TAX_RATES[i];
            }
        }

        return TAX_RATES[TAX_RATES.length - 1];
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
        return calculatePreTaxWithBrackets(postTaxAmount, brackets);
    }

    private double calculatePreTaxWithBrackets(double postTaxAmount, double[] brackets) {
        double cumulativeTax = 0;
        double previousBracket = 0;

        for (int i = 0; i < brackets.length; i++) {
            double bracketTax = (brackets[i] - previousBracket) * TAX_RATES[i];
            double postTaxAtBracketEnd = brackets[i] - (cumulativeTax + bracketTax);

            if (postTaxAmount <= postTaxAtBracketEnd) {
                // Post-tax amount falls within this bracket
                // Solve: postTax = preTax - cumulativeTax - (preTax - previousBracket) * rate
                // postTax = preTax * (1 - rate) - cumulativeTax + previousBracket * rate
                // preTax = (postTax + cumulativeTax - previousBracket * rate) / (1 - rate)
                return (postTaxAmount + cumulativeTax - previousBracket * TAX_RATES[i]) / (1 - TAX_RATES[i]);
            }

            cumulativeTax += bracketTax;
            previousBracket = brackets[i];
        }

        // Post-tax amount exceeds all brackets - use top rate
        double topRate = TAX_RATES[TAX_RATES.length - 1];
        return (postTaxAmount + cumulativeTax - previousBracket * topRate) / (1 - topRate);
    }
}
