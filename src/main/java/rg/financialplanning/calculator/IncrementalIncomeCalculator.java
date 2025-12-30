package rg.financialplanning.calculator;

import rg.financialplanning.model.FilingStatus;

/**
 * Calculates the incremental gross income required to meet a target expense amount
 * after accounting for federal and state income taxes.
 *
 * This calculator solves the "gross-up" problem: given a net amount you need (expenses),
 * how much additional gross income do you need to earn, considering that a portion
 * of that income will go to federal and state taxes?
 *
 * The calculation accounts for marginal tax rates at the given base income level,
 * using an iterative approach to handle cases where the additional income might
 * push the taxpayer into higher tax brackets.
 */
public class IncrementalIncomeCalculator {

    private final FederalTaxCalculator federalTaxCalculator;
    private final NJStateTaxCalculator stateTaxCalculator;

    public IncrementalIncomeCalculator() {
        this.federalTaxCalculator = new FederalTaxCalculator();
        this.stateTaxCalculator = new NJStateTaxCalculator();
    }

    /**
     * Calculates the incremental gross income required to cover expenses after taxes.
     *
     * Uses an iterative approach to accurately account for progressive tax brackets.
     * The method finds the amount X such that:
     * X - incrementalFederalTax(X) - incrementalStateTax(X) = expenses
     *
     * @param baseIncome the current/base taxable income before additional income
     * @param expenses the target expense amount that needs to be covered after taxes
     * @param filingStatus the filing status for tax calculations
     * @return the incremental gross income required
     */
    public double calculateIncrementalIncomeRequired(double baseIncome, double expenses, FilingStatus filingStatus) {
        if (baseIncome < 0) {
            throw new IllegalArgumentException("Base income cannot be negative");
        }
        if (expenses < 0) {
            throw new IllegalArgumentException("Expenses cannot be negative");
        }
        if (filingStatus == null) {
            throw new IllegalArgumentException("Filing status cannot be null");
        }

        if (expenses == 0) {
            return 0;
        }

        // Calculate taxes on base income
        double baseFederalTax = federalTaxCalculator.calculateTax(baseIncome, filingStatus);
        double baseStateTax = stateTaxCalculator.calculateTax(baseIncome, filingStatus);

        // Use iterative approach to find the correct incremental income
        // Start with an estimate using marginal rates
        double federalMarginalRate = federalTaxCalculator.getMarginalTaxRate(baseIncome, filingStatus);
        double stateMarginalRate = stateTaxCalculator.getMarginalTaxRate(baseIncome, filingStatus);
        double combinedMarginalRate = federalMarginalRate + stateMarginalRate;

        // Initial estimate
        double incrementalIncome = expenses / (1 - combinedMarginalRate);

        // Iterate to refine the estimate (handles bracket changes)
        for (int i = 0; i < 10; i++) {
            double totalIncome = baseIncome + incrementalIncome;

            // Calculate taxes on total income
            double totalFederalTax = federalTaxCalculator.calculateTax(totalIncome, filingStatus);
            double totalStateTax = stateTaxCalculator.calculateTax(totalIncome, filingStatus);

            // Calculate incremental taxes
            double incrementalFederalTax = totalFederalTax - baseFederalTax;
            double incrementalStateTax = totalStateTax - baseStateTax;

            // Calculate net income from incremental gross
            double netIncremental = incrementalIncome - incrementalFederalTax - incrementalStateTax;

            // Check if we've converged (within $0.01)
            if (Math.abs(netIncremental - expenses) < 0.01) {
                break;
            }

            // Adjust estimate based on the difference
            double shortfall = expenses - netIncremental;
            double currentEffectiveRate = (incrementalFederalTax + incrementalStateTax) / incrementalIncome;
            incrementalIncome += shortfall / (1 - currentEffectiveRate);
        }

        return incrementalIncome;
    }

    /**
     * Calculates the incremental taxes (federal + state) on additional income.
     *
     * @param baseIncome the current/base taxable income
     * @param additionalIncome the additional income amount
     * @param filingStatus the filing status
     * @return the total incremental tax (federal + state) on the additional income
     */
    public double calculateIncrementalTax(double baseIncome, double additionalIncome, FilingStatus filingStatus) {
        if (baseIncome < 0 || additionalIncome < 0) {
            throw new IllegalArgumentException("Income amounts cannot be negative");
        }
        if (filingStatus == null) {
            throw new IllegalArgumentException("Filing status cannot be null");
        }

        double baseFederalTax = federalTaxCalculator.calculateTax(baseIncome, filingStatus);
        double baseStateTax = stateTaxCalculator.calculateTax(baseIncome, filingStatus);

        double totalIncome = baseIncome + additionalIncome;
        double totalFederalTax = federalTaxCalculator.calculateTax(totalIncome, filingStatus);
        double totalStateTax = stateTaxCalculator.calculateTax(totalIncome, filingStatus);

        return (totalFederalTax - baseFederalTax) + (totalStateTax - baseStateTax);
    }

    /**
     * Calculates the combined marginal tax rate (federal + state) at the given income level.
     *
     * @param income the taxable income
     * @param filingStatus the filing status
     * @return the combined marginal tax rate as a decimal
     */
    public double getCombinedMarginalRate(double income, FilingStatus filingStatus) {
        double federalRate = federalTaxCalculator.getMarginalTaxRate(income, filingStatus);
        double stateRate = stateTaxCalculator.getMarginalTaxRate(income, filingStatus);
        return federalRate + stateRate;
    }

    /**
     * Calculates the net amount remaining after federal and state taxes on additional income.
     *
     * @param baseIncome the current/base taxable income
     * @param additionalIncome the additional income amount
     * @param filingStatus the filing status
     * @return the net amount after taxes
     */
    public double calculateNetAfterTax(double baseIncome, double additionalIncome, FilingStatus filingStatus) {
        double incrementalTax = calculateIncrementalTax(baseIncome, additionalIncome, filingStatus);
        return additionalIncome - incrementalTax;
    }
}
