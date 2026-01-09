package rg.financialplanning.web.dto;

import java.util.List;
import java.util.Map;

/**
 * Request DTO for generating a PDF for a specific scenario.
 * Extends the basic plan data with scenario-specific parameters.
 */
public class ScenarioPdfRequestDTO {
    private FinancialPlanDTO planData;
    private String filingStatus = "MARRIED_FILING_JOINTLY";

    // For Roth Comparison scenarios
    private Double rothConversionThreshold;

    // For Sensitivity Analysis scenarios - rate type and value to override
    private String rateType;
    private Double rateValue;

    // For State Scenario Analysis - override the states configuration
    private List<StateByYearDTO> scenarioStatesByYear;

    // Optional scenario name for PDF title
    private String scenarioName;

    public ScenarioPdfRequestDTO() {
    }

    public FinancialPlanDTO getPlanData() {
        return planData;
    }

    public void setPlanData(FinancialPlanDTO planData) {
        this.planData = planData;
    }

    public String getFilingStatus() {
        return filingStatus;
    }

    public void setFilingStatus(String filingStatus) {
        this.filingStatus = filingStatus;
    }

    public Double getRothConversionThreshold() {
        return rothConversionThreshold;
    }

    public void setRothConversionThreshold(Double rothConversionThreshold) {
        this.rothConversionThreshold = rothConversionThreshold;
    }

    public String getRateType() {
        return rateType;
    }

    public void setRateType(String rateType) {
        this.rateType = rateType;
    }

    public Double getRateValue() {
        return rateValue;
    }

    public void setRateValue(Double rateValue) {
        this.rateValue = rateValue;
    }

    public List<StateByYearDTO> getScenarioStatesByYear() {
        return scenarioStatesByYear;
    }

    public void setScenarioStatesByYear(List<StateByYearDTO> scenarioStatesByYear) {
        this.scenarioStatesByYear = scenarioStatesByYear;
    }

    public String getScenarioName() {
        return scenarioName;
    }

    public void setScenarioName(String scenarioName) {
        this.scenarioName = scenarioName;
    }
}
