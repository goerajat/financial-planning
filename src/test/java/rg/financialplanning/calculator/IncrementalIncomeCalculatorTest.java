package rg.financialplanning.calculator;

import org.junit.Test;
import org.junit.Before;
import rg.financialplanning.model.FilingStatus;
import static org.junit.Assert.*;

public class IncrementalIncomeCalculatorTest {

    private IncrementalIncomeCalculator calculator;

    @Before
    public void setUp() {
        calculator = new IncrementalIncomeCalculator();
    }

    // ===== calculateIncrementalIncomeRequired tests =====

    @Test
    public void testCalculateIncrementalIncomeRequired_zeroExpenses() {
        double result = calculator.calculateIncrementalIncomeRequired(50000, 0, FilingStatus.SINGLE);
        assertEquals(0, result, 0.01);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCalculateIncrementalIncomeRequired_negativeBaseIncomeThrowsException() {
        calculator.calculateIncrementalIncomeRequired(-1000, 50000, FilingStatus.SINGLE);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCalculateIncrementalIncomeRequired_negativeExpensesThrowsException() {
        calculator.calculateIncrementalIncomeRequired(50000, -1000, FilingStatus.SINGLE);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCalculateIncrementalIncomeRequired_nullFilingStatusThrowsException() {
        calculator.calculateIncrementalIncomeRequired(50000, 10000, null);
    }

    @Test
    public void testCalculateIncrementalIncomeRequired_basicCase() {
        double baseIncome = 50000;
        double expenses = 10000;
        double incrementalIncome = calculator.calculateIncrementalIncomeRequired(baseIncome, expenses, FilingStatus.SINGLE);

        // Verify that the net after taxes equals the expenses
        double netAfterTax = calculator.calculateNetAfterTax(baseIncome, incrementalIncome, FilingStatus.SINGLE);
        assertEquals(expenses, netAfterTax, 0.01);
    }

    @Test
    public void testCalculateIncrementalIncomeRequired_higherBaseIncome() {
        double baseIncome = 200000;
        double expenses = 20000;
        double incrementalIncome = calculator.calculateIncrementalIncomeRequired(baseIncome, expenses, FilingStatus.SINGLE);

        double netAfterTax = calculator.calculateNetAfterTax(baseIncome, incrementalIncome, FilingStatus.SINGLE);
        assertEquals(expenses, netAfterTax, 0.01);
    }

    @Test
    public void testCalculateIncrementalIncomeRequired_marriedFilingJointly() {
        double baseIncome = 100000;
        double expenses = 30000;
        double incrementalIncome = calculator.calculateIncrementalIncomeRequired(baseIncome, expenses, FilingStatus.MARRIED_FILING_JOINTLY);

        double netAfterTax = calculator.calculateNetAfterTax(baseIncome, incrementalIncome, FilingStatus.MARRIED_FILING_JOINTLY);
        assertEquals(expenses, netAfterTax, 0.01);
    }

    @Test
    public void testCalculateIncrementalIncomeRequired_incrementalGreaterThanExpenses() {
        // Since we need to pay taxes, we need more income than expenses
        double baseIncome = 100000;
        double expenses = 10000;
        double incrementalIncome = calculator.calculateIncrementalIncomeRequired(baseIncome, expenses, FilingStatus.SINGLE);
        assertTrue(incrementalIncome > expenses);
    }

    // ===== calculateIncrementalTax tests =====

    @Test
    public void testCalculateIncrementalTax_zeroAdditionalIncome() {
        double result = calculator.calculateIncrementalTax(50000, 0, FilingStatus.SINGLE);
        assertEquals(0, result, 0.01);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCalculateIncrementalTax_negativeBaseIncomeThrowsException() {
        calculator.calculateIncrementalTax(-1000, 10000, FilingStatus.SINGLE);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCalculateIncrementalTax_negativeAdditionalIncomeThrowsException() {
        calculator.calculateIncrementalTax(50000, -1000, FilingStatus.SINGLE);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCalculateIncrementalTax_nullFilingStatusThrowsException() {
        calculator.calculateIncrementalTax(50000, 10000, null);
    }

    @Test
    public void testCalculateIncrementalTax_basicCase() {
        double baseIncome = 50000;
        double additionalIncome = 20000;
        double incrementalTax = calculator.calculateIncrementalTax(baseIncome, additionalIncome, FilingStatus.SINGLE);

        // Incremental tax should be positive
        assertTrue(incrementalTax > 0);
        // Incremental tax should be less than additional income
        assertTrue(incrementalTax < additionalIncome);
    }

    @Test
    public void testCalculateIncrementalTax_higherTaxAtHigherBaseIncome() {
        double additionalIncome = 20000;

        // Same additional income at different base incomes
        double taxAtLowBase = calculator.calculateIncrementalTax(30000, additionalIncome, FilingStatus.SINGLE);
        double taxAtHighBase = calculator.calculateIncrementalTax(200000, additionalIncome, FilingStatus.SINGLE);

        // Higher base income should result in higher incremental tax (higher marginal rates)
        assertTrue(taxAtHighBase > taxAtLowBase);
    }

    // ===== getCombinedMarginalRate tests =====

    @Test
    public void testGetCombinedMarginalRate_lowIncome() {
        double rate = calculator.getCombinedMarginalRate(20000, FilingStatus.SINGLE);
        // Federal 10% + NJ 1.75% (approximately)
        assertTrue(rate > 0);
        assertTrue(rate < 0.50); // Should be reasonable
    }

    @Test
    public void testGetCombinedMarginalRate_highIncome() {
        double rate = calculator.getCombinedMarginalRate(500000, FilingStatus.SINGLE);
        // Federal + State combined should be higher
        assertTrue(rate > calculator.getCombinedMarginalRate(20000, FilingStatus.SINGLE));
    }

    @Test
    public void testGetCombinedMarginalRate_allFilingStatuses() {
        double income = 100000;
        for (FilingStatus status : FilingStatus.values()) {
            double rate = calculator.getCombinedMarginalRate(income, status);
            assertTrue(rate > 0);
            assertTrue(rate < 0.60);
        }
    }

    // ===== calculateNetAfterTax tests =====

    @Test
    public void testCalculateNetAfterTax_zeroAdditionalIncome() {
        double result = calculator.calculateNetAfterTax(50000, 0, FilingStatus.SINGLE);
        assertEquals(0, result, 0.01);
    }

    @Test
    public void testCalculateNetAfterTax_basicCase() {
        double baseIncome = 50000;
        double additionalIncome = 20000;
        double netAfterTax = calculator.calculateNetAfterTax(baseIncome, additionalIncome, FilingStatus.SINGLE);

        // Net should be positive but less than additional income
        assertTrue(netAfterTax > 0);
        assertTrue(netAfterTax < additionalIncome);

        // Net should equal additional income minus incremental tax
        double incrementalTax = calculator.calculateIncrementalTax(baseIncome, additionalIncome, FilingStatus.SINGLE);
        assertEquals(additionalIncome - incrementalTax, netAfterTax, 0.01);
    }

    @Test
    public void testCalculateNetAfterTax_consistency() {
        double baseIncome = 100000;
        double additionalIncome = 30000;
        FilingStatus status = FilingStatus.MARRIED_FILING_JOINTLY;

        double netAfterTax = calculator.calculateNetAfterTax(baseIncome, additionalIncome, status);
        double incrementalTax = calculator.calculateIncrementalTax(baseIncome, additionalIncome, status);

        assertEquals(additionalIncome - incrementalTax, netAfterTax, 0.01);
    }

    // ===== Round-trip verification tests =====

    @Test
    public void testRoundTrip_variousScenarios() {
        double[] baseIncomes = {0, 25000, 50000, 100000, 200000, 500000};
        double[] expenses = {5000, 10000, 25000, 50000, 100000};

        for (double baseIncome : baseIncomes) {
            for (double expense : expenses) {
                double incrementalIncome = calculator.calculateIncrementalIncomeRequired(
                        baseIncome, expense, FilingStatus.MARRIED_FILING_JOINTLY);
                double netAfterTax = calculator.calculateNetAfterTax(
                        baseIncome, incrementalIncome, FilingStatus.MARRIED_FILING_JOINTLY);
                assertEquals("Failed for baseIncome=" + baseIncome + ", expense=" + expense,
                        expense, netAfterTax, 0.01);
            }
        }
    }
}
