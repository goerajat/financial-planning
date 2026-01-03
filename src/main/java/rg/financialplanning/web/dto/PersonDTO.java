package rg.financialplanning.web.dto;

import rg.financialplanning.model.Person;
import java.time.Year;

public class PersonDTO {
    private String name;
    private int yearOfBirth;
    private Integer currentAge;

    public PersonDTO() {
    }

    public PersonDTO(String name, int yearOfBirth) {
        this.name = name;
        this.yearOfBirth = yearOfBirth;
        this.currentAge = Year.now().getValue() - yearOfBirth;
    }

    public static PersonDTO fromPerson(Person person) {
        return new PersonDTO(person.name(), person.yearOfBirth());
    }

    public Person toPerson() {
        return new Person(name, yearOfBirth);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getYearOfBirth() {
        return yearOfBirth;
    }

    public void setYearOfBirth(int yearOfBirth) {
        this.yearOfBirth = yearOfBirth;
        this.currentAge = Year.now().getValue() - yearOfBirth;
    }

    public Integer getCurrentAge() {
        if (currentAge == null && yearOfBirth > 0) {
            currentAge = Year.now().getValue() - yearOfBirth;
        }
        return currentAge;
    }

    public void setCurrentAge(Integer currentAge) {
        this.currentAge = currentAge;
    }
}
