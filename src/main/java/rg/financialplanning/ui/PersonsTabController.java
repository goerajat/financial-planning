package rg.financialplanning.ui;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.*;
import javafx.util.converter.IntegerStringConverter;
import rg.financialplanning.ui.dialog.PersonEditDialog;
import rg.financialplanning.ui.model.ObservablePerson;

import java.time.Year;
import java.util.Optional;

/**
 * Controller for the Persons tab.
 * Allows adding, editing, and deleting persons.
 */
public class PersonsTabController {

    private final VBox root;
    private final TableView<ObservablePerson> tableView;
    private final ObservableList<ObservablePerson> persons;

    public PersonsTabController(ObservableList<ObservablePerson> persons) {
        this.persons = persons;
        this.root = new VBox(10);
        this.tableView = new TableView<>();

        initializeUI();
    }

    private void initializeUI() {
        root.setPadding(new Insets(15));

        // Header
        Label headerLabel = new Label("Persons");
        headerLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        Label descriptionLabel = new Label("Add family members or individuals to track in the financial plan.");
        descriptionLabel.setStyle("-fx-text-fill: #666;");

        // Table
        setupTable();
        tableView.setItems(persons);
        tableView.setPlaceholder(new Label("No persons added. Click 'Add Person' to get started."));
        VBox.setVgrow(tableView, Priority.ALWAYS);

        // Buttons
        HBox buttonBar = createButtonBar();

        root.getChildren().addAll(headerLabel, descriptionLabel, tableView, buttonBar);
    }

    private void setupTable() {
        tableView.setEditable(true);
        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        // Name column
        TableColumn<ObservablePerson, String> nameColumn = new TableColumn<>("Name");
        nameColumn.setCellValueFactory(cellData -> cellData.getValue().nameProperty());
        nameColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        nameColumn.setOnEditCommit(event -> {
            String newValue = event.getNewValue();
            if (newValue != null && !newValue.isBlank()) {
                event.getRowValue().setName(newValue);
            } else {
                tableView.refresh();
            }
        });
        nameColumn.setPrefWidth(200);

        // Year of Birth column
        TableColumn<ObservablePerson, Integer> yearColumn = new TableColumn<>("Year of Birth");
        yearColumn.setCellValueFactory(cellData -> cellData.getValue().yearOfBirthProperty().asObject());
        yearColumn.setCellFactory(TextFieldTableCell.forTableColumn(new IntegerStringConverter() {
            @Override
            public Integer fromString(String value) {
                try {
                    int year = Integer.parseInt(value);
                    if (year >= 1900 && year <= 2100) {
                        return year;
                    }
                } catch (NumberFormatException e) {
                    // Invalid input
                }
                return null;
            }
        }));
        yearColumn.setOnEditCommit(event -> {
            Integer newValue = event.getNewValue();
            if (newValue != null) {
                event.getRowValue().setYearOfBirth(newValue);
            }
            tableView.refresh();
        });
        yearColumn.setPrefWidth(120);

        // Current Age column (computed)
        TableColumn<ObservablePerson, Integer> ageColumn = new TableColumn<>("Current Age");
        ageColumn.setCellValueFactory(cellData ->
            new ReadOnlyObjectWrapper<>(cellData.getValue().getCurrentAge()));
        ageColumn.setEditable(false);
        ageColumn.setPrefWidth(100);
        ageColumn.setStyle("-fx-alignment: CENTER;");

        tableView.getColumns().add(nameColumn);
        tableView.getColumns().add(yearColumn);
        tableView.getColumns().add(ageColumn);
    }

    private HBox createButtonBar() {
        HBox buttonBar = new HBox(10);
        buttonBar.setAlignment(Pos.CENTER_LEFT);
        buttonBar.setPadding(new Insets(10, 0, 0, 0));

        Button addButton = new Button("Add Person");
        addButton.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white;");
        addButton.setOnAction(e -> addPerson());

        Button editButton = new Button("Edit");
        editButton.setOnAction(e -> editSelectedPerson());
        editButton.disableProperty().bind(tableView.getSelectionModel().selectedItemProperty().isNull());

        Button deleteButton = new Button("Delete");
        deleteButton.setStyle("-fx-background-color: #f44336; -fx-text-fill: white;");
        deleteButton.setOnAction(e -> deleteSelectedPerson());
        deleteButton.disableProperty().bind(tableView.getSelectionModel().selectedItemProperty().isNull());

        buttonBar.getChildren().addAll(addButton, editButton, deleteButton);
        return buttonBar;
    }

    private void addPerson() {
        PersonEditDialog dialog = new PersonEditDialog(null);
        Optional<ObservablePerson> result = dialog.showAndWait();
        result.ifPresent(person -> {
            // Check for duplicate names
            boolean nameExists = persons.stream()
                    .anyMatch(p -> p.getName().equalsIgnoreCase(person.getName()));
            if (nameExists) {
                showAlert("Duplicate Name", "A person with this name already exists.");
            } else {
                persons.add(person);
            }
        });
    }

    private void editSelectedPerson() {
        ObservablePerson selected = tableView.getSelectionModel().getSelectedItem();
        if (selected != null) {
            PersonEditDialog dialog = new PersonEditDialog(selected);
            Optional<ObservablePerson> result = dialog.showAndWait();
            result.ifPresent(updated -> {
                selected.setName(updated.getName());
                selected.setYearOfBirth(updated.getYearOfBirth());
                tableView.refresh();
            });
        }
    }

    private void deleteSelectedPerson() {
        ObservablePerson selected = tableView.getSelectionModel().getSelectedItem();
        if (selected != null) {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Delete Person");
            confirm.setHeaderText(null);
            confirm.setContentText("Are you sure you want to delete '" + selected.getName() + "'?");
            Optional<ButtonType> result = confirm.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                persons.remove(selected);
            }
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public VBox getRoot() {
        return root;
    }
}
