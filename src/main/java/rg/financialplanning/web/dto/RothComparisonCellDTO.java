package rg.financialplanning.web.dto;

/**
 * A cell in the Roth conversion comparison table.
 */
public class RothComparisonCellDTO {
    private int year;
    private double netWorth;
    private double netWorthDelta;
    private double cumulativeTaxes;
    private double rothBalance;
    private boolean hasShortfall;
    private Integer shortfallYear;

    public RothComparisonCellDTO() {
    }

    public RothComparisonCellDTO(int year, double netWorth, double netWorthDelta,
                                  double cumulativeTaxes, double rothBalance,
                                  boolean hasShortfall, Integer shortfallYear) {
        this.year = year;
        this.netWorth = netWorth;
        this.netWorthDelta = netWorthDelta;
        this.cumulativeTaxes = cumulativeTaxes;
        this.rothBalance = rothBalance;
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

    public double getRothBalance() {
        return rothBalance;
    }

    public void setRothBalance(double rothBalance) {
        this.rothBalance = rothBalance;
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
