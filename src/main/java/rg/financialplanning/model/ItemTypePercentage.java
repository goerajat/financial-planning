package rg.financialplanning.model;

import java.util.Objects;

/**
 * Class representing a percentage increase rate for a specific ItemType.
 */
public class ItemTypePercentage {
    private final ItemType itemType;
    private final double percentageIncrease;

    public ItemTypePercentage(ItemType itemType, double percentageIncrease) {
        if (itemType == null) {
            throw new IllegalArgumentException("ItemType cannot be null");
        }
        this.itemType = itemType;
        this.percentageIncrease = percentageIncrease;
    }

    public ItemType itemType() {
        return itemType;
    }

    public double percentageIncrease() {
        return percentageIncrease;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ItemTypePercentage that = (ItemTypePercentage) o;
        return Double.compare(that.percentageIncrease, percentageIncrease) == 0 &&
                itemType == that.itemType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(itemType, percentageIncrease);
    }

    @Override
    public String toString() {
        return String.format("%s: %.2f%%", itemType, percentageIncrease);
    }
}
