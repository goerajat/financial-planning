package rg.financialplanning.calculator;

import rg.financialplanning.model.State;
import rg.financialplanning.model.StateByYear;

import java.util.List;

/**
 * Factory for creating state tax calculators based on state and year.
 * Supports determining which state's tax calculator to use for a given year
 * when state residency changes over time.
 */
public class StateTaxCalculatorFactory {

    /**
     * Creates a tax calculator for the specified state.
     *
     * @param state the state
     * @return the appropriate TaxCalculator implementation
     */
    public static TaxCalculator createCalculator(State state) {
        if (state == null) {
            throw new IllegalArgumentException("State cannot be null");
        }

        return switch (state) {
            case NJ -> new NJStateTaxCalculator();
            case NY -> new NYStateTaxCalculator();
            case FL -> new FloridaTaxCalculator();
        };
    }

    /**
     * Determines which state applies for a given year based on StateByYear configuration.
     * Returns the first matching state, or NJ as default if no match is found.
     *
     * @param year          the year to check
     * @param statesByYear  list of state configurations by year
     * @return the TaxCalculator for the applicable state
     */
    public static TaxCalculator getCalculatorForYear(int year, List<StateByYear> statesByYear) {
        if (statesByYear == null || statesByYear.isEmpty()) {
            // Default to NJ if no configuration provided
            return new NJStateTaxCalculator();
        }

        // Find the first state configuration that applies to this year
        for (StateByYear stateByYear : statesByYear) {
            if (stateByYear.appliesTo(year)) {
                return createCalculator(stateByYear.state());
            }
        }

        // Default to NJ if no matching configuration found
        return new NJStateTaxCalculator();
    }

    /**
     * Determines which state applies for a given year.
     * Returns the first matching state, or NJ as default.
     *
     * @param year          the year to check
     * @param statesByYear  list of state configurations by year
     * @return the applicable State
     */
    public static State getStateForYear(int year, List<StateByYear> statesByYear) {
        if (statesByYear == null || statesByYear.isEmpty()) {
            return State.NJ;
        }

        for (StateByYear stateByYear : statesByYear) {
            if (stateByYear.appliesTo(year)) {
                return stateByYear.state();
            }
        }

        return State.NJ;
    }
}
