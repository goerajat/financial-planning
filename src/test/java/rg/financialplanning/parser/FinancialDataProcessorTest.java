package rg.financialplanning.parser;

import org.junit.Test;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import rg.financialplanning.model.FinancialEntry;
import rg.financialplanning.model.ItemType;
import rg.financialplanning.model.Person;
import rg.financialplanning.model.YearlySummary;
import rg.financialplanning.strategy.TaxOptimizationStrategy;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class FinancialDataProcessorTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private FinancialDataProcessor processor;

    @Before
    public void setUp() {
        processor = new FinancialDataProcessor();
        // Disable validation for simplified test data that may not have balanced cash flows
        processor.setValidationEnabled(false);
    }

    // ===== loadFromCsv tests =====

    @Test
    public void testLoadFromCsv_validFile() throws IOException {
        File csvFile = tempFolder.newFile("financial.csv");
        String content = "name,item,description,value,startYear,endYear\n" +
                "John,INCOME,Salary,100000,2024,2030\n" +
                "Jane,EXPENSE,Living,50000,2024,2030";
        Files.writeString(csvFile.toPath(), content);

        processor.loadFromCsv(csvFile.getAbsolutePath());
        List<FinancialEntry> entries = processor.getEntries();

        assertEquals(2, entries.size());
    }

    @Test
    public void testLoadFromCsv_withPath() throws IOException {
        File csvFile = tempFolder.newFile("financial.csv");
        String content = "name,item,description,value,startYear,endYear\n" +
                "John,INCOME,Salary,100000,2024,2030";
        Files.writeString(csvFile.toPath(), content);

        processor.loadFromCsv(csvFile.toPath());
        assertEquals(1, processor.getEntries().size());
    }

    @Test
    public void testLoadFromCsv_setsYearBounds() throws IOException {
        File csvFile = tempFolder.newFile("financial.csv");
        String content = "name,item,description,value,startYear,endYear\n" +
                "John,INCOME,Salary,100000,2024,2030\n" +
                "Jane,QUALIFIED,401k,500000,2020,2050";
        Files.writeString(csvFile.toPath(), content);

        processor.loadFromCsv(csvFile.getAbsolutePath());

        assertEquals(2020, processor.getEarliestStartYear());
        assertEquals(2050, processor.getLatestEndYear());
    }

    @Test
    public void testLoadFromCsv_skipsBlankLines() throws IOException {
        File csvFile = tempFolder.newFile("financial.csv");
        String content = "name,item,description,value,startYear,endYear\n" +
                "John,INCOME,Salary,100000,2024,2030\n\n\n" +
                "Jane,EXPENSE,Living,50000,2024,2030\n";
        Files.writeString(csvFile.toPath(), content);

        processor.loadFromCsv(csvFile.getAbsolutePath());
        assertEquals(2, processor.getEntries().size());
    }

    @Test
    public void testLoadFromCsv_clearsExistingData() throws IOException {
        File csvFile1 = tempFolder.newFile("financial1.csv");
        String content1 = "name,item,description,value,startYear,endYear\nJohn,INCOME,Salary,100000,2024,2030";
        Files.writeString(csvFile1.toPath(), content1);

        File csvFile2 = tempFolder.newFile("financial2.csv");
        String content2 = "name,item,description,value,startYear,endYear\nJane,EXPENSE,Living,50000,2025,2035";
        Files.writeString(csvFile2.toPath(), content2);

        processor.loadFromCsv(csvFile1.getAbsolutePath());
        assertEquals(2024, processor.getEarliestStartYear());

        processor.loadFromCsv(csvFile2.getAbsolutePath());
        assertEquals(1, processor.getEntries().size());
        assertEquals(2025, processor.getEarliestStartYear());
    }

    @Test(expected = IOException.class)
    public void testLoadFromCsv_emptyFileThrowsException() throws IOException {
        File csvFile = tempFolder.newFile("empty.csv");
        Files.writeString(csvFile.toPath(), "");

        processor.loadFromCsv(csvFile.getAbsolutePath());
    }

    @Test(expected = IOException.class)
    public void testLoadFromCsv_missingColumnsThrowsException() throws IOException {
        File csvFile = tempFolder.newFile("invalid.csv");
        String content = "name,item,description,value,startYear,endYear\nJohn,INCOME,Salary,100000,2024";
        Files.writeString(csvFile.toPath(), content);

        processor.loadFromCsv(csvFile.getAbsolutePath());
    }

    @Test(expected = IOException.class)
    public void testLoadFromCsv_emptyItemTypeThrowsException() throws IOException {
        File csvFile = tempFolder.newFile("invalid.csv");
        String content = "name,item,description,value,startYear,endYear\nJohn,,Salary,100000,2024,2030";
        Files.writeString(csvFile.toPath(), content);

        processor.loadFromCsv(csvFile.getAbsolutePath());
    }

    @Test
    public void testLoadFromCsv_emptyOptionalFields() throws IOException {
        File csvFile = tempFolder.newFile("financial.csv");
        String content = "name,item,description,value,startYear,endYear\n" +
                ",INCOME,,100000,2024,2030"; // Empty name and description
        Files.writeString(csvFile.toPath(), content);

        processor.loadFromCsv(csvFile.getAbsolutePath());
        FinancialEntry entry = processor.getEntries().get(0);

        assertNull(entry.name());
        assertNull(entry.description());
    }

    // ===== getEntries tests =====

    @Test
    public void testGetEntries_returnsImmutableCopy() throws IOException {
        File csvFile = tempFolder.newFile("financial.csv");
        String content = "name,item,description,value,startYear,endYear\nJohn,INCOME,Salary,100000,2024,2030";
        Files.writeString(csvFile.toPath(), content);

        processor.loadFromCsv(csvFile.getAbsolutePath());
        List<FinancialEntry> entries = processor.getEntries();

        try {
            entries.add(new FinancialEntry("Test", ItemType.EXPENSE, "Test", 1000, 2020, 2025));
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // Expected
        }
    }

    // ===== getEarliestStartYear / getLatestEndYear tests =====

    @Test(expected = IllegalStateException.class)
    public void testGetEarliestStartYear_noEntriesThrowsException() {
        processor.getEarliestStartYear();
    }

    @Test(expected = IllegalStateException.class)
    public void testGetLatestEndYear_noEntriesThrowsException() {
        processor.getLatestEndYear();
    }

    // ===== generateYearlySummaries tests =====

    @Test
    public void testGenerateYearlySummaries_emptyEntries() {
        Map<ItemType, Double> percentages = new HashMap<>();
        Map<String, Person> persons = new HashMap<>();

        YearlySummary[] summaries = processor.generateYearlySummaries(percentages, persons);

        assertEquals(0, summaries.length);
    }

    @Test
    public void testGenerateYearlySummaries_basicCase() throws IOException {
        File csvFile = tempFolder.newFile("financial.csv");
        String content = "name,item,description,value,startYear,endYear\n" +
                "John,INCOME,Salary,100000,2024,2026\n" +
                "John,EXPENSE,Living,50000,2024,2026";
        Files.writeString(csvFile.toPath(), content);

        processor.loadFromCsv(csvFile.getAbsolutePath());

        Map<ItemType, Double> percentages = new HashMap<>();
        percentages.put(ItemType.INCOME, 0.0);
        percentages.put(ItemType.EXPENSE, 0.0);

        Map<String, Person> persons = new HashMap<>();
        persons.put("John", new Person("John", 1970));

        YearlySummary[] summaries = processor.generateYearlySummaries(percentages, persons);

        assertEquals(3, summaries.length); // 2024, 2025, 2026
        assertEquals(2024, summaries[0].year());
        assertEquals(2025, summaries[1].year());
        assertEquals(2026, summaries[2].year());
    }

    @Test
    public void testGenerateYearlySummaries_calculatesIncome() throws IOException {
        File csvFile = tempFolder.newFile("financial.csv");
        String content = "name,item,description,value,startYear,endYear\n" +
                "John,INCOME,Salary,100000,2024,2024";
        Files.writeString(csvFile.toPath(), content);

        processor.loadFromCsv(csvFile.getAbsolutePath());

        Map<ItemType, Double> percentages = new HashMap<>();
        Map<String, Person> persons = new HashMap<>();

        YearlySummary[] summaries = processor.generateYearlySummaries(percentages, persons);

        assertEquals(100000, summaries[0].totalIncome(), 0.01);
    }

    @Test
    public void testGenerateYearlySummaries_calculatesExpenses() throws IOException {
        File csvFile = tempFolder.newFile("financial.csv");
        String content = "name,item,description,value,startYear,endYear\n" +
                "John,EXPENSE,Living,50000,2024,2024";
        Files.writeString(csvFile.toPath(), content);

        processor.loadFromCsv(csvFile.getAbsolutePath());

        Map<ItemType, Double> percentages = new HashMap<>();
        Map<String, Person> persons = new HashMap<>();

        YearlySummary[] summaries = processor.generateYearlySummaries(percentages, persons);

        assertEquals(50000, summaries[0].totalExpenses(), 0.01);
    }

    @Test
    public void testGenerateYearlySummaries_withPercentageIncrease() throws IOException {
        File csvFile = tempFolder.newFile("financial.csv");
        String content = "name,item,description,value,startYear,endYear\n" +
                "John,INCOME,Salary,100000,2024,2026";
        Files.writeString(csvFile.toPath(), content);

        processor.loadFromCsv(csvFile.getAbsolutePath());

        Map<ItemType, Double> percentages = new HashMap<>();
        percentages.put(ItemType.INCOME, 5.0); // 5% annual increase

        Map<String, Person> persons = new HashMap<>();

        YearlySummary[] summaries = processor.generateYearlySummaries(percentages, persons);

        assertEquals(100000, summaries[0].totalIncome(), 0.01);
        assertEquals(100000 * 1.05, summaries[1].totalIncome(), 0.01);
        assertEquals(100000 * 1.05 * 1.05, summaries[2].totalIncome(), 0.01);
    }

    @Test
    public void testGenerateYearlySummaries_compoundsAssets() throws IOException {
        File csvFile = tempFolder.newFile("financial.csv");
        String content = "name,item,description,value,startYear,endYear\n" +
                "John,QUALIFIED,401k,500000,2024,2026";
        Files.writeString(csvFile.toPath(), content);

        processor.loadFromCsv(csvFile.getAbsolutePath());
        // Disable tax optimization strategy for this test to isolate asset compounding behavior
        processor.setTaxOptimizationStrategy(null);

        Map<ItemType, Double> percentages = new HashMap<>();
        percentages.put(ItemType.QUALIFIED, 7.0); // 7% annual growth

        Map<String, Person> persons = new HashMap<>();

        YearlySummary[] summaries = processor.generateYearlySummaries(percentages, persons);

        // Year 1: base value 500000
        assertEquals(500000, summaries[0].qualifiedAssets(), 0.01);
        // Year 2: 500000 * 1.07
        assertEquals(500000 * 1.07, summaries[1].qualifiedAssets(), 0.01);
        // Year 3: 500000 * 1.07 * 1.07
        assertEquals(500000 * 1.07 * 1.07, summaries[2].qualifiedAssets(), 0.01);
    }

    // ===== getSummaryForYear tests =====

    @Test
    public void testGetSummaryForYear_validYear() throws IOException {
        File csvFile = tempFolder.newFile("financial.csv");
        String content = "name,item,description,value,startYear,endYear\n" +
                "John,INCOME,Salary,100000,2024,2026";
        Files.writeString(csvFile.toPath(), content);

        processor.loadFromCsv(csvFile.getAbsolutePath());

        Map<ItemType, Double> percentages = new HashMap<>();
        Map<String, Person> persons = new HashMap<>();

        YearlySummary summary = processor.getSummaryForYear(2025, percentages, persons);

        assertNotNull(summary);
        assertEquals(2025, summary.year());
    }

    @Test
    public void testGetSummaryForYear_yearBeforeRange() throws IOException {
        File csvFile = tempFolder.newFile("financial.csv");
        String content = "name,item,description,value,startYear,endYear\n" +
                "John,INCOME,Salary,100000,2024,2026";
        Files.writeString(csvFile.toPath(), content);

        processor.loadFromCsv(csvFile.getAbsolutePath());

        Map<ItemType, Double> percentages = new HashMap<>();
        Map<String, Person> persons = new HashMap<>();

        YearlySummary summary = processor.getSummaryForYear(2020, percentages, persons);

        assertNull(summary);
    }

    @Test
    public void testGetSummaryForYear_yearAfterRange() throws IOException {
        File csvFile = tempFolder.newFile("financial.csv");
        String content = "name,item,description,value,startYear,endYear\n" +
                "John,INCOME,Salary,100000,2024,2026";
        Files.writeString(csvFile.toPath(), content);

        processor.loadFromCsv(csvFile.getAbsolutePath());

        Map<ItemType, Double> percentages = new HashMap<>();
        Map<String, Person> persons = new HashMap<>();

        YearlySummary summary = processor.getSummaryForYear(2030, percentages, persons);

        assertNull(summary);
    }

    @Test
    public void testGetSummaryForYear_noEntries() {
        Map<ItemType, Double> percentages = new HashMap<>();
        Map<String, Person> persons = new HashMap<>();

        YearlySummary summary = processor.getSummaryForYear(2025, percentages, persons);

        assertNull(summary);
    }

    // ===== Tax optimization strategy tests =====

    @Test
    public void testGetTaxOptimizationStrategy_defaultIsComposite() {
        TaxOptimizationStrategy strategy = processor.getTaxOptimizationStrategy();
        assertNotNull(strategy);
        assertEquals("Composite Tax Optimization", strategy.getStrategyName());
    }

    @Test
    public void testSetTaxOptimizationStrategy() {
        TaxOptimizationStrategy customStrategy = new TaxOptimizationStrategy() {
            @Override
            public void optimize(YearlySummary prev, YearlySummary current) {}

            @Override
            public String getStrategyName() {
                return "Custom Strategy";
            }

            @Override
            public String getDescription() {
                return "A custom strategy";
            }
        };

        processor.setTaxOptimizationStrategy(customStrategy);
        assertEquals("Custom Strategy", processor.getTaxOptimizationStrategy().getStrategyName());
    }

    @Test
    public void testSetTaxOptimizationStrategy_null() {
        processor.setTaxOptimizationStrategy(null);
        assertNull(processor.getTaxOptimizationStrategy());
    }

    // ===== CSV export tests =====

    @Test
    public void testExportYearlySummariesToCsv_basicExport() throws IOException {
        File csvFile = tempFolder.newFile("financial.csv");
        String content = "name,item,description,value,startYear,endYear\n" +
                "John,INCOME,Salary,100000,2024,2025\n" +
                "Jane,INCOME,Salary,80000,2024,2025";
        Files.writeString(csvFile.toPath(), content);

        processor.loadFromCsv(csvFile.getAbsolutePath());

        Map<ItemType, Double> percentages = new HashMap<>();
        Map<String, Person> persons = new HashMap<>();
        persons.put("John", new Person("John", 1960));
        persons.put("Jane", new Person("Jane", 1965));

        YearlySummary[] summaries = processor.generateYearlySummaries(percentages, persons);

        File outputFile = tempFolder.newFile("output.csv");
        processor.exportYearlySummariesToCsv(summaries, outputFile.getAbsolutePath());

        assertTrue(outputFile.exists());
        String csvContent = Files.readString(outputFile.toPath());

        // Check header
        assertTrue(csvContent.contains("Item,2024,2025"));

        // Check total income row
        assertTrue(csvContent.contains("Total Income"));

        // Check individual income rows
        assertTrue(csvContent.contains("John Income"));
        assertTrue(csvContent.contains("Jane Income"));

        // Check tax rows
        assertTrue(csvContent.contains("Federal Income Tax"));
        assertTrue(csvContent.contains("State Income Tax"));
        assertTrue(csvContent.contains("Capital Gains Tax"));
        assertTrue(csvContent.contains("Social Security Tax"));
        assertTrue(csvContent.contains("Medicare Tax"));

        // Check expenses row
        assertTrue(csvContent.contains("Total Expenses"));
    }

    @Test
    public void testExportYearlySummariesToCsv_includesWithdrawals() throws IOException {
        File csvFile = tempFolder.newFile("financial.csv");
        String content = "name,item,description,value,startYear,endYear\n" +
                "John,INCOME,Salary,50000,2024,2024\n" +
                "John,QUALIFIED,401k,500000,2024,2024";
        Files.writeString(csvFile.toPath(), content);

        processor.loadFromCsv(csvFile.getAbsolutePath());

        Map<ItemType, Double> percentages = new HashMap<>();
        Map<String, Person> persons = new HashMap<>();
        persons.put("John", new Person("John", 1952)); // RMD age

        YearlySummary[] summaries = processor.generateYearlySummaries(percentages, persons);

        File outputFile = tempFolder.newFile("output.csv");
        processor.exportYearlySummariesToCsv(summaries, outputFile.getAbsolutePath());

        String csvContent = Files.readString(outputFile.toPath());

        // Check withdrawal rows exist
        assertTrue(csvContent.contains("Total RMD Withdrawals"));
        assertTrue(csvContent.contains("John RMD Withdrawals"));
        assertTrue(csvContent.contains("Total Qualified Withdrawals"));
        assertTrue(csvContent.contains("John Qualified Withdrawals"));
        assertTrue(csvContent.contains("Total Non-Qualified Withdrawals"));
        assertTrue(csvContent.contains("John Non-Qualified Withdrawals"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testExportYearlySummariesToCsv_nullSummaries() throws IOException {
        File outputFile = tempFolder.newFile("output.csv");
        processor.exportYearlySummariesToCsv(null, outputFile.getAbsolutePath());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testExportYearlySummariesToCsv_emptySummaries() throws IOException {
        File outputFile = tempFolder.newFile("output.csv");
        processor.exportYearlySummariesToCsv(new YearlySummary[0], outputFile.getAbsolutePath());
    }

    @Test
    public void testExportYearlySummariesToCsv_formatsValuesCorrectly() throws IOException {
        File csvFile = tempFolder.newFile("financial.csv");
        String content = "name,item,description,value,startYear,endYear\n" +
                "John,INCOME,Salary,100000,2024,2024";
        Files.writeString(csvFile.toPath(), content);

        processor.loadFromCsv(csvFile.getAbsolutePath());

        Map<ItemType, Double> percentages = new HashMap<>();
        Map<String, Person> persons = new HashMap<>();
        persons.put("John", new Person("John", 1960));

        YearlySummary[] summaries = processor.generateYearlySummaries(percentages, persons);

        File outputFile = tempFolder.newFile("output.csv");
        processor.exportYearlySummariesToCsv(summaries, outputFile.getAbsolutePath());

        String csvContent = Files.readString(outputFile.toPath());

        // Values should be formatted with 2 decimal places
        assertTrue(csvContent.contains("100000.00"));
    }

    @Test
    public void testExportYearlySummariesToCsv_withPath() throws IOException {
        File csvFile = tempFolder.newFile("financial.csv");
        String content = "name,item,description,value,startYear,endYear\n" +
                "John,INCOME,Salary,100000,2024,2024";
        Files.writeString(csvFile.toPath(), content);

        processor.loadFromCsv(csvFile.getAbsolutePath());

        Map<ItemType, Double> percentages = new HashMap<>();
        Map<String, Person> persons = new HashMap<>();
        persons.put("John", new Person("John", 1960));

        YearlySummary[] summaries = processor.generateYearlySummaries(percentages, persons);

        File outputFile = tempFolder.newFile("output.csv");
        processor.exportYearlySummariesToCsv(summaries, outputFile.toPath());

        assertTrue(outputFile.exists());
    }

    @Test
    public void testExportYearlySummariesToCsv_includesAgeRows() throws IOException {
        File csvFile = tempFolder.newFile("financial.csv");
        String content = "name,item,description,value,startYear,endYear\n" +
                "John,INCOME,Salary,100000,2024,2025\n" +
                "Jane,INCOME,Salary,80000,2024,2025";
        Files.writeString(csvFile.toPath(), content);

        processor.loadFromCsv(csvFile.getAbsolutePath());

        Map<ItemType, Double> percentages = new HashMap<>();
        Map<String, Person> persons = new HashMap<>();
        persons.put("John", new Person("John", 1960));
        persons.put("Jane", new Person("Jane", 1965));

        YearlySummary[] summaries = processor.generateYearlySummaries(percentages, persons);

        File outputFile = tempFolder.newFile("output.csv");
        processor.exportYearlySummariesToCsv(summaries, outputFile.getAbsolutePath());

        String csvContent = Files.readString(outputFile.toPath());

        // Check age rows exist with correct format (just the age number, not Name(age))
        // John born 1960, should be 64 in 2024 and 65 in 2025
        assertTrue(csvContent.contains("John Age,64,65"));
        // Jane born 1965, should be 59 in 2024 and 60 in 2025
        assertTrue(csvContent.contains("Jane Age,59,60"));
    }

    @Test
    public void testExportYearlySummariesToCsv_includesSocialSecurityBenefits() throws IOException {
        File csvFile = tempFolder.newFile("financial.csv");
        String content = "name,item,description,value,startYear,endYear\n" +
                "John,INCOME,Salary,50000,2024,2024\n" +
                "John,SOCIAL_SECURITY_BENEFITS,SS,24000,2024,2024\n" +
                "Jane,INCOME,Salary,40000,2024,2024\n" +
                "Jane,SOCIAL_SECURITY_BENEFITS,SS,18000,2024,2024";
        Files.writeString(csvFile.toPath(), content);

        processor.loadFromCsv(csvFile.getAbsolutePath());

        Map<ItemType, Double> percentages = new HashMap<>();
        Map<String, Person> persons = new HashMap<>();
        persons.put("John", new Person("John", 1960));
        persons.put("Jane", new Person("Jane", 1965));

        YearlySummary[] summaries = processor.generateYearlySummaries(percentages, persons);

        File outputFile = tempFolder.newFile("output.csv");
        processor.exportYearlySummariesToCsv(summaries, outputFile.getAbsolutePath());

        String csvContent = Files.readString(outputFile.toPath());

        // Check social security rows exist
        assertTrue(csvContent.contains("Total Social Security Benefits"));
        assertTrue(csvContent.contains("John Social Security Benefits"));
        assertTrue(csvContent.contains("Jane Social Security Benefits"));
        // Check values
        assertTrue(csvContent.contains("42000.00")); // Total SS: 24000 + 18000
        assertTrue(csvContent.contains("24000.00")); // John SS
        assertTrue(csvContent.contains("18000.00")); // Jane SS
    }

    // ===== Validation tests =====

    @Test
    public void testValidationEnabled_defaultIsTrue() {
        FinancialDataProcessor newProcessor = new FinancialDataProcessor();
        assertTrue(newProcessor.isValidationEnabled());
    }

    @Test
    public void testSetValidationEnabled() {
        FinancialDataProcessor newProcessor = new FinancialDataProcessor();
        newProcessor.setValidationEnabled(false);
        assertFalse(newProcessor.isValidationEnabled());
        newProcessor.setValidationEnabled(true);
        assertTrue(newProcessor.isValidationEnabled());
    }

    @Test(expected = IllegalStateException.class)
    public void testValidation_throwsOnCashFlowImbalance() throws IOException {
        // Create a processor with validation enabled but NO tax optimization strategy
        // This will cause cash flow imbalance since surplus won't be handled
        FinancialDataProcessor validatingProcessor = new FinancialDataProcessor();
        validatingProcessor.setValidationEnabled(true);
        validatingProcessor.setTaxOptimizationStrategy(null); // Disable strategies to cause imbalance

        File csvFile = tempFolder.newFile("financial_unbalanced.csv");
        // This data has income but no strategy to handle surplus, so cash flow won't balance
        String content = "name,item,description,value,startYear,endYear\n" +
                "John,INCOME,Salary,100000,2024,2024";
        Files.writeString(csvFile.toPath(), content);

        validatingProcessor.loadFromCsv(csvFile.getAbsolutePath());

        Map<ItemType, Double> percentages = new HashMap<>();
        Map<String, Person> persons = new HashMap<>();
        persons.put("John", new Person("John", 1980));

        // This should throw IllegalStateException due to cash flow imbalance
        // (income of 100000 with no expenses, taxes, or contributions recorded)
        validatingProcessor.generateYearlySummaries(percentages, persons);
    }

    @Test
    public void testValidation_passesWhenDisabled() throws IOException {
        // Validation is disabled in setUp, so this should not throw
        File csvFile = tempFolder.newFile("financial_unbalanced2.csv");
        String content = "name,item,description,value,startYear,endYear\n" +
                "John,INCOME,Salary,100000,2024,2024";
        Files.writeString(csvFile.toPath(), content);

        processor.loadFromCsv(csvFile.getAbsolutePath());

        Map<ItemType, Double> percentages = new HashMap<>();
        Map<String, Person> persons = new HashMap<>();
        persons.put("John", new Person("John", 1980));

        // Should not throw since validation is disabled
        YearlySummary[] summaries = processor.generateYearlySummaries(percentages, persons);
        assertEquals(1, summaries.length);
    }
}
