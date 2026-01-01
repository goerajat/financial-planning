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
    private boolean validationEnabled;

    public FinancialDataProcessor() {
        this.entries = new ArrayList<>();
        this.earliestStartYear = Integer.MAX_VALUE;
        this.latestEndYear = Integer.MIN_VALUE;
        this.taxOptimizationStrategy = new CompositeTaxOptimizationStrategy();
        this.validationEnabled = true;
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

    /**
     * Enables or disables validation of yearly summaries after tax optimization.
     * When enabled (default), an IllegalStateException is thrown if validation fails.
     *
     * @param enabled true to enable validation, false to disable
     */
    public void setValidationEnabled(boolean enabled) {
        this.validationEnabled = enabled;
    }

    /**
     * Returns whether validation is enabled.
     *
     * @return true if validation is enabled
     */
    public boolean isValidationEnabled() {
        return validationEnabled;
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

            // Income, Expenses, Social Security, and Contributions: Calculate from active entries
            double totalIncome = 0;
            double totalExpenses = 0;
            double totalSocialSecurity = 0;
            double rothContributions = 0;
            double qualifiedContributions = 0;
            double lifeInsuranceContributions = 0;
            Map<String, Double> incomeByName = new HashMap<>();
            Map<String, Double> socialSecurityByName = new HashMap<>();
            Map<String, Double> rothContributionsByName = new HashMap<>();
            Map<String, Double> qualifiedContributionsByName = new HashMap<>();

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
                        case SOCIAL_SECURITY_BENEFITS -> {
                            totalSocialSecurity += calculatedValue;
                            socialSecurityByName.merge(name, calculatedValue, Double::sum);
                        }
                        case ROTH_CONTRIBUTION -> {
                            rothContributions += calculatedValue;
                            rothContributionsByName.merge(name, calculatedValue, Double::sum);
                        }
                        case QUALIFIED_CONTRIBUTION -> {
                            qualifiedContributions += calculatedValue;
                            qualifiedContributionsByName.merge(name, calculatedValue, Double::sum);
                        }
                        case LIFE_INSURANCE_CONTRIBUTION -> lifeInsuranceContributions += calculatedValue;
                        default -> { /* handled below */ }
                    }
                }
            }

            // Other item types: Compound from previous year + add new entries (new behavior)
            double qualifiedAssets = applyPercentageIncrease(previousSummary != null ? previousSummary.qualifiedAssets() : 0,
                    percentageRates.getOrDefault(ItemType.QUALIFIED, 0.0));
            // Add qualified contributions to the balance
            qualifiedAssets += qualifiedContributions;
            double nonQualifiedAssets = applyPercentageIncrease(previousSummary != null ? previousSummary.nonQualifiedAssets() : 0,
                    percentageRates.getOrDefault(ItemType.NON_QUALIFIED, 0.0));
            double rothAssets = applyPercentageIncrease(previousSummary != null ? previousSummary.rothAssets() : 0,
                    percentageRates.getOrDefault(ItemType.ROTH, 0.0));
            // Add roth contributions to the balance
            rothAssets += rothContributions;
            double cash = applyPercentageIncrease(previousSummary != null ? previousSummary.cash() : 0,
                    percentageRates.getOrDefault(ItemType.CASH, 0.0));
            double realEstate = applyPercentageIncrease(previousSummary != null ? previousSummary.realEstate() : 0,
                    percentageRates.getOrDefault(ItemType.REAL_ESTATE, 0.0));
            double lifeInsuranceBenefits = applyPercentageIncrease(previousSummary != null ? previousSummary.lifeInsuranceBenefits() : 0,
                    percentageRates.getOrDefault(ItemType.LIFE_INSURANCE_BENEFIT, 0.0));
            // Add life insurance contributions to the balance
            lifeInsuranceBenefits += lifeInsuranceContributions;

            // Build individual summaries by carrying forward from previous year and applying percentage increases
            Map<String, IndividualYearlySummary> individualSummaries = new HashMap<>();

            // Step 1: Carry forward individuals from previous year with percentage increases applied to assets
            if (previousSummary != null && previousSummary.individualSummaries() != null) {
                for (Map.Entry<String, IndividualYearlySummary> entry : previousSummary.individualSummaries().entrySet()) {
                    String name = entry.getKey();
                    IndividualYearlySummary prevIndividual = entry.getValue();
                    Person person = prevIndividual.person();

                    // Apply percentage increases to assets from previous year
                    double prevQualified = applyPercentageIncrease(prevIndividual.qualifiedAssets(),
                            percentageRates.getOrDefault(ItemType.QUALIFIED, 0.0));
                    double prevNonQualified = applyPercentageIncrease(prevIndividual.nonQualifiedAssets(),
                            percentageRates.getOrDefault(ItemType.NON_QUALIFIED, 0.0));
                    double prevRoth = applyPercentageIncrease(prevIndividual.rothAssets(),
                            percentageRates.getOrDefault(ItemType.ROTH, 0.0));

                    // Create new individual with carried-forward assets (income and SS will be set below)
                    IndividualYearlySummary individual = new IndividualYearlySummary(
                            person, year,
                            0.0, // income will be set below
                            prevQualified,
                            prevNonQualified,
                            prevRoth,
                            0.0  // social security will be set below
                    );
                    individualSummaries.put(name, individual);
                }
            }

            // Step 2: Add base values for entries that start this year (for asset types)
            for (FinancialEntry entry : entries) {
                if (entry.startYear() == year) {
                    double baseValue = entry.value();
                    String name = entry.name() != null ? entry.name() : "Unknown";

                    switch (entry.item()) {
                        case QUALIFIED -> {
                            qualifiedAssets += baseValue;
                            // Add to individual if exists, or create new one
                            IndividualYearlySummary ind = getOrCreateIndividual(individualSummaries, name, year, personsByName);
                            ind.setQualifiedAssets(ind.qualifiedAssets() + baseValue);
                        }
                        case NON_QUALIFIED -> {
                            nonQualifiedAssets += baseValue;
                            IndividualYearlySummary ind = getOrCreateIndividual(individualSummaries, name, year, personsByName);
                            ind.setNonQualifiedAssets(ind.nonQualifiedAssets() + baseValue);
                        }
                        case ROTH -> {
                            rothAssets += baseValue;
                            IndividualYearlySummary ind = getOrCreateIndividual(individualSummaries, name, year, personsByName);
                            ind.setRothAssets(ind.rothAssets() + baseValue);
                        }
                        case CASH -> cash += baseValue;
                        case REAL_ESTATE -> realEstate += baseValue;
                        case LIFE_INSURANCE_BENEFIT -> lifeInsuranceBenefits += baseValue;
                        default -> { /* Income/Expense/Social Security handled above */ }
                    }
                }
            }

            // Step 3: Collect all unique names (from previous year + current year entries)
            java.util.Set<String> allNames = new java.util.HashSet<>(individualSummaries.keySet());
            allNames.addAll(incomeByName.keySet());
            allNames.addAll(socialSecurityByName.keySet());
            allNames.addAll(rothContributionsByName.keySet());
            allNames.addAll(qualifiedContributionsByName.keySet());

            // Step 4: Apply income, social security, and contributions to individuals
            for (String name : allNames) {
                IndividualYearlySummary individual = getOrCreateIndividual(individualSummaries, name, year, personsByName);

                // Apply income and social security (need to recreate with correct values since they're final)
                double income = incomeByName.getOrDefault(name, 0.0);
                double socialSecurity = socialSecurityByName.getOrDefault(name, 0.0);

                if (income > 0 || socialSecurity > 0) {
                    // Recreate individual with income and social security
                    IndividualYearlySummary updatedIndividual = new IndividualYearlySummary(
                            individual.person(), year,
                            income,
                            individual.qualifiedAssets(),
                            individual.nonQualifiedAssets(),
                            individual.rothAssets(),
                            socialSecurity
                    );
                    individualSummaries.put(name, updatedIndividual);
                    individual = updatedIndividual;
                }

                // Apply contributions
                double rothContrib = rothContributionsByName.getOrDefault(name, 0.0);
                double qualifiedContrib = qualifiedContributionsByName.getOrDefault(name, 0.0);

                individual.setRothContributions(rothContrib);
                individual.setQualifiedContributions(qualifiedContrib);

                // Add contributions to asset balances
                individual.setRothAssets(individual.rothAssets() + rothContrib);
                individual.setQualifiedAssets(individual.qualifiedAssets() + qualifiedContrib);
            }

            summaries[i] = new YearlySummary(year, totalIncome, totalExpenses,
                    qualifiedAssets, nonQualifiedAssets, rothAssets, cash, realEstate,
                    lifeInsuranceBenefits, totalSocialSecurity, rothContributions, individualSummaries);

            // Apply tax optimization strategy after creating the summary
            if (taxOptimizationStrategy != null) {
                taxOptimizationStrategy.optimize(previousSummary, summaries[i]);
            }

            // Validate the yearly summary after optimization strategies have been applied
            validateYearlySummary(summaries[i]);
        }

        return summaries;
    }

    private double applyPercentageIncrease(double value, double percentageRate) {
        return value * (1 + percentageRate / 100);
    }

    /**
     * Gets an existing IndividualYearlySummary from the map, or creates a new one if it doesn't exist.
     *
     * @param individualSummaries the map of individual summaries
     * @param name the name of the individual
     * @param year the year for the summary
     * @param personsByName map of name to Person
     * @return the existing or newly created IndividualYearlySummary
     */
    private IndividualYearlySummary getOrCreateIndividual(Map<String, IndividualYearlySummary> individualSummaries,
                                                          String name, int year, Map<String, Person> personsByName) {
        return individualSummaries.computeIfAbsent(name, n -> {
            Person person = personsByName != null ? personsByName.get(n) : null;
            return new IndividualYearlySummary(person, year, 0.0, 0.0, 0.0, 0.0, 0.0);
        });
    }

    /**
     * Validates a YearlySummary after tax optimization strategies have been applied.
     * Checks both individual totals consistency and cash flow balance.
     * Only performs validation if validationEnabled is true.
     *
     * @param summary the YearlySummary to validate
     * @throws IllegalStateException if validation is enabled and fails
     */
    private void validateYearlySummary(YearlySummary summary) {
        if (!validationEnabled) {
            return;
        }

        if (!summary.validateIndividualTotals()) {
            throw new IllegalStateException(
                    "Year " + summary.year() + " validation failed - individual totals mismatch:\n" +
                    summary.getIndividualTotalsValidationDetails());
        }

        if (!summary.validateCashFlow()) {
            throw new IllegalStateException(
                    "Year " + summary.year() + " validation failed - cash flow imbalance:\n" +
                    summary.getCashFlowValidationDetails());
        }
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
     * - {Person Name} Age (for each individual)
     * - Total Income, {Person Name} Income (for each individual)
     * - Total RMD Withdrawals, {Person Name} RMD Withdrawals (for each individual)
     * - Total Qualified Withdrawals, {Person Name} Qualified Withdrawals (for each individual)
     * - Total Non-Qualified Withdrawals, {Person Name} Non-Qualified Withdrawals (for each individual)
     * - Total Cash Withdrawals, {Person Name} Cash Withdrawals (for each individual)
     * - Total Social Security Benefits, {Person Name} Social Security Benefits (for each individual)
     * - Total Qualified Contributions, {Person Name} Qualified Contributions (for each individual)
     * - Total Non-Qualified Contributions, {Person Name} Non-Qualified Contributions (for each individual)
     * - Total Roth Contributions, {Person Name} Roth Contributions (for each individual)
     * - Total Federal Income Tax
     * - Total State Income Tax
     * - Total Capital Gains Tax
     * - Total Social Security Tax
     * - Total Medicare Tax
     * - Total Expenses
     * - Total Qualified Assets, {Person Name} Qualified Assets (for each individual)
     * - Total Non-Qualified Assets, {Person Name} Non-Qualified Assets (for each individual)
     * - Total Roth Assets, {Person Name} Roth Assets (for each individual)
     * - Total Cash
     * - Total Real Estate
     * - Total Life Insurance Benefits
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

            // Cash Withdrawals section
            writeTotalRow(writer, "Total Cash Withdrawals", summaries, YearlySummary::cashWithdrawals);
            for (String name : allNames) {
                writeIndividualRow(writer, name + " Cash Withdrawals", summaries, name,
                        ind -> ind.cashWithdrawals());
            }

            // Social Security Benefits section
            writeTotalRow(writer, "Total Social Security Benefits", summaries, YearlySummary::totalSocialSecurity);
            for (String name : allNames) {
                writeIndividualRow(writer, name + " Social Security Benefits", summaries, name,
                        ind -> ind.socialSecurityBenefits());
            }

            // Qualified Contributions section
            writeTotalRow(writer, "Total Qualified Contributions", summaries, YearlySummary::qualifiedContributions);
            for (String name : allNames) {
                writeIndividualRow(writer, name + " Qualified Contributions", summaries, name,
                        ind -> ind.qualifiedContributions());
            }

            // Non-Qualified Contributions section
            writeTotalRow(writer, "Total Non-Qualified Contributions", summaries, YearlySummary::nonQualifiedContributions);
            for (String name : allNames) {
                writeIndividualRow(writer, name + " Non-Qualified Contributions", summaries, name,
                        ind -> ind.nonQualifiedContributions());
            }

            // Roth Contributions section
            writeTotalRow(writer, "Total Roth Contributions", summaries, YearlySummary::rothContributions);
            for (String name : allNames) {
                writeIndividualRow(writer, name + " Roth Contributions", summaries, name,
                        ind -> ind.rothContributions());
            }

            // Tax section
            writeTotalRow(writer, "Total Federal Income Tax", summaries, YearlySummary::federalIncomeTax);
            writeTotalRow(writer, "Total State Income Tax", summaries, YearlySummary::stateIncomeTax);
            writeTotalRow(writer, "Total Capital Gains Tax", summaries, YearlySummary::capitalGainsTax);
            writeTotalRow(writer, "Total Social Security Tax", summaries, YearlySummary::socialSecurityTax);
            writeTotalRow(writer, "Total Medicare Tax", summaries, YearlySummary::medicareTax);

            // Expenses section
            writeTotalRow(writer, "Total Expenses", summaries, YearlySummary::totalExpenses);

            // Qualified Assets section
            writeTotalRow(writer, "Total Qualified Assets", summaries, YearlySummary::qualifiedAssets);
            for (String name : allNames) {
                writeIndividualRow(writer, name + " Qualified Assets", summaries, name,
                        ind -> ind.qualifiedAssets());
            }

            // Non-Qualified Assets section
            writeTotalRow(writer, "Total Non-Qualified Assets", summaries, YearlySummary::nonQualifiedAssets);
            for (String name : allNames) {
                writeIndividualRow(writer, name + " Non-Qualified Assets", summaries, name,
                        ind -> ind.nonQualifiedAssets());
            }

            // Roth Assets section
            writeTotalRow(writer, "Total Roth Assets", summaries, YearlySummary::rothAssets);
            for (String name : allNames) {
                writeIndividualRow(writer, name + " Roth Assets", summaries, name,
                        ind -> ind.rothAssets());
            }

            // Cash section
            writeTotalRow(writer, "Total Cash", summaries, YearlySummary::cash);

            // Real Estate section
            writeTotalRow(writer, "Total Real Estate", summaries, YearlySummary::realEstate);

            // Life Insurance Benefits section
            writeTotalRow(writer, "Total Life Insurance Benefits", summaries, YearlySummary::lifeInsuranceBenefits);
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
                        sb.append(",").append(age);
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
