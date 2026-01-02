package rg.financialplanning.ui.dialog;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import rg.financialplanning.model.ItemType;
import rg.financialplanning.ui.model.ObservableFinancialEntry;
import rg.financialplanning.ui.model.ObservablePerson;

import java.time.Year;

/**
 * Dialog for adding or editing a financial entry.
 */
public class EntryEditDialog extends Dialog<ObservableFinancialEntry> {

    private final ComboBox<String> personComboBox;
    private final ComboBox<ItemType> typeComboBox;
    private final TextField descriptionField;
    private final Spinner<Integer> valueSpinner;
    private final ComboBox<Integer> startYearComboBox;
    private final ComboBox<Integer> endYearComboBox;
    private final Label typeDescriptionLabel;

    public EntryEditDialog(ObservableFinancialEntry existingEntry, ObservableList<ObservablePerson> persons) {
        setTitle(existingEntry == null ? "Add Financial Entry" : "Edit Financial Entry");
        setHeaderText(existingEntry == null ? "Enter entry details" : "Modify entry details");

        // Set the button types
        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        // Create the form
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 20, 10, 10));

        // Person selection
        ObservableList<String> personNames = FXCollections.observableArrayList();
        personNames.add(""); // Empty option for entries that don't belong to a person
        persons.forEach(p -> personNames.add(p.getName()));

        personComboBox = new ComboBox<>(personNames);
        personComboBox.setPromptText("Select person (optional)");
        personComboBox.setPrefWidth(200);
        personComboBox.setEditable(true); // Allow entering names not in the list

        // Item type selection
        typeComboBox = new ComboBox<>(FXCollections.observableArrayList(ItemType.values()));
        typeComboBox.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(ItemType item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? "" : getDisplayName(item));
            }
        });
        typeComboBox.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(ItemType item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? "" : getDisplayName(item));
            }
        });
        typeComboBox.setPrefWidth(250);

        typeDescriptionLabel = new Label();
        typeDescriptionLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 11px;");
        typeDescriptionLabel.setWrapText(true);
        typeDescriptionLabel.setMaxWidth(350);

        typeComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                typeDescriptionLabel.setText(getTypeDescription(newVal));
            }
        });

        // Description
        descriptionField = new TextField();
        descriptionField.setPromptText("e.g., Salary, Rent, 401K");
        descriptionField.setPrefWidth(250);

        // Value
        valueSpinner = new Spinner<>(0, 100_000_000, 0, 1000);
        valueSpinner.setEditable(true);
        valueSpinner.setPrefWidth(150);

        // Year range dropdowns
        int currentYear = Year.now().getValue();

        // Create start year dropdown (from current year to 2100)
        startYearComboBox = new ComboBox<>();
        for (int year = currentYear; year <= 2100; year++) {
            startYearComboBox.getItems().add(year);
        }
        startYearComboBox.setValue(currentYear);
        startYearComboBox.setPrefWidth(100);

        // Create end year dropdown (from current year to 2100)
        endYearComboBox = new ComboBox<>();
        for (int year = currentYear; year <= 2100; year++) {
            endYearComboBox.getItems().add(year);
        }
        endYearComboBox.setValue(currentYear + 30);
        endYearComboBox.setPrefWidth(100);

        // Ensure end year >= start year
        startYearComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && endYearComboBox.getValue() < newVal) {
                endYearComboBox.setValue(newVal);
            }
        });

        HBox yearBox = new HBox(10, startYearComboBox, new Label("to"), endYearComboBox);

        // Pre-populate if editing
        if (existingEntry != null) {
            personComboBox.setValue(existingEntry.getPersonName());
            typeComboBox.setValue(existingEntry.getItemType());
            descriptionField.setText(existingEntry.getDescription());
            valueSpinner.getValueFactory().setValue(existingEntry.getValue());
            startYearComboBox.setValue(existingEntry.getStartYear());
            endYearComboBox.setValue(existingEntry.getEndYear());
        } else {
            typeComboBox.setValue(ItemType.INCOME);
        }

        // Layout
        grid.add(new Label("Person:"), 0, 0);
        grid.add(personComboBox, 1, 0);

        grid.add(new Label("Type:"), 0, 1);
        VBox typeBox = new VBox(5, typeComboBox, typeDescriptionLabel);
        grid.add(typeBox, 1, 1);

        grid.add(new Label("Description:"), 0, 2);
        grid.add(descriptionField, 1, 2);

        grid.add(new Label("Annual Value ($):"), 0, 3);
        grid.add(valueSpinner, 1, 3);

        grid.add(new Label("Year Range:"), 0, 4);
        grid.add(yearBox, 1, 4);

        // Add note for mortgage
        Label mortgageNote = new Label("Note: For Mortgage, enter outstanding balance as value and annual payment as description.");
        mortgageNote.setStyle("-fx-text-fill: #f57c00; -fx-font-size: 11px;");
        mortgageNote.setWrapText(true);
        mortgageNote.setMaxWidth(350);
        mortgageNote.setVisible(false);
        mortgageNote.setManaged(false);

        typeComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            boolean isMortgage = newVal == ItemType.MORTGAGE;
            mortgageNote.setVisible(isMortgage);
            mortgageNote.setManaged(isMortgage);
        });

        grid.add(mortgageNote, 0, 5, 2, 1);

        getDialogPane().setContent(grid);
        getDialogPane().setPrefWidth(450);

        // Enable/Disable save button
        javafx.scene.Node saveButton = getDialogPane().lookupButton(saveButtonType);
        saveButton.setDisable(false);

        // Request focus on the person field
        javafx.application.Platform.runLater(personComboBox::requestFocus);

        // Convert the result to ObservableFinancialEntry when save is clicked
        setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                String personName = personComboBox.getValue() != null ? personComboBox.getValue() : "";
                ItemType type = typeComboBox.getValue();
                String description = descriptionField.getText() != null ? descriptionField.getText().trim() : "";
                int value = valueSpinner.getValue();
                int startYear = startYearComboBox.getValue();
                int endYear = endYearComboBox.getValue();

                return new ObservableFinancialEntry(personName, type, description, value, startYear, endYear);
            }
            return null;
        });
    }

    private String getDisplayName(ItemType type) {
        return switch (type) {
            case INCOME -> "Income";
            case EXPENSE -> "Expense";
            case NON_QUALIFIED -> "Non-Qualified Assets";
            case QUALIFIED -> "Qualified Assets (401K/IRA)";
            case ROTH -> "Roth IRA";
            case CASH -> "Cash";
            case LIFE_INSURANCE_BENEFIT -> "Life Insurance Benefit";
            case REAL_ESTATE -> "Real Estate";
            case SOCIAL_SECURITY_BENEFITS -> "Social Security Benefits";
            case ROTH_CONTRIBUTION -> "Roth Contribution";
            case QUALIFIED_CONTRIBUTION -> "Qualified Contribution";
            case LIFE_INSURANCE_CONTRIBUTION -> "Life Insurance Premium";
            case MORTGAGE -> "Mortgage";
            case MORTGAGE_REPAYMENT -> "Mortgage Extra Payment";
        };
    }

    private String getTypeDescription(ItemType type) {
        return switch (type) {
            case INCOME -> "Regular income like salary, wages, or business income";
            case EXPENSE -> "Regular expenses like rent, utilities, groceries";
            case NON_QUALIFIED -> "Taxable investment accounts (brokerage accounts)";
            case QUALIFIED -> "Tax-deferred retirement accounts (401K, Traditional IRA)";
            case ROTH -> "Tax-free retirement accounts (Roth IRA, Roth 401K)";
            case CASH -> "Cash, savings accounts, money market";
            case LIFE_INSURANCE_BENEFIT -> "Death benefit from life insurance policies";
            case REAL_ESTATE -> "Property value (home, investment properties)";
            case SOCIAL_SECURITY_BENEFITS -> "Social Security retirement benefits";
            case ROTH_CONTRIBUTION -> "Annual contribution to Roth accounts";
            case QUALIFIED_CONTRIBUTION -> "Annual contribution to 401K/IRA";
            case LIFE_INSURANCE_CONTRIBUTION -> "Annual life insurance premium payments";
            case MORTGAGE -> "Mortgage loan (value = balance, description = annual payment)";
            case MORTGAGE_REPAYMENT -> "Extra principal payments on mortgage";
        };
    }
}
