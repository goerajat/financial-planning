package rg.financialplanning.web.dto;

import java.util.List;

/**
 * Response DTO for state residence scenario comparison analysis.
 */
public class StateScenarioResponseDTO {
    private int firstDataYear;
    private List<Integer> milestoneYears;
    private List<StateScenarioRowDTO> rows;

    public StateScenarioResponseDTO() {
    }

    public StateScenarioResponseDTO(int firstDataYear, List<Integer> milestoneYears, List<StateScenarioRowDTO> rows) {
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

    public List<StateScenarioRowDTO> getRows() {
        return rows;
    }

    public void setRows(List<StateScenarioRowDTO> rows) {
        this.rows = rows;
    }
}
