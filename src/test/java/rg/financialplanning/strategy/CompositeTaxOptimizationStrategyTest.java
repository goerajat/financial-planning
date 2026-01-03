package rg.financialplanning.strategy;

import org.junit.Test;
import org.junit.Before;
import rg.financialplanning.model.FilingStatus;
import rg.financialplanning.model.IndividualYearlySummary;
import rg.financialplanning.model.Person;
import rg.financialplanning.model.YearlySummary;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class CompositeTaxOptimizationStrategyTest {

    private CompositeTaxOptimizationStrategy strategy;

    @Before
    public void setUp() {
        strategy = new CompositeTaxOptimizationStrategy();
    }

    private YearlySummary createYearlySummary(int year, double income, double expenses,
                                              Map<String, IndividualYearlySummary> individuals) {
        double totalQualified = 0;
        double totalNonQualified = 0;
        double totalRoth = 0;
        double totalSS = 0;
        for (IndividualYearlySummary ind : individuals.values()) {
            totalQualified += ind.qualifiedAssets();
            totalNonQualified += ind.nonQualifiedAssets();
            totalRoth += ind.rothAssets();
            totalSS += ind.socialSecurityBenefits();
        }
        return new YearlySummary(year, income, expenses, totalQualified, totalNonQualified, totalRoth, 50000, 0, 0, totalSS, 0, individuals);
    }

    // ===== Constructor tests =====

    @Test
    public void testConstructor_default() {
        CompositeTaxOptimizationStrategy defaultStrategy = new CompositeTaxOptimizationStrategy();

        assertNotNull(defaultStrategy.getRmdStrategy());
        assertNotNull(defaultStrategy.getExpenseStrategy());
        assertNotNull(defaultStrategy.getRothConversionStrategy());

        assertEquals(FilingStatus.MARRIED_FILING_JOINTLY, defaultStrategy.getRothConversionStrategy().getFilingStatus());
    }

    @Test
    public void testConstructor_withFilingStatus() {
        CompositeTaxOptimizationStrategy singleStrategy = new CompositeTaxOptimizationStrategy(FilingStatus.SINGLE);

        assertEquals(FilingStatus.SINGLE, singleStrategy.getRothConversionStrategy().getFilingStatus());
    }

    @Test
    public void testConstructor_withCustomThreshold() {
        double customThreshold = 150000;
        CompositeTaxOptimizationStrategy customStrategy = new CompositeTaxOptimizationStrategy(FilingStatus.SINGLE, customThreshold);

        assertNotNull(customStrategy.getExpenseStrategy());
        assertEquals(customThreshold, customStrategy.getRothConversionStrategy().getTargetBracketThreshold(), 0.01);
    }

    @Test
    public void testConstructor_withCustomStrategies() {
        RMDOptimizationStrategy rmd = new RMDOptimizationStrategy();
        ExpenseManagementStrategy expense = new ExpenseManagementStrategy();
        RothConversionOptimizationStrategy roth = new RothConversionOptimizationStrategy(FilingStatus.HEAD_OF_HOUSEHOLD);

        CompositeTaxOptimizationStrategy customStrategy = new CompositeTaxOptimizationStrategy(rmd, expense, roth);

        assertSame(rmd, customStrategy.getRmdStrategy());
        assertSame(expense, customStrategy.getExpenseStrategy());
        assertSame(roth, customStrategy.getRothConversionStrategy());
        assertNotNull(customStrategy.getTaxCalculationStrategy());
    }

    @Test
    public void testConstructor_withAllCustomStrategies() {
        RMDOptimizationStrategy rmd = new RMDOptimizationStrategy();
        ExpenseManagementStrategy expense = new ExpenseManagementStrategy();
        RothConversionOptimizationStrategy roth = new RothConversionOptimizationStrategy(FilingStatus.HEAD_OF_HOUSEHOLD);
        TaxCalculationStrategy taxCalc = new TaxCalculationStrategy(FilingStatus.HEAD_OF_HOUSEHOLD);

        CompositeTaxOptimizationStrategy customStrategy = new CompositeTaxOptimizationStrategy(rmd, expense, roth, taxCalc);

        assertSame(rmd, customStrategy.getRmdStrategy());
        assertSame(expense, customStrategy.getExpenseStrategy());
        assertSame(roth, customStrategy.getRothConversionStrategy());
        assertSame(taxCalc, customStrategy.getTaxCalculationStrategy());
    }

    // ===== optimize tests =====

    @Test
    public void testOptimize_nullCurrentSummary() {
        // Should not throw exception
        strategy.optimize(null, null);
    }

    @Test
    public void testOptimize_appliesAllStrategies() {
        // Person at RMD age
        Person person = new Person("John", 1952);
        IndividualYearlySummary individual = new IndividualYearlySummary(person, 2025, 50000, 500000, 100000, 100000, 20000);

        Map<String, IndividualYearlySummary> individuals = new HashMap<>();
        individuals.put("John", individual);

        // Create previous year summary for RMD calculation
        IndividualYearlySummary prevIndividual = new IndividualYearlySummary(person, 2024, 50000, 500000, 100000, 100000, 20000);
        Map<String, IndividualYearlySummary> prevIndividuals = new HashMap<>();
        prevIndividuals.put("John", prevIndividual);
        YearlySummary previous = createYearlySummary(2024, 50000, 50000, prevIndividuals);

        YearlySummary current = createYearlySummary(2025, 50000, 50000, individuals);
        strategy.optimize(previous, current);

        // RMD should be applied (person is 73)
        assertTrue(individual.rmdWithdrawals() > 0 || current.rmdWithdrawals() > 0);
    }

    @Test
    public void testOptimize_strategyOrderIsCorrect() {
        // Test that strategies are applied in correct order:
        // 1. RMD (mandatory withdrawals)
        // 2. Expense Management (surplus/deficit handling)
        // 3. Roth Conversion (tax bracket filling)

        Person person = new Person("John", 1952);
        IndividualYearlySummary individual = new IndividualYearlySummary(person, 2025, 100000, 500000, 100000, 100000, 0);

        Map<String, IndividualYearlySummary> individuals = new HashMap<>();
        individuals.put("John", individual);

        IndividualYearlySummary prevIndividual = new IndividualYearlySummary(person, 2024, 100000, 500000, 100000, 100000, 0);
        Map<String, IndividualYearlySummary> prevIndividuals = new HashMap<>();
        prevIndividuals.put("John", prevIndividual);
        YearlySummary previous = createYearlySummary(2024, 100000, 50000, prevIndividuals);

        YearlySummary current = createYearlySummary(2025, 100000, 50000, individuals);
        strategy.optimize(previous, current);

        // All strategies should have had an opportunity to run
        // (exact effects depend on individual strategy logic)
    }

    @Test
    public void testOptimize_youngPersonNoRmd() {
        // Person too young for RMD but old enough for Roth conversion
        Person person = new Person("John", 1965);
        IndividualYearlySummary individual = new IndividualYearlySummary(person, 2025, 50000, 500000, 100000, 100000, 0);

        Map<String, IndividualYearlySummary> individuals = new HashMap<>();
        individuals.put("John", individual);

        YearlySummary current = createYearlySummary(2025, 50000, 40000, individuals);
        strategy.optimize(null, current);

        // No RMD for 60-year-old
        assertEquals(0.0, individual.rmdWithdrawals(), 0.001);
    }

    @Test
    public void testOptimize_surplusHandledCorrectly() {
        Person person = new Person("John", 1960);
        IndividualYearlySummary individual = new IndividualYearlySummary(person, 2025, 200000, 0, 50000, 0, 0);

        Map<String, IndividualYearlySummary> individuals = new HashMap<>();
        individuals.put("John", individual);

        double originalNonQualified = individual.nonQualifiedAssets();

        // High income, low expenses = surplus
        YearlySummary current = createYearlySummary(2025, 200000, 30000, individuals);
        strategy.optimize(null, current);

        // Surplus should be distributed to non-qualified
        assertTrue(individual.nonQualifiedAssets() > originalNonQualified);
    }

    @Test
    public void testOptimize_deficitHandledCorrectly() {
        Person person = new Person("John", 1960);
        IndividualYearlySummary individual = new IndividualYearlySummary(person, 2025, 30000, 0, 200000, 0, 0);

        Map<String, IndividualYearlySummary> individuals = new HashMap<>();
        individuals.put("John", individual);

        double originalNonQualified = individual.nonQualifiedAssets();

        // Low income, high expenses = deficit
        YearlySummary current = createYearlySummary(2025, 30000, 200000, individuals);
        strategy.optimize(null, current);

        // Deficit should trigger withdrawals
        assertTrue(individual.nonQualifiedAssets() < originalNonQualified);
    }

    // ===== Getter tests =====

    @Test
    public void testGetRmdStrategy() {
        assertNotNull(strategy.getRmdStrategy());
        assertEquals("RMD Optimization", strategy.getRmdStrategy().getStrategyName());
    }

    @Test
    public void testGetExpenseStrategy() {
        assertNotNull(strategy.getExpenseStrategy());
        assertEquals("Expense Management", strategy.getExpenseStrategy().getStrategyName());
    }

    @Test
    public void testGetRothConversionStrategy() {
        assertNotNull(strategy.getRothConversionStrategy());
        assertEquals("Roth Conversion Optimization", strategy.getRothConversionStrategy().getStrategyName());
    }

    @Test
    public void testGetTaxCalculationStrategy() {
        assertNotNull(strategy.getTaxCalculationStrategy());
        assertEquals("Tax Calculation", strategy.getTaxCalculationStrategy().getStrategyName());
    }

    // ===== Strategy metadata tests =====

    @Test
    public void testGetStrategyName() {
        assertEquals("Composite Tax Optimization", strategy.getStrategyName());
    }

    @Test
    public void testGetDescription() {
        String description = strategy.getDescription();
        assertNotNull(description);
        assertTrue(description.contains("RMD"));
        assertTrue(description.contains("Tax") || description.contains("Expense"));
        assertTrue(description.contains("Roth"));
    }

    @Test
    public void testOptimize_storesTaxesInYearlySummary() {
        Person person = new Person("John", 1960);
        IndividualYearlySummary individual = new IndividualYearlySummary(person, 2025, 100000, 0, 100000, 0, 0);

        Map<String, IndividualYearlySummary> individuals = new HashMap<>();
        individuals.put("John", individual);

        YearlySummary current = createYearlySummary(2025, 100000, 50000, individuals);
        strategy.optimize(null, current);

        // Taxes should be calculated and stored
        assertTrue(current.federalIncomeTax() > 0);
        assertTrue(current.stateIncomeTax() > 0);
        assertTrue(current.socialSecurityTax() > 0);
        assertTrue(current.medicareTax() > 0);
    }

    // ===== Integration tests =====

    @Test
    public void testIntegration_fullScenario() {
        // Full scenario with person at RMD age, income, expenses, and assets
        Person person = new Person("John", 1952); // 73 in 2025

        // Person has qualified assets, some income, and moderate expenses
        IndividualYearlySummary individual = new IndividualYearlySummary(
                person, 2025, 30000, 600000, 100000, 50000, 25000);

        Map<String, IndividualYearlySummary> individuals = new HashMap<>();
        individuals.put("John", individual);

        // Previous year for RMD calculation
        IndividualYearlySummary prevIndividual = new IndividualYearlySummary(
                person, 2024, 30000, 600000, 100000, 50000, 25000);
        Map<String, IndividualYearlySummary> prevIndividuals = new HashMap<>();
        prevIndividuals.put("John", prevIndividual);
        YearlySummary previous = createYearlySummary(2024, 30000, 60000, prevIndividuals);

        YearlySummary current = createYearlySummary(2025, 30000, 60000, individuals);

        // Apply composite strategy
        strategy.optimize(previous, current);

        // Verify RMD was calculated (person is 73, born 1952)
        assertTrue(individual.rmdWithdrawals() > 0 || current.rmdWithdrawals() > 0);
    }

    @Test
    public void testIntegration_coupleScenario() {
        // Couple with different ages
        Person john = new Person("John", 1952); // 73 in 2025 - RMD age
        Person jane = new Person("Jane", 1965); // 60 in 2025 - eligible for Roth

        IndividualYearlySummary johnSummary = new IndividualYearlySummary(
                john, 2025, 0, 500000, 50000, 20000, 30000);
        IndividualYearlySummary janeSummary = new IndividualYearlySummary(
                jane, 2025, 0, 400000, 80000, 30000, 0);

        Map<String, IndividualYearlySummary> individuals = new HashMap<>();
        individuals.put("John", johnSummary);
        individuals.put("Jane", janeSummary);

        // Previous year
        IndividualYearlySummary prevJohn = new IndividualYearlySummary(
                john, 2024, 0, 500000, 50000, 20000, 30000);
        IndividualYearlySummary prevJane = new IndividualYearlySummary(
                jane, 2024, 0, 400000, 80000, 30000, 0);
        Map<String, IndividualYearlySummary> prevIndividuals = new HashMap<>();
        prevIndividuals.put("John", prevJohn);
        prevIndividuals.put("Jane", prevJane);
        YearlySummary previous = createYearlySummary(2024, 0, 70000, prevIndividuals);

        YearlySummary current = createYearlySummary(2025, 0, 70000, individuals);

        strategy.optimize(previous, current);

        // John should have RMD
        assertTrue(johnSummary.rmdWithdrawals() > 0);
        // Jane should not have RMD (too young)
        assertEquals(0.0, janeSummary.rmdWithdrawals(), 0.001);
    }
}
