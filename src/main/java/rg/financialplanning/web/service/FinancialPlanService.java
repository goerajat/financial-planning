package rg.financialplanning.web.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.springframework.stereotype.Service;
import rg.financialplanning.export.FinancialPlanInputs;
import rg.financialplanning.export.PdfExporter;
import rg.financialplanning.model.FinancialEntry;
import rg.financialplanning.model.ItemType;
import rg.financialplanning.model.Person;
import rg.financialplanning.model.YearlySummary;
import rg.financialplanning.parser.FinancialDataProcessor;
import rg.financialplanning.ui.model.SaveData;
import rg.financialplanning.web.dto.FinancialEntryDTO;
import rg.financialplanning.web.dto.FinancialPlanDTO;
import rg.financialplanning.web.dto.PersonDTO;
import rg.financialplanning.web.dto.RateDTO;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class FinancialPlanService {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    // Default rates for each item type
    private static final Map<ItemType, Double> DEFAULT_RATES = new LinkedHashMap<>();

    static {
        DEFAULT_RATES.put(ItemType.INCOME, 3.0);
        DEFAULT_RATES.put(ItemType.EXPENSE, 3.0);
        DEFAULT_RATES.put(ItemType.NON_QUALIFIED, 6.0);
        DEFAULT_RATES.put(ItemType.QUALIFIED, 6.0);
        DEFAULT_RATES.put(ItemType.ROTH, 6.0);
        DEFAULT_RATES.put(ItemType.CASH, 2.0);
        DEFAULT_RATES.put(ItemType.REAL_ESTATE, 3.0);
        DEFAULT_RATES.put(ItemType.LIFE_INSURANCE_BENEFIT, 0.0);
        DEFAULT_RATES.put(ItemType.SOCIAL_SECURITY_BENEFITS, 2.0);
        DEFAULT_RATES.put(ItemType.ROTH_CONTRIBUTION, 0.0);
        DEFAULT_RATES.put(ItemType.QUALIFIED_CONTRIBUTION, 0.0);
        DEFAULT_RATES.put(ItemType.LIFE_INSURANCE_CONTRIBUTION, 0.0);
        DEFAULT_RATES.put(ItemType.MORTGAGE, 6.5);
        DEFAULT_RATES.put(ItemType.MORTGAGE_REPAYMENT, 0.0);
    }

    public List<RateDTO> getDefaultRates() {
        return DEFAULT_RATES.entrySet().stream()
                .map(entry -> RateDTO.fromItemType(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
    }

    public byte[] generatePdf(FinancialPlanDTO planData) throws IOException {
        // Convert DTOs to domain models
        List<FinancialEntry> entries = planData.getEntries().stream()
                .map(FinancialEntryDTO::toEntry)
                .collect(Collectors.toList());

        List<Person> persons = planData.getPersons().stream()
                .map(PersonDTO::toPerson)
                .collect(Collectors.toList());

        Map<String, Person> personsByName = persons.stream()
                .collect(Collectors.toMap(Person::name, p -> p, (a, b) -> a));

        Map<ItemType, Double> rates = convertRates(planData.getRates());

        // Use existing FinancialDataProcessor
        FinancialDataProcessor processor = new FinancialDataProcessor();
        processor.setEntries(entries);
        YearlySummary[] summaries = processor.generateYearlySummaries(rates, personsByName);

        // Generate PDF to temp file, then read bytes
        Path tempFile = Files.createTempFile("financial_plan_", ".pdf");
        try {
            FinancialPlanInputs inputs = new FinancialPlanInputs(entries, persons, rates);
            PdfExporter exporter = new PdfExporter();
            exporter.exportYearlySummariesToPdf(summaries, inputs, tempFile.toString());
            return Files.readAllBytes(tempFile);
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    public String exportAsJson(FinancialPlanDTO planData) {
        SaveData saveData = convertToSaveData(planData);
        return GSON.toJson(saveData);
    }

    public FinancialPlanDTO importFromJson(String json) {
        SaveData saveData = GSON.fromJson(json, SaveData.class);
        return convertFromSaveData(saveData);
    }

    private Map<ItemType, Double> convertRates(Map<String, Double> stringRates) {
        Map<ItemType, Double> rates = new HashMap<>();
        for (Map.Entry<String, Double> entry : stringRates.entrySet()) {
            try {
                ItemType type = ItemType.valueOf(entry.getKey());
                rates.put(type, entry.getValue());
            } catch (IllegalArgumentException e) {
                // Skip invalid item types
            }
        }
        // Fill in any missing rates with defaults
        for (Map.Entry<ItemType, Double> defaultEntry : DEFAULT_RATES.entrySet()) {
            rates.putIfAbsent(defaultEntry.getKey(), defaultEntry.getValue());
        }
        return rates;
    }

    private SaveData convertToSaveData(FinancialPlanDTO planData) {
        SaveData saveData = new SaveData();

        // Convert persons
        List<SaveData.PersonData> personDataList = planData.getPersons().stream()
                .map(p -> new SaveData.PersonData(p.getName(), p.getYearOfBirth()))
                .collect(Collectors.toList());
        saveData.setPersons(personDataList);

        // Convert entries
        List<SaveData.EntryData> entryDataList = planData.getEntries().stream()
                .map(e -> new SaveData.EntryData(
                        e.getPersonName(),
                        e.getItemType(),
                        e.getDescription(),
                        e.getValue(),
                        e.getStartYear(),
                        e.getEndYear()))
                .collect(Collectors.toList());
        saveData.setEntries(entryDataList);

        // Convert rates
        saveData.setRates(new HashMap<>(planData.getRates()));

        return saveData;
    }

    private FinancialPlanDTO convertFromSaveData(SaveData saveData) {
        FinancialPlanDTO dto = new FinancialPlanDTO();

        // Convert persons
        if (saveData.getPersons() != null) {
            List<PersonDTO> persons = saveData.getPersons().stream()
                    .map(p -> new PersonDTO(p.getName(), p.getYearOfBirth()))
                    .collect(Collectors.toList());
            dto.setPersons(persons);
        }

        // Convert entries
        if (saveData.getEntries() != null) {
            List<FinancialEntryDTO> entries = new ArrayList<>();
            long id = 1;
            for (SaveData.EntryData e : saveData.getEntries()) {
                FinancialEntryDTO entryDTO = new FinancialEntryDTO(
                        e.getPersonName(),
                        e.getItemType(),
                        e.getDescription(),
                        e.getValue(),
                        e.getStartYear(),
                        e.getEndYear());
                entryDTO.setId(id++);
                entries.add(entryDTO);
            }
            dto.setEntries(entries);
        }

        // Convert rates
        if (saveData.getRates() != null) {
            dto.setRates(new HashMap<>(saveData.getRates()));
        }

        return dto;
    }
}
