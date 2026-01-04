package rg.financialplanning.ui;

import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import rg.financialplanning.model.FinancialEntry;
import rg.financialplanning.model.ItemType;
import rg.financialplanning.model.Person;
import rg.financialplanning.model.YearlySummary;
import rg.financialplanning.parser.FinancialDataProcessor;
import rg.financialplanning.ui.model.ObservableFinancialEntry;
import rg.financialplanning.ui.model.ObservableItemTypeRate;
import rg.financialplanning.ui.model.ObservablePerson;

import java.text.NumberFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Controller for the Sensitivity Analysis tab.
 * Allows analyzing how different rate assumptions affect net worth over time.
 */
public class SensitivityTabController {

    private final VBox root;
    private final ObservableList<ObservablePerson> persons;
    private final ObservableList<ObservableFinancialEntry> entries;
    private final ObservableList<ObservableItemTypeRate> rates;

    // Input controls
    private ComboBox<RateTypeOption> rateTypeComboBox;
    private Spinner<Double> minRateSpinner;
    private Spinner<Double> maxRateSpinner;
    private Spinner<Double> rateIncrementSpinner;
    private Spinner<Integer> yearIncrementSpinner;
    private Button runButton;
    private ProgressIndicator progressIndicator;
    private Label statusLabel;

    // Results table
    private TableView<SensitivityRow> resultsTable;
    private Label resultsHeaderLabel;

    // Rate type options
    private static final List<RateTypeOption> RATE_TYPE_OPTIONS = List.of(
        new RateTypeOption("EXPENSE", "Expense Growth", 2.0, 5.0),
        new RateTypeOption("ALL_ASSETS", "All Asset Growth (Qualified + Non-Qualified + Roth)", 4.0, 8.0),
        new RateTypeOption("QUALIFIED", "Qualified Asset Growth", 4.0, 8.0),
        new RateTypeOption("NON_QUALIFIED", "Non-Qualified Asset Growth", 4.0, 8.0),
        new RateTypeOption("ROTH", "Roth Asset Growth", 4.0, 8.0)
    );

    private final NumberFormat currencyFormat;

    public SensitivityTabController(ObservableList<ObservablePerson> persons,
                                     ObservableList<ObservableFinancialEntry> entries,
                                     ObservableList<ObservableItemTypeRate> rates) {
        this.persons = persons;
        this.entries = entries;
        this.rates = rates;
        this.root = new VBox(15);
        this.currencyFormat = NumberFormat.getCurrencyInstance(Locale.US);
        this.currencyFormat.setMaximumFractionDigits(0);

        initializeUI();
    }

    private void initializeUI() {
        root.setPadding(new Insets(15));

        // Header
        Label headerLabel = new Label("Rate Sensitivity Analysis");
        headerLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        Label descriptionLabel = new Label(
            "Analyze how different growth rates affect your net worth over time. " +
            "Select a rate type, set the range, and see projected net worth at milestone years. " +
            "\"Shortfall\" indicates a deficit occurred before that milestone."
        );
        descriptionLabel.setStyle("-fx-text-fill: #666;");
        descriptionLabel.setWrapText(true);

        // Input controls
        GridPane inputGrid = createInputGrid();

        // Results section
        resultsHeaderLabel = new Label("Results");
        resultsHeaderLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
        resultsHeaderLabel.setVisible(false);

        resultsTable = new TableView<>();
        resultsTable.setPlaceholder(new Label("Run analysis to see results"));
        resultsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        VBox.setVgrow(resultsTable, Priority.ALWAYS);

        // Legend
        HBox legend = createLegend();

        root.getChildren().addAll(headerLabel, descriptionLabel, inputGrid,
                                   resultsHeaderLabel, resultsTable, legend);
    }

    private GridPane createInputGrid() {
        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(10);
        grid.setPadding(new Insets(15));
        grid.setStyle("-fx-background-color: #f9f9f9; -fx-background-radius: 8;");

        int col = 0;

        // Rate Type
        Label rateTypeLabel = new Label("Rate Type:");
        rateTypeComboBox = new ComboBox<>(FXCollections.observableArrayList(RATE_TYPE_OPTIONS));
        rateTypeComboBox.setConverter(new javafx.util.StringConverter<>() {
            @Override
            public String toString(RateTypeOption option) {
                return option != null ? option.label : "";
            }

            @Override
            public RateTypeOption fromString(String string) {
                return RATE_TYPE_OPTIONS.stream()
                    .filter(o -> o.label.equals(string))
                    .findFirst()
                    .orElse(null);
            }
        });
        rateTypeComboBox.getSelectionModel().selectFirst();
        rateTypeComboBox.setPrefWidth(280);
        rateTypeComboBox.setOnAction(e -> onRateTypeChanged());
        grid.add(rateTypeLabel, col, 0);
        grid.add(rateTypeComboBox, col++, 1);

        // Min Rate
        Label minRateLabel = new Label("Min Rate (%):");
        minRateSpinner = new Spinner<>(0.0, 20.0, 2.0, 0.25);
        minRateSpinner.setEditable(true);
        minRateSpinner.setPrefWidth(100);
        grid.add(minRateLabel, col, 0);
        grid.add(minRateSpinner, col++, 1);

        // Max Rate
        Label maxRateLabel = new Label("Max Rate (%):");
        maxRateSpinner = new Spinner<>(0.0, 20.0, 5.0, 0.25);
        maxRateSpinner.setEditable(true);
        maxRateSpinner.setPrefWidth(100);
        grid.add(maxRateLabel, col, 0);
        grid.add(maxRateSpinner, col++, 1);

        // Rate Increment
        Label rateIncLabel = new Label("Rate Increment (%):");
        rateIncrementSpinner = new Spinner<>(0.05, 1.0, 0.25, 0.05);
        rateIncrementSpinner.setEditable(true);
        rateIncrementSpinner.setPrefWidth(100);
        grid.add(rateIncLabel, col, 0);
        grid.add(rateIncrementSpinner, col++, 1);

        // Year Increment
        Label yearIncLabel = new Label("Year Increment:");
        yearIncrementSpinner = new Spinner<>(1, 10, 5, 1);
        yearIncrementSpinner.setEditable(true);
        yearIncrementSpinner.setPrefWidth(80);
        grid.add(yearIncLabel, col, 0);
        grid.add(yearIncrementSpinner, col++, 1);

        // Run Button and Progress
        VBox buttonBox = new VBox(5);
        buttonBox.setAlignment(Pos.CENTER);

        runButton = new Button("Run Analysis");
        runButton.setStyle("-fx-background-color: #4a90d9; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 20;");
        runButton.setOnAction(e -> runAnalysis());

        progressIndicator = new ProgressIndicator();
        progressIndicator.setMaxSize(24, 24);
        progressIndicator.setVisible(false);

        statusLabel = new Label("");
        statusLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 11px;");

        buttonBox.getChildren().addAll(runButton, progressIndicator, statusLabel);
        grid.add(buttonBox, col, 0, 1, 2);

        return grid;
    }

    private HBox createLegend() {
        HBox legend = new HBox(20);
        legend.setPadding(new Insets(10));
        legend.setStyle("-fx-background-color: #f9f9f9; -fx-background-radius: 4;");

        HBox positiveItem = new HBox(5);
        positiveItem.setAlignment(Pos.CENTER_LEFT);
        Label positiveColor = new Label("  ");
        positiveColor.setStyle("-fx-background-color: #e8f5e9; -fx-border-color: #2e7d32; -fx-border-radius: 3; -fx-background-radius: 3;");
        positiveColor.setPrefSize(16, 16);
        positiveItem.getChildren().addAll(positiveColor, new Label("Positive Net Worth"));

        HBox shortfallItem = new HBox(5);
        shortfallItem.setAlignment(Pos.CENTER_LEFT);
        Label shortfallColor = new Label("  ");
        shortfallColor.setStyle("-fx-background-color: #ffebee; -fx-border-color: #c62828; -fx-border-radius: 3; -fx-background-radius: 3;");
        shortfallColor.setPrefSize(16, 16);
        shortfallItem.getChildren().addAll(shortfallColor, new Label("Shortfall (Deficit occurred)"));

        legend.getChildren().addAll(positiveItem, shortfallItem);
        return legend;
    }

    private void onRateTypeChanged() {
        RateTypeOption selected = rateTypeComboBox.getValue();
        if (selected != null) {
            minRateSpinner.getValueFactory().setValue(selected.defaultMin);
            maxRateSpinner.getValueFactory().setValue(selected.defaultMax);
        }
    }

    private void runAnalysis() {
        if (entries.isEmpty()) {
            showAlert("No Data", "Please add some financial entries before running the analysis.");
            return;
        }

        double minRate = minRateSpinner.getValue();
        double maxRate = maxRateSpinner.getValue();

        if (minRate >= maxRate) {
            showAlert("Invalid Range", "Minimum rate must be less than maximum rate.");
            return;
        }

        RateTypeOption selectedRateType = rateTypeComboBox.getValue();
        double rateIncrement = rateIncrementSpinner.getValue();
        int yearIncrement = yearIncrementSpinner.getValue();

        runButton.setDisable(true);
        progressIndicator.setVisible(true);
        statusLabel.setText("Analyzing...");

        Service<SensitivityResult> analysisService = new Service<>() {
            @Override
            protected Task<SensitivityResult> createTask() {
                return new Task<>() {
                    @Override
                    protected SensitivityResult call() {
                        return performAnalysis(selectedRateType, minRate, maxRate, rateIncrement, yearIncrement);
                    }
                };
            }
        };

        analysisService.setOnSucceeded(e -> {
            SensitivityResult result = analysisService.getValue();
            displayResults(result);
            runButton.setDisable(false);
            progressIndicator.setVisible(false);
            statusLabel.setText("Analysis complete");
        });

        analysisService.setOnFailed(e -> {
            runButton.setDisable(false);
            progressIndicator.setVisible(false);
            statusLabel.setText("Error");
            Throwable ex = analysisService.getException();
            showAlert("Analysis Failed", "Failed to run sensitivity analysis:\n" + ex.getMessage());
            ex.printStackTrace();
        });

        analysisService.start();
    }

    private SensitivityResult performAnalysis(RateTypeOption rateTypeOption, double minRate, double maxRate,
                                               double rateIncrement, int yearIncrement) {
        // Convert observable data to domain models
        List<FinancialEntry> financialEntries = entries.stream()
                .map(ObservableFinancialEntry::toFinancialEntry)
                .collect(Collectors.toList());

        List<Person> personList = persons.stream()
                .map(ObservablePerson::toPerson)
                .collect(Collectors.toList());

        Map<String, Person> personsByName = personList.stream()
                .collect(Collectors.toMap(Person::name, p -> p, (a, b) -> a));

        Map<ItemType, Double> baseRates = rates.stream()
                .collect(Collectors.toMap(
                        ObservableItemTypeRate::getItemType,
                        ObservableItemTypeRate::getRate
                ));

        // Determine which ItemType(s) to vary
        List<ItemType> targetItemTypes = switch (rateTypeOption.value) {
            case "ALL_ASSETS" -> List.of(ItemType.QUALIFIED, ItemType.NON_QUALIFIED, ItemType.ROTH);
            case "QUALIFIED" -> List.of(ItemType.QUALIFIED);
            case "NON_QUALIFIED" -> List.of(ItemType.NON_QUALIFIED);
            case "ROTH" -> List.of(ItemType.ROTH);
            default -> List.of(ItemType.EXPENSE);
        };

        // Setup processor
        FinancialDataProcessor processor = new FinancialDataProcessor();
        processor.setEntries(financialEntries);

        int firstDataYear = processor.getEarliestStartYear();
        int lastDataYear = processor.getLatestEndYear();

        // Calculate milestone years
        List<Integer> milestoneYears = new ArrayList<>();
        for (int year = firstDataYear + yearIncrement; year <= lastDataYear; year += yearIncrement) {
            milestoneYears.add(year);
        }
        if (milestoneYears.isEmpty() || milestoneYears.get(milestoneYears.size() - 1) != lastDataYear) {
            milestoneYears.add(lastDataYear);
        }

        // Generate rows for each rate value
        List<SensitivityRow> rows = new ArrayList<>();
        for (double rateValue = minRate; rateValue <= maxRate + 0.001; rateValue += rateIncrement) {
            double roundedRate = Math.round(rateValue * 100.0) / 100.0;

            // Clone rates and override target rate type(s)
            Map<ItemType, Double> testRates = new HashMap<>(baseRates);
            for (ItemType targetType : targetItemTypes) {
                testRates.put(targetType, roundedRate);
            }

            // Generate summaries
            processor.setEntries(financialEntries);
            YearlySummary[] summaries = processor.generateYearlySummaries(testRates, personsByName);

            // Find first shortfall year
            Integer firstShortfallYear = null;
            for (YearlySummary summary : summaries) {
                if (summary.deficit() > 0) {
                    firstShortfallYear = summary.year();
                    break;
                }
            }

            // Build cells for each milestone
            List<SensitivityCell> cells = new ArrayList<>();
            for (int milestoneYear : milestoneYears) {
                int index = milestoneYear - firstDataYear;
                if (index >= 0 && index < summaries.length) {
                    YearlySummary summary = summaries[index];
                    boolean hasShortfall = firstShortfallYear != null && firstShortfallYear <= milestoneYear;

                    if (hasShortfall) {
                        cells.add(new SensitivityCell(milestoneYear, null, true, firstShortfallYear));
                    } else {
                        cells.add(new SensitivityCell(milestoneYear, summary.netWorth(), false, null));
                    }
                }
            }

            rows.add(new SensitivityRow(roundedRate, cells));
        }

        return new SensitivityResult(rateTypeOption.label, firstDataYear, milestoneYears, rows);
    }

    private void displayResults(SensitivityResult result) {
        resultsHeaderLabel.setText(result.rateTypeLabel + " Analysis (First Data Year: " + result.firstDataYear + ")");
        resultsHeaderLabel.setVisible(true);

        // Clear existing columns
        resultsTable.getColumns().clear();

        // Rate column
        TableColumn<SensitivityRow, String> rateColumn = new TableColumn<>("Rate");
        rateColumn.setCellValueFactory(cellData ->
            new SimpleStringProperty(String.format("%.2f%%", cellData.getValue().rate)));
        rateColumn.setPrefWidth(80);
        rateColumn.setStyle("-fx-alignment: CENTER-LEFT; -fx-font-weight: bold;");
        resultsTable.getColumns().add(rateColumn);

        // Year columns
        for (int i = 0; i < result.milestoneYears.size(); i++) {
            final int index = i;
            int year = result.milestoneYears.get(i);
            int yearsFromStart = year - result.firstDataYear;

            TableColumn<SensitivityRow, String> yearColumn = new TableColumn<>(year + "\n(+" + yearsFromStart + " yrs)");
            yearColumn.setCellValueFactory(cellData -> {
                SensitivityCell cell = cellData.getValue().cells.get(index);
                if (cell.hasShortfall) {
                    return new SimpleStringProperty("Shortfall (" + cell.shortfallYear + ")");
                } else {
                    return new SimpleStringProperty(currencyFormat.format(cell.netWorth));
                }
            });
            yearColumn.setCellFactory(col -> new TableCell<>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                        setStyle("");
                    } else {
                        setText(item);
                        if (item.startsWith("Shortfall")) {
                            setStyle("-fx-background-color: #ffebee; -fx-text-fill: #c62828; -fx-font-weight: bold; -fx-alignment: CENTER-RIGHT;");
                        } else {
                            setStyle("-fx-background-color: #e8f5e9; -fx-text-fill: #2e7d32; -fx-alignment: CENTER-RIGHT;");
                        }
                    }
                }
            });
            yearColumn.setPrefWidth(120);
            resultsTable.getColumns().add(yearColumn);
        }

        resultsTable.setItems(FXCollections.observableArrayList(result.rows));
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

    // Inner classes for data structures
    private static class RateTypeOption {
        final String value;
        final String label;
        final double defaultMin;
        final double defaultMax;

        RateTypeOption(String value, String label, double defaultMin, double defaultMax) {
            this.value = value;
            this.label = label;
            this.defaultMin = defaultMin;
            this.defaultMax = defaultMax;
        }
    }

    private static class SensitivityCell {
        final int year;
        final Double netWorth;
        final boolean hasShortfall;
        final Integer shortfallYear;

        SensitivityCell(int year, Double netWorth, boolean hasShortfall, Integer shortfallYear) {
            this.year = year;
            this.netWorth = netWorth;
            this.hasShortfall = hasShortfall;
            this.shortfallYear = shortfallYear;
        }
    }

    private static class SensitivityRow {
        final double rate;
        final List<SensitivityCell> cells;

        SensitivityRow(double rate, List<SensitivityCell> cells) {
            this.rate = rate;
            this.cells = cells;
        }
    }

    private static class SensitivityResult {
        final String rateTypeLabel;
        final int firstDataYear;
        final List<Integer> milestoneYears;
        final List<SensitivityRow> rows;

        SensitivityResult(String rateTypeLabel, int firstDataYear, List<Integer> milestoneYears, List<SensitivityRow> rows) {
            this.rateTypeLabel = rateTypeLabel;
            this.firstDataYear = firstDataYear;
            this.milestoneYears = milestoneYears;
            this.rows = rows;
        }
    }
}
