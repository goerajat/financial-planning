package rg.financialplanning.model;

/**
 * Enum representing the type of financial item.
 */
public enum ItemType {
    INCOME,
    EXPENSE,
    NON_QUALIFIED,
    QUALIFIED,
    ROTH,
    CASH,
    LIFE_INSURANCE_BENEFIT,
    REAL_ESTATE,
    SOCIAL_SECURITY_BENEFITS;

    public static ItemType fromString(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Item type cannot be null or empty");
        }
        return switch (value.trim().toUpperCase()) {
            case "INCOME" -> INCOME;
            case "EXPENSE" -> EXPENSE;
            case "NON_QUALIFIED", "NONQUALIFIED", "ASSET" -> NON_QUALIFIED;
            case "QUALIFIED", "401K" -> QUALIFIED;
            case "ROTH" -> ROTH;
            case "CASH" -> CASH;
            case "LIFE_INSURANCE_BENEFIT", "LIFE INSURANCE BENEFIT" -> LIFE_INSURANCE_BENEFIT;
            case "REAL_ESTATE", "REAL ESTATE" -> REAL_ESTATE;
            case "SOCIAL_SECURITY_BENEFITS", "SOCIAL SECURITY BENEFITS", "SOCIAL_SECURITY", "SSA" -> SOCIAL_SECURITY_BENEFITS;
            default -> throw new IllegalArgumentException("Unknown item type: " + value);
        };
    }
}
