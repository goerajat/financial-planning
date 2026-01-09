package rg.financialplanning.web.dto;

import java.util.List;

/**
 * A row in the state scenario comparison table, representing one scenario.
 */
public class StateScenarioRowDTO {
    private String scenarioName;
    private String scenarioDescription;
    private List<StateScenarioCellDTO> cells;

    public StateScenarioRowDTO() {
    }

    public StateScenarioRowDTO(String scenarioName, String scenarioDescription, List<StateScenarioCellDTO> cells) {
        this.scenarioName = scenarioName;
        this.scenarioDescription = scenarioDescription;
        this.cells = cells;
    }

    public String getScenarioName() {
        return scenarioName;
    }

    public void setScenarioName(String scenarioName) {
        this.scenarioName = scenarioName;
    }

    public String getScenarioDescription() {
        return scenarioDescription;
    }

    public void setScenarioDescription(String scenarioDescription) {
        this.scenarioDescription = scenarioDescription;
    }

    public List<StateScenarioCellDTO> getCells() {
        return cells;
    }

    public void setCells(List<StateScenarioCellDTO> cells) {
        this.cells = cells;
    }
}
