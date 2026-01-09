package rg.financialplanning.model;

import org.junit.Test;

import static org.junit.Assert.*;

public class StateByYearTest {

    // ===== Constructor tests =====

    @Test
    public void testConstructor_validInput() {
        StateByYear stateByYear = new StateByYear(State.NJ, 2024, 2030);
        assertEquals(State.NJ, stateByYear.state());
        assertEquals(2024, stateByYear.startYear());
        assertEquals(2030, stateByYear.endYear());
    }

    @Test
    public void testConstructor_sameStartAndEndYear() {
        StateByYear stateByYear = new StateByYear(State.FL, 2025, 2025);
        assertEquals(State.FL, stateByYear.state());
        assertEquals(2025, stateByYear.startYear());
        assertEquals(2025, stateByYear.endYear());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructor_nullState() {
        new StateByYear(null, 2024, 2030);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructor_startYearAfterEndYear() {
        new StateByYear(State.NJ, 2030, 2024);
    }

    // ===== appliesTo tests =====

    @Test
    public void testAppliesTo_yearWithinRange() {
        StateByYear stateByYear = new StateByYear(State.NJ, 2024, 2030);
        assertTrue(stateByYear.appliesTo(2025));
        assertTrue(stateByYear.appliesTo(2027));
    }

    @Test
    public void testAppliesTo_startYear() {
        StateByYear stateByYear = new StateByYear(State.NJ, 2024, 2030);
        assertTrue(stateByYear.appliesTo(2024));
    }

    @Test
    public void testAppliesTo_endYear() {
        StateByYear stateByYear = new StateByYear(State.NJ, 2024, 2030);
        assertTrue(stateByYear.appliesTo(2030));
    }

    @Test
    public void testAppliesTo_yearBeforeRange() {
        StateByYear stateByYear = new StateByYear(State.NJ, 2024, 2030);
        assertFalse(stateByYear.appliesTo(2023));
    }

    @Test
    public void testAppliesTo_yearAfterRange() {
        StateByYear stateByYear = new StateByYear(State.NJ, 2024, 2030);
        assertFalse(stateByYear.appliesTo(2031));
    }

    @Test
    public void testAppliesTo_singleYearRange() {
        StateByYear stateByYear = new StateByYear(State.FL, 2025, 2025);
        assertTrue(stateByYear.appliesTo(2025));
        assertFalse(stateByYear.appliesTo(2024));
        assertFalse(stateByYear.appliesTo(2026));
    }

    // ===== State enum tests =====

    @Test
    public void testState_displayName() {
        assertEquals("New Jersey", State.NJ.getDisplayName());
        assertEquals("New York", State.NY.getDisplayName());
        assertEquals("Florida", State.FL.getDisplayName());
    }

    @Test
    public void testState_toString() {
        assertEquals("New Jersey", State.NJ.toString());
        assertEquals("New York", State.NY.toString());
        assertEquals("Florida", State.FL.toString());
    }

    @Test
    public void testState_valueOf() {
        assertEquals(State.NJ, State.valueOf("NJ"));
        assertEquals(State.NY, State.valueOf("NY"));
        assertEquals(State.FL, State.valueOf("FL"));
    }

    @Test
    public void testState_values() {
        State[] states = State.values();
        assertEquals(3, states.length);
    }
}
