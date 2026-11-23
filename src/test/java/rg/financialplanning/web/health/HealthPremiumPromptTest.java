package rg.financialplanning.web.health;

import org.junit.Test;
import rg.financialplanning.web.dto.FinancialEntryDTO;
import rg.financialplanning.web.dto.FinancialPlanDTO;
import rg.financialplanning.web.dto.PersonDTO;
import rg.financialplanning.web.dto.StateByYearDTO;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class HealthPremiumPromptTest {

    private static FinancialEntryDTO income(String person, int value, int start, int end) {
        FinancialEntryDTO e = new FinancialEntryDTO();
        e.setPersonName(person);
        e.setItemType("INCOME");
        e.setDescription("Salary");
        e.setValue(value);
        e.setStartYear(start);
        e.setEndYear(end);
        return e;
    }

    private static FinancialPlanDTO plan() {
        FinancialPlanDTO plan = new FinancialPlanDTO();
        plan.setPersons(new ArrayList<>(List.of(new PersonDTO("A", 1973), new PersonDTO("B", 1974))));
        plan.setEntries(new ArrayList<>(List.of(
                income("A", 200000, 2026, 2028),
                income("B", 150000, 2026, 2031))));
        plan.setStatesByYear(new ArrayList<>(List.of(new StateByYearDTO("NJ", 2026, 2070))));
        return plan;
    }

    @Test
    public void gapRunsFromTheHouseholdsLastWageYearToMedicare() {
        HealthPremiumPrompt.Inputs in = HealthPremiumPrompt.inputs(plan(), "A", HealthPremiumPrompt.Tier.SILVER, 2026);
        assertEquals(2028, in.personWageEnd());
        assertEquals(2031, in.householdWageEnd());
        assertEquals(2032, in.gapStart());
        assertEquals(2037, in.gapEnd());
        assertEquals(2038, in.medicareYear());
        assertEquals(61, in.midpointAge());
        assertEquals("NJ", in.preMedicareState());
        assertEquals("New Jersey", in.medicareStateName());
        assertEquals(2, in.householdSize());
        assertTrue(in.hasGap());
    }

    @Test
    public void promptCarriesYearsStateAndTierButNoName() {
        HealthPremiumPrompt.Inputs in = HealthPremiumPrompt.inputs(plan(), "B", HealthPremiumPrompt.Tier.GOLD, 2026);
        String prompt = HealthPremiumPrompt.build(in);
        assertFalse(prompt.contains("B,"));
        assertTrue(prompt.contains("born 1974"));
        assertTrue(prompt.contains("New Jersey 2032-2054"));
        assertTrue(prompt.contains("2032-2038 (ages 58-64)"));
        assertTrue(prompt.contains("Gold plan"));
        assertTrue(prompt.contains("single 61-year-old"));
        assertTrue(prompt.contains("Medigap Plan G at age 65 in New Jersey"));
        assertTrue(prompt.contains("\"medicarePlanPremiumMonthly\""));
    }

    @Test
    public void aMoveShowsAsTwoStateSpans() {
        FinancialPlanDTO plan = plan();
        plan.setStatesByYear(new ArrayList<>(List.of(
                new StateByYearDTO("NJ", 2026, 2040), new StateByYearDTO("FL", 2041, 2070))));
        HealthPremiumPrompt.Inputs in = HealthPremiumPrompt.inputs(plan, "A", HealthPremiumPrompt.Tier.SILVER, 2026);
        assertEquals(2, in.states().size());
        assertEquals("NJ", in.states().get(0).state());
        assertEquals(2040, in.states().get(0).endYear());
        assertEquals("FL", in.states().get(1).state());
        assertEquals("NJ", in.preMedicareState());
        assertEquals("NJ", in.medicareState());
        assertTrue(HealthPremiumPrompt.build(in).contains("Florida 2041-2053"));
    }

    @Test
    public void noGapWhenWagesRunPastMedicare() {
        FinancialPlanDTO plan = plan();
        plan.getEntries().add(income("B", 100000, 2032, 2040));
        HealthPremiumPrompt.Inputs in = HealthPremiumPrompt.inputs(plan, "A", HealthPremiumPrompt.Tier.SILVER, 2026);
        assertFalse(in.hasGap());
        assertNull(in.preMedicareState());
        assertTrue(HealthPremiumPrompt.build(in).contains("preMedicarePremiumMonthly: 0"));
    }

    @Test
    public void gapNeverStartsBeforeThisYear() {
        FinancialPlanDTO plan = plan();
        plan.setEntries(new ArrayList<>());
        HealthPremiumPrompt.Inputs in = HealthPremiumPrompt.inputs(plan, "A", HealthPremiumPrompt.Tier.SILVER, 2030);
        assertEquals(2030, in.gapStart());
        assertEquals(0, in.householdWageEnd());
    }

    @Test
    public void yearOutsideEveryStateRangeTakesTheNearest() {
        List<StateByYearDTO> states = List.of(new StateByYearDTO("NY", 2030, 2035));
        assertEquals("NY", HealthPremiumPrompt.stateFor(states, 2026));
        assertEquals("NY", HealthPremiumPrompt.stateFor(states, 2050));
    }

    @Test
    public void missingPersonBirthYearOrStateIsRejected() {
        FinancialPlanDTO plan = plan();
        try {
            HealthPremiumPrompt.inputs(plan, "Z", HealthPremiumPrompt.Tier.SILVER, 2026);
            fail();
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("No person"));
        }
        plan.setStatesByYear(new ArrayList<>());
        try {
            HealthPremiumPrompt.inputs(plan, "A", HealthPremiumPrompt.Tier.SILVER, 2026);
            fail();
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("States tab"));
        }
        plan = plan();
        plan.getPersons().get(0).setYearOfBirth(0);
        try {
            HealthPremiumPrompt.inputs(plan, "A", HealthPremiumPrompt.Tier.SILVER, 2026);
            fail();
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("year of birth"));
        }
    }
}
