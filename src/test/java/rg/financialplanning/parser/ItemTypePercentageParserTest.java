package rg.financialplanning.parser;

import org.junit.Test;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import rg.financialplanning.model.ItemType;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Map;

import static org.junit.Assert.*;

public class ItemTypePercentageParserTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private ItemTypePercentageParser parser;

    @Before
    public void setUp() {
        parser = new ItemTypePercentageParser();
    }

    @Test
    public void testLoadFromCsv_validFile() throws IOException {
        File csvFile = tempFolder.newFile("percentages.csv");
        String content = "item_type,percentage_increase\nINCOME,3.0\nEXPENSE,2.5";
        Files.writeString(csvFile.toPath(), content);

        parser.loadFromCsv(csvFile.getAbsolutePath());
        Map<ItemType, Double> percentages = parser.getPercentagesByType();

        assertEquals(2, percentages.size());
        assertEquals(3.0, percentages.get(ItemType.INCOME), 0.001);
        assertEquals(2.5, percentages.get(ItemType.EXPENSE), 0.001);
    }

    @Test
    public void testLoadFromCsv_withPath() throws IOException {
        File csvFile = tempFolder.newFile("percentages.csv");
        String content = "item_type,percentage_increase\nQUALIFIED,7.0";
        Files.writeString(csvFile.toPath(), content);

        parser.loadFromCsv(csvFile.toPath());
        Map<ItemType, Double> percentages = parser.getPercentagesByType();

        assertEquals(1, percentages.size());
        assertEquals(7.0, percentages.get(ItemType.QUALIFIED), 0.001);
    }

    @Test
    public void testLoadFromCsv_allItemTypes() throws IOException {
        File csvFile = tempFolder.newFile("percentages.csv");
        String content = "item_type,percentage_increase\n" +
                "INCOME,3.0\n" +
                "EXPENSE,2.5\n" +
                "QUALIFIED,7.0\n" +
                "NON_QUALIFIED,6.0\n" +
                "ROTH,7.0\n" +
                "CASH,1.0\n" +
                "REAL_ESTATE,3.0\n" +
                "LIFE_INSURANCE_BENEFIT,0.0\n" +
                "SOCIAL_SECURITY_BENEFITS,2.0";
        Files.writeString(csvFile.toPath(), content);

        parser.loadFromCsv(csvFile.getAbsolutePath());
        Map<ItemType, Double> percentages = parser.getPercentagesByType();

        assertEquals(9, percentages.size());
        assertEquals(7.0, percentages.get(ItemType.QUALIFIED), 0.001);
        assertEquals(6.0, percentages.get(ItemType.NON_QUALIFIED), 0.001);
    }

    @Test
    public void testLoadFromCsv_skipsBlankLines() throws IOException {
        File csvFile = tempFolder.newFile("percentages.csv");
        String content = "item_type,percentage_increase\nINCOME,3.0\n\n\nEXPENSE,2.5\n";
        Files.writeString(csvFile.toPath(), content);

        parser.loadFromCsv(csvFile.getAbsolutePath());
        Map<ItemType, Double> percentages = parser.getPercentagesByType();

        assertEquals(2, percentages.size());
    }

    @Test
    public void testLoadFromCsv_clearsExistingData() throws IOException {
        File csvFile1 = tempFolder.newFile("percentages1.csv");
        String content1 = "item_type,percentage_increase\nINCOME,3.0";
        Files.writeString(csvFile1.toPath(), content1);

        File csvFile2 = tempFolder.newFile("percentages2.csv");
        String content2 = "item_type,percentage_increase\nEXPENSE,2.5";
        Files.writeString(csvFile2.toPath(), content2);

        parser.loadFromCsv(csvFile1.getAbsolutePath());
        assertEquals(1, parser.getPercentagesByType().size());
        assertNotNull(parser.getPercentageForType(ItemType.INCOME));

        parser.loadFromCsv(csvFile2.getAbsolutePath());
        assertEquals(1, parser.getPercentagesByType().size());
        assertNull(parser.getPercentageForType(ItemType.INCOME));
        assertNotNull(parser.getPercentageForType(ItemType.EXPENSE));
    }

    @Test(expected = IOException.class)
    public void testLoadFromCsv_emptyFileThrowsException() throws IOException {
        File csvFile = tempFolder.newFile("empty.csv");
        Files.writeString(csvFile.toPath(), "");

        parser.loadFromCsv(csvFile.getAbsolutePath());
    }

    @Test(expected = IOException.class)
    public void testLoadFromCsv_missingColumnsThrowsException() throws IOException {
        File csvFile = tempFolder.newFile("invalid.csv");
        String content = "item_type,percentage_increase\nINCOME";
        Files.writeString(csvFile.toPath(), content);

        parser.loadFromCsv(csvFile.getAbsolutePath());
    }

    @Test(expected = IOException.class)
    public void testLoadFromCsv_emptyItemTypeThrowsException() throws IOException {
        File csvFile = tempFolder.newFile("invalid.csv");
        String content = "item_type,percentage_increase\n,3.0";
        Files.writeString(csvFile.toPath(), content);

        parser.loadFromCsv(csvFile.getAbsolutePath());
    }

    @Test(expected = IOException.class)
    public void testLoadFromCsv_emptyPercentageThrowsException() throws IOException {
        File csvFile = tempFolder.newFile("invalid.csv");
        String content = "item_type,percentage_increase\nINCOME,";
        Files.writeString(csvFile.toPath(), content);

        parser.loadFromCsv(csvFile.getAbsolutePath());
    }

    @Test(expected = IOException.class)
    public void testLoadFromCsv_invalidPercentageThrowsException() throws IOException {
        File csvFile = tempFolder.newFile("invalid.csv");
        String content = "item_type,percentage_increase\nINCOME,abc";
        Files.writeString(csvFile.toPath(), content);

        parser.loadFromCsv(csvFile.getAbsolutePath());
    }

    @Test(expected = IOException.class)
    public void testLoadFromCsv_invalidItemTypeThrowsException() throws IOException {
        File csvFile = tempFolder.newFile("invalid.csv");
        String content = "item_type,percentage_increase\nINVALID_TYPE,3.0";
        Files.writeString(csvFile.toPath(), content);

        parser.loadFromCsv(csvFile.getAbsolutePath());
    }

    @Test
    public void testGetPercentagesByType_returnsImmutableCopy() throws IOException {
        File csvFile = tempFolder.newFile("percentages.csv");
        String content = "item_type,percentage_increase\nINCOME,3.0";
        Files.writeString(csvFile.toPath(), content);

        parser.loadFromCsv(csvFile.getAbsolutePath());
        Map<ItemType, Double> percentages = parser.getPercentagesByType();

        try {
            percentages.put(ItemType.EXPENSE, 2.5);
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // Expected
        }
    }

    @Test
    public void testGetPercentageForType_found() throws IOException {
        File csvFile = tempFolder.newFile("percentages.csv");
        String content = "item_type,percentage_increase\nINCOME,3.0";
        Files.writeString(csvFile.toPath(), content);

        parser.loadFromCsv(csvFile.getAbsolutePath());
        Double percentage = parser.getPercentageForType(ItemType.INCOME);

        assertNotNull(percentage);
        assertEquals(3.0, percentage, 0.001);
    }

    @Test
    public void testGetPercentageForType_notFound() throws IOException {
        File csvFile = tempFolder.newFile("percentages.csv");
        String content = "item_type,percentage_increase\nINCOME,3.0";
        Files.writeString(csvFile.toPath(), content);

        parser.loadFromCsv(csvFile.getAbsolutePath());
        Double percentage = parser.getPercentageForType(ItemType.EXPENSE);

        assertNull(percentage);
    }

    @Test
    public void testGetPercentageForTypeOrDefault_found() throws IOException {
        File csvFile = tempFolder.newFile("percentages.csv");
        String content = "item_type,percentage_increase\nINCOME,3.0";
        Files.writeString(csvFile.toPath(), content);

        parser.loadFromCsv(csvFile.getAbsolutePath());
        double percentage = parser.getPercentageForTypeOrDefault(ItemType.INCOME, 5.0);

        assertEquals(3.0, percentage, 0.001);
    }

    @Test
    public void testGetPercentageForTypeOrDefault_notFound() throws IOException {
        File csvFile = tempFolder.newFile("percentages.csv");
        String content = "item_type,percentage_increase\nINCOME,3.0";
        Files.writeString(csvFile.toPath(), content);

        parser.loadFromCsv(csvFile.getAbsolutePath());
        double percentage = parser.getPercentageForTypeOrDefault(ItemType.EXPENSE, 5.0);

        assertEquals(5.0, percentage, 0.001);
    }

    @Test
    public void testLoadFromCsv_negativePercentage() throws IOException {
        File csvFile = tempFolder.newFile("percentages.csv");
        String content = "item_type,percentage_increase\nEXPENSE,-2.0";
        Files.writeString(csvFile.toPath(), content);

        parser.loadFromCsv(csvFile.getAbsolutePath());
        assertEquals(-2.0, parser.getPercentageForType(ItemType.EXPENSE), 0.001);
    }

    @Test
    public void testLoadFromCsv_decimalPercentage() throws IOException {
        File csvFile = tempFolder.newFile("percentages.csv");
        String content = "item_type,percentage_increase\nINCOME,3.75";
        Files.writeString(csvFile.toPath(), content);

        parser.loadFromCsv(csvFile.getAbsolutePath());
        assertEquals(3.75, parser.getPercentageForType(ItemType.INCOME), 0.001);
    }

    @Test
    public void testLoadFromCsv_trimsWhitespace() throws IOException {
        File csvFile = tempFolder.newFile("percentages.csv");
        String content = "item_type,percentage_increase\n  INCOME  ,  3.0  ";
        Files.writeString(csvFile.toPath(), content);

        parser.loadFromCsv(csvFile.getAbsolutePath());
        assertEquals(3.0, parser.getPercentageForType(ItemType.INCOME), 0.001);
    }
}
