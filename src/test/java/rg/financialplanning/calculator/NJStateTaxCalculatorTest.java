package rg.financialplanning.calculator;

import org.junit.Test;
import org.junit.Before;
import rg.financialplanning.model.FilingStatus;
import static org.junit.Assert.*;

public class NJStateTaxCalculatorTest {

    private NJStateTaxCalculator calculator;

    @Before
    public void setUp() {
        calculator = new NJStateTaxCalculator();
    }

    // ===== calculateTax tests =====

    @Test
    public void testCalculateTax_zeroIncome() {
        assertEquals(0, calculator.calculateTax(0, FilingStatus.SINGLE), 0.01);
        assertEquals(0, calculator.calculateTax(0, FilingStatus.MARRIED_FILING_JOINTLY), 0.01);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCalculateTax_negativeIncomeThrowsException() {
        calculator.calculateTax(-1000, FilingStatus.SINGLE);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCalculateTax_nullFilingStatusThrowsException() {
        calculator.calculateTax(50000, null);
    }

    @Test
    public void testCalculateTax_single_firstBracket() {
        // First $20,000 taxed at 1.4%
        double tax = calculator.calculateTax(15000, FilingStatus.SINGLE);
        assertEquals(15000 * 0.014, tax, 0.01);
    }

    @Test
    public void testCalculateTax_single_secondBracket() {
        // $20,000 at 1.4% + remaining at 1.75%
        double income = 30000;
        double expectedTax = 20000 * 0.014 + (30000 - 20000) * 0.0175;
        assertEquals(expectedTax, calculator.calculateTax(income, FilingStatus.SINGLE), 0.01);
    }

    @Test
    public void testCalculateTax_single_highIncome() {
        // Test income above highest bracket
        double income = 1500000;
        double tax = calculator.calculateTax(income, FilingStatus.SINGLE);
        assertTrue(tax > 0);
        assertTrue(tax < income);
    }

    @Test
    public void testCalculateTax_marriedFilingJointly() {
        // First $20,000 taxed at 1.4%
        double tax = calculator.calculateTax(15000, FilingStatus.MARRIED_FILING_JOINTLY);
        assertEquals(15000 * 0.014, tax, 0.01);
    }

    @Test
    public void testCalculateTax_marriedFilingSeparately() {
        double tax = calculator.calculateTax(50000, FilingStatus.MARRIED_FILING_SEPARATELY);
        assertTrue(tax > 0);
    }

    @Test
    public void testCalculateTax_headOfHousehold() {
        double tax = calculator.calculateTax(50000, FilingStatus.HEAD_OF_HOUSEHOLD);
        assertTrue(tax > 0);
    }

    // ===== getEffectiveTaxRate tests =====

    @Test
    public void testGetEffectiveTaxRate_zeroIncome() {
        assertEquals(0, calculator.getEffectiveTaxRate(0, FilingStatus.SINGLE), 0.001);
    }

    @Test
    public void testGetEffectiveTaxRate_negativeIncome() {
        assertEquals(0, calculator.getEffectiveTaxRate(-1000, FilingStatus.SINGLE), 0.001);
    }

    @Test
    public void testGetEffectiveTaxRate_lowIncome() {
        // For income entirely in 1.4% bracket
        double rate = calculator.getEffectiveTaxRate(15000, FilingStatus.SINGLE);
        assertEquals(0.014, rate, 0.001);
    }

    @Test
    public void testGetEffectiveTaxRate_higherIncome() {
        double rate = calculator.getEffectiveTaxRate(100000, FilingStatus.SINGLE);
        assertTrue(rate > 0.014); // Should be above lowest rate
        assertTrue(rate < 0.1075); // Should be below highest rate
    }

    // ===== getMarginalTaxRate tests =====

    @Test
    public void testGetMarginalTaxRate_zeroIncome() {
        assertEquals(0.014, calculator.getMarginalTaxRate(0, FilingStatus.SINGLE), 0.001);
    }

    @Test
    public void testGetMarginalTaxRate_negativeIncome() {
        assertEquals(0.014, calculator.getMarginalTaxRate(-1000, FilingStatus.SINGLE), 0.001);
    }

    @Test
    public void testGetMarginalTaxRate_single_firstBracket() {
        assertEquals(0.014, calculator.getMarginalTaxRate(15000, FilingStatus.SINGLE), 0.001);
    }

    @Test
    public void testGetMarginalTaxRate_single_secondBracket() {
        // Between $20,000 and $35,000
        assertEquals(0.0175, calculator.getMarginalTaxRate(25000, FilingStatus.SINGLE), 0.001);
    }

    @Test
    public void testGetMarginalTaxRate_single_highestBracket() {
        // Above $1,000,000
        assertEquals(0.1075, calculator.getMarginalTaxRate(1500000, FilingStatus.SINGLE), 0.001);
    }

    @Test
    public void testGetMarginalTaxRate_marriedFilingJointly() {
        // First bracket: up to $20,000 at 1.4%
        assertEquals(0.014, calculator.getMarginalTaxRate(15000, FilingStatus.MARRIED_FILING_JOINTLY), 0.001);
    }

    // ===== calculatePreTaxAmount tests =====

    @Test
    public void testCalculatePreTaxAmount_zeroPostTax() {
        assertEquals(0, calculator.calculatePreTaxAmount(0, FilingStatus.SINGLE), 0.01);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCalculatePreTaxAmount_negativePostTaxThrowsException() {
        calculator.calculatePreTaxAmount(-1000, FilingStatus.SINGLE);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCalculatePreTaxAmount_nullFilingStatusThrowsException() {
        calculator.calculatePreTaxAmount(50000, null);
    }

    @Test
    public void testCalculatePreTaxAmount_inverseOfCalculateTax() {
        double postTax = 50000;
        double preTax = calculator.calculatePreTaxAmount(postTax, FilingStatus.SINGLE);
        double tax = calculator.calculateTax(preTax, FilingStatus.SINGLE);
        assertEquals(postTax, preTax - tax, 0.01);
    }

    @Test
    public void testCalculatePreTaxAmount_roundTrip() {
        double[] testAmounts = {10000, 50000, 100000, 200000, 500000};
        for (double postTax : testAmounts) {
            double preTax = calculator.calculatePreTaxAmount(postTax, FilingStatus.MARRIED_FILING_JOINTLY);
            double tax = calculator.calculateTax(preTax, FilingStatus.MARRIED_FILING_JOINTLY);
            assertEquals("Round trip failed for postTax=" + postTax, postTax, preTax - tax, 0.01);
        }
    }

    @Test
    public void testCalculatePreTaxAmount_preTaxGreaterThanPostTax() {
        double postTax = 50000;
        double preTax = calculator.calculatePreTaxAmount(postTax, FilingStatus.SINGLE);
        assertTrue(preTax > postTax);
    }
}
