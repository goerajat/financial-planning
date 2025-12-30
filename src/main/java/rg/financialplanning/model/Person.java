package rg.financialplanning.model;

import java.time.Year;
import java.util.Objects;

/**
 * Class representing a person with their name and year of birth.
 */
public class Person {
    private final String name;
    private final int yearOfBirth;

    public Person(String name, int yearOfBirth) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Name cannot be null or empty");
        }
        if (yearOfBirth < 1900 || yearOfBirth > 2100) {
            throw new IllegalArgumentException("Year of birth must be between 1900 and 2100");
        }
        this.name = name;
        this.yearOfBirth = yearOfBirth;
    }

    public String name() {
        return name;
    }

    public int yearOfBirth() {
        return yearOfBirth;
    }

    public int getAgeInYear(int year) {
        return year - yearOfBirth;
    }

    public int getCurrentAge() {
        return Year.now().getValue() - yearOfBirth;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Person person = (Person) o;
        return yearOfBirth == person.yearOfBirth && Objects.equals(name, person.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, yearOfBirth);
    }

    @Override
    public String toString() {
        return String.format("%s (born %d)", name, yearOfBirth);
    }
}
