package rg.financialplanning.strategy;

import rg.financialplanning.model.FilingStatus;
import rg.financialplanning.model.YearlySummary;

/**
 * Composite tax optimization strategy that applies multiple strategies in sequence.
 *
 * This strategy applies the following optimization flow:
 *
 * 1. RMD Optimization - Calculates and applies Required Minimum Distributions
 *
 * 2. Tax-Expense Loop (Pre-Roth) - Iterates until cash flow is balanced:
 *    a. Tax Calculation - Calculate all taxes based on current state
 *    b. Expense Management - Handle surplus/deficit based on cash inflows - outflows
 *    (Loop continues while no deficit AND cash flow is not balanced)
 *
 * 3. Roth Conversion - Performs strategic Roth conversions to fill tax brackets
 *    (Only if no deficit exists)
 *
 * 4. Tax-Expense Loop (Post-Roth) - Re-balance after Roth conversion:
 *    a. Tax Calculation - Recalculate taxes including Roth conversion impact
 *    b. Expense Management - Handle any surplus/deficit from conversion taxes
 *    (Loop continues while no deficit AND cash flow is not balanced)
 *
 * The iterative approach ensures:
 * - Taxes are calculated before expense management can determine true surplus/deficit
 * - Withdrawals made to cover deficits trigger tax recalculation
 * - The system converges to a balanced state where cash inflows = outflows
 */
public class CompositeTaxOptimizationStrategy implements TaxOptimizationStrategy {

    /**
     * Threshold for considering cash flow "close to zero" (balanced).
     * Values within this range are considered balanced.
     */
    private static final double CASH_FLOW_BALANCE_THRESHOLD = 1.0;

    /**
     * Maximum iterations to prevent infinite loops.
     */
    private static final int MAX_ITERATIONS = 20;

    private final RMDOptimizationStrategy rmdStrategy;
    private final ExpenseManagementStrategy expenseStrategy;
    private final RothConversionOptimizationStrategy rothConversionStrategy;
    private final TaxCalculationStrategy taxCalculationStrategy;

    /**
     * Creates a composite strategy with default settings.
     * Uses Married Filing Jointly and default tax bracket thresholds.
     */
    public CompositeTaxOptimizationStrategy() {
        this(FilingStatus.MARRIED_FILING_JOINTLY);
    }

    /**
     * Creates a composite strategy with the specified filing status.
     *
     * @param filingStatus the filing status for tax calculations
     */
    public CompositeTaxOptimizationStrategy(FilingStatus filingStatus) {
        this.rmdStrategy = new RMDOptimizationStrategy();
        this.expenseStrategy = new ExpenseManagementStrategy();
        this.rothConversionStrategy = new RothConversionOptimizationStrategy(filingStatus);
        this.taxCalculationStrategy = new TaxCalculationStrategy(filingStatus);
    }

    /**
     * Creates a composite strategy with custom settings.
     *
     * @param filingStatus the filing status for tax calculations
     * @param rothConversionTargetBracket the income threshold for Roth conversions
     */
    public CompositeTaxOptimizationStrategy(FilingStatus filingStatus, double rothConversionTargetBracket) {
        this.rmdStrategy = new RMDOptimizationStrategy();
        this.expenseStrategy = new ExpenseManagementStrategy();
        this.rothConversionStrategy = new RothConversionOptimizationStrategy(filingStatus, rothConversionTargetBracket);
        this.taxCalculationStrategy = new TaxCalculationStrategy(filingStatus);
    }

    /**
     * Creates a composite strategy with fully custom component strategies.
     *
     * @param rmdStrategy the RMD optimization strategy
     * @param expenseStrategy the expense management strategy
     * @param rothConversionStrategy the Roth conversion strategy
     */
    public CompositeTaxOptimizationStrategy(RMDOptimizationStrategy rmdStrategy,
                                            ExpenseManagementStrategy expenseStrategy,
                                            RothConversionOptimizationStrategy rothConversionStrategy) {
        this.rmdStrategy = rmdStrategy;
        this.expenseStrategy = expenseStrategy;
        this.rothConversionStrategy = rothConversionStrategy;
        this.taxCalculationStrategy = new TaxCalculationStrategy(rothConversionStrategy.getFilingStatus());
    }

    /**
     * Creates a composite strategy with fully custom component strategies including tax calculation.
     *
     * @param rmdStrategy the RMD optimization strategy
     * @param expenseStrategy the expense management strategy
     * @param rothConversionStrategy the Roth conversion strategy
     * @param taxCalculationStrategy the tax calculation strategy
     */
    public CompositeTaxOptimizationStrategy(RMDOptimizationStrategy rmdStrategy,
                                            ExpenseManagementStrategy expenseStrategy,
                                            RothConversionOptimizationStrategy rothConversionStrategy,
                                            TaxCalculationStrategy taxCalculationStrategy) {
        this.rmdStrategy = rmdStrategy;
        this.expenseStrategy = expenseStrategy;
        this.rothConversionStrategy = rothConversionStrategy;
        this.taxCalculationStrategy = taxCalculationStrategy;
    }

    @Override
    public void optimize(YearlySummary previousYearlySummary, YearlySummary currentYearlySummary) {
        if (currentYearlySummary == null) {
            return;
        }

        // Step 1: Apply RMD optimization (mandatory distributions)
        rmdStrategy.optimize(previousYearlySummary, currentYearlySummary);

        // Step 2: Tax-Expense Loop (Pre-Roth)
        // Loop while no deficit AND cash flow is not balanced
        runTaxExpenseLoop(previousYearlySummary, currentYearlySummary);

        // Step 3: Apply Roth conversion optimization (only if no deficit)
        if (!hasDeficit(currentYearlySummary)) {
            rothConversionStrategy.optimize(previousYearlySummary, currentYearlySummary);
        }

        // Step 4: Tax-Expense Loop (Post-Roth)
        // Re-balance after Roth conversion
        runTaxExpenseLoop(previousYearlySummary, currentYearlySummary);
    }

    /**
     * Runs the Tax Calculation → Expense Management loop until:
     * - A deficit occurs, OR
     * - Cash flow is balanced (inflows - outflows close to 0), OR
     * - Maximum iterations reached
     *
     * @param previousYearlySummary the previous year's summary
     * @param currentYearlySummary the current year's summary to optimize
     */
    private void runTaxExpenseLoop(YearlySummary previousYearlySummary, YearlySummary currentYearlySummary) {
        for (int iteration = 0; iteration < MAX_ITERATIONS; iteration++) {
            // Calculate taxes based on current state
            taxCalculationStrategy.optimize(previousYearlySummary, currentYearlySummary);

            // Check if we should continue looping
            if (hasDeficit(currentYearlySummary)) {
                // Deficit exists - run expense management one more time and exit
                expenseStrategy.optimize(previousYearlySummary, currentYearlySummary);
                break;
            }

            if (isCashFlowBalanced(currentYearlySummary)) {
                // Cash flow is balanced - we're done
                break;
            }

            // Apply expense management to handle surplus/deficit
            expenseStrategy.optimize(previousYearlySummary, currentYearlySummary);

            // Check again after expense management
            if (hasDeficit(currentYearlySummary) || isCashFlowBalanced(currentYearlySummary)) {
                break;
            }
        }
    }

    /**
     * Checks if the yearly summary has a deficit.
     *
     * @param summary the yearly summary
     * @return true if there is a deficit
     */
    private boolean hasDeficit(YearlySummary summary) {
        return summary.deficit() > 0;
    }

    /**
     * Checks if cash flow is balanced (inflows - outflows close to zero).
     *
     * @param summary the yearly summary
     * @return true if cash flow is balanced
     */
    private boolean isCashFlowBalanced(YearlySummary summary) {
        double cashFlowDifference = summary.totalCashInflows() - summary.totalCashOutflows();
        return Math.abs(cashFlowDifference) < CASH_FLOW_BALANCE_THRESHOLD;
    }

    public RMDOptimizationStrategy getRmdStrategy() {
        return rmdStrategy;
    }

    public ExpenseManagementStrategy getExpenseStrategy() {
        return expenseStrategy;
    }

    public RothConversionOptimizationStrategy getRothConversionStrategy() {
        return rothConversionStrategy;
    }

    public TaxCalculationStrategy getTaxCalculationStrategy() {
        return taxCalculationStrategy;
    }

    @Override
    public String getStrategyName() {
        return "Composite Tax Optimization";
    }

    @Override
    public String getDescription() {
        return "Applies multiple tax optimization strategies in an iterative sequence: " +
                "(1) RMD Optimization - calculates and applies Required Minimum Distributions, " +
                "(2) Tax-Expense Loop - iterates Tax Calculation and Expense Management until balanced, " +
                "(3) Roth Conversion - performs strategic conversions to fill lower tax brackets (if no deficit), " +
                "(4) Tax-Expense Loop - re-balances after Roth conversion.";
    }
}
