package rg.financialplanning.web.dto;

public class SensitivityAnalysisRequestDTO {
    private FinancialPlanDTO planData;
    private String rateType;  // EXPENSE, QUALIFIED, NON_QUALIFIED, ROTH
    private double minRate;
    private double maxRate;
    private double rateIncrement;
    private int yearIncrement;

    public FinancialPlanDTO getPlanData() {
        return planData;
    }

    public void setPlanData(FinancialPlanDTO planData) {
        this.planData = planData;
    }

    public String getRateType() {
        return rateType != null ? rateType : "EXPENSE";
    }

    public void setRateType(String rateType) {
        this.rateType = rateType;
    }

    public double getMinRate() {
        return minRate;
    }

    public void setMinRate(double minRate) {
        this.minRate = minRate;
    }

    public double getMaxRate() {
        return maxRate;
    }

    public void setMaxRate(double maxRate) {
        this.maxRate = maxRate;
    }

    public double getRateIncrement() {
        return rateIncrement > 0 ? rateIncrement : 0.25;
    }

    public void setRateIncrement(double rateIncrement) {
        this.rateIncrement = rateIncrement;
    }

    public int getYearIncrement() {
        return yearIncrement > 0 ? yearIncrement : 5;
    }

    public void setYearIncrement(int yearIncrement) {
        this.yearIncrement = yearIncrement;
    }
}
