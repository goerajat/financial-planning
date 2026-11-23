package rg.financialplanning.web.dto;

import rg.financialplanning.model.FinancialEntry;
import rg.financialplanning.model.ItemType;

public class FinancialEntryDTO {
    private Long id;
    private String personName;
    private String itemType;
    private String description;
    private int value;
    private int startYear;
    private int endYear;
    private String source;
    /** MORTGAGE only: fixed yearly payment, principal plus interest. */
    private double annualPayment;

    public FinancialEntryDTO() {
    }

    public FinancialEntryDTO(String personName, String itemType, String description,
                             int value, int startYear, int endYear) {
        this.personName = personName;
        this.itemType = itemType;
        this.description = description;
        this.value = value;
        this.startYear = startYear;
        this.endYear = endYear;
    }

    public static FinancialEntryDTO fromEntry(FinancialEntry entry, Long id) {
        FinancialEntryDTO dto = new FinancialEntryDTO(
                entry.name(),
                entry.item().name(),
                entry.description(),
                entry.value(),
                entry.startYear(),
                entry.endYear()
        );
        dto.setId(id);
        dto.setAnnualPayment(entry.annualPayment());
        return dto;
    }

    public FinancialEntry toEntry() {
        ItemType type;
        try {
            type = ItemType.valueOf(itemType);
        } catch (IllegalArgumentException e) {
            type = ItemType.fromString(itemType);
        }
        return new FinancialEntry(personName, type, description, value, startYear, endYear, annualPayment);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPersonName() {
        return personName;
    }

    public void setPersonName(String personName) {
        this.personName = personName;
    }

    public String getItemType() {
        return itemType;
    }

    public void setItemType(String itemType) {
        this.itemType = itemType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }

    public int getStartYear() {
        return startYear;
    }

    public void setStartYear(int startYear) {
        this.startYear = startYear;
    }

    public int getEndYear() {
        return endYear;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public void setEndYear(int endYear) {
        this.endYear = endYear;
    }

    public double getAnnualPayment() {
        return annualPayment;
    }

    public void setAnnualPayment(double annualPayment) {
        this.annualPayment = annualPayment;
    }
}
