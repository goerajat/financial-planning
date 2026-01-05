package rg.financialplanning.ui.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import javafx.collections.ObservableList;
import rg.financialplanning.model.ItemType;
import rg.financialplanning.model.State;
import rg.financialplanning.ui.model.ObservableFinancialEntry;
import rg.financialplanning.ui.model.ObservableItemTypeRate;
import rg.financialplanning.ui.model.ObservablePerson;
import rg.financialplanning.ui.model.ObservableStateByYear;
import rg.financialplanning.ui.model.SaveData;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Utility class for saving and loading financial plan data to/from JSON files.
 */
public class DataPersistence {

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .create();

    /**
     * Saves the current data to a JSON file.
     *
     * @param file         the file to save to
     * @param persons      the list of persons
     * @param entries      the list of financial entries
     * @param rates        the list of rates
     * @param statesByYear the list of state configurations by year
     * @throws IOException if writing fails
     */
    public static void saveToFile(File file,
                                   ObservableList<ObservablePerson> persons,
                                   ObservableList<ObservableFinancialEntry> entries,
                                   ObservableList<ObservableItemTypeRate> rates,
                                   ObservableList<ObservableStateByYear> statesByYear) throws IOException {
        SaveData saveData = new SaveData();

        // Convert persons
        List<SaveData.PersonData> personDataList = new ArrayList<>();
        for (ObservablePerson person : persons) {
            personDataList.add(new SaveData.PersonData(person.getName(), person.getYearOfBirth()));
        }
        saveData.setPersons(personDataList);

        // Convert entries
        List<SaveData.EntryData> entryDataList = new ArrayList<>();
        for (ObservableFinancialEntry entry : entries) {
            entryDataList.add(new SaveData.EntryData(
                    entry.getPersonName(),
                    entry.getItemType().name(),
                    entry.getDescription(),
                    entry.getValue(),
                    entry.getStartYear(),
                    entry.getEndYear()
            ));
        }
        saveData.setEntries(entryDataList);

        // Convert rates
        Map<String, Double> rateMap = new HashMap<>();
        for (ObservableItemTypeRate rate : rates) {
            rateMap.put(rate.getItemType().name(), rate.getRate());
        }
        saveData.setRates(rateMap);

        // Convert states by year
        List<SaveData.StateByYearData> statesByYearDataList = new ArrayList<>();
        for (ObservableStateByYear stateByYear : statesByYear) {
            statesByYearDataList.add(new SaveData.StateByYearData(
                    stateByYear.getState().name(),
                    stateByYear.getStartYear(),
                    stateByYear.getEndYear()
            ));
        }
        saveData.setStatesByYear(statesByYearDataList);

        // Write to file
        String json = GSON.toJson(saveData);
        Files.writeString(file.toPath(), json, StandardCharsets.UTF_8);
    }

    /**
     * Loads data from a JSON file.
     *
     * @param file         the file to load from
     * @param persons      the list to populate with persons
     * @param entries      the list to populate with financial entries
     * @param rates        the list to update with rates
     * @param statesByYear the list to populate with state configurations
     * @throws IOException if reading fails
     */
    public static void loadFromFile(File file,
                                     ObservableList<ObservablePerson> persons,
                                     ObservableList<ObservableFinancialEntry> entries,
                                     ObservableList<ObservableItemTypeRate> rates,
                                     ObservableList<ObservableStateByYear> statesByYear) throws IOException {
        String json = Files.readString(file.toPath(), StandardCharsets.UTF_8);
        SaveData saveData = GSON.fromJson(json, SaveData.class);

        // Clear existing data
        persons.clear();
        entries.clear();
        statesByYear.clear();

        // Load persons
        if (saveData.getPersons() != null) {
            for (SaveData.PersonData personData : saveData.getPersons()) {
                persons.add(new ObservablePerson(personData.getName(), personData.getYearOfBirth()));
            }
        }

        // Load entries
        if (saveData.getEntries() != null) {
            for (SaveData.EntryData entryData : saveData.getEntries()) {
                ItemType itemType;
                try {
                    itemType = ItemType.valueOf(entryData.getItemType());
                } catch (IllegalArgumentException e) {
                    // Try to parse using the fromString method for compatibility
                    itemType = ItemType.fromString(entryData.getItemType());
                }
                entries.add(new ObservableFinancialEntry(
                        entryData.getPersonName(),
                        itemType,
                        entryData.getDescription(),
                        entryData.getValue(),
                        entryData.getStartYear(),
                        entryData.getEndYear()
                ));
            }
        }

        // Load rates (update existing rates, don't replace the list)
        if (saveData.getRates() != null) {
            for (ObservableItemTypeRate rate : rates) {
                String key = rate.getItemType().name();
                if (saveData.getRates().containsKey(key)) {
                    rate.setRate(saveData.getRates().get(key));
                }
            }
        }

        // Load states by year
        if (saveData.getStatesByYear() != null) {
            for (SaveData.StateByYearData stateByYearData : saveData.getStatesByYear()) {
                try {
                    State state = State.valueOf(stateByYearData.getState());
                    statesByYear.add(new ObservableStateByYear(
                            state,
                            stateByYearData.getStartYear(),
                            stateByYearData.getEndYear()
                    ));
                } catch (IllegalArgumentException e) {
                    // Skip invalid states
                }
            }
        }
    }
}
