package rg.financialplanning.ui.dialog;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import rg.financialplanning.ui.model.ObservablePerson;

import java.time.Year;

/**
 * Dialog for adding or editing a person.
 */
public class PersonEditDialog extends Dialog<ObservablePerson> {

    private final TextField nameField;
    private final ComboBox<Integer> yearComboBox;
    private final Label ageLabel;

    public PersonEditDialog(ObservablePerson existingPerson) {
        setTitle(existingPerson == null ? "Add Person" : "Edit Person");
        setHeaderText(existingPerson == null ? "Enter person details" : "Modify person details");

        // Set the button types
        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        // Create the form
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        nameField = new TextField();
        nameField.setPromptText("Full Name");
        nameField.setPrefWidth(200);

        int currentYear = Year.now().getValue();

        // Create year dropdown (from 1920 to current year)
        yearComboBox = new ComboBox<>();
        for (int year = currentYear; year >= 1920; year--) {
            yearComboBox.getItems().add(year);
        }
        yearComboBox.setValue(currentYear - 30); // Default to 30 years ago
        yearComboBox.setPrefWidth(100);

        ageLabel = new Label();
        ageLabel.setStyle("-fx-text-fill: #666;");

        // Update age label when year changes
        yearComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                int age = currentYear - newVal;
                ageLabel.setText("(Currently " + age + " years old)");
            }
        });

        // Pre-populate if editing
        if (existingPerson != null) {
            nameField.setText(existingPerson.getName());
            yearComboBox.setValue(existingPerson.getYearOfBirth());
        }

        // Trigger initial age calculation
        int initialAge = currentYear - yearComboBox.getValue();
        ageLabel.setText("(Currently " + initialAge + " years old)");

        grid.add(new Label("Name:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Year of Birth:"), 0, 1);
        grid.add(yearComboBox, 1, 1);
        grid.add(ageLabel, 2, 1);

        getDialogPane().setContent(grid);

        // Enable/Disable save button depending on whether name was entered
        javafx.scene.Node saveButton = getDialogPane().lookupButton(saveButtonType);
        saveButton.setDisable(true);

        nameField.textProperty().addListener((observable, oldValue, newValue) -> {
            saveButton.setDisable(newValue == null || newValue.trim().isEmpty());
        });

        // Request focus on the name field
        javafx.application.Platform.runLater(nameField::requestFocus);

        // Convert the result to ObservablePerson when save is clicked
        setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                String name = nameField.getText().trim();
                int yearOfBirth = yearComboBox.getValue();
                return new ObservablePerson(name, yearOfBirth);
            }
            return null;
        });
    }
}
