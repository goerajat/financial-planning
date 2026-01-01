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

public class TaxCalculationStrategyTest {

    private TaxCalculationStrategy strategy;

    @Before
    public void setUp() {
        strategy = new TaxCalculationStrategy();
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
        TaxCalculationStrategy defaultStrategy = new TaxCalculationStrategy();
        assertEquals(FilingStatus.MARRIED_FILING_JOINTLY, defaultStrategy.getFilingStatus());
    }

    @Test
    public void testConstructor_withFilingStatus() {
        TaxCalculationStrategy singleStrategy = new TaxCalculationStrategy(FilingStatus.SINGLE);
        assertEquals(FilingStatus.SINGLE, singleStrategy.getFilingStatus());
    }

    // ===== optimize tests =====

    @Test
    public void testOptimize_nullCurrentSummary() {
        // Should not throw exception
        strategy.optimize(null, null);
    }

    @Test
    public void testOptimize_calculatesAndStoresFederalTax() {
        Person person = new Person("John", 1960);
        IndividualYearlySummary individual = new IndividualYearlySummary(person, 2025, 100000, 0, 0, 0, 0);

        Map<String, IndividualYearlySummary> individuals = new HashMap<>();
        individuals.put("John", individual);

        YearlySummary current = createYearlySummary(2025, 100000, 50000, individuals);
        strategy.optimize(null, current);

        // Federal tax should be calculated and stored
        assertTrue(current.federalIncomeTax() > 0);
    }

    @Test
    public void testOptimize_calculatesAndStoresStateTax() {
        Person person = new Person("John", 1960);
        IndividualYearlySummary individual = new IndividualYearlySummary(person, 2025, 100000, 0, 0, 0, 0);

        Map<String, IndividualYearlySummary> individuals = new HashMap<>();
        individuals.put("John", individual);

        YearlySummary current = createYearlySummary(2025, 100000, 50000, individuals);
        strategy.optimize(null, current);

        // State tax should be calculated and stored
        assertTrue(current.stateIncomeTax() > 0);
    }

    @Test
    public void testOptimize_calculatesAndStoresSocialSecurityTax() {
        Person person = new Person("John", 1960);
        IndividualYearlySummary individual = new IndividualYearlySummary(person, 2025, 100000, 0, 0, 0, 0);

        Map<String, IndividualYearlySummary> individuals = new HashMap<>();
        individuals.put("John", individual);

        YearlySummary current = createYearlySummary(2025, 100000, 50000, individuals);
        strategy.optimize(null, current);

        // Social Security tax should be 6.2% of earned income (up to wage base)
        double expectedSSTax = 100000 * 0.062;
        assertEquals(expectedSSTax, current.socialSecurityTax(), 0.01);
    }

    @Test
    public void testOptimize_calculatesAndStoresMedicareTax() {
        Person person = new Person("John", 1960);
        IndividualYearlySummary individual = new IndividualYearlySummary(person, 2025, 100000, 0, 0, 0, 0);

        Map<String, IndividualYearlySummary> individuals = new HashMap<>();
        individuals.put("John", individual);

        YearlySummary current = createYearlySummary(2025, 100000, 50000, individuals);
        strategy.optimize(null, current);

        // Medicare tax should be 1.45% of earned income
        double expectedMedicareTax = 100000 * 0.0145;
        assertEquals(expectedMedicareTax, current.medicareTax(), 0.01);
    }

    @Test
    public void testOptimize_calculatesCapitalGainsTaxOnNonQualifiedWithdrawals() {
        Person person = new Person("John", 1960);
        IndividualYearlySummary individual = new IndividualYearlySummary(person, 2025, 50000, 0, 100000, 0, 0);
        individual.setNonQualifiedWithdrawals(50000);

        Map<String, IndividualYearlySummary> individuals = new HashMap<>();
        individuals.put("John", individual);

        YearlySummary current = createYearlySummary(2025, 50000, 50000, individuals);
        current.setNonQualifiedWithdrawals(50000);

        strategy.optimize(null, current);

        // Capital gains tax should be calculated on the gain portion (75% of withdrawal)
        // Federal rate is 20%, plus NJ state tax on the gain
        assertTrue(current.capitalGainsTax() > 0);
    }

    @Test
    public void testOptimize_noCapitalGainsTaxWithoutWithdrawals() {
        Person person = new Person("John", 1960);
        IndividualYearlySummary individual = new IndividualYearlySummary(person, 2025, 100000, 0, 100000, 0, 0);

        Map<String, IndividualYearlySummary> individuals = new HashMap<>();
        individuals.put("John", individual);

        YearlySummary current = createYearlySummary(2025, 100000, 50000, individuals);
        strategy.optimize(null, current);

        // No capital gains tax without withdrawals
        assertEquals(0.0, current.capitalGainsTax(), 0.001);
    }

    @Test
    public void testOptimize_zeroIncomeZeroTaxes() {
        Person person = new Person("John", 1960);
        IndividualYearlySummary individual = new IndividualYearlySummary(person, 2025, 0, 0, 0, 0, 0);

        Map<String, IndividualYearlySummary> individuals = new HashMap<>();
        individuals.put("John", individual);

        YearlySummary current = createYearlySummary(2025, 0, 0, individuals);
        strategy.optimize(null, current);

        assertEquals(0.0, current.federalIncomeTax(), 0.001);
        assertEquals(0.0, current.stateIncomeTax(), 0.001);
        assertEquals(0.0, current.socialSecurityTax(), 0.001);
        assertEquals(0.0, current.medicareTax(), 0.001);
        assertEquals(0.0, current.capitalGainsTax(), 0.001);
    }

    @Test
    public void testOptimize_includesRMDInTaxableIncome() {
        Person person = new Person("John", 1952); // 73 in 2025
        IndividualYearlySummary individual = new IndividualYearlySummary(person, 2025, 50000, 500000, 0, 0, 0);
        individual.setRmdWithdrawals(20000);

        Map<String, IndividualYearlySummary> individuals = new HashMap<>();
        individuals.put("John", individual);

        YearlySummary current = createYearlySummary(2025, 50000, 50000, individuals);
        current.setRmdWithdrawals(20000);

        strategy.optimize(null, current);

        // Federal tax should be based on $50,000 income + $20,000 RMD = $70,000
        // This should result in higher taxes than just $50,000 income
        assertTrue(current.federalIncomeTax() > 0);
    }

    @Test
    public void testOptimize_includesSocialSecurityInTaxableIncome() {
        Person person = new Person("John", 1960);
        IndividualYearlySummary individual = new IndividualYearlySummary(person, 2025, 50000, 0, 0, 0, 30000);

        Map<String, IndividualYearlySummary> individuals = new HashMap<>();
        individuals.put("John", individual);

        // Create summary with Social Security
        YearlySummary current = new YearlySummary(2025, 50000, 50000, 0, 0, 0, 50000, 0, 0, 30000, 0, individuals);
        strategy.optimize(null, current);

        // Federal tax should be based on $50,000 income + 85% of $30,000 SS = $75,500
        assertTrue(current.federalIncomeTax() > 0);
    }

    @Test
    public void testOptimize_totalTaxesMethod() {
        Person person = new Person("John", 1960);
        IndividualYearlySummary individual = new IndividualYearlySummary(person, 2025, 100000, 0, 100000, 0, 0);
        individual.setNonQualifiedWithdrawals(10000);

        Map<String, IndividualYearlySummary> individuals = new HashMap<>();
        individuals.put("John", individual);

        YearlySummary current = createYearlySummary(2025, 100000, 50000, individuals);
        current.setNonQualifiedWithdrawals(10000);

        strategy.optimize(null, current);

        // Total taxes should equal sum of all individual taxes
        double expectedTotal = current.federalIncomeTax() + current.stateIncomeTax() +
                current.capitalGainsTax() + current.socialSecurityTax() + current.medicareTax();
        assertEquals(expectedTotal, current.totalTaxes(), 0.01);
    }

    // ===== Filing status tests =====

    @Test
    public void testOptimize_singleFilingStatus() {
        TaxCalculationStrategy singleStrategy = new TaxCalculationStrategy(FilingStatus.SINGLE);

        Person person = new Person("John", 1960);
        IndividualYearlySummary individual = new IndividualYearlySummary(person, 2025, 100000, 0, 0, 0, 0);

        Map<String, IndividualYearlySummary> individuals = new HashMap<>();
        individuals.put("John", individual);

        YearlySummary current = createYearlySummary(2025, 100000, 50000, individuals);
        singleStrategy.optimize(null, current);

        // Single filers typically pay more federal tax than MFJ at same income level
        assertTrue(current.federalIncomeTax() > 0);
    }

    // ===== Strategy metadata tests =====

    @Test
    public void testGetStrategyName() {
        assertEquals("Tax Calculation", strategy.getStrategyName());
    }

    @Test
    public void testGetDescription() {
        String description = strategy.getDescription();
        assertNotNull(description);
        assertTrue(description.contains("federal"));
        assertTrue(description.contains("state"));
        assertTrue(description.contains("Social Security"));
        assertTrue(description.contains("Medicare"));
        assertTrue(description.contains("capital gains"));
    }
}
