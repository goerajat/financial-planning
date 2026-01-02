package rg.financialplanning.ui.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import javafx.collections.ObservableList;
import rg.financialplanning.model.ItemType;
import rg.financialplanning.ui.model.ObservableFinancialEntry;
import rg.financialplanning.ui.model.ObservableItemTypeRate;
import rg.financialplanning.ui.model.ObservablePerson;
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
     * @param file    the file to save to
     * @param persons the list of persons
     * @param entries the list of financial entries
     * @param rates   the list of rates
     * @throws IOException if writing fails
     */
    public static void saveToFile(File file,
                                   ObservableList<ObservablePerson> persons,
                                   ObservableList<ObservableFinancialEntry> entries,
                                   ObservableList<ObservableItemTypeRate> rates) throws IOException {
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

        // Write to file
        String json = GSON.toJson(saveData);
        Files.writeString(file.toPath(), json, StandardCharsets.UTF_8);
    }

    /**
     * Loads data from a JSON file.
     *
     * @param file    the file to load from
     * @param persons the list to populate with persons
     * @param entries the list to populate with financial entries
     * @param rates   the list to update with rates
     * @throws IOException if reading fails
     */
    public static void loadFromFile(File file,
                                     ObservableList<ObservablePerson> persons,
                                     ObservableList<ObservableFinancialEntry> entries,
                                     ObservableList<ObservableItemTypeRate> rates) throws IOException {
        String json = Files.readString(file.toPath(), StandardCharsets.UTF_8);
        SaveData saveData = GSON.fromJson(json, SaveData.class);

        // Clear existing data
        persons.clear();
        entries.clear();

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
    }
}
