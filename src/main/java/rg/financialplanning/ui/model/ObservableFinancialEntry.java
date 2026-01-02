package rg.financialplanning.ui.model;

import javafx.beans.property.*;
import rg.financialplanning.model.FinancialEntry;
import rg.financialplanning.model.ItemType;

import java.time.Year;

/**
 * Observable wrapper for FinancialEntry to support JavaFX TableView binding.
 */
public class ObservableFinancialEntry {
    private final StringProperty personName = new SimpleStringProperty();
    private final ObjectProperty<ItemType> itemType = new SimpleObjectProperty<>();
    private final StringProperty description = new SimpleStringProperty();
    private final IntegerProperty value = new SimpleIntegerProperty();
    private final IntegerProperty startYear = new SimpleIntegerProperty();
    private final IntegerProperty endYear = new SimpleIntegerProperty();

    public ObservableFinancialEntry() {
        int currentYear = Year.now().getValue();
        this.personName.set("");
        this.itemType.set(ItemType.INCOME);
        this.description.set("");
        this.value.set(0);
        this.startYear.set(currentYear);
        this.endYear.set(currentYear + 30);
    }

    public ObservableFinancialEntry(String personName, ItemType itemType, String description,
                                     int value, int startYear, int endYear) {
        this.personName.set(personName);
        this.itemType.set(itemType);
        this.description.set(description);
        this.value.set(value);
        this.startYear.set(startYear);
        this.endYear.set(endYear);
    }

    public ObservableFinancialEntry(FinancialEntry entry) {
        this(entry.name(), entry.item(), entry.description(),
             entry.value(), entry.startYear(), entry.endYear());
    }

    // Person Name
    public String getPersonName() {
        return personName.get();
    }

    public void setPersonName(String personName) {
        this.personName.set(personName);
    }

    public StringProperty personNameProperty() {
        return personName;
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

    // Description
    public String getDescription() {
        return description.get();
    }

    public void setDescription(String description) {
        this.description.set(description);
    }

    public StringProperty descriptionProperty() {
        return description;
    }

    // Value
    public int getValue() {
        return value.get();
    }

    public void setValue(int value) {
        this.value.set(value);
    }

    public IntegerProperty valueProperty() {
        return value;
    }

    // Start Year
    public int getStartYear() {
        return startYear.get();
    }

    public void setStartYear(int startYear) {
        this.startYear.set(startYear);
    }

    public IntegerProperty startYearProperty() {
        return startYear;
    }

    // End Year
    public int getEndYear() {
        return endYear.get();
    }

    public void setEndYear(int endYear) {
        this.endYear.set(endYear);
    }

    public IntegerProperty endYearProperty() {
        return endYear;
    }

    public FinancialEntry toFinancialEntry() {
        return new FinancialEntry(
            getPersonName(),
            getItemType(),
            getDescription(),
            getValue(),
            getStartYear(),
            getEndYear()
        );
    }

    @Override
    public String toString() {
        return String.format("%s - %s: %s", getPersonName(), getItemType(), getDescription());
    }
}
