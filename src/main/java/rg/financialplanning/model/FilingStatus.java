package rg.financialplanning.model;

/**
 * Enum representing federal tax filing status.
 */
public enum FilingStatus {
    SINGLE,
    MARRIED_FILING_JOINTLY,
    MARRIED_FILING_SEPARATELY,
    HEAD_OF_HOUSEHOLD;

    public static FilingStatus fromString(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Filing status cannot be null or empty");
        }
        return switch (value.trim().toUpperCase().replace(" ", "_")) {
            case "SINGLE" -> SINGLE;
            case "MARRIED_FILING_JOINTLY", "MFJ" -> MARRIED_FILING_JOINTLY;
            case "MARRIED_FILING_SEPARATELY", "MFS" -> MARRIED_FILING_SEPARATELY;
            case "HEAD_OF_HOUSEHOLD", "HOH" -> HEAD_OF_HOUSEHOLD;
            default -> throw new IllegalArgumentException("Unknown filing status: " + value);
        };
    }
}
