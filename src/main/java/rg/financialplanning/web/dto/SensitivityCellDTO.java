package rg.financialplanning.web.dto;

public class SensitivityCellDTO {
    private int year;
    private Double netWorth;
    private boolean hasShortfall;
    private Integer shortfallYear;

    public SensitivityCellDTO() {
    }

    public SensitivityCellDTO(int year, Double netWorth, boolean hasShortfall, Integer shortfallYear) {
        this.year = year;
        this.netWorth = netWorth;
        this.hasShortfall = hasShortfall;
        this.shortfallYear = shortfallYear;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public Double getNetWorth() {
        return netWorth;
    }

    public void setNetWorth(Double netWorth) {
        this.netWorth = netWorth;
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
