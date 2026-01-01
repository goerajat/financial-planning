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

public class ExpenseManagementStrategyTest {

    private ExpenseManagementStrategy strategy;

    @Before
    public void setUp() {
        strategy = new ExpenseManagementStrategy();
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
        ExpenseManagementStrategy defaultStrategy = new ExpenseManagementStrategy();
        assertEquals(FilingStatus.MARRIED_FILING_JOINTLY, defaultStrategy.getFilingStatus());
    }

    @Test
    public void testConstructor_withFilingStatus() {
        ExpenseManagementStrategy singleStrategy = new ExpenseManagementStrategy(FilingStatus.SINGLE);
        assertEquals(FilingStatus.SINGLE, singleStrategy.getFilingStatus());
    }

    // ===== optimize tests =====

    @Test
    public void testOptimize_nullCurrentSummary() {
        // Should not throw exception
        strategy.optimize(null, null);
    }

    @Test
    public void testOptimize_noIndividuals() {
        Map<String, IndividualYearlySummary> individuals = new HashMap<>();
        YearlySummary current = createYearlySummary(2025, 100000, 50000, individuals);
        strategy.optimize(null, current);
        // No exception
    }

    @Test
    public void testOptimize_surplus_distributedToNonQualified() {
        Person person = new Person("John", 1960);
        IndividualYearlySummary individual = new IndividualYearlySummary(person, 2025, 200000, 0, 100000, 0, 0);

        Map<String, IndividualYearlySummary> individuals = new HashMap<>();
        individuals.put("John", individual);

        double originalNonQualified = individual.nonQualifiedAssets();

        // High income, low expenses = surplus
        YearlySummary current = createYearlySummary(2025, 200000, 50000, individuals);
        strategy.optimize(null, current);

        // Non-qualified assets should increase due to surplus
        assertTrue(individual.nonQualifiedAssets() > originalNonQualified);
        assertTrue(individual.nonQualifiedContributions() > 0);
    }

    @Test
    public void testOptimize_surplus_distributedEquallyAmongIndividuals() {
        Person john = new Person("John", 1960);
        Person jane = new Person("Jane", 1962);

        IndividualYearlySummary johnSummary = new IndividualYearlySummary(john, 2025, 100000, 0, 50000, 0, 0);
        IndividualYearlySummary janeSummary = new IndividualYearlySummary(jane, 2025, 100000, 0, 50000, 0, 0);

        Map<String, IndividualYearlySummary> individuals = new HashMap<>();
        individuals.put("John", johnSummary);
        individuals.put("Jane", janeSummary);

        // High income, low expenses
        YearlySummary current = createYearlySummary(2025, 200000, 50000, individuals);
        strategy.optimize(null, current);

        // Both should receive equal surplus
        assertEquals(johnSummary.nonQualifiedContributions(), janeSummary.nonQualifiedContributions(), 0.01);
    }

    @Test
    public void testOptimize_deficit_withdrawFromNonQualified() {
        // Person at age eligible for qualified withdrawals
        Person person = new Person("John", 1960);
        IndividualYearlySummary individual = new IndividualYearlySummary(person, 2025, 50000, 0, 200000, 0, 0);

        Map<String, IndividualYearlySummary> individuals = new HashMap<>();
        individuals.put("John", individual);

        double originalNonQualified = individual.nonQualifiedAssets();

        // Low income, high expenses = deficit
        YearlySummary current = createYearlySummary(2025, 50000, 150000, individuals);
        strategy.optimize(null, current);

        // Non-qualified should decrease
        assertTrue(individual.nonQualifiedAssets() < originalNonQualified);
        assertTrue(individual.nonQualifiedWithdrawals() > 0);
    }

    @Test
    public void testOptimize_deficit_withdrawFromQualifiedIfNonQualifiedInsufficient() {
        // Person at age eligible for qualified withdrawals (65 in 2025)
        Person person = new Person("John", 1960);
        IndividualYearlySummary individual = new IndividualYearlySummary(person, 2025, 50000, 500000, 10000, 0, 0);

        Map<String, IndividualYearlySummary> individuals = new HashMap<>();
        individuals.put("John", individual);

        double originalQualified = individual.qualifiedAssets();

        // Very high expenses, low non-qualified
        YearlySummary current = createYearlySummary(2025, 50000, 200000, individuals);
        strategy.optimize(null, current);

        // Should withdraw from qualified after non-qualified is exhausted
        assertTrue(individual.qualifiedAssets() < originalQualified);
    }

    @Test
    public void testOptimize_deficit_underageCannotWithdrawFromQualified() {
        // Person too young for qualified withdrawals (45 in 2025)
        Person person = new Person("John", 1980);
        IndividualYearlySummary individual = new IndividualYearlySummary(person, 2025, 50000, 500000, 10000, 0, 0);

        Map<String, IndividualYearlySummary> individuals = new HashMap<>();
        individuals.put("John", individual);

        double originalQualified = individual.qualifiedAssets();

        // High expenses, but person is too young for qualified withdrawal
        YearlySummary current = createYearlySummary(2025, 50000, 200000, individuals);
        strategy.optimize(null, current);

        // Qualified assets should remain unchanged (too young to withdraw)
        assertEquals(originalQualified, individual.qualifiedAssets(), 0.01);
    }

    @Test
    public void testOptimize_deficit_withdrawFromRothIfQualifiedInsufficient() {
        Person person = new Person("John", 1960);
        IndividualYearlySummary individual = new IndividualYearlySummary(person, 2025, 50000, 10000, 10000, 200000, 0);

        Map<String, IndividualYearlySummary> individuals = new HashMap<>();
        individuals.put("John", individual);

        double originalRoth = individual.rothAssets();

        // Very high expenses
        YearlySummary current = createYearlySummary(2025, 50000, 250000, individuals);
        strategy.optimize(null, current);

        // Should withdraw from Roth after qualified is exhausted
        assertTrue(individual.rothAssets() < originalRoth);
        assertTrue(individual.rothWithdrawals() > 0);
    }

    @Test
    public void testOptimize_deficit_withdrawFromCashIfOtherAssetsInsufficient() {
        Person person = new Person("John", 1960);
        IndividualYearlySummary individual = new IndividualYearlySummary(person, 2025, 50000, 10000, 10000, 10000, 0);

        Map<String, IndividualYearlySummary> individuals = new HashMap<>();
        individuals.put("John", individual);

        // Create summary with cash
        YearlySummary current = new YearlySummary(2025, 50000, 300000, 10000, 10000, 10000, 100000, 0, 0, 0, 0, individuals);

        double originalCash = current.cash();
        strategy.optimize(null, current);

        // Should withdraw from cash
        assertTrue(current.cash() < originalCash);
        assertTrue(current.cashWithdrawals() > 0);
    }

    @Test
    public void testOptimize_deficit_tracksRemainingDeficit() {
        Person person = new Person("John", 1960);
        IndividualYearlySummary individual = new IndividualYearlySummary(person, 2025, 0, 10000, 10000, 10000, 0);

        Map<String, IndividualYearlySummary> individuals = new HashMap<>();
        individuals.put("John", individual);

        // Very high expenses, insufficient assets
        YearlySummary current = new YearlySummary(2025, 0, 500000, 10000, 10000, 10000, 10000, 0, 0, 0, 0, individuals);
        strategy.optimize(null, current);

        // Should track remaining deficit
        assertTrue(current.deficit() > 0);
    }

    @Test
    public void testOptimize_noDeficitNoSurplus() {
        Person person = new Person("John", 1960);
        IndividualYearlySummary individual = new IndividualYearlySummary(person, 2025, 100000, 0, 50000, 0, 0);

        Map<String, IndividualYearlySummary> individuals = new HashMap<>();
        individuals.put("John", individual);

        double originalNonQualified = individual.nonQualifiedAssets();

        // Income exactly covers expenses after taxes (approximately)
        YearlySummary current = createYearlySummary(2025, 100000, 100000, individuals);
        strategy.optimize(null, current);

        // This test just verifies no exception - actual balance depends on tax calculations
    }

    // ===== calculateSurplus tests =====

    @Test
    public void testCalculateSurplus_nullSummary() {
        double surplus = strategy.calculateSurplus(null);
        assertEquals(0.0, surplus, 0.001);
    }

    @Test
    public void testCalculateSurplus_positiveSurplus() {
        Person person = new Person("John", 1960);
        IndividualYearlySummary individual = new IndividualYearlySummary(person, 2025, 200000, 0, 100000, 0, 0);

        Map<String, IndividualYearlySummary> individuals = new HashMap<>();
        individuals.put("John", individual);

        YearlySummary summary = createYearlySummary(2025, 200000, 50000, individuals);
        double surplus = strategy.calculateSurplus(summary);

        // Should have positive surplus (high income, low expenses)
        assertTrue(surplus > 0);
    }

    @Test
    public void testCalculateSurplus_negativeSurplus() {
        Person person = new Person("John", 1960);
        IndividualYearlySummary individual = new IndividualYearlySummary(person, 2025, 50000, 0, 100000, 0, 0);

        Map<String, IndividualYearlySummary> individuals = new HashMap<>();
        individuals.put("John", individual);

        YearlySummary summary = createYearlySummary(2025, 50000, 200000, individuals);
        double surplus = strategy.calculateSurplus(summary);

        // Should have negative surplus (low income, high expenses)
        assertTrue(surplus < 0);
    }

    // ===== calculateTaxBreakdown tests =====

    @Test
    public void testCalculateTaxBreakdown_nullSummary() {
        double[] breakdown = strategy.calculateTaxBreakdown(null);
        assertEquals(5, breakdown.length);
        for (double value : breakdown) {
            assertEquals(0.0, value, 0.001);
        }
    }

    @Test
    public void testCalculateTaxBreakdown_validSummary() {
        Person person = new Person("John", 1960);
        IndividualYearlySummary individual = new IndividualYearlySummary(person, 2025, 100000, 0, 50000, 0, 0);

        Map<String, IndividualYearlySummary> individuals = new HashMap<>();
        individuals.put("John", individual);

        YearlySummary summary = createYearlySummary(2025, 100000, 50000, individuals);
        double[] breakdown = strategy.calculateTaxBreakdown(summary);

        assertEquals(5, breakdown.length);
        // [federalTax, stateTax, socialSecurityTax, medicareTax, totalTaxes]
        assertTrue(breakdown[0] > 0); // Federal tax
        assertTrue(breakdown[1] > 0); // State tax
        assertTrue(breakdown[2] > 0); // Social Security tax
        assertTrue(breakdown[3] > 0); // Medicare tax
        assertEquals(breakdown[0] + breakdown[1] + breakdown[2] + breakdown[3], breakdown[4], 0.01);
    }

    // ===== Strategy metadata tests =====

    @Test
    public void testGetStrategyName() {
        assertEquals("Expense Management", strategy.getStrategyName());
    }

    @Test
    public void testGetDescription() {
        String description = strategy.getDescription();
        assertNotNull(description);
        assertTrue(description.contains("expense"));
        assertTrue(description.contains("surplus") || description.contains("deficit"));
    }

    // ===== Constants tests =====

    @Test
    public void testQualifiedWithdrawalMinAge() {
        assertEquals(59, ExpenseManagementStrategy.QUALIFIED_WITHDRAWAL_MIN_AGE);
    }
}
