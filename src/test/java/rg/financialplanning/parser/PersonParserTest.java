package rg.financialplanning.parser;

import org.junit.Test;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import rg.financialplanning.model.Person;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class PersonParserTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private PersonParser parser;

    @Before
    public void setUp() {
        parser = new PersonParser();
    }

    @Test
    public void testLoadFromCsv_validFile() throws IOException {
        File csvFile = tempFolder.newFile("persons.csv");
        String content = "name,yearOfBirth\nJohn,1970\nJane,1975";
        Files.writeString(csvFile.toPath(), content);

        parser.loadFromCsv(csvFile.getAbsolutePath());
        List<Person> persons = parser.getPersons();

        assertEquals(2, persons.size());
        assertEquals("John", persons.get(0).name());
        assertEquals(1970, persons.get(0).yearOfBirth());
        assertEquals("Jane", persons.get(1).name());
        assertEquals(1975, persons.get(1).yearOfBirth());
    }

    @Test
    public void testLoadFromCsv_withPath() throws IOException {
        File csvFile = tempFolder.newFile("persons.csv");
        String content = "name,yearOfBirth\nAlice,1980";
        Files.writeString(csvFile.toPath(), content);

        parser.loadFromCsv(csvFile.toPath());
        List<Person> persons = parser.getPersons();

        assertEquals(1, persons.size());
        assertEquals("Alice", persons.get(0).name());
    }

    @Test
    public void testLoadFromCsv_skipsBlankLines() throws IOException {
        File csvFile = tempFolder.newFile("persons.csv");
        String content = "name,yearOfBirth\nJohn,1970\n\n\nJane,1975\n";
        Files.writeString(csvFile.toPath(), content);

        parser.loadFromCsv(csvFile.getAbsolutePath());
        List<Person> persons = parser.getPersons();

        assertEquals(2, persons.size());
    }

    @Test
    public void testLoadFromCsv_clearsExistingData() throws IOException {
        File csvFile1 = tempFolder.newFile("persons1.csv");
        String content1 = "name,yearOfBirth\nJohn,1970";
        Files.writeString(csvFile1.toPath(), content1);

        File csvFile2 = tempFolder.newFile("persons2.csv");
        String content2 = "name,yearOfBirth\nJane,1975";
        Files.writeString(csvFile2.toPath(), content2);

        parser.loadFromCsv(csvFile1.getAbsolutePath());
        assertEquals(1, parser.getPersons().size());

        parser.loadFromCsv(csvFile2.getAbsolutePath());
        assertEquals(1, parser.getPersons().size());
        assertEquals("Jane", parser.getPersons().get(0).name());
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
        String content = "name,yearOfBirth\nJohn";
        Files.writeString(csvFile.toPath(), content);

        parser.loadFromCsv(csvFile.getAbsolutePath());
    }

    @Test(expected = IOException.class)
    public void testLoadFromCsv_emptyNameThrowsException() throws IOException {
        File csvFile = tempFolder.newFile("invalid.csv");
        String content = "name,yearOfBirth\n,1970";
        Files.writeString(csvFile.toPath(), content);

        parser.loadFromCsv(csvFile.getAbsolutePath());
    }

    @Test(expected = IOException.class)
    public void testLoadFromCsv_emptyYearOfBirthThrowsException() throws IOException {
        File csvFile = tempFolder.newFile("invalid.csv");
        String content = "name,yearOfBirth\nJohn,";
        Files.writeString(csvFile.toPath(), content);

        parser.loadFromCsv(csvFile.getAbsolutePath());
    }

    @Test(expected = IOException.class)
    public void testLoadFromCsv_invalidYearOfBirthThrowsException() throws IOException {
        File csvFile = tempFolder.newFile("invalid.csv");
        String content = "name,yearOfBirth\nJohn,abc";
        Files.writeString(csvFile.toPath(), content);

        parser.loadFromCsv(csvFile.getAbsolutePath());
    }

    @Test
    public void testGetPersons_returnsImmutableCopy() throws IOException {
        File csvFile = tempFolder.newFile("persons.csv");
        String content = "name,yearOfBirth\nJohn,1970";
        Files.writeString(csvFile.toPath(), content);

        parser.loadFromCsv(csvFile.getAbsolutePath());
        List<Person> persons = parser.getPersons();

        try {
            persons.add(new Person("Test", 1980));
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // Expected
        }
    }

    @Test
    public void testGetPersonByName_found() throws IOException {
        File csvFile = tempFolder.newFile("persons.csv");
        String content = "name,yearOfBirth\nJohn,1970\nJane,1975";
        Files.writeString(csvFile.toPath(), content);

        parser.loadFromCsv(csvFile.getAbsolutePath());
        Person john = parser.getPersonByName("John");

        assertNotNull(john);
        assertEquals("John", john.name());
        assertEquals(1970, john.yearOfBirth());
    }

    @Test
    public void testGetPersonByName_notFound() throws IOException {
        File csvFile = tempFolder.newFile("persons.csv");
        String content = "name,yearOfBirth\nJohn,1970";
        Files.writeString(csvFile.toPath(), content);

        parser.loadFromCsv(csvFile.getAbsolutePath());
        Person unknown = parser.getPersonByName("Unknown");

        assertNull(unknown);
    }

    @Test
    public void testGetPersonsByName() throws IOException {
        File csvFile = tempFolder.newFile("persons.csv");
        String content = "name,yearOfBirth\nJohn,1970\nJane,1975";
        Files.writeString(csvFile.toPath(), content);

        parser.loadFromCsv(csvFile.getAbsolutePath());
        Map<String, Person> personsByName = parser.getPersonsByName();

        assertEquals(2, personsByName.size());
        assertEquals(1970, personsByName.get("John").yearOfBirth());
        assertEquals(1975, personsByName.get("Jane").yearOfBirth());
    }

    @Test
    public void testLoadFromCsv_trimsWhitespace() throws IOException {
        File csvFile = tempFolder.newFile("persons.csv");
        String content = "name,yearOfBirth\n  John  ,  1970  ";
        Files.writeString(csvFile.toPath(), content);

        parser.loadFromCsv(csvFile.getAbsolutePath());
        Person john = parser.getPersons().get(0);

        assertEquals("John", john.name());
        assertEquals(1970, john.yearOfBirth());
    }
}
