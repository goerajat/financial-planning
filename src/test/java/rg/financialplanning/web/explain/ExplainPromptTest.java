package rg.financialplanning.web.explain;

import org.junit.Test;
import rg.financialplanning.model.YearlySummary;
import rg.financialplanning.web.dto.ExplanationDTO;
import rg.financialplanning.web.dto.FinancialPlanDTO;
import rg.financialplanning.web.dto.PersonDTO;
import rg.financialplanning.web.dto.StateByYearDTO;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class ExplainPromptTest {

    private static FinancialPlanDTO plan() {
        FinancialPlanDTO plan = new FinancialPlanDTO();
        plan.setPersons(new ArrayList<>(List.of(new PersonDTO("Blake", 1975), new PersonDTO("Avery", 1973))));
        plan.setStatesByYear(new ArrayList<>(List.of(new StateByYearDTO("NJ", 2026, 2070))));
        Map<String, Double> rates = new HashMap<>();
        rates.put("QUALIFIED", 8.0);
        rates.put("EXPENSE", 3.0);
        plan.setRates(rates);
        return plan;
    }

    private static YearlySummary year(int year, double income, double roth) {
        return new YearlySummary(year, income, 100_000, 1_000_000, 2_000_000, roth, 50_000, 0, 0, 0, 0, Map.of());
    }

    @Test
    public void labelsFollowAgeAndSurviveTheRoundTrip() {
        ExplainPrompt.Labels labels = ExplainPrompt.Labels.of(plan().getPersons());
        assertEquals("older spouse", labels.label("Avery"));
        assertEquals("younger spouse", labels.label("Blake"));

        String prompt = labels.toPrompt("Roth conversion: $32k moved from Avery's retirement account; Blake turns 65.");
        assertEquals("Roth conversion: $32k moved from the older spouse's retirement account; the younger spouse turns 65.",
                prompt);
        assertEquals("Avery's account grew while Blake waited.",
                labels.toDisplay("The older spouse's account grew while the younger spouse waited."));
    }

    @Test
    public void namesInsideWordsAreLeftAlone() {
        ExplainPrompt.Labels labels = ExplainPrompt.Labels.of(List.of(new PersonDTO("Al", 1970), new PersonDTO("Bo", 1972)));
        assertEquals("Also, the older spouse and the younger spouse.", labels.toPrompt("Also, Al and Bo."));
    }

    @Test
    public void promptCarriesTheTableTheRulesAndNoNames() {
        FinancialPlanDTO plan = plan();
        YearlySummary[] s = {year(2026, 400_000, 10_000), year(2027, 0, 40_000)};
        s[1].addNote("Roth conversion: $32k moved from Avery's retirement account to Roth and taxed as income; "
                + "$29k landed in Roth after the tax on it; the amount was limited by the next Medicare IRMAA tier.");
        ExplainPrompt.Labels labels = ExplainPrompt.Labels.of(plan.getPersons());
        List<ExplanationDTO> prior = List.of(
                new ExplanationDTO(1, "Why does Avery's Roth grow?", "Because Avery converted.", "DONE", "t", "t", 5, null));

        String prompt = ExplainPrompt.build(plan, s, prior, "Why does Roth increase in 2027 for Avery?", labels);

        assertFalse(prompt, prompt.contains("Avery"));
        assertFalse(prompt, prompt.contains("Blake"));
        assertTrue(prompt.contains("Year,Net worth,Qualified,Non-qualified,Roth,Cash,Other,Mortgage balance,Wages,"));
        assertTrue(prompt.contains("\n2027,3090,1000,2000,40,50,0,0,0,0,0,0,100,0,0,0,0,0,0\n"));
        assertTrue(prompt.contains("2027: Wages stop; 2026 was the last year with wages. Roth conversion: $32k moved from "
                + "the older spouse's retirement account"));
        assertTrue(prompt.contains("HOW THE PLANNER WORKS\n- Balances are shown at the end of each year."));
        assertTrue(prompt.contains("- The older spouse: age 53 in 2026, claims Social Security at full retirement age"));
        assertTrue(prompt.contains("- State of residence: New Jersey 2026-2070."));
        assertTrue(prompt.contains("retirement accounts 8%, living expenses rise 3%"));
        assertTrue(prompt.contains("Q: Why does the older spouse's Roth grow?\nA: Because the older spouse converted."));
        assertTrue(prompt.endsWith("QUESTION\nWhy does Roth increase in 2027 for the older spouse?\n"));
    }

    @Test
    public void suggestionsComeFromWhatHappens() {
        YearlySummary[] s = {year(2026, 400_000, 10_000), year(2027, 0, 40_000), year(2028, 0, 70_000)};
        s[1].addNote("Roth conversion: $32k moved.");
        s[2].addNote("Roth conversion: $30k moved.");
        s[2].setIrmaaSurcharge(5_000);
        assertEquals(List.of("Why do withdrawals jump in 2027?", "Why does Roth increase in 2027?",
                "Why does an IRMAA surcharge appear in 2028?"), ExplainEvents.suggestions(s));
        assertEquals(List.of("How does net worth change over the plan?", "What are the biggest costs in the first ten years?"),
                ExplainEvents.suggestions(new YearlySummary[] {year(2026, 400_000, 0)}));
    }

    @Test
    public void moneyReadsLikeTheTable() {
        assertEquals("$32k", ExplainEvents.money(31_600));
        assertEquals("$1.2M", ExplainEvents.money(1_240_000));
        assertEquals("-$5k", ExplainEvents.money(-5_200));
        assertEquals("$0k", ExplainEvents.money(400));
    }
}
