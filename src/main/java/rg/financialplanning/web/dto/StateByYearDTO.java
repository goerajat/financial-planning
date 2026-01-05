package rg.financialplanning.web.dto;

/**
 * Data Transfer Object for state tax configuration by year.
 */
public class StateByYearDTO {
    private String state;
    private int startYear;
    private int endYear;

    public StateByYearDTO() {
    }

    public StateByYearDTO(String state, int startYear, int endYear) {
        this.state = state;
        this.startYear = startYear;
        this.endYear = endYear;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public int getStartYear() {
        return startYear;
    }

    public void setStartYear(int startYear) {
        this.startYear = startYear;
    }

    public int getEndYear() {
        return endYear;
    }

    public void setEndYear(int endYear) {
        this.endYear = endYear;
    }
}
