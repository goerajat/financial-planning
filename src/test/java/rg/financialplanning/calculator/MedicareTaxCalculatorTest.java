package rg.financialplanning.calculator;

import org.junit.Test;
import org.junit.Before;
import rg.financialplanning.model.FilingStatus;
import rg.financialplanning.model.IndividualYearlySummary;
import rg.financialplanning.model.Person;
import rg.financialplanning.model.YearlySummary;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class MedicareTaxCalculatorTest {

    private MedicareTaxCalculator calculator;

    @Before
    public void setUp() {
        calculator = new MedicareTaxCalculator();
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

    // ===== TaxCalculator interface tests =====

    @Test
    public void testCalculateTax_withYearlySummary_employee() {
        Person employee = new Person("John", 1970, false);
        IndividualYearlySummary individual = new IndividualYearlySummary(
                employee, 2024, 100000, 0, 0, 0, 0);

        Map<String, IndividualYearlySummary> individuals = new HashMap<>();
        individuals.put("John", individual);

        YearlySummary summary = new YearlySummary(2024, 100000, 50000,
                0, 0, 0, 0, 0, 0, 0, 0, individuals);

        double expected = 100000 * 0.0145; // Employee rate, below additional Medicare threshold
        assertEquals(expected, calculator.calculateTax(summary, FilingStatus.MARRIED_FILING_JOINTLY), 0.01);
    }

    @Test
    public void testCalculateTax_withYearlySummary_selfEmployed() {
        Person selfEmployed = new Person("Jane", 1975, true);
        IndividualYearlySummary individual = new IndividualYearlySummary(
                selfEmployed, 2024, 100000, 0, 0, 0, 0);

        Map<String, IndividualYearlySummary> individuals = new HashMap<>();
        individuals.put("Jane", individual);

        YearlySummary summary = new YearlySummary(2024, 100000, 50000,
                0, 0, 0, 0, 0, 0, 0, 0, individuals);

        double expected = 100000 * 0.029; // Self-employed rate, below additional Medicare threshold
        assertEquals(expected, calculator.calculateTax(summary, FilingStatus.MARRIED_FILING_JOINTLY), 0.01);
    }

    @Test
    public void testCalculateTax_withYearlySummary_mixedEmploymentStatus() {
        Person employee = new Person("John", 1970, false);
        Person selfEmployed = new Person("Jane", 1975, true);

        IndividualYearlySummary johnSummary = new IndividualYearlySummary(
                employee, 2024, 100000, 0, 0, 0, 0);
        IndividualYearlySummary janeSummary = new IndividualYearlySummary(
                selfEmployed, 2024, 80000, 0, 0, 0, 0);

        Map<String, IndividualYearlySummary> individuals = new HashMap<>();
        individuals.put("John", johnSummary);
        individuals.put("Jane", janeSummary);

        YearlySummary summary = new YearlySummary(2024, 180000, 70000,
                0, 0, 0, 0, 0, 0, 0, 0, individuals);

        double johnTax = 100000 * 0.0145;  // Employee rate
        double janeTax = 80000 * 0.029;    // Self-employed rate
        // No additional Medicare tax since total income $180,000 < $250,000 threshold for MFJ
        double expected = johnTax + janeTax;

        assertEquals(expected, calculator.calculateTax(summary, FilingStatus.MARRIED_FILING_JOINTLY), 0.01);
    }

    @Test
    public void testCalculateTax_withAdditionalMedicareTax() {
        Person highEarner = new Person("John", 1970, false);
        IndividualYearlySummary individual = new IndividualYearlySummary(
                highEarner, 2024, 300000, 0, 0, 0, 0);

        Map<String, IndividualYearlySummary> individuals = new HashMap<>();
        individuals.put("John", individual);

        YearlySummary summary = new YearlySummary(2024, 300000, 50000,
                0, 0, 0, 0, 0, 0, 0, 0, individuals);

        double baseMedicare = 300000 * 0.0145; // Employee rate
        double additionalMedicare = (300000 - 250000) * 0.009; // $50k above MFJ threshold
        double expected = baseMedicare + additionalMedicare;

        assertEquals(expected, calculator.calculateTax(summary, FilingStatus.MARRIED_FILING_JOINTLY), 0.01);
    }

    @Test
    public void testCalculateTax_nullSummary() {
        assertEquals(0.0, calculator.calculateTax(null, FilingStatus.MARRIED_FILING_JOINTLY), 0.001);
    }

    @Test
    public void testCalculateOrdinaryIncome() {
        YearlySummary summary = new YearlySummary(2024, 100000, 50000,
                0, 0, 0, 0, 0, 0, 0, 0, null);

        assertEquals(100000, calculator.calculateOrdinaryIncome(summary), 0.01);
    }

    @Test
    public void testCalculateOrdinaryIncome_nullSummary() {
        assertEquals(0.0, calculator.calculateOrdinaryIncome(null), 0.001);
    }

    // ===== Rate getter tests =====

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
