package rg.financialplanning.model;

import org.junit.Test;
import static org.junit.Assert.*;

public class FilingStatusTest {

    @Test
    public void testFromString_single() {
        assertEquals(FilingStatus.SINGLE, FilingStatus.fromString("SINGLE"));
        assertEquals(FilingStatus.SINGLE, FilingStatus.fromString("single"));
        assertEquals(FilingStatus.SINGLE, FilingStatus.fromString("Single"));
    }

    @Test
    public void testFromString_marriedFilingJointly() {
        assertEquals(FilingStatus.MARRIED_FILING_JOINTLY, FilingStatus.fromString("MARRIED_FILING_JOINTLY"));
        assertEquals(FilingStatus.MARRIED_FILING_JOINTLY, FilingStatus.fromString("MARRIED FILING JOINTLY"));
        assertEquals(FilingStatus.MARRIED_FILING_JOINTLY, FilingStatus.fromString("MFJ"));
        assertEquals(FilingStatus.MARRIED_FILING_JOINTLY, FilingStatus.fromString("mfj"));
    }

    @Test
    public void testFromString_marriedFilingSeparately() {
        assertEquals(FilingStatus.MARRIED_FILING_SEPARATELY, FilingStatus.fromString("MARRIED_FILING_SEPARATELY"));
        assertEquals(FilingStatus.MARRIED_FILING_SEPARATELY, FilingStatus.fromString("MARRIED FILING SEPARATELY"));
        assertEquals(FilingStatus.MARRIED_FILING_SEPARATELY, FilingStatus.fromString("MFS"));
    }

    @Test
    public void testFromString_headOfHousehold() {
        assertEquals(FilingStatus.HEAD_OF_HOUSEHOLD, FilingStatus.fromString("HEAD_OF_HOUSEHOLD"));
        assertEquals(FilingStatus.HEAD_OF_HOUSEHOLD, FilingStatus.fromString("HEAD OF HOUSEHOLD"));
        assertEquals(FilingStatus.HEAD_OF_HOUSEHOLD, FilingStatus.fromString("HOH"));
    }

    @Test
    public void testFromString_trimsWhitespace() {
        assertEquals(FilingStatus.SINGLE, FilingStatus.fromString("  SINGLE  "));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFromString_nullThrowsException() {
        FilingStatus.fromString(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFromString_emptyThrowsException() {
        FilingStatus.fromString("");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFromString_blankThrowsException() {
        FilingStatus.fromString("   ");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFromString_unknownStatusThrowsException() {
        FilingStatus.fromString("UNKNOWN_STATUS");
    }
}
