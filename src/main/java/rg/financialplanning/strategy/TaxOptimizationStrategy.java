package rg.financialplanning.strategy;

import rg.financialplanning.model.YearlySummary;

/**
 * Interface for tax optimization strategies.
 *
 * Implementations of this interface provide different approaches to optimizing
 * tax liability across years, such as:
 * - Roth conversion strategies
 * - Tax-loss harvesting
 * - Income smoothing across years
 * - Optimal withdrawal sequencing from different account types
 * - RMD optimization
 *
 * Each strategy can analyze the previous and current year summaries to make
 * recommendations or modifications that minimize overall tax burden.
 */
public interface TaxOptimizationStrategy {

    /**
     * Applies the tax optimization strategy to the current year summary.
     *
     * The implementation may modify the currentYearlySummary to reflect
     * optimized withdrawals, conversions, or other tax-related decisions.
     *
     * @param previousYearlySummary the financial summary from the previous year,
     *                               may be null for the first year
     * @param currentYearlySummary the financial summary for the current year to optimize
     */
    void optimize(YearlySummary previousYearlySummary, YearlySummary currentYearlySummary);

    /**
     * Returns the name of this optimization strategy.
     *
     * @return the strategy name
     */
    String getStrategyName();

    /**
     * Returns a description of what this optimization strategy does.
     *
     * @return the strategy description
     */
    String getDescription();
}
