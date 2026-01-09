package rg.financialplanning.web.dto;

import java.util.List;

/**
 * Request DTO for state residence scenario comparison analysis.
 */
public class StateScenarioRequestDTO {
    private FinancialPlanDTO planData;
    private List<StateScenarioDTO> scenarios;
    private int yearIncrement = 5;
    private String filingStatus = "MARRIED_FILING_JOINTLY";

    public StateScenarioRequestDTO() {
    }

    public StateScenarioRequestDTO(FinancialPlanDTO planData, List<StateScenarioDTO> scenarios,
                                    int yearIncrement, String filingStatus) {
        this.planData = planData;
        this.scenarios = scenarios;
        this.yearIncrement = yearIncrement;
        this.filingStatus = filingStatus;
    }

    public FinancialPlanDTO getPlanData() {
        return planData;
    }

    public void setPlanData(FinancialPlanDTO planData) {
        this.planData = planData;
    }

    public List<StateScenarioDTO> getScenarios() {
        return scenarios;
    }

    public void setScenarios(List<StateScenarioDTO> scenarios) {
        this.scenarios = scenarios;
    }

    public int getYearIncrement() {
        return yearIncrement;
    }

    public void setYearIncrement(int yearIncrement) {
        this.yearIncrement = yearIncrement;
    }

    public String getFilingStatus() {
        return filingStatus;
    }

    public void setFilingStatus(String filingStatus) {
        this.filingStatus = filingStatus;
    }
}
