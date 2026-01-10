package rg.financialplanning.ui.model;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
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
    private final BooleanProperty selfEmployed = new SimpleBooleanProperty();

    public ObservablePerson() {
        this("", Year.now().getValue() - 30, false);
    }

    public ObservablePerson(String name, int yearOfBirth) {
        this(name, yearOfBirth, false);
    }

    public ObservablePerson(String name, int yearOfBirth, boolean selfEmployed) {
        this.name.set(name);
        this.yearOfBirth.set(yearOfBirth);
        this.selfEmployed.set(selfEmployed);
    }

    public ObservablePerson(Person person) {
        this(person.name(), person.yearOfBirth(), person.isSelfEmployed());
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

    public boolean isSelfEmployed() {
        return selfEmployed.get();
    }

    public void setSelfEmployed(boolean selfEmployed) {
        this.selfEmployed.set(selfEmployed);
    }

    public BooleanProperty selfEmployedProperty() {
        return selfEmployed;
    }

    public int getCurrentAge() {
        return Year.now().getValue() - yearOfBirth.get();
    }

    public Person toPerson() {
        return new Person(getName(), getYearOfBirth(), isSelfEmployed());
    }

    @Override
    public String toString() {
        return getName();
    }
}
