package rg.financialplanning.model;

import org.junit.Test;
import static org.junit.Assert.*;

public class ItemTypePercentageTest {

    @Test
    public void testConstructor_validItemTypePercentage() {
        ItemTypePercentage itp = new ItemTypePercentage(ItemType.INCOME, 3.0);
        assertEquals(ItemType.INCOME, itp.itemType());
        assertEquals(3.0, itp.percentageIncrease(), 0.001);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructor_nullItemTypeThrowsException() {
        new ItemTypePercentage(null, 3.0);
    }

    @Test
    public void testConstructor_negativePercentage() {
        ItemTypePercentage itp = new ItemTypePercentage(ItemType.EXPENSE, -2.0);
        assertEquals(-2.0, itp.percentageIncrease(), 0.001);
    }

    @Test
    public void testConstructor_zeroPercentage() {
        ItemTypePercentage itp = new ItemTypePercentage(ItemType.CASH, 0.0);
        assertEquals(0.0, itp.percentageIncrease(), 0.001);
    }

    @Test
    public void testEquals_sameValues() {
        ItemTypePercentage itp1 = new ItemTypePercentage(ItemType.INCOME, 3.0);
        ItemTypePercentage itp2 = new ItemTypePercentage(ItemType.INCOME, 3.0);
        assertEquals(itp1, itp2);
    }

    @Test
    public void testEquals_differentItemType() {
        ItemTypePercentage itp1 = new ItemTypePercentage(ItemType.INCOME, 3.0);
        ItemTypePercentage itp2 = new ItemTypePercentage(ItemType.EXPENSE, 3.0);
        assertNotEquals(itp1, itp2);
    }

    @Test
    public void testEquals_differentPercentage() {
        ItemTypePercentage itp1 = new ItemTypePercentage(ItemType.INCOME, 3.0);
        ItemTypePercentage itp2 = new ItemTypePercentage(ItemType.INCOME, 5.0);
        assertNotEquals(itp1, itp2);
    }

    @Test
    public void testEquals_null() {
        ItemTypePercentage itp = new ItemTypePercentage(ItemType.INCOME, 3.0);
        assertNotEquals(itp, null);
    }

    @Test
    public void testHashCode_sameForEqualObjects() {
        ItemTypePercentage itp1 = new ItemTypePercentage(ItemType.INCOME, 3.0);
        ItemTypePercentage itp2 = new ItemTypePercentage(ItemType.INCOME, 3.0);
        assertEquals(itp1.hashCode(), itp2.hashCode());
    }

    @Test
    public void testToString() {
        ItemTypePercentage itp = new ItemTypePercentage(ItemType.INCOME, 3.5);
        String result = itp.toString();
        assertTrue(result.contains("INCOME"));
        assertTrue(result.contains("3.50"));
    }
}
