package rg.financialplanning.ui.model;

import javafx.beans.property.*;
import rg.financialplanning.model.ItemType;

/**
 * Observable wrapper for ItemType rate configuration to support JavaFX TableView binding.
 */
public class ObservableItemTypeRate {
    private final ObjectProperty<ItemType> itemType = new SimpleObjectProperty<>();
    private final DoubleProperty rate = new SimpleDoubleProperty();

    public ObservableItemTypeRate(ItemType itemType, double rate) {
        this.itemType.set(itemType);
        this.rate.set(rate);
    }

    // Item Type
    public ItemType getItemType() {
        return itemType.get();
    }

    public void setItemType(ItemType itemType) {
        this.itemType.set(itemType);
    }

    public ObjectProperty<ItemType> itemTypeProperty() {
        return itemType;
    }

    // Rate
    public double getRate() {
        return rate.get();
    }

    public void setRate(double rate) {
        this.rate.set(rate);
    }

    public DoubleProperty rateProperty() {
        return rate;
    }

    /**
     * Returns a friendly display name for the item type.
     */
    public String getDisplayName() {
        return switch (getItemType()) {
            case INCOME -> "Income";
            case EXPENSE -> "Expense";
            case NON_QUALIFIED -> "Non-Qualified Assets";
            case QUALIFIED -> "Qualified Assets (401K, IRA)";
            case ROTH -> "Roth IRA";
            case CASH -> "Cash";
            case LIFE_INSURANCE_BENEFIT -> "Life Insurance Benefit";
            case REAL_ESTATE -> "Real Estate";
            case SOCIAL_SECURITY_BENEFITS -> "Social Security Benefits";
            case ROTH_CONTRIBUTION -> "Roth Contribution";
            case QUALIFIED_CONTRIBUTION -> "Qualified Contribution";
            case LIFE_INSURANCE_CONTRIBUTION -> "Life Insurance Contribution";
            case MORTGAGE -> "Mortgage Interest Rate";
            case MORTGAGE_REPAYMENT -> "Mortgage Repayment";
        };
    }

    @Override
    public String toString() {
        return String.format("%s: %.2f%%", getDisplayName(), getRate());
    }
}
