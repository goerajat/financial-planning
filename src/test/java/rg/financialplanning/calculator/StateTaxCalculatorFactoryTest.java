package rg.financialplanning.calculator;

import org.junit.Test;
import rg.financialplanning.model.State;
import rg.financialplanning.model.StateByYear;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class StateTaxCalculatorFactoryTest {

    // ===== createCalculator tests =====

    @Test
    public void testCreateCalculator_NJ() {
        TaxCalculator calculator = StateTaxCalculatorFactory.createCalculator(State.NJ);
        assertNotNull(calculator);
        assertTrue(calculator instanceof NJStateTaxCalculator);
    }

    @Test
    public void testCreateCalculator_NY() {
        TaxCalculator calculator = StateTaxCalculatorFactory.createCalculator(State.NY);
        assertNotNull(calculator);
        assertTrue(calculator instanceof NYStateTaxCalculator);
    }

    @Test
    public void testCreateCalculator_FL() {
        TaxCalculator calculator = StateTaxCalculatorFactory.createCalculator(State.FL);
        assertNotNull(calculator);
        assertTrue(calculator instanceof FloridaTaxCalculator);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateCalculator_null() {
        StateTaxCalculatorFactory.createCalculator(null);
    }

    // ===== getCalculatorForYear tests =====

    @Test
    public void testGetCalculatorForYear_emptyList() {
        TaxCalculator calculator = StateTaxCalculatorFactory.getCalculatorForYear(2025, new ArrayList<>());
        assertNotNull(calculator);
        assertTrue("Should default to NJ when list is empty", calculator instanceof NJStateTaxCalculator);
    }

    @Test
    public void testGetCalculatorForYear_nullList() {
        TaxCalculator calculator = StateTaxCalculatorFactory.getCalculatorForYear(2025, null);
        assertNotNull(calculator);
        assertTrue("Should default to NJ when list is null", calculator instanceof NJStateTaxCalculator);
    }

    @Test
    public void testGetCalculatorForYear_singleStateConfig() {
        List<StateByYear> statesByYear = List.of(
                new StateByYear(State.FL, 2024, 2030)
        );

        TaxCalculator calculator = StateTaxCalculatorFactory.getCalculatorForYear(2025, statesByYear);
        assertTrue(calculator instanceof FloridaTaxCalculator);
    }

    @Test
    public void testGetCalculatorForYear_multipleStateConfigs() {
        List<StateByYear> statesByYear = List.of(
                new StateByYear(State.NJ, 2020, 2025),
                new StateByYear(State.FL, 2026, 2040)
        );

        // Year in NJ range
        TaxCalculator njCalculator = StateTaxCalculatorFactory.getCalculatorForYear(2023, statesByYear);
        assertTrue(njCalculator instanceof NJStateTaxCalculator);

        // Year in FL range
        TaxCalculator flCalculator = StateTaxCalculatorFactory.getCalculatorForYear(2030, statesByYear);
        assertTrue(flCalculator instanceof FloridaTaxCalculator);
    }

    @Test
    public void testGetCalculatorForYear_yearNotInAnyRange() {
        List<StateByYear> statesByYear = List.of(
                new StateByYear(State.FL, 2026, 2040)
        );

        // Year before any range should default to NJ
        TaxCalculator calculator = StateTaxCalculatorFactory.getCalculatorForYear(2020, statesByYear);
        assertTrue("Should default to NJ when year not in range", calculator instanceof NJStateTaxCalculator);
    }

    @Test
    public void testGetCalculatorForYear_boundaryYears() {
        List<StateByYear> statesByYear = List.of(
                new StateByYear(State.NY, 2024, 2030)
        );

        // Start year
        TaxCalculator startCalc = StateTaxCalculatorFactory.getCalculatorForYear(2024, statesByYear);
        assertTrue(startCalc instanceof NYStateTaxCalculator);

        // End year
        TaxCalculator endCalc = StateTaxCalculatorFactory.getCalculatorForYear(2030, statesByYear);
        assertTrue(endCalc instanceof NYStateTaxCalculator);

        // Just before start
        TaxCalculator beforeCalc = StateTaxCalculatorFactory.getCalculatorForYear(2023, statesByYear);
        assertTrue(beforeCalc instanceof NJStateTaxCalculator);

        // Just after end
        TaxCalculator afterCalc = StateTaxCalculatorFactory.getCalculatorForYear(2031, statesByYear);
        assertTrue(afterCalc instanceof NJStateTaxCalculator);
    }

    // ===== getStateForYear tests =====

    @Test
    public void testGetStateForYear_emptyList() {
        State state = StateTaxCalculatorFactory.getStateForYear(2025, new ArrayList<>());
        assertEquals(State.NJ, state);
    }

    @Test
    public void testGetStateForYear_nullList() {
        State state = StateTaxCalculatorFactory.getStateForYear(2025, null);
        assertEquals(State.NJ, state);
    }

    @Test
    public void testGetStateForYear_matchingConfig() {
        List<StateByYear> statesByYear = List.of(
                new StateByYear(State.FL, 2024, 2030)
        );

        State state = StateTaxCalculatorFactory.getStateForYear(2025, statesByYear);
        assertEquals(State.FL, state);
    }

    @Test
    public void testGetStateForYear_noMatchingConfig() {
        List<StateByYear> statesByYear = List.of(
                new StateByYear(State.FL, 2030, 2040)
        );

        State state = StateTaxCalculatorFactory.getStateForYear(2025, statesByYear);
        assertEquals(State.NJ, state);
    }

    @Test
    public void testGetStateForYear_firstMatchWins() {
        // Overlapping ranges - first match should win
        List<StateByYear> statesByYear = List.of(
                new StateByYear(State.NY, 2024, 2028),
                new StateByYear(State.FL, 2026, 2030)
        );

        // Year 2027 is in both ranges, but NY is listed first
        State state = StateTaxCalculatorFactory.getStateForYear(2027, statesByYear);
        assertEquals(State.NY, state);
    }

    // ===== Integration tests - verify tax rates =====

    @Test
    public void testFloridaCalculator_zeroTax() {
        FloridaTaxCalculator flCalculator = (FloridaTaxCalculator) StateTaxCalculatorFactory.createCalculator(State.FL);
        double tax = flCalculator.calculateTax(100000, rg.financialplanning.model.FilingStatus.MARRIED_FILING_JOINTLY);
        assertEquals("Florida should have 0% state tax", 0.0, tax, 0.01);
    }

    @Test
    public void testNJCalculator_positiveTax() {
        NJStateTaxCalculator njCalculator = (NJStateTaxCalculator) StateTaxCalculatorFactory.createCalculator(State.NJ);
        double tax = njCalculator.calculateTax(100000, rg.financialplanning.model.FilingStatus.MARRIED_FILING_JOINTLY);
        assertTrue("NJ should have positive state tax", tax > 0);
    }

    @Test
    public void testNYCalculator_positiveTax() {
        NYStateTaxCalculator nyCalculator = (NYStateTaxCalculator) StateTaxCalculatorFactory.createCalculator(State.NY);
        double tax = nyCalculator.calculateTax(100000, rg.financialplanning.model.FilingStatus.MARRIED_FILING_JOINTLY);
        assertTrue("NY should have positive state tax", tax > 0);
    }
}
