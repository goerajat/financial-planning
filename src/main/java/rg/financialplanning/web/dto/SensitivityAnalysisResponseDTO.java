package rg.financialplanning.web.dto;

import java.util.List;

public class SensitivityAnalysisResponseDTO {
    private String rateType;
    private String rateTypeLabel;
    private int firstDataYear;
    private List<Integer> milestoneYears;
    private List<SensitivityRowDTO> rows;

    public String getRateType() {
        return rateType;
    }

    public void setRateType(String rateType) {
        this.rateType = rateType;
    }

    public String getRateTypeLabel() {
        return rateTypeLabel;
    }

    public void setRateTypeLabel(String rateTypeLabel) {
        this.rateTypeLabel = rateTypeLabel;
    }

    public int getFirstDataYear() {
        return firstDataYear;
    }

    public void setFirstDataYear(int firstDataYear) {
        this.firstDataYear = firstDataYear;
    }

    public List<Integer> getMilestoneYears() {
        return milestoneYears;
    }

    public void setMilestoneYears(List<Integer> milestoneYears) {
        this.milestoneYears = milestoneYears;
    }

    public List<SensitivityRowDTO> getRows() {
        return rows;
    }

    public void setRows(List<SensitivityRowDTO> rows) {
        this.rows = rows;
    }
}
