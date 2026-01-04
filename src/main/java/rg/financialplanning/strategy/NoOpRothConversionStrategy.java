package rg.financialplanning.strategy;

import rg.financialplanning.model.FilingStatus;
import rg.financialplanning.model.YearlySummary;

/**
 * A no-operation Roth conversion strategy that performs no conversions.
 * Used as a baseline for comparing different Roth conversion strategies.
 */
public class NoOpRothConversionStrategy extends RothConversionOptimizationStrategy {

    /**
     * Creates a no-op Roth conversion strategy.
     *
     * @param filingStatus the filing status (required for parent class)
     */
    public NoOpRothConversionStrategy(FilingStatus filingStatus) {
        super(filingStatus, 0); // Zero threshold means no conversions
    }

    @Override
    public void optimize(YearlySummary previousYearlySummary, YearlySummary currentYearlySummary) {
        // Do nothing - no Roth conversions
    }

    @Override
    public String getStrategyName() {
        return "No Roth Conversion";
    }

    @Override
    public String getDescription() {
        return "Baseline strategy that performs no Roth conversions. " +
                "Used for comparing the impact of different conversion strategies.";
    }
}
