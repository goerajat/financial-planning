package rg.financialplanning.web.dto;

import java.util.List;

/**
 * Response DTO for Roth conversion comparison analysis.
 */
public class RothComparisonResponseDTO {
    private int firstDataYear;
    private List<Integer> milestoneYears;
    private List<RothComparisonRowDTO> rows;

    public RothComparisonResponseDTO() {
    }

    public RothComparisonResponseDTO(int firstDataYear, List<Integer> milestoneYears, List<RothComparisonRowDTO> rows) {
        this.firstDataYear = firstDataYear;
        this.milestoneYears = milestoneYears;
        this.rows = rows;
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

    public List<RothComparisonRowDTO> getRows() {
        return rows;
    }

    public void setRows(List<RothComparisonRowDTO> rows) {
        this.rows = rows;
    }
}
