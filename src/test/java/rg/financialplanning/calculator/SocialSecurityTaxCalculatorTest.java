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

        double expected = 100000 * 0.062; // Employee rate
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

        double expected = 100000 * 0.124; // Self-employed rate
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

        double johnTax = 100000 * 0.062;  // Employee rate
        double janeTax = 80000 * 0.124;   // Self-employed rate
        double expected = johnTax + janeTax;

        assertEquals(expected, calculator.calculateTax(summary, FilingStatus.MARRIED_FILING_JOINTLY), 0.01);
    }

    @Test
    public void testCalculateTax_nullSummary() {
        assertEquals(0.0, calculator.calculateTax(null, FilingStatus.MARRIED_FILING_JOINTLY), 0.001);
    }

    @Test
    public void testCalculateOrdinaryIncome_belowWageBase() {
        YearlySummary summary = new YearlySummary(2024, 100000, 50000,
                0, 0, 0, 0, 0, 0, 0, 0, null);

        assertEquals(100000, calculator.calculateOrdinaryIncome(summary), 0.01);
    }

    @Test
    public void testCalculateOrdinaryIncome_aboveWageBase() {
        YearlySummary summary = new YearlySummary(2024, 200000, 50000,
                0, 0, 0, 0, 0, 0, 0, 0, null);

        assertEquals(168600, calculator.calculateOrdinaryIncome(summary), 0.01);
    }

    @Test
    public void testCalculateOrdinaryIncome_nullSummary() {
        assertEquals(0.0, calculator.calculateOrdinaryIncome(null), 0.001);
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
}
