package rg.financialplanning.web.dto;

/**
 * Request DTO for Roth conversion comparison analysis.
 */
public class RothComparisonRequestDTO {
    private FinancialPlanDTO planData;
    private int yearIncrement = 5;
    private String filingStatus = "MARRIED_FILING_JOINTLY";

    public RothComparisonRequestDTO() {
    }

    public RothComparisonRequestDTO(FinancialPlanDTO planData, int yearIncrement, String filingStatus) {
        this.planData = planData;
        this.yearIncrement = yearIncrement;
        this.filingStatus = filingStatus;
    }

    public FinancialPlanDTO getPlanData() {
        return planData;
    }

    public void setPlanData(FinancialPlanDTO planData) {
        this.planData = planData;
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
