package rg.financialplanning.calculator;

import rg.financialplanning.model.FilingStatus;
import rg.financialplanning.model.YearlySummary;

/**
 * Calculates New York state income tax for tax year 2026.
 * Uses projected 2026 tax brackets based on current NY tax structure.
 */
public class NYStateTaxCalculator implements TaxCalculator {

    // 2026 projected tax brackets for Single filers
    private static final double[] SINGLE_BRACKETS = {8_500, 11_700, 13_900, 80_650, 215_400, 1_077_550, 5_000_000, 25_000_000};
    private static final double[] SINGLE_RATES = {0.04, 0.045, 0.0525, 0.055, 0.06, 0.0685, 0.0965, 0.103, 0.109};

    // 2026 projected tax brackets for Married Filing Jointly
    private static final double[] MFJ_BRACKETS = {17_150, 23_600, 27_900, 161_550, 323_200, 2_155_350, 5_000_000, 25_000_000};
    private static final double[] MFJ_RATES = {0.04, 0.045, 0.0525, 0.055, 0.06, 0.0685, 0.0965, 0.103, 0.109};

    // 2026 projected tax brackets for Married Filing Separately (half of MFJ)
    private static final double[] MFS_BRACKETS = {8_500, 11_700, 13_900, 80_650, 161_550, 1_077_550, 2_500_000, 12_500_000};
    private static final double[] MFS_RATES = {0.04, 0.045, 0.0525, 0.055, 0.06, 0.0685, 0.0965, 0.103, 0.109};

    // 2026 projected tax brackets for Head of Household
    private static final double[] HOH_BRACKETS = {12_800, 17_650, 20_900, 107_650, 269_300, 1_616_450, 5_000_000, 25_000_000};
    private static final double[] HOH_RATES = {0.04, 0.045, 0.0525, 0.055, 0.06, 0.0685, 0.0965, 0.103, 0.109};

    @Override
    public double calculateTax(YearlySummary summary, FilingStatus filingStatus) {
        if (summary == null) {
            throw new IllegalArgumentException("Summary cannot be null");
        }
        double ordinaryIncome = calculateOrdinaryIncome(summary);
        return calculateTax(ordinaryIncome, filingStatus);
    }

    /**
     * Calculates the NY ordinary/taxable income from a yearly summary.
     * NY taxable income includes:
     * - Total income
     * - RMD withdrawals (fully taxable)
     * - Qualified withdrawals (fully taxable)
     * Note: Social Security benefits are NOT taxable in New York.
     *
     * @param summary the yearly financial summary
     * @return the calculated NY taxable income
     */
    @Override
    public double calculateOrdinaryIncome(YearlySummary summary) {
        if (summary == null) {
            return 0;
        }
        double income = summary.totalIncome();
        double rmdWithdrawals = summary.rmdWithdrawals();
        double qualifiedWithdrawals = summary.qualifiedWithdrawals();
        // Social Security benefits are NOT taxable in NY
        return income + rmdWithdrawals + qualifiedWithdrawals;
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
        double[] rates = getRates(filingStatus);
        return calculateTaxWithBrackets(taxableIncome, brackets, rates);
    }

    private double[] getBrackets(FilingStatus filingStatus) {
        return switch (filingStatus) {
            case SINGLE -> SINGLE_BRACKETS;
            case MARRIED_FILING_JOINTLY -> MFJ_BRACKETS;
            case MARRIED_FILING_SEPARATELY -> MFS_BRACKETS;
            case HEAD_OF_HOUSEHOLD -> HOH_BRACKETS;
        };
    }

    private double[] getRates(FilingStatus filingStatus) {
        return switch (filingStatus) {
            case SINGLE -> SINGLE_RATES;
            case MARRIED_FILING_JOINTLY -> MFJ_RATES;
            case MARRIED_FILING_SEPARATELY -> MFS_RATES;
            case HEAD_OF_HOUSEHOLD -> HOH_RATES;
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
        tax += (taxableIncome - previousBracket) * rates[rates.length - 1];
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
}
