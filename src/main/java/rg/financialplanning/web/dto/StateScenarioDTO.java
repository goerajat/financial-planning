package rg.financialplanning.web.dto;

import java.util.List;

/**
 * Data Transfer Object for a state residence scenario.
 * Contains a named scenario with a list of state configurations by year.
 */
public class StateScenarioDTO {
    private String name;
    private List<StateByYearDTO> statesByYear;

    public StateScenarioDTO() {
    }

    public StateScenarioDTO(String name, List<StateByYearDTO> statesByYear) {
        this.name = name;
        this.statesByYear = statesByYear;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<StateByYearDTO> getStatesByYear() {
        return statesByYear;
    }

    public void setStatesByYear(List<StateByYearDTO> statesByYear) {
        this.statesByYear = statesByYear;
    }
}
