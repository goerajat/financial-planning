package rg.financialplanning.ui.model;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import rg.financialplanning.model.Person;

import java.time.Year;

/**
 * Observable wrapper for Person to support JavaFX TableView binding.
 */
public class ObservablePerson {
    private final StringProperty name = new SimpleStringProperty();
    private final IntegerProperty yearOfBirth = new SimpleIntegerProperty();

    public ObservablePerson() {
        this("", Year.now().getValue() - 30);
    }

    public ObservablePerson(String name, int yearOfBirth) {
        this.name.set(name);
        this.yearOfBirth.set(yearOfBirth);
    }

    public ObservablePerson(Person person) {
        this(person.name(), person.yearOfBirth());
    }

    public String getName() {
        return name.get();
    }

    public void setName(String name) {
        this.name.set(name);
    }

    public StringProperty nameProperty() {
        return name;
    }

    public int getYearOfBirth() {
        return yearOfBirth.get();
    }

    public void setYearOfBirth(int yearOfBirth) {
        this.yearOfBirth.set(yearOfBirth);
    }

    public IntegerProperty yearOfBirthProperty() {
        return yearOfBirth;
    }

    public int getCurrentAge() {
        return Year.now().getValue() - yearOfBirth.get();
    }

    public Person toPerson() {
        return new Person(getName(), getYearOfBirth());
    }

    @Override
    public String toString() {
        return getName();
    }
}
