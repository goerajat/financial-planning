package rg.financialplanning.ui;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.*;
import javafx.util.converter.DoubleStringConverter;
import rg.financialplanning.ui.model.ObservableItemTypeRate;

/**
 * Controller for the Rates tab.
 * Allows configuring percentage rates for each item type.
 */
public class RatesTabController {

    private final VBox root;
    private final TableView<ObservableItemTypeRate> tableView;
    private final ObservableList<ObservableItemTypeRate> rates;

    public RatesTabController(ObservableList<ObservableItemTypeRate> rates) {
        this.rates = rates;
        this.root = new VBox(10);
        this.tableView = new TableView<>();

        initializeUI();
    }

    private void initializeUI() {
        root.setPadding(new Insets(15));

        // Header
        Label headerLabel = new Label("Annual Growth/Interest Rates");
        headerLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        Label descriptionLabel = new Label("Configure annual percentage rates for each item type. " +
                "These rates are used to project future values.");
        descriptionLabel.setStyle("-fx-text-fill: #666;");
        descriptionLabel.setWrapText(true);

        // Table
        setupTable();
        tableView.setItems(rates);
        tableView.setEditable(true);
        VBox.setVgrow(tableView, Priority.ALWAYS);

        // Info panel
        VBox infoPanel = createInfoPanel();

        root.getChildren().addAll(headerLabel, descriptionLabel, tableView, infoPanel);
    }

    private void setupTable() {
        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        // Item Type column (display name)
        TableColumn<ObservableItemTypeRate, String> typeColumn = new TableColumn<>("Item Type");
        typeColumn.setCellValueFactory(cellData ->
            new SimpleStringProperty(cellData.getValue().getDisplayName()));
        typeColumn.setEditable(false);
        typeColumn.setPrefWidth(300);

        // Rate column
        TableColumn<ObservableItemTypeRate, Double> rateColumn = new TableColumn<>("Annual Rate (%)");
        rateColumn.setCellValueFactory(cellData -> cellData.getValue().rateProperty().asObject());
        rateColumn.setCellFactory(col -> new TextFieldTableCell<>(new DoubleStringConverter() {
            @Override
            public Double fromString(String value) {
                try {
                    String cleaned = value.replace("%", "").trim();
                    double rate = Double.parseDouble(cleaned);
                    if (rate >= -50 && rate <= 100) {
                        return rate;
                    }
                } catch (NumberFormatException e) {
                    // Invalid input
                }
                return null;
            }

            @Override
            public String toString(Double value) {
                return value != null ? String.format("%.2f%%", value) : "";
            }
        }));
        rateColumn.setOnEditCommit(event -> {
            Double newValue = event.getNewValue();
            if (newValue != null) {
                event.getRowValue().setRate(newValue);
            }
            tableView.refresh();
        });
        rateColumn.setPrefWidth(150);
        rateColumn.setStyle("-fx-alignment: CENTER-RIGHT;");

        // Description column
        TableColumn<ObservableItemTypeRate, String> descColumn = new TableColumn<>("Description");
        descColumn.setCellValueFactory(cellData ->
            new SimpleStringProperty(getRateDescription(cellData.getValue())));
        descColumn.setEditable(false);
        descColumn.setPrefWidth(300);
        descColumn.setStyle("-fx-text-fill: #666;");

        tableView.getColumns().add(typeColumn);
        tableView.getColumns().add(rateColumn);
        tableView.getColumns().add(descColumn);
    }

    private String getRateDescription(ObservableItemTypeRate rate) {
        return switch (rate.getItemType()) {
            case INCOME -> "Annual salary/income increase rate";
            case EXPENSE -> "Annual expense inflation rate";
            case NON_QUALIFIED -> "Non-qualified investment return rate";
            case QUALIFIED -> "401K/IRA investment return rate";
            case ROTH -> "Roth IRA investment return rate";
            case CASH -> "Cash/savings interest rate";
            case REAL_ESTATE -> "Real estate appreciation rate";
            case LIFE_INSURANCE_BENEFIT -> "Life insurance benefit growth rate";
            case SOCIAL_SECURITY_BENEFITS -> "Social security COLA rate";
            case ROTH_CONTRIBUTION -> "Roth contribution increase rate";
            case QUALIFIED_CONTRIBUTION -> "401K contribution increase rate";
            case LIFE_INSURANCE_CONTRIBUTION -> "Premium increase rate";
            case MORTGAGE -> "Mortgage interest rate";
            case MORTGAGE_REPAYMENT -> "Extra payment increase rate";
        };
    }

    private VBox createInfoPanel() {
        VBox infoPanel = new VBox(5);
        infoPanel.setPadding(new Insets(15));
        infoPanel.setStyle("-fx-background-color: #e3f2fd; -fx-background-radius: 5;");

        Label infoTitle = new Label("Rate Guidelines");
        infoTitle.setStyle("-fx-font-weight: bold;");

        Label infoText = new Label(
            "- Double-click a rate cell to edit\n" +
            "- Income/Expense: Typically 2-4% for inflation\n" +
            "- Investment Returns: Historically 6-8% for stocks, 2-4% for bonds\n" +
            "- Real Estate: Typically 2-4% appreciation\n" +
            "- Mortgage Rate: Your current mortgage interest rate"
        );
        infoText.setStyle("-fx-text-fill: #1565c0;");

        infoPanel.getChildren().addAll(infoTitle, infoText);
        return infoPanel;
    }

    public VBox getRoot() {
        return root;
    }
}
