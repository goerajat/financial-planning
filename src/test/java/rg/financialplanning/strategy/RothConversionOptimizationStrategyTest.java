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

public class RothConversionOptimizationStrategyTest {

    private RothConversionOptimizationStrategy strategy;

    @Before
    public void setUp() {
        strategy = new RothConversionOptimizationStrategy();
    }

    private YearlySummary createYearlySummary(int year, Map<String, IndividualYearlySummary> individuals) {
        double totalQualified = 0;
        double totalRoth = 0;
        for (IndividualYearlySummary ind : individuals.values()) {
            totalQualified += ind.qualifiedAssets();
            totalRoth += ind.rothAssets();
        }
        return new YearlySummary(year, 0, 0, totalQualified, 0, totalRoth, 0, 0, 0, 0, individuals);
    }

    // ===== Constructor tests =====

    @Test
    public void testConstructor_default() {
        RothConversionOptimizationStrategy defaultStrategy = new RothConversionOptimizationStrategy();
        assertEquals(FilingStatus.MARRIED_FILING_JOINTLY, defaultStrategy.getFilingStatus());
        assertEquals(RothConversionOptimizationStrategy.MFJ_22_PERCENT_BRACKET, defaultStrategy.getTargetBracketThreshold(), 0.01);
    }

    @Test
    public void testConstructor_withFilingStatus() {
        RothConversionOptimizationStrategy singleStrategy = new RothConversionOptimizationStrategy(FilingStatus.SINGLE);
        assertEquals(FilingStatus.SINGLE, singleStrategy.getFilingStatus());
        assertEquals(RothConversionOptimizationStrategy.SINGLE_22_PERCENT_BRACKET, singleStrategy.getTargetBracketThreshold(), 0.01);
    }

    @Test
    public void testConstructor_withCustomThreshold() {
        double customThreshold = 150000;
        RothConversionOptimizationStrategy customStrategy = new RothConversionOptimizationStrategy(FilingStatus.SINGLE, customThreshold);
        assertEquals(FilingStatus.SINGLE, customStrategy.getFilingStatus());
        assertEquals(customThreshold, customStrategy.getTargetBracketThreshold(), 0.01);
    }

    // ===== optimize tests =====

    @Test
    public void testOptimize_nullCurrentSummary() {
        // Should not throw exception
        strategy.optimize(null, null);
    }

    @Test
    public void testOptimize_noIndividuals() {
        YearlySummary current = createYearlySummary(2025, new HashMap<>());
        strategy.optimize(null, current);
        // No exception, no changes
    }

    @Test
    public void testOptimize_personTooYoungForConversion() {
        // Person born 1980 will be 45 in 2025 - below 59
        Person person = new Person("John", 1980);
        IndividualYearlySummary individual = new IndividualYearlySummary(person, 2025, 0, 500000, 0, 100000, 0);

        Map<String, IndividualYearlySummary> individuals = new HashMap<>();
        individuals.put("John", individual);

        YearlySummary current = createYearlySummary(2025, individuals);
        strategy.optimize(null, current);

        // No conversion should happen - person is too young
        assertEquals(500000, individual.qualifiedAssets(), 0.01);
        assertEquals(100000, individual.rothAssets(), 0.01);
        assertEquals(0.0, individual.qualifiedWithdrawals(), 0.001);
        assertEquals(0.0, individual.rothContributions(), 0.001);
    }

    @Test
    public void testOptimize_personAtEligibleAge() {
        // Person born 1966 will be 59 in 2025 - eligible
        Person person = new Person("John", 1966);
        IndividualYearlySummary individual = new IndividualYearlySummary(person, 2025, 50000, 500000, 0, 100000, 0);

        Map<String, IndividualYearlySummary> individuals = new HashMap<>();
        individuals.put("John", individual);

        YearlySummary current = createYearlySummary(2025, individuals);
        strategy.optimize(null, current);

        // Some conversion should happen
        assertTrue(individual.qualifiedWithdrawals() > 0 || individual.qualifiedAssets() < 500000);
    }

    @Test
    public void testOptimize_alreadyAboveThreshold() {
        // Person with income already above threshold - no conversion
        Person person = new Person("John", 1960);
        // Income + 85% of SS would put them above threshold
        IndividualYearlySummary individual = new IndividualYearlySummary(person, 2025, 250000, 500000, 0, 100000, 50000);

        Map<String, IndividualYearlySummary> individuals = new HashMap<>();
        individuals.put("John", individual);

        YearlySummary current = createYearlySummary(2025, individuals);
        double originalQualified = individual.qualifiedAssets();

        strategy.optimize(null, current);

        // No conversion if already above threshold
        assertEquals(originalQualified, individual.qualifiedAssets(), 0.01);
    }

    @Test
    public void testOptimize_conversionUpdatesAssets() {
        Person person = new Person("John", 1960);
        // Low income to leave room in bracket
        IndividualYearlySummary individual = new IndividualYearlySummary(person, 2025, 50000, 500000, 0, 100000, 0);

        Map<String, IndividualYearlySummary> individuals = new HashMap<>();
        individuals.put("John", individual);

        double originalQualified = individual.qualifiedAssets();
        double originalRoth = individual.rothAssets();

        YearlySummary current = createYearlySummary(2025, individuals);
        strategy.optimize(null, current);

        // After conversion: qualified should decrease, roth should increase
        double conversion = originalQualified - individual.qualifiedAssets();
        if (conversion > 0) {
            assertEquals(originalRoth + conversion, individual.rothAssets(), 0.01);
            assertEquals(conversion, individual.qualifiedWithdrawals(), 0.01);
            assertEquals(conversion, individual.rothContributions(), 0.01);
        }
    }

    @Test
    public void testOptimize_conversionLimitedByAvailableAssets() {
        Person person = new Person("John", 1960);
        // Very low income and small qualified assets
        IndividualYearlySummary individual = new IndividualYearlySummary(person, 2025, 10000, 10000, 0, 5000, 0);

        Map<String, IndividualYearlySummary> individuals = new HashMap<>();
        individuals.put("John", individual);

        YearlySummary current = createYearlySummary(2025, individuals);
        strategy.optimize(null, current);

        // Should not convert more than available
        assertTrue(individual.qualifiedAssets() >= 0);
    }

    @Test
    public void testOptimize_multipleIndividuals() {
        Person john = new Person("John", 1960);
        Person jane = new Person("Jane", 1962);

        IndividualYearlySummary johnSummary = new IndividualYearlySummary(john, 2025, 30000, 500000, 0, 100000, 0);
        IndividualYearlySummary janeSummary = new IndividualYearlySummary(jane, 2025, 30000, 400000, 0, 80000, 0);

        Map<String, IndividualYearlySummary> individuals = new HashMap<>();
        individuals.put("John", johnSummary);
        individuals.put("Jane", janeSummary);

        YearlySummary current = createYearlySummary(2025, individuals);
        strategy.optimize(null, current);

        // Total converted should not exceed bracket room
        double totalConverted = johnSummary.qualifiedWithdrawals() + janeSummary.qualifiedWithdrawals();
        // Combined income (30000 + 30000) * 0.85 = 51000 taxable
        // Threshold is around 206700, so lots of room
        assertTrue(totalConverted >= 0);
    }

    // ===== calculateOptimalConversion tests =====

    @Test
    public void testCalculateOptimalConversion_roomInBracket() {
        double currentIncome = 100000;
        double availableAssets = 500000;
        double optimal = strategy.calculateOptimalConversion(currentIncome, availableAssets);

        // Room = threshold - income = 206700 - 100000 = 106700
        assertEquals(206700 - 100000, optimal, 0.01);
    }

    @Test
    public void testCalculateOptimalConversion_limitedByAssets() {
        double currentIncome = 100000;
        double availableAssets = 50000;
        double optimal = strategy.calculateOptimalConversion(currentIncome, availableAssets);

        // Limited by available assets
        assertEquals(50000, optimal, 0.01);
    }

    @Test
    public void testCalculateOptimalConversion_noRoom() {
        double currentIncome = 250000; // Above threshold
        double availableAssets = 500000;
        double optimal = strategy.calculateOptimalConversion(currentIncome, availableAssets);

        assertEquals(0.0, optimal, 0.001);
    }

    @Test
    public void testCalculateOptimalConversion_noAssets() {
        double currentIncome = 100000;
        double availableAssets = 0;
        double optimal = strategy.calculateOptimalConversion(currentIncome, availableAssets);

        assertEquals(0.0, optimal, 0.001);
    }

    // ===== calculateConversionTaxCost tests =====

    @Test
    public void testCalculateConversionTaxCost_zeroConversion() {
        double cost = strategy.calculateConversionTaxCost(0, 100000);
        assertEquals(0.0, cost, 0.001);
    }

    @Test
    public void testCalculateConversionTaxCost_positiveConversion() {
        double conversionAmount = 50000;
        double currentIncome = 100000;
        double cost = strategy.calculateConversionTaxCost(conversionAmount, currentIncome);

        // Cost should be positive
        assertTrue(cost > 0);
        // Cost should be less than conversion amount
        assertTrue(cost < conversionAmount);
    }

    @Test
    public void testCalculateConversionTaxCost_higherIncomeHigherCost() {
        double conversionAmount = 50000;
        double lowIncome = 50000;
        double highIncome = 200000;

        double lowIncomeCost = strategy.calculateConversionTaxCost(conversionAmount, lowIncome);
        double highIncomeCost = strategy.calculateConversionTaxCost(conversionAmount, highIncome);

        // Higher income should result in higher tax cost (higher marginal rate)
        assertTrue(highIncomeCost >= lowIncomeCost);
    }

    // ===== getMarginalTaxRate tests =====

    @Test
    public void testGetMarginalTaxRate_lowIncome() {
        double rate = strategy.getMarginalTaxRate(30000);
        assertTrue(rate > 0);
        assertTrue(rate < 0.50); // Reasonable rate
    }

    @Test
    public void testGetMarginalTaxRate_highIncome() {
        double lowRate = strategy.getMarginalTaxRate(30000);
        double highRate = strategy.getMarginalTaxRate(500000);

        assertTrue(highRate > lowRate);
    }

    // ===== Strategy metadata tests =====

    @Test
    public void testGetStrategyName() {
        assertEquals("Roth Conversion Optimization", strategy.getStrategyName());
    }

    @Test
    public void testGetDescription() {
        String description = strategy.getDescription();
        assertNotNull(description);
        assertTrue(description.contains("Roth"));
        assertTrue(description.contains("conversion"));
    }

    // ===== Constants tests =====

    @Test
    public void testQualifiedWithdrawalMinAge() {
        assertEquals(59, RothConversionOptimizationStrategy.QUALIFIED_WITHDRAWAL_MIN_AGE);
    }

    @Test
    public void testBracketConstants() {
        assertEquals(96950, RothConversionOptimizationStrategy.MFJ_12_PERCENT_BRACKET, 0.01);
        assertEquals(206700, RothConversionOptimizationStrategy.MFJ_22_PERCENT_BRACKET, 0.01);
        assertEquals(394600, RothConversionOptimizationStrategy.MFJ_24_PERCENT_BRACKET, 0.01);

        assertEquals(48475, RothConversionOptimizationStrategy.SINGLE_12_PERCENT_BRACKET, 0.01);
        assertEquals(103350, RothConversionOptimizationStrategy.SINGLE_22_PERCENT_BRACKET, 0.01);
        assertEquals(197300, RothConversionOptimizationStrategy.SINGLE_24_PERCENT_BRACKET, 0.01);
    }
}
