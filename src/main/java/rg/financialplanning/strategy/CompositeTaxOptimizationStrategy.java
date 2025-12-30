package rg.financialplanning.strategy;

import rg.financialplanning.model.FilingStatus;
import rg.financialplanning.model.YearlySummary;

/**
 * Composite tax optimization strategy that applies multiple strategies in sequence.
 *
 * This strategy applies the following optimizations in order:
 * 1. RMD Optimization - Calculates and applies Required Minimum Distributions
 * 2. Expense Management - Manages surplus/deficit after taxes and expenses
 * 3. Roth Conversion - Performs strategic Roth conversions to fill tax brackets
 * 4. Tax Calculation - Calculates and stores all taxes in YearlySummary
 *
 * The order is important because:
 * - RMDs must be calculated first as they are mandatory and affect taxable income
 * - Expense management determines if there's surplus to invest or deficit requiring withdrawals
 * - Roth conversions are performed last to optimize remaining tax bracket room
 * - Tax calculation runs last to capture the final tax picture after all optimizations
 */
public class CompositeTaxOptimizationStrategy implements TaxOptimizationStrategy {

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
        this.expenseStrategy = new ExpenseManagementStrategy(filingStatus);
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
        this.expenseStrategy = new ExpenseManagementStrategy(filingStatus);
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
        this.taxCalculationStrategy = new TaxCalculationStrategy(expenseStrategy.getFilingStatus());
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

        // Step 2: Apply expense management (surplus/deficit handling)
        expenseStrategy.optimize(previousYearlySummary, currentYearlySummary);

        // Step 3: Apply Roth conversion optimization (tax bracket filling)
        rothConversionStrategy.optimize(previousYearlySummary, currentYearlySummary);

        // Step 4: Calculate and store all taxes (must be last to capture final state)
        taxCalculationStrategy.optimize(previousYearlySummary, currentYearlySummary);
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
        return "Applies multiple tax optimization strategies in sequence: " +
                "(1) RMD Optimization - calculates and applies Required Minimum Distributions, " +
                "(2) Expense Management - handles surplus by investing in non-qualified assets " +
                "or deficit by withdrawing from non-qualified assets, " +
                "(3) Roth Conversion - performs strategic conversions to fill lower tax brackets, " +
                "(4) Tax Calculation - calculates and stores all taxes in YearlySummary.";
    }
}
