package rg.financialplanning.ui.model;

import rg.financialplanning.model.ItemType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * Data model for saving and loading financial plan inputs.
 * This class is designed to be serialized to/from JSON.
 */
public class SaveData {

    private List<PersonData> persons = new ArrayList<>();
    private List<EntryData> entries = new ArrayList<>();
    private Map<String, Double> rates = new HashMap<>();
    private List<StateByYearData> statesByYear = new ArrayList<>();

    public SaveData() {
    }

    public List<PersonData> getPersons() {
        return persons;
    }

    public void setPersons(List<PersonData> persons) {
        this.persons = persons;
    }

    public List<EntryData> getEntries() {
        return entries;
    }

    public void setEntries(List<EntryData> entries) {
        this.entries = entries;
    }

    public Map<String, Double> getRates() {
        return rates;
    }

    public void setRates(Map<String, Double> rates) {
        this.rates = rates;
    }

    public List<StateByYearData> getStatesByYear() {
        return statesByYear;
    }

    public void setStatesByYear(List<StateByYearData> statesByYear) {
        this.statesByYear = statesByYear;
    }

    /**
     * Simple data class for person information.
     */
    public static class PersonData {
        private String name;
        private int yearOfBirth;
        private boolean selfEmployed;

        public PersonData() {
        }

        public PersonData(String name, int yearOfBirth) {
            this(name, yearOfBirth, false);
        }

        public PersonData(String name, int yearOfBirth, boolean selfEmployed) {
            this.name = name;
            this.yearOfBirth = yearOfBirth;
            this.selfEmployed = selfEmployed;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getYearOfBirth() {
            return yearOfBirth;
        }

        public void setYearOfBirth(int yearOfBirth) {
            this.yearOfBirth = yearOfBirth;
        }

        public boolean isSelfEmployed() {
            return selfEmployed;
        }

        public void setSelfEmployed(boolean selfEmployed) {
            this.selfEmployed = selfEmployed;
        }
    }

    /**
     * Simple data class for financial entry information.
     */
    public static class EntryData {
        private String personName;
        private String itemType;
        private String description;
        private int value;
        private int startYear;
        private int endYear;

        public EntryData() {
        }

        public EntryData(String personName, String itemType, String description,
                         int value, int startYear, int endYear) {
            this.personName = personName;
            this.itemType = itemType;
            this.description = description;
            this.value = value;
            this.startYear = startYear;
            this.endYear = endYear;
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

        public void setEndYear(int endYear) {
            this.endYear = endYear;
        }
    }

    /**
     * Simple data class for state by year information.
     */
    public static class StateByYearData {
        private String state;
        private int startYear;
        private int endYear;

        public StateByYearData() {
        }

        public StateByYearData(String state, int startYear, int endYear) {
            this.state = state;
            this.startYear = startYear;
            this.endYear = endYear;
        }

        public String getState() {
            return state;
        }

        public void setState(String state) {
            this.state = state;
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

        public void setEndYear(int endYear) {
            this.endYear = endYear;
        }
    }
}
