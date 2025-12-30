package rg.financialplanning.model;

import org.junit.Test;
import java.time.Year;
import static org.junit.Assert.*;

public class PersonTest {

    @Test
    public void testConstructor_validPerson() {
        Person person = new Person("John Doe", 1970);
        assertEquals("John Doe", person.name());
        assertEquals(1970, person.yearOfBirth());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructor_nullNameThrowsException() {
        new Person(null, 1970);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructor_emptyNameThrowsException() {
        new Person("", 1970);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructor_blankNameThrowsException() {
        new Person("   ", 1970);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructor_yearOfBirthTooEarlyThrowsException() {
        new Person("John Doe", 1899);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructor_yearOfBirthTooLateThrowsException() {
        new Person("John Doe", 2101);
    }

    @Test
    public void testConstructor_yearOfBirthBoundaries() {
        Person person1900 = new Person("Test", 1900);
        assertEquals(1900, person1900.yearOfBirth());

        Person person2100 = new Person("Test", 2100);
        assertEquals(2100, person2100.yearOfBirth());
    }

    @Test
    public void testGetAgeInYear() {
        Person person = new Person("John Doe", 1970);
        assertEquals(30, person.getAgeInYear(2000));
        assertEquals(55, person.getAgeInYear(2025));
        assertEquals(0, person.getAgeInYear(1970));
    }

    @Test
    public void testGetCurrentAge() {
        int currentYear = Year.now().getValue();
        Person person = new Person("John Doe", currentYear - 30);
        assertEquals(30, person.getCurrentAge());
    }

    @Test
    public void testEquals_samePerson() {
        Person person1 = new Person("John Doe", 1970);
        Person person2 = new Person("John Doe", 1970);
        assertEquals(person1, person2);
    }

    @Test
    public void testEquals_differentName() {
        Person person1 = new Person("John Doe", 1970);
        Person person2 = new Person("Jane Doe", 1970);
        assertNotEquals(person1, person2);
    }

    @Test
    public void testEquals_differentYearOfBirth() {
        Person person1 = new Person("John Doe", 1970);
        Person person2 = new Person("John Doe", 1980);
        assertNotEquals(person1, person2);
    }

    @Test
    public void testEquals_null() {
        Person person = new Person("John Doe", 1970);
        assertNotEquals(person, null);
    }

    @Test
    public void testHashCode_sameForEqualPersons() {
        Person person1 = new Person("John Doe", 1970);
        Person person2 = new Person("John Doe", 1970);
        assertEquals(person1.hashCode(), person2.hashCode());
    }

    @Test
    public void testToString() {
        Person person = new Person("John Doe", 1970);
        String expected = "John Doe (born 1970)";
        assertEquals(expected, person.toString());
    }
}
