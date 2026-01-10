package rg.financialplanning.parser;

import rg.financialplanning.model.Person;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Parser for reading person data from a CSV file.
 * Expected format: name,yearOfBirth[,isSelfEmployed]
 * The isSelfEmployed column is optional and defaults to false.
 */
public class PersonParser {

    private final List<Person> persons;

    public PersonParser() {
        this.persons = new ArrayList<>();
    }

    public void loadFromCsv(String filePath) throws IOException {
        loadFromCsv(Path.of(filePath));
    }

    public void loadFromCsv(Path filePath) throws IOException {
        persons.clear();

        try (BufferedReader reader = Files.newBufferedReader(filePath)) {
            String headerLine = reader.readLine();
            if (headerLine == null) {
                throw new IOException("CSV file is empty");
            }

            String line;
            int lineNumber = 1;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (line.isBlank()) {
                    continue;
                }
                try {
                    Person person = parseLine(line);
                    persons.add(person);
                } catch (Exception e) {
                    throw new IOException("Error parsing line " + lineNumber + ": " + e.getMessage(), e);
                }
            }
        }
    }

    private Person parseLine(String line) {
        String[] parts = line.split(",", -1);
        if (parts.length < 2) {
            throw new IllegalArgumentException("Expected at least 2 columns (name, yearOfBirth), found " + parts.length);
        }

        String name = parts[0].trim();
        String yearOfBirthStr = parts[1].trim();

        if (name.isEmpty()) {
            throw new IllegalArgumentException("Name cannot be empty");
        }
        if (yearOfBirthStr.isEmpty()) {
            throw new IllegalArgumentException("Year of birth cannot be empty");
        }

        int yearOfBirth = Integer.parseInt(yearOfBirthStr);

        // Parse optional isSelfEmployed column (defaults to false)
        boolean isSelfEmployed = false;
        if (parts.length >= 3 && !parts[2].trim().isEmpty()) {
            String selfEmployedStr = parts[2].trim().toLowerCase();
            isSelfEmployed = selfEmployedStr.equals("true") || selfEmployedStr.equals("yes") || selfEmployedStr.equals("1");
        }

        return new Person(name, yearOfBirth, isSelfEmployed);
    }

    public List<Person> getPersons() {
        return List.copyOf(persons);
    }

    public Person getPersonByName(String name) {
        return persons.stream()
                .filter(p -> p.name().equals(name))
                .findFirst()
                .orElse(null);
    }

    public Map<String, Person> getPersonsByName() {
        return persons.stream()
                .collect(Collectors.toMap(Person::name, p -> p));
    }
}
