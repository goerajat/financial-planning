package rg.financialplanning.model;

import org.junit.Test;
import static org.junit.Assert.*;

public class FinancialEntryTest {

    @Test
    public void testConstructor_validEntry() {
        FinancialEntry entry = new FinancialEntry("John", ItemType.INCOME, "Salary", 100000, 2024, 2030);
        assertEquals("John", entry.name());
        assertEquals(ItemType.INCOME, entry.item());
        assertEquals("Salary", entry.description());
        assertEquals(100000, entry.value());
        assertEquals(2024, entry.startYear());
        assertEquals(2030, entry.endYear());
    }

    @Test
    public void testConstructor_nullName() {
        FinancialEntry entry = new FinancialEntry(null, ItemType.INCOME, "Salary", 100000, 2024, 2030);
        assertNull(entry.name());
    }

    @Test
    public void testConstructor_nullDescription() {
        FinancialEntry entry = new FinancialEntry("John", ItemType.INCOME, null, 100000, 2024, 2030);
        assertNull(entry.description());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructor_startYearAfterEndYearThrowsException() {
        new FinancialEntry("John", ItemType.INCOME, "Salary", 100000, 2030, 2024);
    }

    @Test
    public void testConstructor_startYearEqualsEndYear() {
        FinancialEntry entry = new FinancialEntry("John", ItemType.INCOME, "Salary", 100000, 2024, 2024);
        assertEquals(2024, entry.startYear());
        assertEquals(2024, entry.endYear());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructor_negativeValueThrowsException() {
        new FinancialEntry("John", ItemType.INCOME, "Salary", -1, 2024, 2030);
    }

    @Test
    public void testConstructor_zeroValue() {
        FinancialEntry entry = new FinancialEntry("John", ItemType.INCOME, "Salary", 0, 2024, 2030);
        assertEquals(0, entry.value());
    }

    @Test
    public void testIsActiveInYear_beforeStartYear() {
        FinancialEntry entry = new FinancialEntry("John", ItemType.INCOME, "Salary", 100000, 2024, 2030);
        assertFalse(entry.isActiveInYear(2023));
    }

    @Test
    public void testIsActiveInYear_inStartYear() {
        FinancialEntry entry = new FinancialEntry("John", ItemType.INCOME, "Salary", 100000, 2024, 2030);
        assertTrue(entry.isActiveInYear(2024));
    }

    @Test
    public void testIsActiveInYear_midRange() {
        FinancialEntry entry = new FinancialEntry("John", ItemType.INCOME, "Salary", 100000, 2024, 2030);
        assertTrue(entry.isActiveInYear(2027));
    }

    @Test
    public void testIsActiveInYear_inEndYear() {
        FinancialEntry entry = new FinancialEntry("John", ItemType.INCOME, "Salary", 100000, 2024, 2030);
        assertTrue(entry.isActiveInYear(2030));
    }

    @Test
    public void testIsActiveInYear_afterEndYear() {
        FinancialEntry entry = new FinancialEntry("John", ItemType.INCOME, "Salary", 100000, 2024, 2030);
        assertFalse(entry.isActiveInYear(2031));
    }

    @Test
    public void testGetValueForYear_notActive() {
        FinancialEntry entry = new FinancialEntry("John", ItemType.INCOME, "Salary", 100000, 2024, 2030);
        assertEquals(0, entry.getValueForYear(2023, 3.0), 0.001);
        assertEquals(0, entry.getValueForYear(2031, 3.0), 0.001);
    }

    @Test
    public void testGetValueForYear_firstYear() {
        FinancialEntry entry = new FinancialEntry("John", ItemType.INCOME, "Salary", 100000, 2024, 2030);
        assertEquals(100000, entry.getValueForYear(2024, 3.0), 0.001);
    }

    @Test
    public void testGetValueForYear_withPercentageIncrease() {
        FinancialEntry entry = new FinancialEntry("John", ItemType.INCOME, "Salary", 100000, 2024, 2030);
        // After 1 year with 3% increase
        assertEquals(100000 * 1.03, entry.getValueForYear(2025, 3.0), 0.01);
        // After 2 years with 3% increase
        assertEquals(100000 * 1.03 * 1.03, entry.getValueForYear(2026, 3.0), 0.01);
    }

    @Test
    public void testGetValueForYear_zeroPercentageIncrease() {
        FinancialEntry entry = new FinancialEntry("John", ItemType.INCOME, "Salary", 100000, 2024, 2030);
        assertEquals(100000, entry.getValueForYear(2025, 0.0), 0.001);
        assertEquals(100000, entry.getValueForYear(2030, 0.0), 0.001);
    }

    @Test
    public void testGetValueForYear_negativePercentageIncrease() {
        FinancialEntry entry = new FinancialEntry("John", ItemType.INCOME, "Salary", 100000, 2024, 2030);
        // After 1 year with -5% decrease
        assertEquals(100000 * 0.95, entry.getValueForYear(2025, -5.0), 0.01);
    }
}
