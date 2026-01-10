package rg.financialplanning.web.dto;

import rg.financialplanning.model.Person;
import java.time.Year;

public class PersonDTO {
    private String name;
    private int yearOfBirth;
    private Integer currentAge;
    private boolean selfEmployed;

    public PersonDTO() {
    }

    public PersonDTO(String name, int yearOfBirth) {
        this(name, yearOfBirth, false);
    }

    public PersonDTO(String name, int yearOfBirth, boolean selfEmployed) {
        this.name = name;
        this.yearOfBirth = yearOfBirth;
        this.currentAge = Year.now().getValue() - yearOfBirth;
        this.selfEmployed = selfEmployed;
    }

    public static PersonDTO fromPerson(Person person) {
        return new PersonDTO(person.name(), person.yearOfBirth(), person.isSelfEmployed());
    }

    public Person toPerson() {
        return new Person(name, yearOfBirth, selfEmployed);
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

    public boolean isSelfEmployed() {
        return selfEmployed;
    }

    public void setSelfEmployed(boolean selfEmployed) {
        this.selfEmployed = selfEmployed;
    }
}
