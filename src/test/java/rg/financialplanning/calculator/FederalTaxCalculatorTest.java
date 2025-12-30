package rg.financialplanning.calculator;

import org.junit.Test;
import org.junit.Before;
import rg.financialplanning.model.FilingStatus;
import static org.junit.Assert.*;

public class FederalTaxCalculatorTest {

    private FederalTaxCalculator calculator;

    @Before
    public void setUp() {
        calculator = new FederalTaxCalculator();
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
        // First $11,925 taxed at 10%
        double tax = calculator.calculateTax(10000, FilingStatus.SINGLE);
        assertEquals(10000 * 0.10, tax, 0.01);
    }

    @Test
    public void testCalculateTax_single_secondBracket() {
        // $11,925 at 10% + remaining at 12%
        double income = 30000;
        double expectedTax = 11925 * 0.10 + (30000 - 11925) * 0.12;
        assertEquals(expectedTax, calculator.calculateTax(income, FilingStatus.SINGLE), 0.01);
    }

    @Test
    public void testCalculateTax_single_highIncome() {
        // Test income above highest bracket
        double income = 700000;
        double tax = calculator.calculateTax(income, FilingStatus.SINGLE);
        assertTrue(tax > 0);
        assertTrue(tax < income); // Tax should be less than income
    }

    @Test
    public void testCalculateTax_marriedFilingJointly_firstBracket() {
        // First $23,850 taxed at 10%
        double tax = calculator.calculateTax(20000, FilingStatus.MARRIED_FILING_JOINTLY);
        assertEquals(20000 * 0.10, tax, 0.01);
    }

    @Test
    public void testCalculateTax_marriedFilingJointly_secondBracket() {
        // $23,850 at 10% + remaining at 12%
        double income = 50000;
        double expectedTax = 23850 * 0.10 + (50000 - 23850) * 0.12;
        assertEquals(expectedTax, calculator.calculateTax(income, FilingStatus.MARRIED_FILING_JOINTLY), 0.01);
    }

    @Test
    public void testCalculateTax_marriedFilingSeparately() {
        double tax = calculator.calculateTax(30000, FilingStatus.MARRIED_FILING_SEPARATELY);
        assertTrue(tax > 0);
    }

    @Test
    public void testCalculateTax_headOfHousehold() {
        double tax = calculator.calculateTax(30000, FilingStatus.HEAD_OF_HOUSEHOLD);
        assertTrue(tax > 0);
    }

    @Test
    public void testCalculateTax_mfjPayLessThanSingle() {
        double income = 100000;
        double singleTax = calculator.calculateTax(income, FilingStatus.SINGLE);
        double mfjTax = calculator.calculateTax(income, FilingStatus.MARRIED_FILING_JOINTLY);
        // MFJ should generally pay less or equal tax at same income level
        assertTrue(mfjTax <= singleTax);
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
        // For income entirely in 10% bracket
        double rate = calculator.getEffectiveTaxRate(10000, FilingStatus.SINGLE);
        assertEquals(0.10, rate, 0.001);
    }

    @Test
    public void testGetEffectiveTaxRate_higherIncome() {
        double rate = calculator.getEffectiveTaxRate(100000, FilingStatus.SINGLE);
        assertTrue(rate > 0.10); // Should be above lowest rate
        assertTrue(rate < 0.37); // Should be below highest rate
    }

    // ===== getMarginalTaxRate tests =====

    @Test
    public void testGetMarginalTaxRate_zeroIncome() {
        assertEquals(0.10, calculator.getMarginalTaxRate(0, FilingStatus.SINGLE), 0.001);
    }

    @Test
    public void testGetMarginalTaxRate_negativeIncome() {
        assertEquals(0.10, calculator.getMarginalTaxRate(-1000, FilingStatus.SINGLE), 0.001);
    }

    @Test
    public void testGetMarginalTaxRate_single_firstBracket() {
        assertEquals(0.10, calculator.getMarginalTaxRate(10000, FilingStatus.SINGLE), 0.001);
    }

    @Test
    public void testGetMarginalTaxRate_single_secondBracket() {
        // Between $11,925 and $48,475
        assertEquals(0.12, calculator.getMarginalTaxRate(30000, FilingStatus.SINGLE), 0.001);
    }

    @Test
    public void testGetMarginalTaxRate_single_thirdBracket() {
        // Between $48,475 and $103,350
        assertEquals(0.22, calculator.getMarginalTaxRate(75000, FilingStatus.SINGLE), 0.001);
    }

    @Test
    public void testGetMarginalTaxRate_single_topBracket() {
        // Above $626,350
        assertEquals(0.37, calculator.getMarginalTaxRate(700000, FilingStatus.SINGLE), 0.001);
    }

    @Test
    public void testGetMarginalTaxRate_marriedFilingJointly() {
        // First bracket: up to $23,850 at 10%
        assertEquals(0.10, calculator.getMarginalTaxRate(20000, FilingStatus.MARRIED_FILING_JOINTLY), 0.001);
        // Second bracket: $23,850 to $96,950 at 12%
        assertEquals(0.12, calculator.getMarginalTaxRate(50000, FilingStatus.MARRIED_FILING_JOINTLY), 0.001);
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
        // If we calculate pre-tax from post-tax, then calculate tax and subtract from pre-tax,
        // we should get back the original post-tax amount
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
