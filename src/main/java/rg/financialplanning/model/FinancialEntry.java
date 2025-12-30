package rg.financialplanning.model;

/**
 * Record representing a single financial entry from the CSV file.
 */
public record FinancialEntry(
    String name,
    ItemType item,
    String description,
    int value,
    int startYear,
    int endYear
) {
    public FinancialEntry {
        if (startYear > endYear) {
            throw new IllegalArgumentException("Start year cannot be after end year");
        }
        if (value < 0) {
            throw new IllegalArgumentException("Value cannot be negative");
        }
    }

    public boolean isActiveInYear(int year) {
        return year >= startYear && year <= endYear;
    }

    public double getValueForYear(int year, double percentageIncrease) {
        if (!isActiveInYear(year)) {
            return 0;
        }
        int yearsFromStart = year - startYear;
        return value * Math.pow(1 + percentageIncrease / 100, yearsFromStart);
    }
}
