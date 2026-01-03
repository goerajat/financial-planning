package rg.financialplanning.web.dto;

import rg.financialplanning.model.ItemType;

public class RateDTO {
    private String itemType;
    private double rate;
    private String displayName;
    private String description;

    public RateDTO() {
    }

    public RateDTO(String itemType, double rate, String displayName, String description) {
        this.itemType = itemType;
        this.rate = rate;
        this.displayName = displayName;
        this.description = description;
    }

    public static RateDTO fromItemType(ItemType type, double rate) {
        return new RateDTO(
                type.name(),
                rate,
                getDisplayName(type),
                getDescription(type)
        );
    }

    private static String getDisplayName(ItemType type) {
        return switch (type) {
            case INCOME -> "Income";
            case EXPENSE -> "Expense";
            case NON_QUALIFIED -> "Non-Qualified Assets";
            case QUALIFIED -> "Qualified Assets (401K/IRA)";
            case ROTH -> "Roth IRA";
            case CASH -> "Cash";
            case LIFE_INSURANCE_BENEFIT -> "Life Insurance Benefit";
            case REAL_ESTATE -> "Real Estate";
            case SOCIAL_SECURITY_BENEFITS -> "Social Security Benefits";
            case ROTH_CONTRIBUTION -> "Roth Contribution";
            case QUALIFIED_CONTRIBUTION -> "Qualified Contribution";
            case LIFE_INSURANCE_CONTRIBUTION -> "Life Insurance Premium";
            case MORTGAGE -> "Mortgage";
            case MORTGAGE_REPAYMENT -> "Mortgage Extra Payment";
        };
    }

    private static String getDescription(ItemType type) {
        return switch (type) {
            case INCOME -> "Annual growth rate for income (e.g., salary increases)";
            case EXPENSE -> "Annual growth rate for expenses (inflation)";
            case NON_QUALIFIED -> "Expected annual return for taxable investment accounts";
            case QUALIFIED -> "Expected annual return for tax-deferred retirement accounts";
            case ROTH -> "Expected annual return for Roth accounts";
            case CASH -> "Expected annual return for cash/savings";
            case LIFE_INSURANCE_BENEFIT -> "Growth rate for life insurance death benefit";
            case REAL_ESTATE -> "Expected annual appreciation for real estate";
            case SOCIAL_SECURITY_BENEFITS -> "Annual COLA adjustment for Social Security";
            case ROTH_CONTRIBUTION -> "Growth rate for Roth contribution amounts";
            case QUALIFIED_CONTRIBUTION -> "Growth rate for 401K/IRA contribution amounts";
            case LIFE_INSURANCE_CONTRIBUTION -> "Growth rate for insurance premiums";
            case MORTGAGE -> "Mortgage interest rate";
            case MORTGAGE_REPAYMENT -> "Not applicable (set to 0)";
        };
    }

    public String getItemType() {
        return itemType;
    }

    public void setItemType(String itemType) {
        this.itemType = itemType;
    }

    public double getRate() {
        return rate;
    }

    public void setRate(double rate) {
        this.rate = rate;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
