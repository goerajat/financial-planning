package rg.financialplanning.calculator;

import rg.financialplanning.model.FilingStatus;
import rg.financialplanning.model.YearlySummary;

/**
 * Calculates federal income tax for tax year 2026.
 * Uses projected 2026 tax brackets based on inflation adjustments.
 */
public class FederalTaxCalculator implements TaxCalculator {

    /**
     * Percentage of Social Security benefits that is taxable for federal income tax purposes.
     * For higher earners, up to 85% of Social Security benefits may be taxable.
     */
    private static final double SOCIAL_SECURITY_TAXABLE_PERCENTAGE = 0.85;

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

    @Override
    public double calculateTax(YearlySummary summary, FilingStatus filingStatus) {
        if (summary == null) {
            throw new IllegalArgumentException("Summary cannot be null");
        }
        double ordinaryIncome = calculateOrdinaryIncome(summary);
        return calculateTax(ordinaryIncome, filingStatus);
    }

    @Override
    public double calculateOrdinaryIncome(YearlySummary summary) {
        if (summary == null) {
            return 0;
        }
        double income = summary.totalIncome();
        double rmdWithdrawals = summary.rmdWithdrawals();
        double qualifiedWithdrawals = summary.qualifiedWithdrawals();
        double taxableSocialSecurity = summary.totalSocialSecurity() * SOCIAL_SECURITY_TAXABLE_PERCENTAGE;
        return income + rmdWithdrawals + qualifiedWithdrawals + taxableSocialSecurity;
    }

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
        tax += (taxableIncome - previousBracket) * TAX_RATES[TAX_RATES.length - 1];
        return tax;
    }

    public double getEffectiveTaxRate(double taxableIncome, FilingStatus filingStatus) {
        if (taxableIncome <= 0) {
            return 0;
        }
        return calculateTax(taxableIncome, filingStatus) / taxableIncome;
    }

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
                return (postTaxAmount + cumulativeTax - previousBracket * TAX_RATES[i]) / (1 - TAX_RATES[i]);
            }
            cumulativeTax += bracketTax;
            previousBracket = brackets[i];
        }
        double topRate = TAX_RATES[TAX_RATES.length - 1];
        return (postTaxAmount + cumulativeTax - previousBracket * topRate) / (1 - topRate);
    }
}
