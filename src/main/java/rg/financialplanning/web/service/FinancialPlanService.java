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
import rg.financialplanning.web.dto.SensitivityAnalysisRequestDTO;
import rg.financialplanning.web.dto.SensitivityAnalysisResponseDTO;
import rg.financialplanning.web.dto.SensitivityRowDTO;
import rg.financialplanning.web.dto.SensitivityCellDTO;
import rg.financialplanning.web.dto.RothComparisonRequestDTO;
import rg.financialplanning.web.dto.RothComparisonResponseDTO;
import rg.financialplanning.web.dto.RothComparisonRowDTO;
import rg.financialplanning.web.dto.RothComparisonCellDTO;
import rg.financialplanning.model.FilingStatus;
import rg.financialplanning.model.State;
import rg.financialplanning.model.StateByYear;
import rg.financialplanning.strategy.CompositeTaxOptimizationStrategy;
import rg.financialplanning.strategy.NoOpRothConversionStrategy;
import rg.financialplanning.strategy.RMDOptimizationStrategy;
import rg.financialplanning.strategy.ExpenseManagementStrategy;
import rg.financialplanning.strategy.RothConversionOptimizationStrategy;
import rg.financialplanning.strategy.TaxCalculationStrategy;
import rg.financialplanning.web.dto.StateByYearDTO;

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

    private List<StateByYear> convertStatesByYear(List<StateByYearDTO> statesByYearDTO) {
        if (statesByYearDTO == null || statesByYearDTO.isEmpty()) {
            return List.of();
        }

        List<StateByYear> statesByYear = new ArrayList<>();
        for (StateByYearDTO dto : statesByYearDTO) {
            try {
                State state = State.valueOf(dto.getState());
                statesByYear.add(new StateByYear(state, dto.getStartYear(), dto.getEndYear()));
            } catch (IllegalArgumentException e) {
                // Skip invalid states
            }
        }
        return statesByYear;
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

    public SensitivityAnalysisResponseDTO runSensitivityAnalysis(SensitivityAnalysisRequestDTO request) {
        FinancialPlanDTO planData = request.getPlanData();
        String rateTypeStr = request.getRateType();
        double minRate = request.getMinRate();
        double maxRate = request.getMaxRate();
        double rateIncrement = request.getRateIncrement();
        int yearIncrement = request.getYearIncrement();

        // Determine which ItemType(s) to vary based on rateType
        List<ItemType> targetItemTypes;
        String rateTypeLabel;
        switch (rateTypeStr.toUpperCase()) {
            case "ALL_ASSETS":
                targetItemTypes = List.of(ItemType.QUALIFIED, ItemType.NON_QUALIFIED, ItemType.ROTH);
                rateTypeLabel = "All Asset Growth";
                break;
            case "QUALIFIED":
                targetItemTypes = List.of(ItemType.QUALIFIED);
                rateTypeLabel = "Qualified Asset Growth";
                break;
            case "NON_QUALIFIED":
                targetItemTypes = List.of(ItemType.NON_QUALIFIED);
                rateTypeLabel = "Non-Qualified Asset Growth";
                break;
            case "ROTH":
                targetItemTypes = List.of(ItemType.ROTH);
                rateTypeLabel = "Roth Asset Growth";
                break;
            case "EXPENSE":
            default:
                targetItemTypes = List.of(ItemType.EXPENSE);
                rateTypeLabel = "Expense Growth";
                break;
        }

        // Convert DTOs to domain models
        List<FinancialEntry> entries = planData.getEntries().stream()
                .map(FinancialEntryDTO::toEntry)
                .collect(Collectors.toList());

        List<Person> persons = planData.getPersons().stream()
                .map(PersonDTO::toPerson)
                .collect(Collectors.toList());

        Map<String, Person> personsByName = persons.stream()
                .collect(Collectors.toMap(Person::name, p -> p, (a, b) -> a));

        Map<ItemType, Double> baseRates = convertRates(planData.getRates());

        // Setup processor to find year range
        FinancialDataProcessor processor = new FinancialDataProcessor();
        processor.setEntries(entries);

        int firstDataYear = processor.getEarliestStartYear();
        int lastDataYear = processor.getLatestEndYear();

        // Calculate milestone years (Year 5, Year 10, etc. from first data year)
        List<Integer> milestoneYears = new ArrayList<>();
        for (int year = firstDataYear + yearIncrement; year <= lastDataYear; year += yearIncrement) {
            milestoneYears.add(year);
        }
        // Always include the final year if not already included
        if (milestoneYears.isEmpty() || milestoneYears.get(milestoneYears.size() - 1) != lastDataYear) {
            milestoneYears.add(lastDataYear);
        }

        // Generate rows for each rate value
        List<SensitivityRowDTO> rows = new ArrayList<>();
        for (double rateValue = minRate; rateValue <= maxRate + 0.001; rateValue += rateIncrement) {
            // Round to avoid floating point issues
            double roundedRate = Math.round(rateValue * 100.0) / 100.0;

            // Clone rates and override the target rate type(s)
            Map<ItemType, Double> rates = new HashMap<>(baseRates);
            for (ItemType targetType : targetItemTypes) {
                rates.put(targetType, roundedRate);
            }

            // Generate summaries with this rate
            processor.setEntries(entries);
            YearlySummary[] summaries = processor.generateYearlySummaries(rates, personsByName);

            // Track first shortfall year
            Integer firstShortfallYear = null;
            for (YearlySummary summary : summaries) {
                if (summary.deficit() > 0) {
                    firstShortfallYear = summary.year();
                    break;
                }
            }

            // Build cells for each milestone
            List<SensitivityCellDTO> cells = new ArrayList<>();
            for (int milestoneYear : milestoneYears) {
                int index = milestoneYear - firstDataYear;
                if (index >= 0 && index < summaries.length) {
                    YearlySummary summary = summaries[index];

                    // Check if any year up to this milestone had a shortfall
                    boolean hasShortfall = firstShortfallYear != null && firstShortfallYear <= milestoneYear;

                    if (hasShortfall) {
                        cells.add(new SensitivityCellDTO(milestoneYear, null, true, firstShortfallYear));
                    } else {
                        cells.add(new SensitivityCellDTO(milestoneYear, summary.netWorth(), false, null));
                    }
                }
            }

            rows.add(new SensitivityRowDTO(roundedRate, cells));
        }

        SensitivityAnalysisResponseDTO response = new SensitivityAnalysisResponseDTO();
        response.setRateType(rateTypeStr);
        response.setRateTypeLabel(rateTypeLabel);
        response.setFirstDataYear(firstDataYear);
        response.setMilestoneYears(milestoneYears);
        response.setRows(rows);
        return response;
    }

    public RothComparisonResponseDTO runRothConversionComparison(RothComparisonRequestDTO request) {
        FinancialPlanDTO planData = request.getPlanData();
        int yearIncrement = request.getYearIncrement();

        // Parse filing status
        FilingStatus filingStatus;
        try {
            filingStatus = FilingStatus.valueOf(request.getFilingStatus());
        } catch (IllegalArgumentException e) {
            filingStatus = FilingStatus.MARRIED_FILING_JOINTLY;
        }

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

        // Setup processor to find year range
        FinancialDataProcessor processor = new FinancialDataProcessor();
        processor.setEntries(entries);

        int firstDataYear = processor.getEarliestStartYear();
        int lastDataYear = processor.getLatestEndYear();

        // Calculate milestone years
        List<Integer> milestoneYears = new ArrayList<>();
        for (int year = firstDataYear + yearIncrement; year <= lastDataYear; year += yearIncrement) {
            milestoneYears.add(year);
        }
        if (milestoneYears.isEmpty() || milestoneYears.get(milestoneYears.size() - 1) != lastDataYear) {
            milestoneYears.add(lastDataYear);
        }

        // Define scenarios with bracket thresholds
        double bracket12 = filingStatus == FilingStatus.SINGLE || filingStatus == FilingStatus.MARRIED_FILING_SEPARATELY
                ? RothConversionOptimizationStrategy.SINGLE_12_PERCENT_BRACKET
                : RothConversionOptimizationStrategy.MFJ_12_PERCENT_BRACKET;
        double bracket22 = filingStatus == FilingStatus.SINGLE || filingStatus == FilingStatus.MARRIED_FILING_SEPARATELY
                ? RothConversionOptimizationStrategy.SINGLE_22_PERCENT_BRACKET
                : RothConversionOptimizationStrategy.MFJ_22_PERCENT_BRACKET;
        double bracket24 = filingStatus == FilingStatus.SINGLE || filingStatus == FilingStatus.MARRIED_FILING_SEPARATELY
                ? RothConversionOptimizationStrategy.SINGLE_24_PERCENT_BRACKET
                : RothConversionOptimizationStrategy.MFJ_24_PERCENT_BRACKET;

        // Convert states by year
        List<StateByYear> statesByYear = convertStatesByYear(planData.getStatesByYear());

        // Generate baseline summaries (no conversion)
        FinancialDataProcessor baselineProcessor = new FinancialDataProcessor();
        baselineProcessor.setEntries(entries);
        baselineProcessor.setTaxOptimizationStrategy(
                new CompositeTaxOptimizationStrategy(
                        new RMDOptimizationStrategy(),
                        new ExpenseManagementStrategy(),
                        new NoOpRothConversionStrategy(filingStatus),
                        new TaxCalculationStrategy(filingStatus, statesByYear)
                )
        );
        YearlySummary[] baselineSummaries = baselineProcessor.generateYearlySummaries(rates, personsByName);

        // Build rows for each scenario
        List<RothComparisonRowDTO> rows = new ArrayList<>();

        // Scenario 1: No Conversion (baseline)
        rows.add(buildRothComparisonRow("No Conversion", null, entries, rates, personsByName,
                filingStatus, statesByYear, milestoneYears, firstDataYear, baselineSummaries, baselineSummaries));

        // Scenario 2: Fill 12% Bracket
        rows.add(buildRothComparisonRow("Fill 12% Bracket", bracket12, entries, rates, personsByName,
                filingStatus, statesByYear, milestoneYears, firstDataYear, baselineSummaries, null));

        // Scenario 3: Fill 22% Bracket
        rows.add(buildRothComparisonRow("Fill 22% Bracket", bracket22, entries, rates, personsByName,
                filingStatus, statesByYear, milestoneYears, firstDataYear, baselineSummaries, null));

        // Scenario 4: Fill 24% Bracket
        rows.add(buildRothComparisonRow("Fill 24% Bracket", bracket24, entries, rates, personsByName,
                filingStatus, statesByYear, milestoneYears, firstDataYear, baselineSummaries, null));

        RothComparisonResponseDTO response = new RothComparisonResponseDTO();
        response.setFirstDataYear(firstDataYear);
        response.setMilestoneYears(milestoneYears);
        response.setRows(rows);
        return response;
    }

    private RothComparisonRowDTO buildRothComparisonRow(
            String scenarioName,
            Double bracketThreshold,
            List<FinancialEntry> entries,
            Map<ItemType, Double> rates,
            Map<String, Person> personsByName,
            FilingStatus filingStatus,
            List<StateByYear> statesByYear,
            List<Integer> milestoneYears,
            int firstDataYear,
            YearlySummary[] baselineSummaries,
            YearlySummary[] precomputedSummaries) {

        YearlySummary[] summaries;
        if (precomputedSummaries != null) {
            summaries = precomputedSummaries;
        } else {
            FinancialDataProcessor processor = new FinancialDataProcessor();
            processor.setEntries(entries);
            processor.setTaxOptimizationStrategy(
                    new CompositeTaxOptimizationStrategy(filingStatus, statesByYear, bracketThreshold)
            );
            summaries = processor.generateYearlySummaries(rates, personsByName);
        }

        // Track first shortfall year
        Integer firstShortfallYear = null;
        for (YearlySummary summary : summaries) {
            if (summary.deficit() > 0) {
                firstShortfallYear = summary.year();
                break;
            }
        }

        // Build cells for each milestone
        List<RothComparisonCellDTO> cells = new ArrayList<>();
        for (int milestoneYear : milestoneYears) {
            int index = milestoneYear - firstDataYear;
            if (index >= 0 && index < summaries.length) {
                YearlySummary summary = summaries[index];
                YearlySummary baseline = baselineSummaries[index];

                // Calculate cumulative taxes
                double cumulativeTaxes = 0;
                for (int i = 0; i <= index; i++) {
                    cumulativeTaxes += summaries[i].federalIncomeTax() + summaries[i].stateIncomeTax();
                }

                boolean hasShortfall = firstShortfallYear != null && firstShortfallYear <= milestoneYear;

                cells.add(new RothComparisonCellDTO(
                        milestoneYear,
                        summary.netWorth(),
                        summary.netWorth() - baseline.netWorth(),
                        cumulativeTaxes,
                        summary.rothAssets(),
                        hasShortfall,
                        hasShortfall ? firstShortfallYear : null
                ));
            }
        }

        return new RothComparisonRowDTO(scenarioName, bracketThreshold, cells);
    }
}
