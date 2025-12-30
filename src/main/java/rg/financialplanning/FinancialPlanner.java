package rg.financialplanning;

import rg.financialplanning.model.FinancialEntry;
import rg.financialplanning.model.Person;
import rg.financialplanning.model.YearlySummary;
import rg.financialplanning.parser.FinancialDataProcessor;
import rg.financialplanning.parser.ItemTypePercentageParser;
import rg.financialplanning.parser.PersonParser;

import java.io.IOException;

public class FinancialPlanner {
    public static void main(String[] args) {
        if (args.length < 3) {
            System.out.println("Usage: java FinancialPlanner <financial-csv-path> <persons-csv-path> <percentages-csv-path>");
            System.out.println("Example: java FinancialPlanner financial_data.csv persons.csv item_percentages.csv");
            return;
        }

        String financialCsvPath = args[0];
        String personsCsvPath = args[1];
        String percentagesCsvPath = args[2];

        FinancialDataProcessor processor = new FinancialDataProcessor();
        PersonParser personParser = new PersonParser();
        ItemTypePercentageParser percentageParser = new ItemTypePercentageParser();

        try {
            processor.loadFromCsv(financialCsvPath);
            personParser.loadFromCsv(personsCsvPath);
            percentageParser.loadFromCsv(percentagesCsvPath);

            System.out.println("=== Financial Planning Report ===\n");

            System.out.println("=== Persons ===");
            for (Person person : personParser.getPersons()) {
                System.out.println(person);
            }

            System.out.println("\n=== Percentage Rates ===");
            for (var entry : percentageParser.getPercentagesByType().entrySet()) {
                System.out.printf("%s: %.2f%%%n", entry.getKey(), entry.getValue());
            }

            System.out.println("\nLoaded " + processor.getEntries().size() + " financial entries");
            System.out.println("Earliest start year: " + processor.getEarliestStartYear());
            System.out.println("Latest end year: " + processor.getLatestEndYear());

            System.out.println("\n=== Entries ===");
            for (FinancialEntry entry : processor.getEntries()) {
                System.out.printf("%s: %s - %s - $%d [%d-%d]%n",
                    entry.name(), entry.item(), entry.description(), entry.value(),
                    entry.startYear(), entry.endYear());
            }

            System.out.println("\n=== Yearly Summaries ===");
            YearlySummary[] summaries = processor.generateYearlySummaries(
                    percentageParser.getPercentagesByType(), personParser.getPersonsByName());
            for (YearlySummary summary : summaries) {
                System.out.println(summary);
                // Print age of each person for this year
                for (Person person : personParser.getPersons()) {
                    System.out.printf("  %s age: %d%n", person.name(), person.getAgeInYear(summary.year()));
                }
            }

            // Export yearly summaries to CSV
            String outputCsvPath = "FinancialPlanner-Out.csv";
            processor.exportYearlySummariesToCsv(summaries, outputCsvPath);
            System.out.println("\n=== CSV Export ===");
            System.out.println("Yearly summaries exported to: " + outputCsvPath);

        } catch (IOException e) {
            System.err.println("Error reading CSV file: " + e.getMessage());
        }
    }
}
