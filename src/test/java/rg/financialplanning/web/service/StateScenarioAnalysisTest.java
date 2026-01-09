package rg.financialplanning.web.service;

import org.junit.Before;
import org.junit.Test;
import rg.financialplanning.web.dto.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Tests for the state scenario analysis feature in FinancialPlanService.
 */
public class StateScenarioAnalysisTest {

    private FinancialPlanService planService;
    private FinancialPlanDTO basePlanData;

    @Before
    public void setUp() {
        planService = new FinancialPlanService();

        // Create a basic financial plan for testing
        basePlanData = new FinancialPlanDTO();

        // Add persons
        List<PersonDTO> persons = new ArrayList<>();
        persons.add(new PersonDTO("John", 1970));
        persons.add(new PersonDTO("Jane", 1972));
        basePlanData.setPersons(persons);

        // Add financial entries
        List<FinancialEntryDTO> entries = new ArrayList<>();
        // Income
        FinancialEntryDTO income = new FinancialEntryDTO("John", "INCOME", "Salary", 200000, 2025, 2035);
        income.setId(1L);
        entries.add(income);

        // Expenses
        FinancialEntryDTO expense = new FinancialEntryDTO("John", "EXPENSE", "Living", 80000, 2025, 2060);
        expense.setId(2L);
        entries.add(expense);

        // Qualified assets
        FinancialEntryDTO qualified = new FinancialEntryDTO("John", "QUALIFIED", "401k", 1000000, 2025, 2025);
        qualified.setId(3L);
        entries.add(qualified);

        // Social Security
        FinancialEntryDTO ss = new FinancialEntryDTO("John", "SOCIAL_SECURITY_BENEFITS", "SS", 30000, 2037, 2060);
        ss.setId(4L);
        entries.add(ss);

        basePlanData.setEntries(entries);

        // Add default rates
        Map<String, Double> rates = new HashMap<>();
        rates.put("INCOME", 3.0);
        rates.put("EXPENSE", 3.0);
        rates.put("QUALIFIED", 6.0);
        rates.put("NON_QUALIFIED", 6.0);
        rates.put("ROTH", 6.0);
        rates.put("CASH", 2.0);
        rates.put("REAL_ESTATE", 3.0);
        rates.put("LIFE_INSURANCE_BENEFIT", 0.0);
        rates.put("SOCIAL_SECURITY_BENEFITS", 2.0);
        rates.put("ROTH_CONTRIBUTION", 0.0);
        rates.put("QUALIFIED_CONTRIBUTION", 0.0);
        rates.put("LIFE_INSURANCE_CONTRIBUTION", 0.0);
        rates.put("MORTGAGE", 6.5);
        rates.put("MORTGAGE_REPAYMENT", 0.0);
        basePlanData.setRates(rates);
    }

    // ===== Validation tests =====

    @Test(expected = IllegalArgumentException.class)
    public void testRunStateScenarioAnalysis_throwsWithLessThanTwoScenarios() {
        StateScenarioRequestDTO request = new StateScenarioRequestDTO();
        request.setPlanData(basePlanData);

        // Only one scenario
        List<StateScenarioDTO> scenarios = new ArrayList<>();
        scenarios.add(createScenario("Stay in NJ", "NJ", 2025, 2060));
        request.setScenarios(scenarios);

        planService.runStateScenarioAnalysis(request);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRunStateScenarioAnalysis_throwsWithNullScenarios() {
        StateScenarioRequestDTO request = new StateScenarioRequestDTO();
        request.setPlanData(basePlanData);
        request.setScenarios(null);

        planService.runStateScenarioAnalysis(request);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRunStateScenarioAnalysis_throwsWithMoreThanFiveScenarios() {
        StateScenarioRequestDTO request = new StateScenarioRequestDTO();
        request.setPlanData(basePlanData);

        List<StateScenarioDTO> scenarios = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            scenarios.add(createScenario("Scenario " + i, "NJ", 2025, 2060));
        }
        request.setScenarios(scenarios);

        planService.runStateScenarioAnalysis(request);
    }

    // ===== Basic functionality tests =====

    @Test
    public void testRunStateScenarioAnalysis_returnsCorrectNumberOfRows() {
        StateScenarioRequestDTO request = createBasicRequest();

        StateScenarioResponseDTO response = planService.runStateScenarioAnalysis(request);

        assertEquals("Should have 2 rows for 2 scenarios", 2, response.getRows().size());
    }

    @Test
    public void testRunStateScenarioAnalysis_returnsCorrectScenarioNames() {
        StateScenarioRequestDTO request = createBasicRequest();

        StateScenarioResponseDTO response = planService.runStateScenarioAnalysis(request);

        assertEquals("Stay in NJ", response.getRows().get(0).getScenarioName());
        assertEquals("Move to FL", response.getRows().get(1).getScenarioName());
    }

    @Test
    public void testRunStateScenarioAnalysis_generatesScenarioDescriptions() {
        StateScenarioRequestDTO request = createBasicRequest();

        StateScenarioResponseDTO response = planService.runStateScenarioAnalysis(request);

        assertNotNull(response.getRows().get(0).getScenarioDescription());
        assertNotNull(response.getRows().get(1).getScenarioDescription());
        assertTrue(response.getRows().get(0).getScenarioDescription().contains("NJ"));
    }

    @Test
    public void testRunStateScenarioAnalysis_hasMilestoneYears() {
        StateScenarioRequestDTO request = createBasicRequest();
        request.setYearIncrement(5);

        StateScenarioResponseDTO response = planService.runStateScenarioAnalysis(request);

        assertNotNull(response.getMilestoneYears());
        assertFalse(response.getMilestoneYears().isEmpty());
    }

    @Test
    public void testRunStateScenarioAnalysis_cellsMatchMilestoneYears() {
        StateScenarioRequestDTO request = createBasicRequest();
        request.setYearIncrement(5);

        StateScenarioResponseDTO response = planService.runStateScenarioAnalysis(request);

        for (StateScenarioRowDTO row : response.getRows()) {
            assertEquals("Cells should match milestone years",
                    response.getMilestoneYears().size(),
                    row.getCells().size());
        }
    }

    // ===== Delta calculation tests =====

    @Test
    public void testRunStateScenarioAnalysis_baselineHasZeroDelta() {
        StateScenarioRequestDTO request = createBasicRequest();

        StateScenarioResponseDTO response = planService.runStateScenarioAnalysis(request);

        // First row is baseline, should have 0 delta for all cells
        StateScenarioRowDTO baselineRow = response.getRows().get(0);
        for (StateScenarioCellDTO cell : baselineRow.getCells()) {
            assertEquals("Baseline should have 0 delta", 0.0, cell.getNetWorthDelta(), 0.01);
        }
    }

    @Test
    public void testRunStateScenarioAnalysis_floridaHasPositiveDelta() {
        StateScenarioRequestDTO request = createBasicRequest();

        StateScenarioResponseDTO response = planService.runStateScenarioAnalysis(request);

        // Second row is FL scenario - should have positive delta due to no state tax
        StateScenarioRowDTO flRow = response.getRows().get(1);

        // Find a cell in later years where the difference should be noticeable
        boolean foundPositiveDelta = false;
        for (StateScenarioCellDTO cell : flRow.getCells()) {
            if (cell.getNetWorthDelta() > 0) {
                foundPositiveDelta = true;
                break;
            }
        }

        assertTrue("FL scenario should have positive delta vs NJ baseline in at least one year", foundPositiveDelta);
    }

    // ===== State tax savings tests =====

    @Test
    public void testRunStateScenarioAnalysis_baselineHasZeroStateTaxSavings() {
        StateScenarioRequestDTO request = createBasicRequest();

        StateScenarioResponseDTO response = planService.runStateScenarioAnalysis(request);

        StateScenarioRowDTO baselineRow = response.getRows().get(0);
        for (StateScenarioCellDTO cell : baselineRow.getCells()) {
            assertEquals("Baseline should have 0 state tax savings", 0.0, cell.getStateTaxSavings(), 0.01);
        }
    }

    @Test
    public void testRunStateScenarioAnalysis_floridaHasPositiveStateTaxSavings() {
        StateScenarioRequestDTO request = createBasicRequest();

        StateScenarioResponseDTO response = planService.runStateScenarioAnalysis(request);

        StateScenarioRowDTO flRow = response.getRows().get(1);

        // FL should have positive state tax savings vs NJ baseline in later years
        boolean foundPositiveSavings = false;
        for (StateScenarioCellDTO cell : flRow.getCells()) {
            if (cell.getStateTaxSavings() > 0) {
                foundPositiveSavings = true;
                break;
            }
        }

        assertTrue("FL scenario should have positive state tax savings vs NJ baseline", foundPositiveSavings);
    }

    // ===== Active state tests =====

    @Test
    public void testRunStateScenarioAnalysis_tracksActiveState() {
        StateScenarioRequestDTO request = createBasicRequest();

        StateScenarioResponseDTO response = planService.runStateScenarioAnalysis(request);

        for (StateScenarioRowDTO row : response.getRows()) {
            for (StateScenarioCellDTO cell : row.getCells()) {
                assertNotNull("Active state should not be null", cell.getActiveState());
                assertFalse("Active state should not be empty", cell.getActiveState().isEmpty());
            }
        }
    }

    @Test
    public void testRunStateScenarioAnalysis_activeStateMatchesConfiguration() {
        StateScenarioRequestDTO request = createBasicRequest();

        StateScenarioResponseDTO response = planService.runStateScenarioAnalysis(request);

        // First row (NJ scenario) should show NJ for all years
        StateScenarioRowDTO njRow = response.getRows().get(0);
        for (StateScenarioCellDTO cell : njRow.getCells()) {
            assertTrue("NJ scenario should show NJ or New Jersey",
                    cell.getActiveState().contains("New Jersey") || cell.getActiveState().equals("NJ"));
        }
    }

    // ===== Multi-state scenario tests =====

    @Test
    public void testRunStateScenarioAnalysis_handlesMultiStateScenario() {
        StateScenarioRequestDTO request = new StateScenarioRequestDTO();
        request.setPlanData(basePlanData);
        request.setYearIncrement(5);
        request.setFilingStatus("MARRIED_FILING_JOINTLY");

        List<StateScenarioDTO> scenarios = new ArrayList<>();

        // Scenario 1: Stay in NJ
        scenarios.add(createScenario("Stay in NJ", "NJ", 2025, 2060));

        // Scenario 2: NJ then NY then FL
        StateScenarioDTO multiState = new StateScenarioDTO();
        multiState.setName("NJ to NY to FL");
        List<StateByYearDTO> multiStateRanges = new ArrayList<>();
        multiStateRanges.add(new StateByYearDTO("NJ", 2025, 2030));
        multiStateRanges.add(new StateByYearDTO("NY", 2031, 2040));
        multiStateRanges.add(new StateByYearDTO("FL", 2041, 2060));
        multiState.setStatesByYear(multiStateRanges);
        scenarios.add(multiState);

        request.setScenarios(scenarios);

        StateScenarioResponseDTO response = planService.runStateScenarioAnalysis(request);

        assertEquals(2, response.getRows().size());
        assertNotNull(response.getRows().get(1).getScenarioDescription());
        assertTrue("Multi-state description should mention all states",
                response.getRows().get(1).getScenarioDescription().contains("NJ") &&
                response.getRows().get(1).getScenarioDescription().contains("NY") &&
                response.getRows().get(1).getScenarioDescription().contains("FL"));
    }

    // ===== Filing status tests =====

    @Test
    public void testRunStateScenarioAnalysis_respectsFilingStatus() {
        StateScenarioRequestDTO request = createBasicRequest();
        request.setFilingStatus("SINGLE");

        StateScenarioResponseDTO response = planService.runStateScenarioAnalysis(request);

        // Should complete without error
        assertNotNull(response);
        assertEquals(2, response.getRows().size());
    }

    @Test
    public void testRunStateScenarioAnalysis_defaultsToMFJ() {
        StateScenarioRequestDTO request = createBasicRequest();
        request.setFilingStatus("INVALID_STATUS");

        // Should default to MFJ and not throw
        StateScenarioResponseDTO response = planService.runStateScenarioAnalysis(request);
        assertNotNull(response);
    }

    // ===== Helper methods =====

    private StateScenarioRequestDTO createBasicRequest() {
        StateScenarioRequestDTO request = new StateScenarioRequestDTO();
        request.setPlanData(basePlanData);
        request.setYearIncrement(5);
        request.setFilingStatus("MARRIED_FILING_JOINTLY");

        List<StateScenarioDTO> scenarios = new ArrayList<>();
        scenarios.add(createScenario("Stay in NJ", "NJ", 2025, 2060));
        scenarios.add(createScenario("Move to FL", "FL", 2025, 2060));
        request.setScenarios(scenarios);

        return request;
    }

    private StateScenarioDTO createScenario(String name, String state, int startYear, int endYear) {
        StateScenarioDTO scenario = new StateScenarioDTO();
        scenario.setName(name);

        List<StateByYearDTO> statesByYear = new ArrayList<>();
        statesByYear.add(new StateByYearDTO(state, startYear, endYear));
        scenario.setStatesByYear(statesByYear);

        return scenario;
    }
}
