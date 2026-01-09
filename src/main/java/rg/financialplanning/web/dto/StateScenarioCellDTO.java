package rg.financialplanning.web.dto;

/**
 * A cell in the state scenario comparison table.
 */
public class StateScenarioCellDTO {
    private int year;
    private double netWorth;
    private double netWorthDelta;
    private double cumulativeTaxes;
    private double cumulativeStateTaxes;
    private double stateTaxSavings;
    private String activeState;
    private boolean hasShortfall;
    private Integer shortfallYear;

    public StateScenarioCellDTO() {
    }

    public StateScenarioCellDTO(int year, double netWorth, double netWorthDelta,
                                 double cumulativeTaxes, double cumulativeStateTaxes,
                                 double stateTaxSavings, String activeState,
                                 boolean hasShortfall, Integer shortfallYear) {
        this.year = year;
        this.netWorth = netWorth;
        this.netWorthDelta = netWorthDelta;
        this.cumulativeTaxes = cumulativeTaxes;
        this.cumulativeStateTaxes = cumulativeStateTaxes;
        this.stateTaxSavings = stateTaxSavings;
        this.activeState = activeState;
        this.hasShortfall = hasShortfall;
        this.shortfallYear = shortfallYear;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public double getNetWorth() {
        return netWorth;
    }

    public void setNetWorth(double netWorth) {
        this.netWorth = netWorth;
    }

    public double getNetWorthDelta() {
        return netWorthDelta;
    }

    public void setNetWorthDelta(double netWorthDelta) {
        this.netWorthDelta = netWorthDelta;
    }

    public double getCumulativeTaxes() {
        return cumulativeTaxes;
    }

    public void setCumulativeTaxes(double cumulativeTaxes) {
        this.cumulativeTaxes = cumulativeTaxes;
    }

    public double getCumulativeStateTaxes() {
        return cumulativeStateTaxes;
    }

    public void setCumulativeStateTaxes(double cumulativeStateTaxes) {
        this.cumulativeStateTaxes = cumulativeStateTaxes;
    }

    public double getStateTaxSavings() {
        return stateTaxSavings;
    }

    public void setStateTaxSavings(double stateTaxSavings) {
        this.stateTaxSavings = stateTaxSavings;
    }

    public String getActiveState() {
        return activeState;
    }

    public void setActiveState(String activeState) {
        this.activeState = activeState;
    }

    public boolean isHasShortfall() {
        return hasShortfall;
    }

    public void setHasShortfall(boolean hasShortfall) {
        this.hasShortfall = hasShortfall;
    }

    public Integer getShortfallYear() {
        return shortfallYear;
    }

    public void setShortfallYear(Integer shortfallYear) {
        this.shortfallYear = shortfallYear;
    }
}
