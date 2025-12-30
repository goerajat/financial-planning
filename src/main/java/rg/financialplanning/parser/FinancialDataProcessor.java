package rg.financialplanning.parser;

import rg.financialplanning.model.FinancialEntry;
import rg.financialplanning.model.IndividualYearlySummary;
import rg.financialplanning.model.ItemType;
import rg.financialplanning.model.Person;
import rg.financialplanning.model.YearlySummary;
import rg.financialplanning.strategy.CompositeTaxOptimizationStrategy;
import rg.financialplanning.strategy.TaxOptimizationStrategy;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Processes financial data from a CSV file and generates yearly summaries.
 */
public class FinancialDataProcessor {

    private final List<FinancialEntry> entries;
    private int earliestStartYear;
    private int latestEndYear;
    private TaxOptimizationStrategy taxOptimizationStrategy;

    public FinancialDataProcessor() {
        this.entries = new ArrayList<>();
        this.earliestStartYear = Integer.MAX_VALUE;
        this.latestEndYear = Integer.MIN_VALUE;
        this.taxOptimizationStrategy = new CompositeTaxOptimizationStrategy();
    }

    /**
     * Sets a custom tax optimization strategy.
     *
     * @param strategy the tax optimization strategy to use
     */
    public void setTaxOptimizationStrategy(TaxOptimizationStrategy strategy) {
        this.taxOptimizationStrategy = strategy;
    }

    /**
     * Gets the current tax optimization strategy.
     *
     * @return the tax optimization strategy
     */
    public TaxOptimizationStrategy getTaxOptimizationStrategy() {
        return taxOptimizationStrategy;
    }

    public void loadFromCsv(String filePath) throws IOException {
        loadFromCsv(Path.of(filePath));
    }

    public void loadFromCsv(Path filePath) throws IOException {
        entries.clear();
        earliestStartYear = Integer.MAX_VALUE;
        latestEndYear = Integer.MIN_VALUE;

        try (BufferedReader reader = Files.newBufferedReader(filePath)) {
            String headerLine = reader.readLine();
            if (headerLine == null) {
                throw new IOException("CSV file is empty");
            }

            String line;
            int lineNumber = 1;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (line.isBlank()) {
                    continue;
                }
                try {
                    FinancialEntry entry = parseLine(line);
                    entries.add(entry);
                    updateYearBounds(entry);
                } catch (Exception e) {
                    throw new IOException("Error parsing line " + lineNumber + ": " + e.getMessage(), e);
                }
            }
        }
    }

    private FinancialEntry parseLine(String line) {
        String[] parts = line.split(",", -1);
        if (parts.length < 6) {
            throw new IllegalArgumentException("Expected 6 columns, found " + parts.length);
        }

        String nameStr = parts[0].trim();
        String itemTypeStr = parts[1].trim();
        String description = parts[2].trim();
        String valueStr = parts[3].trim();
        String startYearStr = parts[4].trim();
        String endYearStr = parts[5].trim();

        if (itemTypeStr.isEmpty()) {
            throw new IllegalArgumentException("Item type cannot be empty");
        }

        String name = nameStr.isEmpty() ? null : nameStr;
        ItemType item = ItemType.fromString(itemTypeStr);
        String desc = description.isEmpty() ? null : description;
        int value = valueStr.isEmpty() ? Integer.MIN_VALUE : Integer.parseInt(valueStr);
        int startYear = startYearStr.isEmpty() ? Integer.MIN_VALUE : Integer.parseInt(startYearStr);
        int endYear = endYearStr.isEmpty() ? Integer.MIN_VALUE : Integer.parseInt(endYearStr);

        return new FinancialEntry(name, item, desc, value, startYear, endYear);
    }

    private void updateYearBounds(FinancialEntry entry) {
        if (entry.startYear() < earliestStartYear) {
            earliestStartYear = entry.startYear();
        }
        if (entry.endYear() > latestEndYear) {
            latestEndYear = entry.endYear();
        }
    }

    public int getEarliestStartYear() {
        if (entries.isEmpty()) {
            throw new IllegalStateException("No entries loaded");
        }
        return earliestStartYear;
    }

    public int getLatestEndYear() {
        if (entries.isEmpty()) {
            throw new IllegalStateException("No entries loaded");
        }
        return latestEndYear;
    }

    public List<FinancialEntry> getEntries() {
        return List.copyOf(entries);
    }

    /**
     * Generates yearly summaries as an array using the provided percentage rates.
     * Index 0 represents earliestStartYear, array length is (latestEndYear - earliestStartYear + 1).
     *
     * Calculation logic:
     * - If it's the start year of an entry, add the entry's base value
     * - Apply percentage increase to previous year's totals and add to current year
     *
     * @param percentageRates map of ItemType to percentage increase rates
     * @param personsByName map of name to Person for individual summaries
     * @return array of YearlySummary, or empty array if no entries loaded
     */
    public YearlySummary[] generateYearlySummaries(Map<ItemType, Double> percentageRates, Map<String, Person> personsByName) {
        if (entries.isEmpty()) {
            return new YearlySummary[0];
        }

        int arrayLength = latestEndYear - earliestStartYear + 1;
        YearlySummary[] summaries = new YearlySummary[arrayLength];

        for (int i = 0; i < arrayLength; i++) {
            int year = earliestStartYear + i;
            YearlySummary previousSummary = i > 0 ? summaries[i - 1] : null;

            // Income and Expenses: Calculate from active entries (old behavior)
            double totalIncome = 0;
            double totalExpenses = 0;
            Map<String, Double> incomeByName = new HashMap<>();

            for (FinancialEntry entry : entries) {
                if (entry.isActiveInYear(year)) {
                    double calculatedValue = entry.getValueForYear(year,
                            percentageRates.getOrDefault(entry.item(), 0.0));
                    String name = entry.name() != null ? entry.name() : "Unknown";

                    switch (entry.item()) {
                        case INCOME -> {
                            totalIncome += calculatedValue;
                            incomeByName.merge(name, calculatedValue, Double::sum);
                        }
                        case EXPENSE -> totalExpenses += calculatedValue;
                        default -> { /* handled below */ }
                    }
                }
            }

            // Other item types: Compound from previous year + add new entries (new behavior)
            double qualifiedAssets = applyPercentageIncrease(previousSummary != null ? previousSummary.qualifiedAssets() : 0,
                    percentageRates.getOrDefault(ItemType.QUALIFIED, 0.0));
            double nonQualifiedAssets = applyPercentageIncrease(previousSummary != null ? previousSummary.nonQualifiedAssets() : 0,
                    percentageRates.getOrDefault(ItemType.NON_QUALIFIED, 0.0));
            double rothAssets = applyPercentageIncrease(previousSummary != null ? previousSummary.rothAssets() : 0,
                    percentageRates.getOrDefault(ItemType.ROTH, 0.0));
            double cash = applyPercentageIncrease(previousSummary != null ? previousSummary.cash() : 0,
                    percentageRates.getOrDefault(ItemType.CASH, 0.0));
            double realEstate = applyPercentageIncrease(previousSummary != null ? previousSummary.realEstate() : 0,
                    percentageRates.getOrDefault(ItemType.REAL_ESTATE, 0.0));
            double lifeInsuranceBenefits = applyPercentageIncrease(previousSummary != null ? previousSummary.lifeInsuranceBenefits() : 0,
                    percentageRates.getOrDefault(ItemType.LIFE_INSURANCE_BENEFIT, 0.0));
            double totalSocialSecurity = applyPercentageIncrease(previousSummary != null ? previousSummary.totalSocialSecurity() : 0,
                    percentageRates.getOrDefault(ItemType.SOCIAL_SECURITY_BENEFITS, 0.0));

            Map<String, Double> qualifiedByName = new HashMap<>();
            Map<String, Double> nonQualifiedByName = new HashMap<>();
            Map<String, Double> rothByName = new HashMap<>();
            Map<String, Double> socialSecurityByName = new HashMap<>();

            // Add base values for entries that start this year (for non-income/expense types)
            for (FinancialEntry entry : entries) {
                if (entry.startYear() == year) {
                    double baseValue = entry.value();
                    String name = entry.name() != null ? entry.name() : "Unknown";

                    switch (entry.item()) {
                        case QUALIFIED -> {
                            qualifiedAssets += baseValue;
                            qualifiedByName.merge(name, baseValue, Double::sum);
                        }
                        case NON_QUALIFIED -> {
                            nonQualifiedAssets += baseValue;
                            nonQualifiedByName.merge(name, baseValue, Double::sum);
                        }
                        case ROTH -> {
                            rothAssets += baseValue;
                            rothByName.merge(name, baseValue, Double::sum);
                        }
                        case SOCIAL_SECURITY_BENEFITS -> {
                            totalSocialSecurity += baseValue;
                            socialSecurityByName.merge(name, baseValue, Double::sum);
                        }
                        case CASH -> cash += baseValue;
                        case REAL_ESTATE -> realEstate += baseValue;
                        case LIFE_INSURANCE_BENEFIT -> lifeInsuranceBenefits += baseValue;
                        default -> { /* Income/Expense already handled above */ }
                    }
                }
            }

            // Build individual summaries (simplified - tracks new entries this year)
            Map<String, IndividualYearlySummary> individualSummaries = new HashMap<>();

            // Collect all unique names from all maps
            java.util.Set<String> allNames = new java.util.HashSet<>();
            allNames.addAll(incomeByName.keySet());
            allNames.addAll(qualifiedByName.keySet());
            allNames.addAll(nonQualifiedByName.keySet());
            allNames.addAll(rothByName.keySet());
            allNames.addAll(socialSecurityByName.keySet());

            for (String name : allNames) {
                Person person = personsByName != null ? personsByName.get(name) : null;
                individualSummaries.put(name, new IndividualYearlySummary(
                        person, year,
                        incomeByName.getOrDefault(name, 0.0),
                        qualifiedByName.getOrDefault(name, 0.0),
                        nonQualifiedByName.getOrDefault(name, 0.0),
                        rothByName.getOrDefault(name, 0.0),
                        socialSecurityByName.getOrDefault(name, 0.0)
                ));
            }

            summaries[i] = new YearlySummary(year, totalIncome, totalExpenses,
                    qualifiedAssets, nonQualifiedAssets, rothAssets, cash, realEstate,
                    lifeInsuranceBenefits, totalSocialSecurity, individualSummaries);

            // Apply tax optimization strategy after creating the summary
            if (taxOptimizationStrategy != null) {
                taxOptimizationStrategy.optimize(previousSummary, summaries[i]);
            }
        }

        return summaries;
    }

    private double applyPercentageIncrease(double value, double percentageRate) {
        return value * (1 + percentageRate / 100);
    }

    /**
     * Gets the summary for a specific year.
     *
     * @param year the year to get summary for
     * @param percentageRates map of ItemType to percentage increase rates
     * @param personsByName map of name to Person for individual summaries
     * @return YearlySummary for the year, or null if year is out of range
     */
    public YearlySummary getSummaryForYear(int year, Map<ItemType, Double> percentageRates, Map<String, Person> personsByName) {
        if (entries.isEmpty() || year < earliestStartYear || year > latestEndYear) {
            return null;
        }
        int index = year - earliestStartYear;
        return generateYearlySummaries(percentageRates, personsByName)[index];
    }

    /**
     * Exports yearly summaries to a CSV file.
     *
     * The CSV format has years as columns and the following rows:
     * - Total Income, {Person Name} Income (for each individual)
     * - Total RMD Withdrawals, {Person Name} RMD Withdrawals (for each individual)
     * - Total Qualified Withdrawals, {Person Name} Qualified Withdrawals (for each individual)
     * - Total Non-Qualified Withdrawals, {Person Name} Non-Qualified Withdrawals (for each individual)
     * - Total Federal Income Tax
     * - Total State Income Tax
     * - Total Capital Gains Tax
     * - Total Social Security Tax
     * - Total Medicare Tax
     * - Total Expenses
     *
     * @param summaries the array of yearly summaries to export
     * @param filePath the path to write the CSV file
     * @throws IOException if writing fails
     */
    public void exportYearlySummariesToCsv(YearlySummary[] summaries, String filePath) throws IOException {
        exportYearlySummariesToCsv(summaries, Path.of(filePath));
    }

    /**
     * Exports yearly summaries to a CSV file.
     *
     * @param summaries the array of yearly summaries to export
     * @param filePath the path to write the CSV file
     * @throws IOException if writing fails
     */
    public void exportYearlySummariesToCsv(YearlySummary[] summaries, Path filePath) throws IOException {
        if (summaries == null || summaries.length == 0) {
            throw new IllegalArgumentException("Summaries array cannot be null or empty");
        }

        // Collect all unique individual names across all years
        Set<String> allNames = new LinkedHashSet<>();
        for (YearlySummary summary : summaries) {
            if (summary != null && summary.individualSummaries() != null) {
                allNames.addAll(summary.individualSummaries().keySet());
            }
        }

        try (BufferedWriter writer = Files.newBufferedWriter(filePath)) {
            // Write header row with years
            writeHeaderRow(writer, summaries);

            // Write age row for each individual
            writeAgeRows(writer, summaries, allNames);

            // Income section
            writeTotalRow(writer, "Total Income", summaries, YearlySummary::totalIncome);
            for (String name : allNames) {
                writeIndividualRow(writer, name + " Income", summaries, name,
                        ind -> ind.income());
            }

            // RMD Withdrawals section
            writeTotalRow(writer, "Total RMD Withdrawals", summaries, YearlySummary::rmdWithdrawals);
            for (String name : allNames) {
                writeIndividualRow(writer, name + " RMD Withdrawals", summaries, name,
                        ind -> ind.rmdWithdrawals());
            }

            // Qualified Withdrawals section
            writeTotalRow(writer, "Total Qualified Withdrawals", summaries, YearlySummary::qualifiedWithdrawals);
            for (String name : allNames) {
                writeIndividualRow(writer, name + " Qualified Withdrawals", summaries, name,
                        ind -> ind.qualifiedWithdrawals());
            }

            // Non-Qualified Withdrawals section
            writeTotalRow(writer, "Total Non-Qualified Withdrawals", summaries, YearlySummary::nonQualifiedWithdrawals);
            for (String name : allNames) {
                writeIndividualRow(writer, name + " Non-Qualified Withdrawals", summaries, name,
                        ind -> ind.nonQualifiedWithdrawals());
            }

            // Social Security Benefits section
            writeTotalRow(writer, "Total Social Security Benefits", summaries, YearlySummary::totalSocialSecurity);
            for (String name : allNames) {
                writeIndividualRow(writer, name + " Social Security Benefits", summaries, name,
                        ind -> ind.socialSecurityBenefits());
            }

            // Tax section
            writeTotalRow(writer, "Total Federal Income Tax", summaries, YearlySummary::federalIncomeTax);
            writeTotalRow(writer, "Total State Income Tax", summaries, YearlySummary::stateIncomeTax);
            writeTotalRow(writer, "Total Capital Gains Tax", summaries, YearlySummary::capitalGainsTax);
            writeTotalRow(writer, "Total Social Security Tax", summaries, YearlySummary::socialSecurityTax);
            writeTotalRow(writer, "Total Medicare Tax", summaries, YearlySummary::medicareTax);

            // Expenses section
            writeTotalRow(writer, "Total Expenses", summaries, YearlySummary::totalExpenses);
        }
    }

    /**
     * Writes the header row with "Item" and all years.
     */
    private void writeHeaderRow(BufferedWriter writer, YearlySummary[] summaries) throws IOException {
        StringBuilder sb = new StringBuilder("Item");
        for (YearlySummary summary : summaries) {
            if (summary != null) {
                sb.append(",").append(summary.year());
            }
        }
        writer.write(sb.toString());
        writer.newLine();
    }

    /**
     * Writes a row with total values from YearlySummary.
     */
    private void writeTotalRow(BufferedWriter writer, String label, YearlySummary[] summaries,
                               java.util.function.ToDoubleFunction<YearlySummary> valueExtractor) throws IOException {
        StringBuilder sb = new StringBuilder(label);
        for (YearlySummary summary : summaries) {
            if (summary != null) {
                sb.append(",").append(formatValue(valueExtractor.applyAsDouble(summary)));
            }
        }
        writer.write(sb.toString());
        writer.newLine();
    }

    /**
     * Writes a row with individual values from IndividualYearlySummary.
     */
    private void writeIndividualRow(BufferedWriter writer, String label, YearlySummary[] summaries,
                                    String individualName,
                                    java.util.function.ToDoubleFunction<IndividualYearlySummary> valueExtractor) throws IOException {
        StringBuilder sb = new StringBuilder(label);
        for (YearlySummary summary : summaries) {
            double value = 0.0;
            if (summary != null) {
                IndividualYearlySummary individual = summary.getIndividualSummary(individualName);
                if (individual != null) {
                    value = valueExtractor.applyAsDouble(individual);
                }
            }
            sb.append(",").append(formatValue(value));
        }
        writer.write(sb.toString());
        writer.newLine();
    }

    /**
     * Writes a row for each individual showing their name and age for each year.
     */
    private void writeAgeRows(BufferedWriter writer, YearlySummary[] summaries, Set<String> allNames) throws IOException {
        for (String name : allNames) {
            StringBuilder sb = new StringBuilder(name + " Age");
            for (YearlySummary summary : summaries) {
                if (summary != null) {
                    IndividualYearlySummary individual = summary.getIndividualSummary(name);
                    if (individual != null && individual.person() != null) {
                        int age = individual.person().getAgeInYear(summary.year());
                        sb.append(",").append(name).append("(").append(age).append(")");
                    } else {
                        sb.append(",");
                    }
                }
            }
            writer.write(sb.toString());
            writer.newLine();
        }
    }

    /**
     * Formats a double value for CSV output (2 decimal places).
     */
    private String formatValue(double value) {
        return String.format("%.2f", value);
    }
}
