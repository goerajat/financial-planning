package rg.financialplanning.model;

import org.junit.Test;
import static org.junit.Assert.*;

public class ItemTypeTest {

    @Test
    public void testFromString_income() {
        assertEquals(ItemType.INCOME, ItemType.fromString("INCOME"));
        assertEquals(ItemType.INCOME, ItemType.fromString("income"));
        assertEquals(ItemType.INCOME, ItemType.fromString("Income"));
    }

    @Test
    public void testFromString_expense() {
        assertEquals(ItemType.EXPENSE, ItemType.fromString("EXPENSE"));
        assertEquals(ItemType.EXPENSE, ItemType.fromString("expense"));
    }

    @Test
    public void testFromString_nonQualified() {
        assertEquals(ItemType.NON_QUALIFIED, ItemType.fromString("NON_QUALIFIED"));
        assertEquals(ItemType.NON_QUALIFIED, ItemType.fromString("NONQUALIFIED"));
        assertEquals(ItemType.NON_QUALIFIED, ItemType.fromString("ASSET"));
    }

    @Test
    public void testFromString_qualified() {
        assertEquals(ItemType.QUALIFIED, ItemType.fromString("QUALIFIED"));
        assertEquals(ItemType.QUALIFIED, ItemType.fromString("401K"));
    }

    @Test
    public void testFromString_roth() {
        assertEquals(ItemType.ROTH, ItemType.fromString("ROTH"));
        assertEquals(ItemType.ROTH, ItemType.fromString("roth"));
    }

    @Test
    public void testFromString_cash() {
        assertEquals(ItemType.CASH, ItemType.fromString("CASH"));
    }

    @Test
    public void testFromString_lifeInsuranceBenefit() {
        assertEquals(ItemType.LIFE_INSURANCE_BENEFIT, ItemType.fromString("LIFE_INSURANCE_BENEFIT"));
        assertEquals(ItemType.LIFE_INSURANCE_BENEFIT, ItemType.fromString("LIFE INSURANCE BENEFIT"));
    }

    @Test
    public void testFromString_realEstate() {
        assertEquals(ItemType.REAL_ESTATE, ItemType.fromString("REAL_ESTATE"));
        assertEquals(ItemType.REAL_ESTATE, ItemType.fromString("REAL ESTATE"));
    }

    @Test
    public void testFromString_socialSecurityBenefits() {
        assertEquals(ItemType.SOCIAL_SECURITY_BENEFITS, ItemType.fromString("SOCIAL_SECURITY_BENEFITS"));
        assertEquals(ItemType.SOCIAL_SECURITY_BENEFITS, ItemType.fromString("SOCIAL SECURITY BENEFITS"));
        assertEquals(ItemType.SOCIAL_SECURITY_BENEFITS, ItemType.fromString("SOCIAL_SECURITY"));
        assertEquals(ItemType.SOCIAL_SECURITY_BENEFITS, ItemType.fromString("SSA"));
    }

    @Test
    public void testFromString_trimsWhitespace() {
        assertEquals(ItemType.INCOME, ItemType.fromString("  INCOME  "));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFromString_nullThrowsException() {
        ItemType.fromString(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFromString_emptyThrowsException() {
        ItemType.fromString("");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFromString_blankThrowsException() {
        ItemType.fromString("   ");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFromString_unknownTypeThrowsException() {
        ItemType.fromString("UNKNOWN_TYPE");
    }
}
