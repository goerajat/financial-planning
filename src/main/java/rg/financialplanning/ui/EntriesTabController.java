package rg.financialplanning.ui;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import rg.financialplanning.model.ItemType;
import rg.financialplanning.ui.dialog.EntryEditDialog;
import rg.financialplanning.ui.model.ObservableFinancialEntry;
import rg.financialplanning.ui.model.ObservablePerson;

import java.text.NumberFormat;
import java.util.Optional;

/**
 * Controller for the Financial Entries tab.
 * Allows adding, editing, and deleting financial entries with filtering.
 */
public class EntriesTabController {

    private final VBox root;
    private final TableView<ObservableFinancialEntry> tableView;
    private final ObservableList<ObservableFinancialEntry> entries;
    private final ObservableList<ObservablePerson> persons;
    private final FilteredList<ObservableFinancialEntry> filteredEntries;
    private ComboBox<String> filterComboBox;

    private static final NumberFormat CURRENCY_FORMAT = NumberFormat.getCurrencyInstance();

    public EntriesTabController(ObservableList<ObservableFinancialEntry> entries,
                                 ObservableList<ObservablePerson> persons) {
        this.entries = entries;
        this.persons = persons;
        this.filteredEntries = new FilteredList<>(entries, p -> true);
        this.root = new VBox(10);
        this.tableView = new TableView<>();

        initializeUI();
    }

    private void initializeUI() {
        root.setPadding(new Insets(15));

        // Header
        Label headerLabel = new Label("Financial Entries");
        headerLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        Label descriptionLabel = new Label("Add income, expenses, assets, and other financial items.");
        descriptionLabel.setStyle("-fx-text-fill: #666;");

        // Filter bar
        HBox filterBar = createFilterBar();

        // Table
        setupTable();
        tableView.setItems(filteredEntries);
        tableView.setPlaceholder(new Label("No entries added. Click 'Add Entry' to get started."));
        VBox.setVgrow(tableView, Priority.ALWAYS);

        // Buttons
        HBox buttonBar = createButtonBar();

        root.getChildren().addAll(headerLabel, descriptionLabel, filterBar, tableView, buttonBar);
    }

    private HBox createFilterBar() {
        HBox filterBar = new HBox(10);
        filterBar.setAlignment(Pos.CENTER_LEFT);
        filterBar.setPadding(new Insets(5, 0, 5, 0));

        Label filterLabel = new Label("Filter by Type:");

        filterComboBox = new ComboBox<>();
        filterComboBox.getItems().add("All");
        for (ItemType type : ItemType.values()) {
            filterComboBox.getItems().add(getDisplayName(type));
        }
        filterComboBox.setValue("All");
        filterComboBox.setOnAction(e -> applyFilter());

        filterBar.getChildren().addAll(filterLabel, filterComboBox);
        return filterBar;
    }

    private void applyFilter() {
        String selected = filterComboBox.getValue();
        if ("All".equals(selected)) {
            filteredEntries.setPredicate(p -> true);
        } else {
            filteredEntries.setPredicate(entry ->
                getDisplayName(entry.getItemType()).equals(selected));
        }
    }

    private void setupTable() {
        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        // Person Name column
        TableColumn<ObservableFinancialEntry, String> personColumn = new TableColumn<>("Person");
        personColumn.setCellValueFactory(cellData -> cellData.getValue().personNameProperty());
        personColumn.setPrefWidth(100);

        // Item Type column
        TableColumn<ObservableFinancialEntry, String> typeColumn = new TableColumn<>("Type");
        typeColumn.setCellValueFactory(cellData ->
            new SimpleStringProperty(getDisplayName(cellData.getValue().getItemType())));
        typeColumn.setPrefWidth(150);

        // Description column
        TableColumn<ObservableFinancialEntry, String> descColumn = new TableColumn<>("Description");
        descColumn.setCellValueFactory(cellData -> cellData.getValue().descriptionProperty());
        descColumn.setPrefWidth(200);

        // Value column
        TableColumn<ObservableFinancialEntry, String> valueColumn = new TableColumn<>("Annual Value");
        valueColumn.setCellValueFactory(cellData ->
            new SimpleStringProperty(CURRENCY_FORMAT.format(cellData.getValue().getValue())));
        valueColumn.setStyle("-fx-alignment: CENTER-RIGHT;");
        valueColumn.setPrefWidth(120);

        // Start Year column
        TableColumn<ObservableFinancialEntry, Integer> startColumn = new TableColumn<>("Start Year");
        startColumn.setCellValueFactory(cellData -> cellData.getValue().startYearProperty().asObject());
        startColumn.setStyle("-fx-alignment: CENTER;");
        startColumn.setPrefWidth(80);

        // End Year column
        TableColumn<ObservableFinancialEntry, Integer> endColumn = new TableColumn<>("End Year");
        endColumn.setCellValueFactory(cellData -> cellData.getValue().endYearProperty().asObject());
        endColumn.setStyle("-fx-alignment: CENTER;");
        endColumn.setPrefWidth(80);

        tableView.getColumns().add(personColumn);
        tableView.getColumns().add(typeColumn);
        tableView.getColumns().add(descColumn);
        tableView.getColumns().add(valueColumn);
        tableView.getColumns().add(startColumn);
        tableView.getColumns().add(endColumn);
    }

    private HBox createButtonBar() {
        HBox buttonBar = new HBox(10);
        buttonBar.setAlignment(Pos.CENTER_LEFT);
        buttonBar.setPadding(new Insets(10, 0, 0, 0));

        Button addButton = new Button("Add Entry");
        addButton.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white;");
        addButton.setOnAction(e -> addEntry());

        Button editButton = new Button("Edit");
        editButton.setOnAction(e -> editSelectedEntry());
        editButton.disableProperty().bind(tableView.getSelectionModel().selectedItemProperty().isNull());

        Button duplicateButton = new Button("Duplicate");
        duplicateButton.setOnAction(e -> duplicateSelectedEntry());
        duplicateButton.disableProperty().bind(tableView.getSelectionModel().selectedItemProperty().isNull());

        Button deleteButton = new Button("Delete");
        deleteButton.setStyle("-fx-background-color: #f44336; -fx-text-fill: white;");
        deleteButton.setOnAction(e -> deleteSelectedEntry());
        deleteButton.disableProperty().bind(tableView.getSelectionModel().selectedItemProperty().isNull());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label countLabel = new Label();
        countLabel.textProperty().bind(
            javafx.beans.binding.Bindings.createStringBinding(
                () -> filteredEntries.size() + " of " + entries.size() + " entries",
                filteredEntries, entries
            )
        );
        countLabel.setStyle("-fx-text-fill: #666;");

        buttonBar.getChildren().addAll(addButton, editButton, duplicateButton, deleteButton, spacer, countLabel);
        return buttonBar;
    }

    private void addEntry() {
        EntryEditDialog dialog = new EntryEditDialog(null, persons);
        Optional<ObservableFinancialEntry> result = dialog.showAndWait();
        result.ifPresent(entries::add);
    }

    private void editSelectedEntry() {
        ObservableFinancialEntry selected = tableView.getSelectionModel().getSelectedItem();
        if (selected != null) {
            EntryEditDialog dialog = new EntryEditDialog(selected, persons);
            Optional<ObservableFinancialEntry> result = dialog.showAndWait();
            result.ifPresent(updated -> {
                selected.setPersonName(updated.getPersonName());
                selected.setItemType(updated.getItemType());
                selected.setDescription(updated.getDescription());
                selected.setValue(updated.getValue());
                selected.setStartYear(updated.getStartYear());
                selected.setEndYear(updated.getEndYear());
                tableView.refresh();
            });
        }
    }

    private void duplicateSelectedEntry() {
        ObservableFinancialEntry selected = tableView.getSelectionModel().getSelectedItem();
        if (selected != null) {
            ObservableFinancialEntry duplicate = new ObservableFinancialEntry(
                selected.getPersonName(),
                selected.getItemType(),
                selected.getDescription() + " (Copy)",
                selected.getValue(),
                selected.getStartYear(),
                selected.getEndYear()
            );
            entries.add(duplicate);
        }
    }

    private void deleteSelectedEntry() {
        ObservableFinancialEntry selected = tableView.getSelectionModel().getSelectedItem();
        if (selected != null) {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Delete Entry");
            confirm.setHeaderText(null);
            confirm.setContentText("Are you sure you want to delete this entry?");
            Optional<ButtonType> result = confirm.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                entries.remove(selected);
            }
        }
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

    public VBox getRoot() {
        return root;
    }
}
