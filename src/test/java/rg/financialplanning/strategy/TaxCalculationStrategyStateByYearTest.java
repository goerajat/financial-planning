package rg.financialplanning.strategy;

import org.junit.Test;
import rg.financialplanning.model.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Tests for TaxCalculationStrategy with state-by-year configuration.
 */
public class TaxCalculationStrategyStateByYearTest {

    private YearlySummary createYearlySummary(int year, double income) {
        Person person = new Person("John", 1960);
        IndividualYearlySummary individual = new IndividualYearlySummary(person, year, income, 0, 0, 0, 0);
        Map<String, IndividualYearlySummary> individuals = new HashMap<>();
        individuals.put("John", individual);

        return new YearlySummary(year, income, 50000, 500000, 200000, 100000, 50000, 0, 0, 0, 0, individuals);
    }

    // ===== Constructor tests with statesByYear =====

    @Test
    public void testConstructor_withEmptyStatesByYear() {
        TaxCalculationStrategy strategy = new TaxCalculationStrategy(FilingStatus.MARRIED_FILING_JOINTLY, List.of());
        assertEquals(FilingStatus.MARRIED_FILING_JOINTLY, strategy.getFilingStatus());
        assertTrue(strategy.getStatesByYear().isEmpty());
    }

    @Test
    public void testConstructor_withNullStatesByYear() {
        TaxCalculationStrategy strategy = new TaxCalculationStrategy(FilingStatus.SINGLE, null);
        assertEquals(FilingStatus.SINGLE, strategy.getFilingStatus());
        assertTrue(strategy.getStatesByYear().isEmpty());
    }

    @Test
    public void testConstructor_withStatesByYear() {
        List<StateByYear> statesByYear = List.of(
                new StateByYear(State.NJ, 2024, 2030),
                new StateByYear(State.FL, 2031, 2050)
        );

        TaxCalculationStrategy strategy = new TaxCalculationStrategy(FilingStatus.MARRIED_FILING_JOINTLY, statesByYear);
        assertEquals(2, strategy.getStatesByYear().size());
    }

    @Test
    public void testConstructor_defensiveCopy() {
        List<StateByYear> statesByYear = new java.util.ArrayList<>();
        statesByYear.add(new StateByYear(State.NJ, 2024, 2030));

        TaxCalculationStrategy strategy = new TaxCalculationStrategy(FilingStatus.MARRIED_FILING_JOINTLY, statesByYear);

        // Modifying original list should not affect strategy
        statesByYear.add(new StateByYear(State.FL, 2031, 2050));
        assertEquals(1, strategy.getStatesByYear().size());
    }

    // ===== State tax calculation tests =====

    @Test
    public void testOptimize_usesNJByDefault() {
        TaxCalculationStrategy strategy = new TaxCalculationStrategy(FilingStatus.MARRIED_FILING_JOINTLY);

        YearlySummary summary = createYearlySummary(2025, 100000);
        strategy.optimize(null, summary);

        // With no state config, should use NJ (positive tax)
        assertTrue("State tax should be positive with default NJ", summary.stateIncomeTax() > 0);
    }

    @Test
    public void testOptimize_usesFloridaWhenConfigured() {
        List<StateByYear> statesByYear = List.of(
                new StateByYear(State.FL, 2024, 2030)
        );

        TaxCalculationStrategy strategy = new TaxCalculationStrategy(FilingStatus.MARRIED_FILING_JOINTLY, statesByYear);

        YearlySummary summary = createYearlySummary(2025, 100000);
        strategy.optimize(null, summary);

        // Florida has 0% state tax
        assertEquals("Florida should have 0 state tax", 0.0, summary.stateIncomeTax(), 0.01);
    }

    @Test
    public void testOptimize_switchesStateByYear() {
        List<StateByYear> statesByYear = List.of(
                new StateByYear(State.NJ, 2024, 2025),
                new StateByYear(State.FL, 2026, 2030)
        );

        TaxCalculationStrategy strategy = new TaxCalculationStrategy(FilingStatus.MARRIED_FILING_JOINTLY, statesByYear);

        // Year 2025 - should use NJ
        YearlySummary njSummary = createYearlySummary(2025, 100000);
        strategy.optimize(null, njSummary);
        double njTax = njSummary.stateIncomeTax();
        assertTrue("NJ state tax should be positive", njTax > 0);

        // Year 2026 - should use FL
        YearlySummary flSummary = createYearlySummary(2026, 100000);
        strategy.optimize(null, flSummary);
        double flTax = flSummary.stateIncomeTax();
        assertEquals("FL state tax should be 0", 0.0, flTax, 0.01);
    }

    @Test
    public void testOptimize_fallsBackToNJWhenYearNotConfigured() {
        List<StateByYear> statesByYear = List.of(
                new StateByYear(State.FL, 2030, 2040)
        );

        TaxCalculationStrategy strategy = new TaxCalculationStrategy(FilingStatus.MARRIED_FILING_JOINTLY, statesByYear);

        // Year 2025 is not configured, should fall back to NJ
        YearlySummary summary = createYearlySummary(2025, 100000);
        strategy.optimize(null, summary);

        assertTrue("Should fall back to NJ (positive tax)", summary.stateIncomeTax() > 0);
    }

    @Test
    public void testOptimize_usesNYWhenConfigured() {
        List<StateByYear> statesByYear = List.of(
                new StateByYear(State.NY, 2024, 2030)
        );

        TaxCalculationStrategy strategy = new TaxCalculationStrategy(FilingStatus.MARRIED_FILING_JOINTLY, statesByYear);

        YearlySummary summary = createYearlySummary(2025, 100000);
        strategy.optimize(null, summary);

        // NY has positive state tax
        assertTrue("NY should have positive state tax", summary.stateIncomeTax() > 0);
    }

    // ===== Tax comparison tests =====

    @Test
    public void testOptimize_floridaTaxSavings() {
        // Compare total taxes in NJ vs FL
        double income = 200000;

        // NJ scenario
        TaxCalculationStrategy njStrategy = new TaxCalculationStrategy(
                FilingStatus.MARRIED_FILING_JOINTLY,
                List.of(new StateByYear(State.NJ, 2024, 2030))
        );
        YearlySummary njSummary = createYearlySummary(2025, income);
        njStrategy.optimize(null, njSummary);
        double njTotalTax = njSummary.federalIncomeTax() + njSummary.stateIncomeTax();

        // FL scenario
        TaxCalculationStrategy flStrategy = new TaxCalculationStrategy(
                FilingStatus.MARRIED_FILING_JOINTLY,
                List.of(new StateByYear(State.FL, 2024, 2030))
        );
        YearlySummary flSummary = createYearlySummary(2025, income);
        flStrategy.optimize(null, flSummary);
        double flTotalTax = flSummary.federalIncomeTax() + flSummary.stateIncomeTax();

        // Federal tax should be the same
        assertEquals("Federal tax should be same", njSummary.federalIncomeTax(), flSummary.federalIncomeTax(), 0.01);

        // FL total tax should be lower due to no state tax
        assertTrue("FL total tax should be lower than NJ", flTotalTax < njTotalTax);

        // The difference should be exactly the NJ state tax
        assertEquals("Difference should be NJ state tax", njSummary.stateIncomeTax(), njTotalTax - flTotalTax, 0.01);
    }

    // ===== Strategy metadata tests =====

    @Test
    public void testGetStrategyName() {
        TaxCalculationStrategy strategy = new TaxCalculationStrategy();
        assertEquals("Tax Calculation", strategy.getStrategyName());
    }

    @Test
    public void testGetDescription_mentionsStateConfiguration() {
        TaxCalculationStrategy strategy = new TaxCalculationStrategy();
        String description = strategy.getDescription();
        assertTrue("Description should mention state tax", description.contains("state"));
    }
}
