package rg.financialplanning.model;

/**
 * Record representing a single financial entry from the CSV file.
 * {@code annualPayment} is only meaningful for {@link ItemType#MORTGAGE}: the
 * fixed yearly payment (principal plus interest) on a balance of {@code value}.
 */
public record FinancialEntry(
    String name,
    ItemType item,
    String description,
    int value,
    int startYear,
    int endYear,
    double annualPayment
) {
    public FinancialEntry {
        if (startYear > endYear) {
            throw new IllegalArgumentException("Start year cannot be after end year");
        }
        if (value < 0) {
            throw new IllegalArgumentException("Value cannot be negative");
        }
        if (annualPayment < 0) {
            throw new IllegalArgumentException("Annual payment cannot be negative");
        }
    }

    public FinancialEntry(String name, ItemType item, String description, int value, int startYear, int endYear) {
        this(name, item, description, value, startYear, endYear, 0);
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
