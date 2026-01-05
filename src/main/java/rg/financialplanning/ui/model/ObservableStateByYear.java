package rg.financialplanning.ui.model;

import javafx.beans.property.*;
import rg.financialplanning.model.State;
import rg.financialplanning.model.StateByYear;

/**
 * Observable wrapper for StateByYear model for JavaFX data binding.
 */
public class ObservableStateByYear {
    private final ObjectProperty<State> state;
    private final IntegerProperty startYear;
    private final IntegerProperty endYear;

    public ObservableStateByYear(State state, int startYear, int endYear) {
        this.state = new SimpleObjectProperty<>(state);
        this.startYear = new SimpleIntegerProperty(startYear);
        this.endYear = new SimpleIntegerProperty(endYear);
    }

    public ObservableStateByYear(StateByYear stateByYear) {
        this(stateByYear.state(), stateByYear.startYear(), stateByYear.endYear());
    }

    // State property
    public ObjectProperty<State> stateProperty() {
        return state;
    }

    public State getState() {
        return state.get();
    }

    public void setState(State state) {
        this.state.set(state);
    }

    // Start year property
    public IntegerProperty startYearProperty() {
        return startYear;
    }

    public int getStartYear() {
        return startYear.get();
    }

    public void setStartYear(int startYear) {
        this.startYear.set(startYear);
    }

    // End year property
    public IntegerProperty endYearProperty() {
        return endYear;
    }

    public int getEndYear() {
        return endYear.get();
    }

    public void setEndYear(int endYear) {
        this.endYear.set(endYear);
    }

    /**
     * Converts this observable to a domain model StateByYear.
     */
    public StateByYear toStateByYear() {
        return new StateByYear(getState(), getStartYear(), getEndYear());
    }

    /**
     * Gets the duration in years.
     */
    public int getDuration() {
        return getEndYear() - getStartYear() + 1;
    }

    @Override
    public String toString() {
        return getState().getDisplayName() + " (" + getStartYear() + "-" + getEndYear() + ")";
    }
}
