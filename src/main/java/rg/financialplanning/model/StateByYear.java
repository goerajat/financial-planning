package rg.financialplanning.model;

/**
 * Represents a state tax jurisdiction for a specific year range.
 * This allows modeling state tax changes over time (e.g., moving from one state to another).
 */
public record StateByYear(State state, int startYear, int endYear) {

    /**
     * Creates a StateByYear record.
     *
     * @param state     the state for tax calculations
     * @param startYear the first year this state applies (inclusive)
     * @param endYear   the last year this state applies (inclusive)
     */
    public StateByYear {
        if (state == null) {
            throw new IllegalArgumentException("State cannot be null");
        }
        if (startYear > endYear) {
            throw new IllegalArgumentException("Start year cannot be after end year: " + startYear + " > " + endYear);
        }
    }

    /**
     * Checks if this state configuration applies to the given year.
     *
     * @param year the year to check
     * @return true if the year falls within the range [startYear, endYear]
     */
    public boolean appliesTo(int year) {
        return year >= startYear && year <= endYear;
    }
}
