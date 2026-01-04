package rg.financialplanning.web.dto;

import java.util.List;

/**
 * A row in the Roth conversion comparison table, representing one scenario.
 */
public class RothComparisonRowDTO {
    private String scenarioName;
    private Double bracketThreshold;
    private List<RothComparisonCellDTO> cells;

    public RothComparisonRowDTO() {
    }

    public RothComparisonRowDTO(String scenarioName, Double bracketThreshold, List<RothComparisonCellDTO> cells) {
        this.scenarioName = scenarioName;
        this.bracketThreshold = bracketThreshold;
        this.cells = cells;
    }

    public String getScenarioName() {
        return scenarioName;
    }

    public void setScenarioName(String scenarioName) {
        this.scenarioName = scenarioName;
    }

    public Double getBracketThreshold() {
        return bracketThreshold;
    }

    public void setBracketThreshold(Double bracketThreshold) {
        this.bracketThreshold = bracketThreshold;
    }

    public List<RothComparisonCellDTO> getCells() {
        return cells;
    }

    public void setCells(List<RothComparisonCellDTO> cells) {
        this.cells = cells;
    }
}
