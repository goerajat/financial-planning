package rg.financialplanning.calculator;

import org.junit.Test;
import org.junit.Before;
import rg.financialplanning.model.FilingStatus;
import static org.junit.Assert.*;

public class SocialSecurityTaxCalculatorTest {

    private SocialSecurityTaxCalculator calculator;

    @Before
    public void setUp() {
        calculator = new SocialSecurityTaxCalculator();
    }

    // ===== Constructor tests =====

    @Test
    public void testConstructor_defaultWageBase() {
        assertEquals(168600.0, calculator.getSocialSecurityWageBase(), 0.001);
    }

    @Test
    public void testConstructor_customWageBase() {
        SocialSecurityTaxCalculator customCalc = new SocialSecurityTaxCalculator(200000);
        assertEquals(200000, customCalc.getSocialSecurityWageBase(), 0.001);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructor_negativeWageBaseThrowsException() {
        new SocialSecurityTaxCalculator(-1000);
    }

    // ===== calculateSocialSecurityTax tests =====

    @Test
    public void testCalculateSocialSecurityTax_employee() {
        // Employee rate is 6.2%
        double wages = 100000;
        double expected = wages * 0.062;
        assertEquals(expected, calculator.calculateSocialSecurityTax(wages), 0.01);
    }

    @Test
    public void testCalculateSocialSecurityTax_employee_aboveWageBase() {
        // Only wages up to $168,600 are subject to SS tax
        double wages = 200000;
        double expected = 168600 * 0.062;
        assertEquals(expected, calculator.calculateSocialSecurityTax(wages), 0.01);
    }

    @Test
    public void testCalculateSocialSecurityTax_selfEmployed() {
        // Self-employed rate is 12.4%
        double wages = 100000;
        double expected = wages * 0.124;
        assertEquals(expected, calculator.calculateSocialSecurityTax(wages, true), 0.01);
    }

    @Test
    public void testCalculateSocialSecurityTax_selfEmployed_aboveWageBase() {
        double wages = 200000;
        double expected = 168600 * 0.124;
        assertEquals(expected, calculator.calculateSocialSecurityTax(wages, true), 0.01);
    }

    @Test
    public void testCalculateSocialSecurityTax_zeroWages() {
        assertEquals(0, calculator.calculateSocialSecurityTax(0), 0.001);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCalculateSocialSecurityTax_negativeWagesThrowsException() {
        calculator.calculateSocialSecurityTax(-1000);
    }

    // ===== calculateMedicareTax tests =====

    @Test
    public void testCalculateMedicareTax_employee() {
        // Employee rate is 1.45%
        double wages = 100000;
        double expected = wages * 0.0145;
        assertEquals(expected, calculator.calculateMedicareTax(wages), 0.01);
    }

    @Test
    public void testCalculateMedicareTax_employee_noWageLimit() {
        // Medicare has no wage limit
        double wages = 500000;
        double expected = wages * 0.0145;
        assertEquals(expected, calculator.calculateMedicareTax(wages), 0.01);
    }

    @Test
    public void testCalculateMedicareTax_selfEmployed() {
        // Self-employed rate is 2.9%
        double wages = 100000;
        double expected = wages * 0.029;
        assertEquals(expected, calculator.calculateMedicareTax(wages, true), 0.01);
    }

    @Test
    public void testCalculateMedicareTax_zeroWages() {
        assertEquals(0, calculator.calculateMedicareTax(0), 0.001);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCalculateMedicareTax_negativeWagesThrowsException() {
        calculator.calculateMedicareTax(-1000);
    }

    // ===== calculateAdditionalMedicareTax tests =====

    @Test
    public void testCalculateAdditionalMedicareTax_single_belowThreshold() {
        double wages = 150000; // Below $200,000 threshold
        assertEquals(0, calculator.calculateAdditionalMedicareTax(wages, FilingStatus.SINGLE), 0.001);
    }

    @Test
    public void testCalculateAdditionalMedicareTax_single_aboveThreshold() {
        double wages = 250000; // $50,000 above $200,000 threshold
        double expected = (250000 - 200000) * 0.009;
        assertEquals(expected, calculator.calculateAdditionalMedicareTax(wages, FilingStatus.SINGLE), 0.01);
    }

    @Test
    public void testCalculateAdditionalMedicareTax_mfj_belowThreshold() {
        double wages = 200000; // Below $250,000 threshold
        assertEquals(0, calculator.calculateAdditionalMedicareTax(wages, FilingStatus.MARRIED_FILING_JOINTLY), 0.001);
    }

    @Test
    public void testCalculateAdditionalMedicareTax_mfj_aboveThreshold() {
        double wages = 300000; // $50,000 above $250,000 threshold
        double expected = (300000 - 250000) * 0.009;
        assertEquals(expected, calculator.calculateAdditionalMedicareTax(wages, FilingStatus.MARRIED_FILING_JOINTLY), 0.01);
    }

    @Test
    public void testCalculateAdditionalMedicareTax_mfs_belowThreshold() {
        double wages = 100000; // Below $125,000 threshold
        assertEquals(0, calculator.calculateAdditionalMedicareTax(wages, FilingStatus.MARRIED_FILING_SEPARATELY), 0.001);
    }

    @Test
    public void testCalculateAdditionalMedicareTax_mfs_aboveThreshold() {
        double wages = 175000; // $50,000 above $125,000 threshold
        double expected = (175000 - 125000) * 0.009;
        assertEquals(expected, calculator.calculateAdditionalMedicareTax(wages, FilingStatus.MARRIED_FILING_SEPARATELY), 0.01);
    }

    @Test
    public void testCalculateAdditionalMedicareTax_hoh_aboveThreshold() {
        double wages = 250000; // $50,000 above $200,000 threshold (same as single)
        double expected = (250000 - 200000) * 0.009;
        assertEquals(expected, calculator.calculateAdditionalMedicareTax(wages, FilingStatus.HEAD_OF_HOUSEHOLD), 0.01);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCalculateAdditionalMedicareTax_negativeWagesThrowsException() {
        calculator.calculateAdditionalMedicareTax(-1000, FilingStatus.SINGLE);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCalculateAdditionalMedicareTax_nullFilingStatusThrowsException() {
        calculator.calculateAdditionalMedicareTax(250000, null);
    }

    // ===== getAdditionalMedicareThreshold tests =====

    @Test
    public void testGetAdditionalMedicareThreshold_single() {
        assertEquals(200000, calculator.getAdditionalMedicareThreshold(FilingStatus.SINGLE), 0.001);
    }

    @Test
    public void testGetAdditionalMedicareThreshold_mfj() {
        assertEquals(250000, calculator.getAdditionalMedicareThreshold(FilingStatus.MARRIED_FILING_JOINTLY), 0.001);
    }

    @Test
    public void testGetAdditionalMedicareThreshold_mfs() {
        assertEquals(125000, calculator.getAdditionalMedicareThreshold(FilingStatus.MARRIED_FILING_SEPARATELY), 0.001);
    }

    @Test
    public void testGetAdditionalMedicareThreshold_hoh() {
        assertEquals(200000, calculator.getAdditionalMedicareThreshold(FilingStatus.HEAD_OF_HOUSEHOLD), 0.001);
    }

    // ===== Rate getter tests =====

    @Test
    public void testGetSocialSecurityRateEmployee() {
        assertEquals(0.062, calculator.getSocialSecurityRateEmployee(), 0.0001);
    }

    @Test
    public void testGetSocialSecurityRateSelfEmployed() {
        assertEquals(0.124, calculator.getSocialSecurityRateSelfEmployed(), 0.0001);
    }

    @Test
    public void testGetMedicareRateEmployee() {
        assertEquals(0.0145, calculator.getMedicareRateEmployee(), 0.0001);
    }

    @Test
    public void testGetMedicareRateSelfEmployed() {
        assertEquals(0.029, calculator.getMedicareRateSelfEmployed(), 0.0001);
    }

    @Test
    public void testGetAdditionalMedicareRate() {
        assertEquals(0.009, calculator.getAdditionalMedicareRate(), 0.0001);
    }
}
