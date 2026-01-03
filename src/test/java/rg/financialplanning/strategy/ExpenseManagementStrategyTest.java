package rg.financialplanning.strategy;

import org.junit.Test;
import org.junit.Before;
import rg.financialplanning.model.IndividualYearlySummary;
import rg.financialplanning.model.Person;
import rg.financialplanning.model.YearlySummary;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class ExpenseManagementStrategyTest {

    private ExpenseManagementStrategy strategy;
    private TaxCalculationStrategy taxCalculationStrategy;

    @Before
    public void setUp() {
        strategy = new ExpenseManagementStrategy();
        taxCalculationStrategy = new TaxCalculationStrategy();
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

    /**
     * Helper method to apply tax calculation before expense management.
     * This mirrors the flow in CompositeTaxOptimizationStrategy.
     */
    private void applyTaxThenExpense(YearlySummary summary) {
        taxCalculationStrategy.optimize(null, summary);
        strategy.optimize(null, summary);
    }

    // ===== Constructor tests =====

    @Test
    public void testConstructor_default() {
        ExpenseManagementStrategy defaultStrategy = new ExpenseManagementStrategy();
        assertNotNull(defaultStrategy);
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
        applyTaxThenExpense(current);
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
        applyTaxThenExpense(current);

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
        applyTaxThenExpense(current);

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
        applyTaxThenExpense(current);

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
        applyTaxThenExpense(current);

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
        applyTaxThenExpense(current);

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
        applyTaxThenExpense(current);

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
        applyTaxThenExpense(current);

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
        applyTaxThenExpense(current);

        // Should track remaining deficit
        assertTrue(current.deficit() > 0);
    }

    @Test
    public void testOptimize_noDeficitNoSurplus() {
        Person person = new Person("John", 1960);
        IndividualYearlySummary individual = new IndividualYearlySummary(person, 2025, 100000, 0, 50000, 0, 0);

        Map<String, IndividualYearlySummary> individuals = new HashMap<>();
        individuals.put("John", individual);

        // Income exactly covers expenses after taxes (approximately)
        YearlySummary current = createYearlySummary(2025, 100000, 100000, individuals);
        applyTaxThenExpense(current);

        // This test just verifies no exception - actual balance depends on tax calculations
    }

    // ===== Cash flow balance tests =====

    @Test
    public void testOptimize_usesCashInflowMinusOutflow() {
        Person person = new Person("John", 1960);
        IndividualYearlySummary individual = new IndividualYearlySummary(person, 2025, 100000, 0, 50000, 0, 0);

        Map<String, IndividualYearlySummary> individuals = new HashMap<>();
        individuals.put("John", individual);

        YearlySummary current = createYearlySummary(2025, 100000, 50000, individuals);

        // Apply tax calculation first (as CompositeTaxOptimizationStrategy does)
        taxCalculationStrategy.optimize(null, current);

        // Before expense management, check cash flow
        double initialInflows = current.totalCashInflows();
        double initialOutflows = current.totalCashOutflows();

        // Apply expense management
        strategy.optimize(null, current);

        // After expense management, cash flow should be closer to balanced
        // (surplus distributed to non-qualified contributions increases outflows)
        double finalOutflows = current.totalCashOutflows();

        // If there was a surplus, outflows should have increased due to non-qualified contributions
        if (initialInflows > initialOutflows) {
            assertTrue(finalOutflows >= initialOutflows);
        }
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
        assertTrue(description.contains("expense") || description.contains("Expense"));
        assertTrue(description.contains("surplus") || description.contains("deficit"));
        assertTrue(description.contains("cash inflows") || description.contains("inflows"));
    }

    // ===== Constants tests =====

    @Test
    public void testQualifiedWithdrawalMinAge() {
        assertEquals(59, ExpenseManagementStrategy.QUALIFIED_WITHDRAWAL_MIN_AGE);
    }
}
