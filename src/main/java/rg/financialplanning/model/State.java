package rg.financialplanning.model;

/**
 * Enumeration of supported states for tax calculations.
 */
public enum State {
    NJ("New Jersey"),
    NY("New York"),
    FL("Florida");

    private final String displayName;

    State(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
