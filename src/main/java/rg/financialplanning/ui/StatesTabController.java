package rg.financialplanning.ui;

import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Callback;
import rg.financialplanning.model.State;
import rg.financialplanning.ui.model.ObservableStateByYear;

import java.time.Year;

/**
 * Controller for the States tab, managing state tax configurations by year.
 */
public class StatesTabController {

    private final VBox root;
    private final ObservableList<ObservableStateByYear> statesByYear;

    // Form controls
    private ComboBox<State> stateComboBox;
    private Spinner<Integer> startYearSpinner;
    private Spinner<Integer> endYearSpinner;
    private Button addButton;
    private Button updateButton;
    private Button cancelButton;

    // Table
    private TableView<ObservableStateByYear> table;
    private ObservableStateByYear editingItem;

    public StatesTabController(ObservableList<ObservableStateByYear> statesByYear) {
        this.statesByYear = statesByYear;
        this.root = new VBox(15);
        this.root.setPadding(new Insets(20));

        initializeUI();
    }

    private void initializeUI() {
        // Title
        Label titleLabel = new Label("State Tax Configuration");
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 18));

        // Description
        Label descLabel = new Label(
                "Configure which state's tax laws apply for different years. This is useful if you plan to move " +
                "to a different state during retirement. If no configuration is provided, New Jersey tax rates will be used by default."
        );
        descLabel.setWrapText(true);
        descLabel.setStyle("-fx-text-fill: #666;");

        // Form
        GridPane form = createForm();

        // Table
        table = createTable();

        // Info panel
        VBox infoPanel = createInfoPanel();

        root.getChildren().addAll(titleLabel, descLabel, form, table, infoPanel);
        VBox.setVgrow(table, Priority.ALWAYS);
    }

    private GridPane createForm() {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(10));
        grid.setStyle("-fx-background-color: #f5f5f5; -fx-background-radius: 5;");

        // State dropdown
        Label stateLabel = new Label("State:");
        stateComboBox = new ComboBox<>();
        stateComboBox.getItems().addAll(State.values());
        stateComboBox.setValue(State.NJ);
        stateComboBox.setPrefWidth(200);

        // Start year spinner
        Label startYearLabel = new Label("Start Year:");
        int currentYear = Year.now().getValue();
        startYearSpinner = new Spinner<>(1900, 2100, currentYear);
        startYearSpinner.setEditable(true);
        startYearSpinner.setPrefWidth(120);

        // End year spinner
        Label endYearLabel = new Label("End Year:");
        endYearSpinner = new Spinner<>(1900, 2100, currentYear + 10);
        endYearSpinner.setEditable(true);
        endYearSpinner.setPrefWidth(120);

        // Buttons
        HBox buttonBox = new HBox(10);
        addButton = new Button("Add State");
        addButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;");
        addButton.setOnAction(e -> handleAdd());

        updateButton = new Button("Update");
        updateButton.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white;");
        updateButton.setOnAction(e -> handleUpdate());
        updateButton.setVisible(false);

        cancelButton = new Button("Cancel");
        cancelButton.setOnAction(e -> handleCancel());
        cancelButton.setVisible(false);

        buttonBox.getChildren().addAll(addButton, updateButton, cancelButton);
        buttonBox.setAlignment(Pos.CENTER_LEFT);

        // Add to grid
        grid.add(stateLabel, 0, 0);
        grid.add(stateComboBox, 1, 0);
        grid.add(startYearLabel, 2, 0);
        grid.add(startYearSpinner, 3, 0);
        grid.add(endYearLabel, 4, 0);
        grid.add(endYearSpinner, 5, 0);
        grid.add(buttonBox, 6, 0);

        return grid;
    }

    private TableView<ObservableStateByYear> createTable() {
        TableView<ObservableStateByYear> tableView = new TableView<>(statesByYear);
        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tableView.setPlaceholder(new Label("No state configurations. New Jersey will be used as default."));

        // State column
        TableColumn<ObservableStateByYear, State> stateCol = new TableColumn<>("State");
        stateCol.setCellValueFactory(new PropertyValueFactory<>("state"));
        stateCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(State state, boolean empty) {
                super.updateItem(state, empty);
                if (empty || state == null) {
                    setText(null);
                } else {
                    setText(state.getDisplayName());
                }
            }
        });
        stateCol.setPrefWidth(150);

        // Start year column
        TableColumn<ObservableStateByYear, Integer> startYearCol = new TableColumn<>("Start Year");
        startYearCol.setCellValueFactory(new PropertyValueFactory<>("startYear"));
        startYearCol.setPrefWidth(100);

        // End year column
        TableColumn<ObservableStateByYear, Integer> endYearCol = new TableColumn<>("End Year");
        endYearCol.setCellValueFactory(new PropertyValueFactory<>("endYear"));
        endYearCol.setPrefWidth(100);

        // Duration column (computed)
        TableColumn<ObservableStateByYear, String> durationCol = new TableColumn<>("Duration");
        durationCol.setCellValueFactory(cellData -> {
            int years = cellData.getValue().getDuration();
            return new javafx.beans.property.SimpleStringProperty(years + " year" + (years != 1 ? "s" : ""));
        });
        durationCol.setPrefWidth(100);

        // Actions column
        TableColumn<ObservableStateByYear, Void> actionsCol = new TableColumn<>("Actions");
        actionsCol.setCellFactory(createActionsColumn());
        actionsCol.setPrefWidth(150);

        tableView.getColumns().addAll(stateCol, startYearCol, endYearCol, durationCol, actionsCol);

        return tableView;
    }

    private Callback<TableColumn<ObservableStateByYear, Void>, TableCell<ObservableStateByYear, Void>> createActionsColumn() {
        return new Callback<>() {
            @Override
            public TableCell<ObservableStateByYear, Void> call(TableColumn<ObservableStateByYear, Void> param) {
                return new TableCell<>() {
                    private final Button editBtn = new Button("Edit");
                    private final Button deleteBtn = new Button("Delete");
                    private final HBox pane = new HBox(5, editBtn, deleteBtn);

                    {
                        editBtn.setStyle("-fx-font-size: 11px;");
                        deleteBtn.setStyle("-fx-font-size: 11px; -fx-background-color: #f44336; -fx-text-fill: white;");
                        pane.setAlignment(Pos.CENTER);

                        editBtn.setOnAction(event -> {
                            ObservableStateByYear item = getTableView().getItems().get(getIndex());
                            handleEdit(item);
                        });

                        deleteBtn.setOnAction(event -> {
                            ObservableStateByYear item = getTableView().getItems().get(getIndex());
                            handleDelete(item);
                        });
                    }

                    @Override
                    protected void updateItem(Void item, boolean empty) {
                        super.updateItem(item, empty);
                        setGraphic(empty ? null : pane);
                    }
                };
            }
        };
    }

    private VBox createInfoPanel() {
        VBox panel = new VBox(10);
        panel.setPadding(new Insets(15));
        panel.setStyle("-fx-background-color: #e3f2fd; -fx-background-radius: 5; -fx-border-color: #2196F3; -fx-border-radius: 5;");

        Label infoTitle = new Label("Tax Rate Information");
        infoTitle.setFont(Font.font("System", FontWeight.BOLD, 14));

        Label njInfo = new Label("• New Jersey: Progressive state income tax (1.4% - 10.75%). Social Security benefits are not taxable.");
        Label nyInfo = new Label("• New York: Progressive state income tax (4% - 10.9%). Social Security benefits are not taxable.");
        Label flInfo = new Label("• Florida: No state income tax (0%).");

        njInfo.setWrapText(true);
        nyInfo.setWrapText(true);
        flInfo.setWrapText(true);

        Label noteLabel = new Label("Note: If year ranges overlap, the first matching configuration will be used. " +
                "Ensure your configurations don't overlap for predictable results.");
        noteLabel.setWrapText(true);
        noteLabel.setStyle("-fx-font-style: italic; -fx-text-fill: #555;");

        panel.getChildren().addAll(infoTitle, njInfo, nyInfo, flInfo, noteLabel);

        // Only show if there are states configured
        panel.visibleProperty().bind(javafx.beans.binding.Bindings.isNotEmpty(statesByYear));
        panel.managedProperty().bind(panel.visibleProperty());

        return panel;
    }

    private void handleAdd() {
        State state = stateComboBox.getValue();
        int startYear = startYearSpinner.getValue();
        int endYear = endYearSpinner.getValue();

        if (startYear > endYear) {
            showAlert(Alert.AlertType.WARNING, "Invalid Years", "Start year must be before or equal to end year.");
            return;
        }

        ObservableStateByYear newState = new ObservableStateByYear(state, startYear, endYear);
        statesByYear.add(newState);

        // Reset form
        resetForm();
    }

    private void handleEdit(ObservableStateByYear item) {
        editingItem = item;

        // Populate form
        stateComboBox.setValue(item.getState());
        startYearSpinner.getValueFactory().setValue(item.getStartYear());
        endYearSpinner.getValueFactory().setValue(item.getEndYear());

        // Show update/cancel buttons
        addButton.setVisible(false);
        updateButton.setVisible(true);
        cancelButton.setVisible(true);
    }

    private void handleUpdate() {
        if (editingItem == null) return;

        State state = stateComboBox.getValue();
        int startYear = startYearSpinner.getValue();
        int endYear = endYearSpinner.getValue();

        if (startYear > endYear) {
            showAlert(Alert.AlertType.WARNING, "Invalid Years", "Start year must be before or equal to end year.");
            return;
        }

        editingItem.setState(state);
        editingItem.setStartYear(startYear);
        editingItem.setEndYear(endYear);

        table.refresh();
        resetForm();
    }

    private void handleDelete(ObservableStateByYear item) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Delete");
        confirm.setHeaderText(null);
        confirm.setContentText("Are you sure you want to delete this state configuration?");

        if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            statesByYear.remove(item);
            if (editingItem == item) {
                resetForm();
            }
        }
    }

    private void handleCancel() {
        resetForm();
    }

    private void resetForm() {
        editingItem = null;
        stateComboBox.setValue(State.NJ);
        startYearSpinner.getValueFactory().setValue(Year.now().getValue());
        endYearSpinner.getValueFactory().setValue(Year.now().getValue() + 10);

        addButton.setVisible(true);
        updateButton.setVisible(false);
        cancelButton.setVisible(false);
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public VBox getRoot() {
        return root;
    }
}
