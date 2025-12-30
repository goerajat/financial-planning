package rg.financialplanning.calculator;

import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;

public class LongTermCapitalGainsCalculatorTest {

    private LongTermCapitalGainsCalculator calculator;

    @Before
    public void setUp() {
        calculator = new LongTermCapitalGainsCalculator();
    }

    // ===== Constructor tests =====

    @Test
    public void testConstructor_defaultValues() {
        assertEquals(0.25, calculator.getCostBasisFactor(), 0.001);
        assertEquals(0.20, calculator.getCapitalGainsRate(), 0.001);
    }

    @Test
    public void testConstructor_customValues() {
        LongTermCapitalGainsCalculator customCalc = new LongTermCapitalGainsCalculator(0.50, 0.15);
        assertEquals(0.50, customCalc.getCostBasisFactor(), 0.001);
        assertEquals(0.15, customCalc.getCapitalGainsRate(), 0.001);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructor_negativeCostBasisThrowsException() {
        new LongTermCapitalGainsCalculator(-0.1, 0.20);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructor_costBasisGreaterThan1ThrowsException() {
        new LongTermCapitalGainsCalculator(1.1, 0.20);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructor_negativeCapitalGainsRateThrowsException() {
        new LongTermCapitalGainsCalculator(0.25, -0.1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructor_capitalGainsRateGreaterThan1ThrowsException() {
        new LongTermCapitalGainsCalculator(0.25, 1.1);
    }

    @Test
    public void testConstructor_boundaryValues() {
        // 0 and 1 should be valid
        LongTermCapitalGainsCalculator calc1 = new LongTermCapitalGainsCalculator(0, 0);
        assertEquals(0, calc1.getCostBasisFactor(), 0.001);
        assertEquals(0, calc1.getCapitalGainsRate(), 0.001);

        LongTermCapitalGainsCalculator calc2 = new LongTermCapitalGainsCalculator(1, 1);
        assertEquals(1, calc2.getCostBasisFactor(), 0.001);
        assertEquals(1, calc2.getCapitalGainsRate(), 0.001);
    }

    // ===== calculateCostBasis tests =====

    @Test
    public void testCalculateCostBasis_withDefaultFactor() {
        // Default cost basis factor is 0.25
        double proceeds = 100000;
        double expected = 100000 * 0.25;
        assertEquals(expected, calculator.calculateCostBasis(proceeds), 0.01);
    }

    @Test
    public void testCalculateCostBasis_zeroProceeds() {
        assertEquals(0, calculator.calculateCostBasis(0), 0.001);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCalculateCostBasis_negativeProceedsThrowsException() {
        calculator.calculateCostBasis(-100000);
    }

    // ===== calculateCapitalGain tests =====

    @Test
    public void testCalculateCapitalGain_withDefaultFactor() {
        // Gain = Proceeds - Cost Basis = Proceeds * (1 - 0.25) = Proceeds * 0.75
        double proceeds = 100000;
        double expected = 100000 * 0.75;
        assertEquals(expected, calculator.calculateCapitalGain(proceeds), 0.01);
    }

    @Test
    public void testCalculateCapitalGain_zeroProceeds() {
        assertEquals(0, calculator.calculateCapitalGain(0), 0.001);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCalculateCapitalGain_negativeProceedsThrowsException() {
        calculator.calculateCapitalGain(-100000);
    }

    // ===== calculateTax tests =====

    @Test
    public void testCalculateTax_withDefaultValues() {
        // Tax = Gain * Rate = Proceeds * 0.75 * 0.20 = Proceeds * 0.15
        double proceeds = 100000;
        double expected = 100000 * 0.75 * 0.20;
        assertEquals(expected, calculator.calculateTax(proceeds), 0.01);
    }

    @Test
    public void testCalculateTax_zeroProceeds() {
        assertEquals(0, calculator.calculateTax(0), 0.001);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCalculateTax_negativeProceedsThrowsException() {
        calculator.calculateTax(-100000);
    }

    // ===== calculateNetProceeds tests =====

    @Test
    public void testCalculateNetProceeds_withDefaultValues() {
        // Net = Proceeds - Tax = Proceeds - Proceeds * 0.15 = Proceeds * 0.85
        double proceeds = 100000;
        double expected = 100000 - (100000 * 0.75 * 0.20);
        assertEquals(expected, calculator.calculateNetProceeds(proceeds), 0.01);
    }

    @Test
    public void testCalculateNetProceeds_zeroProceeds() {
        assertEquals(0, calculator.calculateNetProceeds(0), 0.001);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCalculateNetProceeds_negativeProceedsThrowsException() {
        calculator.calculateNetProceeds(-100000);
    }

    // ===== calculateTotalSalesProceeds tests =====

    @Test
    public void testCalculateTotalSalesProceeds_roundTrip() {
        // If we calculate net from gross, then calculate gross from net,
        // we should get back the original gross
        double grossProceeds = 100000;
        double netProceeds = calculator.calculateNetProceeds(grossProceeds);
        double calculatedGross = calculator.calculateTotalSalesProceeds(netProceeds);
        assertEquals(grossProceeds, calculatedGross, 0.01);
    }

    @Test
    public void testCalculateTotalSalesProceeds_zeroNet() {
        assertEquals(0, calculator.calculateTotalSalesProceeds(0), 0.001);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCalculateTotalSalesProceeds_negativeNetThrowsException() {
        calculator.calculateTotalSalesProceeds(-50000);
    }

    @Test
    public void testCalculateTotalSalesProceeds_variousAmounts() {
        double[] testAmounts = {10000, 50000, 100000, 500000, 1000000};
        for (double net : testAmounts) {
            double gross = calculator.calculateTotalSalesProceeds(net);
            double netFromGross = calculator.calculateNetProceeds(gross);
            assertEquals("Round trip failed for net=" + net, net, netFromGross, 0.01);
        }
    }

    @Test
    public void testCalculateTotalSalesProceeds_grossGreaterThanNet() {
        double net = 85000;
        double gross = calculator.calculateTotalSalesProceeds(net);
        assertTrue(gross > net);
    }

    // ===== Custom calculator tests =====

    @Test
    public void testCustomCalculator_differentCostBasis() {
        LongTermCapitalGainsCalculator customCalc = new LongTermCapitalGainsCalculator(0.50, 0.20);
        double proceeds = 100000;
        // Cost basis = 50,000, Gain = 50,000, Tax = 10,000
        assertEquals(50000, customCalc.calculateCostBasis(proceeds), 0.01);
        assertEquals(50000, customCalc.calculateCapitalGain(proceeds), 0.01);
        assertEquals(10000, customCalc.calculateTax(proceeds), 0.01);
    }

    @Test
    public void testCustomCalculator_differentCapitalGainsRate() {
        LongTermCapitalGainsCalculator customCalc = new LongTermCapitalGainsCalculator(0.25, 0.15);
        double proceeds = 100000;
        // Cost basis = 25,000, Gain = 75,000, Tax = 11,250
        assertEquals(75000 * 0.15, customCalc.calculateTax(proceeds), 0.01);
    }
}
