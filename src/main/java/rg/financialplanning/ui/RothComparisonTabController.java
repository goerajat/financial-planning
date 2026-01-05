package rg.financialplanning.ui;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import rg.financialplanning.model.FilingStatus;
import rg.financialplanning.model.FinancialEntry;
import rg.financialplanning.model.ItemType;
import rg.financialplanning.model.Person;
import rg.financialplanning.model.StateByYear;
import rg.financialplanning.model.YearlySummary;
import rg.financialplanning.parser.FinancialDataProcessor;
import rg.financialplanning.strategy.CompositeTaxOptimizationStrategy;
import rg.financialplanning.strategy.ExpenseManagementStrategy;
import rg.financialplanning.strategy.NoOpRothConversionStrategy;
import rg.financialplanning.strategy.RMDOptimizationStrategy;
import rg.financialplanning.strategy.RothConversionOptimizationStrategy;
import rg.financialplanning.ui.model.ObservableFinancialEntry;
import rg.financialplanning.ui.model.ObservableItemTypeRate;
import rg.financialplanning.ui.model.ObservablePerson;
import rg.financialplanning.ui.model.ObservableStateByYear;

import java.text.NumberFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Controller for the Roth Conversion Comparison tab.
 * Compares net worth outcomes with different Roth conversion strategies.
 */
public class RothComparisonTabController {

    private final VBox root;
    private final ObservableList<ObservablePerson> persons;
    private final ObservableList<ObservableFinancialEntry> entries;
    private final ObservableList<ObservableItemTypeRate> rates;
    private final ObservableList<ObservableStateByYear> statesByYear;

    // Input controls
    private ComboBox<FilingStatusOption> filingStatusComboBox;
    private Spinner<Integer> yearIncrementSpinner;
    private Button runButton;
    private ProgressIndicator progressIndicator;
    private Label statusLabel;

    // Results table
    private TableView<ComparisonRow> resultsTable;
    private Label resultsHeaderLabel;

    // Filing status options
    private static final List<FilingStatusOption> FILING_STATUS_OPTIONS = List.of(
        new FilingStatusOption(FilingStatus.MARRIED_FILING_JOINTLY, "Married Filing Jointly"),
        new FilingStatusOption(FilingStatus.SINGLE, "Single"),
        new FilingStatusOption(FilingStatus.MARRIED_FILING_SEPARATELY, "Married Filing Separately"),
        new FilingStatusOption(FilingStatus.HEAD_OF_HOUSEHOLD, "Head of Household")
    );

    private final NumberFormat currencyFormat;

    public RothComparisonTabController(ObservableList<ObservablePerson> persons,
                                        ObservableList<ObservableFinancialEntry> entries,
                                        ObservableList<ObservableItemTypeRate> rates,
                                        ObservableList<ObservableStateByYear> statesByYear) {
        this.persons = persons;
        this.entries = entries;
        this.rates = rates;
        this.statesByYear = statesByYear;
        this.root = new VBox(15);
        this.currencyFormat = NumberFormat.getCurrencyInstance(Locale.US);
        this.currencyFormat.setMaximumFractionDigits(0);

        initializeUI();
    }

    private void initializeUI() {
        root.setPadding(new Insets(15));

        // Header
        Label headerLabel = new Label("Roth Conversion Strategy Comparison");
        headerLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        Label descriptionLabel = new Label(
            "Compare 5-year net worth projections with different Roth conversion strategies. " +
            "See how filling different tax brackets affects your long-term financial outcomes. " +
            "Each cell shows net worth, delta vs baseline, cumulative taxes, and Roth balance."
        );
        descriptionLabel.setStyle("-fx-text-fill: #666;");
        descriptionLabel.setWrapText(true);

        // Input controls
        HBox inputBox = createInputBox();

        // Results section
        resultsHeaderLabel = new Label("Results");
        resultsHeaderLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
        resultsHeaderLabel.setVisible(false);

        resultsTable = new TableView<>();
        resultsTable.setPlaceholder(new Label("Run comparison to see results"));
        resultsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        VBox.setVgrow(resultsTable, Priority.ALWAYS);

        // Legend
        HBox legend = createLegend();

        // Notes
        VBox notes = createNotes();

        root.getChildren().addAll(headerLabel, descriptionLabel, inputBox,
                                   resultsHeaderLabel, resultsTable, legend, notes);
    }

    private HBox createInputBox() {
        HBox inputBox = new HBox(20);
        inputBox.setPadding(new Insets(15));
        inputBox.setAlignment(Pos.CENTER_LEFT);
        inputBox.setStyle("-fx-background-color: #f9f9f9; -fx-background-radius: 8;");

        // Filing Status
        VBox filingStatusBox = new VBox(5);
        Label filingStatusLabel = new Label("Filing Status:");
        filingStatusComboBox = new ComboBox<>(FXCollections.observableArrayList(FILING_STATUS_OPTIONS));
        filingStatusComboBox.setConverter(new javafx.util.StringConverter<>() {
            @Override
            public String toString(FilingStatusOption option) {
                return option != null ? option.label : "";
            }

            @Override
            public FilingStatusOption fromString(String string) {
                return FILING_STATUS_OPTIONS.stream()
                    .filter(o -> o.label.equals(string))
                    .findFirst()
                    .orElse(null);
            }
        });
        filingStatusComboBox.getSelectionModel().selectFirst();
        filingStatusComboBox.setPrefWidth(200);
        filingStatusBox.getChildren().addAll(filingStatusLabel, filingStatusComboBox);

        // Year Increment
        VBox yearIncrementBox = new VBox(5);
        Label yearIncLabel = new Label("Year Increment:");
        yearIncrementSpinner = new Spinner<>(1, 10, 5, 1);
        yearIncrementSpinner.setEditable(true);
        yearIncrementSpinner.setPrefWidth(80);
        yearIncrementBox.getChildren().addAll(yearIncLabel, yearIncrementSpinner);

        // Run Button and Progress
        VBox buttonBox = new VBox(5);
        buttonBox.setAlignment(Pos.CENTER);

        runButton = new Button("Run Comparison");
        runButton.setStyle("-fx-background-color: #5c6bc0; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 20;");
        runButton.setOnAction(e -> runComparison());

        progressIndicator = new ProgressIndicator();
        progressIndicator.setMaxSize(24, 24);
        progressIndicator.setVisible(false);

        statusLabel = new Label("");
        statusLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 11px;");

        buttonBox.getChildren().addAll(runButton, progressIndicator, statusLabel);

        inputBox.getChildren().addAll(filingStatusBox, yearIncrementBox, buttonBox);

        return inputBox;
    }

    private HBox createLegend() {
        HBox legend = new HBox(20);
        legend.setPadding(new Insets(10));
        legend.setStyle("-fx-background-color: #f9f9f9; -fx-background-radius: 4;");

        HBox baselineItem = createLegendItem("#f5f5f5", "#cccccc", "Baseline (No Conversion)");
        HBox positiveItem = createLegendItem("#e8f5e9", "#2e7d32", "Higher Net Worth vs Baseline");
        HBox negativeItem = createLegendItem("#fff3e0", "#e65100", "Lower Net Worth vs Baseline");
        HBox shortfallItem = createLegendItem("#ffebee", "#c62828", "Cash Shortfall");

        legend.getChildren().addAll(baselineItem, positiveItem, negativeItem, shortfallItem);
        return legend;
    }

    private HBox createLegendItem(String bgColor, String borderColor, String text) {
        HBox item = new HBox(5);
        item.setAlignment(Pos.CENTER_LEFT);
        Label colorBox = new Label("  ");
        colorBox.setStyle("-fx-background-color: " + bgColor + "; -fx-border-color: " + borderColor +
                         "; -fx-border-radius: 3; -fx-background-radius: 3;");
        colorBox.setPrefSize(16, 16);
        item.getChildren().addAll(colorBox, new Label(text));
        return item;
    }

    private VBox createNotes() {
        VBox notes = new VBox(5);
        notes.setPadding(new Insets(10));
        notes.setStyle("-fx-background-color: #f5f5f5; -fx-background-radius: 4;");

        Label notesHeader = new Label("Understanding the Results");
        notesHeader.setStyle("-fx-font-weight: bold;");

        Label note1 = new Label("- No Conversion: Baseline scenario with no Roth conversions performed.");
        Label note2 = new Label("- Fill 12%/22%/24% Bracket: Convert enough to fill each federal tax bracket.");
        Label note3 = new Label("- Delta: Difference in net worth compared to no-conversion baseline.");
        Label note4 = new Label("- Tax: Cumulative federal + state income taxes paid from start to milestone.");

        note1.setStyle("-fx-font-size: 11px; -fx-text-fill: #555;");
        note2.setStyle("-fx-font-size: 11px; -fx-text-fill: #555;");
        note3.setStyle("-fx-font-size: 11px; -fx-text-fill: #555;");
        note4.setStyle("-fx-font-size: 11px; -fx-text-fill: #555;");

        notes.getChildren().addAll(notesHeader, note1, note2, note3, note4);
        return notes;
    }

    private void runComparison() {
        if (entries.isEmpty()) {
            showAlert("No Data", "Please add some financial entries before running the comparison.");
            return;
        }

        FilingStatusOption selectedFilingStatus = filingStatusComboBox.getValue();
        int yearIncrement = yearIncrementSpinner.getValue();

        runButton.setDisable(true);
        progressIndicator.setVisible(true);
        statusLabel.setText("Analyzing...");

        Service<ComparisonResult> comparisonService = new Service<>() {
            @Override
            protected Task<ComparisonResult> createTask() {
                return new Task<>() {
                    @Override
                    protected ComparisonResult call() {
                        return performComparison(selectedFilingStatus.filingStatus, yearIncrement);
                    }
                };
            }
        };

        comparisonService.setOnSucceeded(e -> {
            ComparisonResult result = comparisonService.getValue();
            displayResults(result);
            runButton.setDisable(false);
            progressIndicator.setVisible(false);
            statusLabel.setText("Comparison complete");
        });

        comparisonService.setOnFailed(e -> {
            runButton.setDisable(false);
            progressIndicator.setVisible(false);
            statusLabel.setText("Error");
            Throwable ex = comparisonService.getException();
            showAlert("Comparison Failed", "Failed to run Roth comparison:\n" + ex.getMessage());
            ex.printStackTrace();
        });

        comparisonService.start();
    }

    private ComparisonResult performComparison(FilingStatus filingStatus, int yearIncrement) {
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

        // Setup processor to find year range
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

        // Define bracket thresholds based on filing status
        double bracket12 = filingStatus == FilingStatus.SINGLE || filingStatus == FilingStatus.MARRIED_FILING_SEPARATELY
                ? RothConversionOptimizationStrategy.SINGLE_12_PERCENT_BRACKET
                : RothConversionOptimizationStrategy.MFJ_12_PERCENT_BRACKET;
        double bracket22 = filingStatus == FilingStatus.SINGLE || filingStatus == FilingStatus.MARRIED_FILING_SEPARATELY
                ? RothConversionOptimizationStrategy.SINGLE_22_PERCENT_BRACKET
                : RothConversionOptimizationStrategy.MFJ_22_PERCENT_BRACKET;
        double bracket24 = filingStatus == FilingStatus.SINGLE || filingStatus == FilingStatus.MARRIED_FILING_SEPARATELY
                ? RothConversionOptimizationStrategy.SINGLE_24_PERCENT_BRACKET
                : RothConversionOptimizationStrategy.MFJ_24_PERCENT_BRACKET;

        // Generate baseline summaries (no conversion)
        FinancialDataProcessor baselineProcessor = new FinancialDataProcessor();
        baselineProcessor.setEntries(financialEntries);
        baselineProcessor.setTaxOptimizationStrategy(
                new CompositeTaxOptimizationStrategy(
                        new RMDOptimizationStrategy(),
                        new ExpenseManagementStrategy(),
                        new NoOpRothConversionStrategy(filingStatus)
                )
        );
        YearlySummary[] baselineSummaries = baselineProcessor.generateYearlySummaries(baseRates, personsByName);

        // Build rows for each scenario
        List<ComparisonRow> rows = new ArrayList<>();

        // Scenario 1: No Conversion (baseline)
        rows.add(buildComparisonRow("No Conversion", null, financialEntries, baseRates, personsByName,
                filingStatus, milestoneYears, firstDataYear, baselineSummaries, baselineSummaries));

        // Scenario 2: Fill 12% Bracket
        rows.add(buildComparisonRow("Fill 12% Bracket", bracket12, financialEntries, baseRates, personsByName,
                filingStatus, milestoneYears, firstDataYear, baselineSummaries, null));

        // Scenario 3: Fill 22% Bracket
        rows.add(buildComparisonRow("Fill 22% Bracket", bracket22, financialEntries, baseRates, personsByName,
                filingStatus, milestoneYears, firstDataYear, baselineSummaries, null));

        // Scenario 4: Fill 24% Bracket
        rows.add(buildComparisonRow("Fill 24% Bracket", bracket24, financialEntries, baseRates, personsByName,
                filingStatus, milestoneYears, firstDataYear, baselineSummaries, null));

        return new ComparisonResult(firstDataYear, milestoneYears, rows);
    }

    private ComparisonRow buildComparisonRow(
            String scenarioName,
            Double bracketThreshold,
            List<FinancialEntry> entries,
            Map<ItemType, Double> rates,
            Map<String, Person> personsByName,
            FilingStatus filingStatus,
            List<Integer> milestoneYears,
            int firstDataYear,
            YearlySummary[] baselineSummaries,
            YearlySummary[] precomputedSummaries) {

        YearlySummary[] summaries;
        if (precomputedSummaries != null) {
            summaries = precomputedSummaries;
        } else {
            List<StateByYear> statesByYearList = statesByYear.stream()
                    .map(ObservableStateByYear::toStateByYear)
                    .collect(Collectors.toList());

            FinancialDataProcessor processor = new FinancialDataProcessor();
            processor.setEntries(entries);
            processor.setTaxOptimizationStrategy(
                    new CompositeTaxOptimizationStrategy(filingStatus, statesByYearList, bracketThreshold)
            );
            summaries = processor.generateYearlySummaries(rates, personsByName);
        }

        // Track first shortfall year
        Integer firstShortfallYear = null;
        for (YearlySummary summary : summaries) {
            if (summary.deficit() > 0) {
                firstShortfallYear = summary.year();
                break;
            }
        }

        // Build cells for each milestone
        List<ComparisonCell> cells = new ArrayList<>();
        for (int milestoneYear : milestoneYears) {
            int index = milestoneYear - firstDataYear;
            if (index >= 0 && index < summaries.length) {
                YearlySummary summary = summaries[index];
                YearlySummary baseline = baselineSummaries[index];

                // Calculate cumulative taxes
                double cumulativeTaxes = 0;
                for (int i = 0; i <= index; i++) {
                    cumulativeTaxes += summaries[i].federalIncomeTax() + summaries[i].stateIncomeTax();
                }

                boolean hasShortfall = firstShortfallYear != null && firstShortfallYear <= milestoneYear;

                cells.add(new ComparisonCell(
                        milestoneYear,
                        summary.netWorth(),
                        summary.netWorth() - baseline.netWorth(),
                        cumulativeTaxes,
                        summary.rothAssets(),
                        hasShortfall,
                        hasShortfall ? firstShortfallYear : null
                ));
            }
        }

        return new ComparisonRow(scenarioName, bracketThreshold, cells);
    }

    private void displayResults(ComparisonResult result) {
        resultsHeaderLabel.setText("Roth Conversion Strategy Comparison (First Data Year: " + result.firstDataYear + ")");
        resultsHeaderLabel.setVisible(true);

        // Clear existing columns
        resultsTable.getColumns().clear();

        // Scenario column
        TableColumn<ComparisonRow, String> scenarioColumn = new TableColumn<>("Scenario");
        scenarioColumn.setCellValueFactory(cellData -> {
            ComparisonRow row = cellData.getValue();
            String text = row.scenarioName;
            if (row.bracketThreshold != null) {
                text += "\n(up to " + currencyFormat.format(row.bracketThreshold) + ")";
            }
            return new SimpleStringProperty(text);
        });
        scenarioColumn.setPrefWidth(160);
        scenarioColumn.setStyle("-fx-alignment: CENTER-LEFT;");
        resultsTable.getColumns().add(scenarioColumn);

        // Year columns
        for (int i = 0; i < result.milestoneYears.size(); i++) {
            final int index = i;
            int year = result.milestoneYears.get(i);
            int yearsFromStart = year - result.firstDataYear;

            TableColumn<ComparisonRow, String> yearColumn = new TableColumn<>(year + "\n(+" + yearsFromStart + " yrs)");
            yearColumn.setCellValueFactory(cellData -> {
                ComparisonCell cell = cellData.getValue().cells.get(index);
                if (cell.hasShortfall) {
                    return new SimpleStringProperty("Shortfall\n(Year " + cell.shortfallYear + ")");
                } else {
                    String deltaPrefix = cell.netWorthDelta >= 0 ? "+" : "";
                    return new SimpleStringProperty(
                        currencyFormat.format(cell.netWorth) + "\n" +
                        deltaPrefix + currencyFormat.format(cell.netWorthDelta) + "\n" +
                        "Tax: " + currencyFormat.format(cell.cumulativeTaxes) + "\n" +
                        "Roth: " + currencyFormat.format(cell.rothBalance)
                    );
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
                        int rowIndex = getTableRow() != null ? getTableRow().getIndex() : -1;
                        ComparisonRow row = rowIndex >= 0 && rowIndex < resultsTable.getItems().size()
                                ? resultsTable.getItems().get(rowIndex)
                                : null;

                        if (item.startsWith("Shortfall")) {
                            setStyle("-fx-background-color: #ffebee; -fx-text-fill: #c62828; -fx-font-weight: bold; -fx-alignment: CENTER; -fx-font-size: 10px;");
                        } else if (row != null && row.bracketThreshold == null) {
                            // Baseline row
                            setStyle("-fx-background-color: #f5f5f5; -fx-alignment: CENTER; -fx-font-size: 10px;");
                        } else if (row != null && row.cells.get(index).netWorthDelta > 0) {
                            setStyle("-fx-background-color: #e8f5e9; -fx-text-fill: #2e7d32; -fx-alignment: CENTER; -fx-font-size: 10px;");
                        } else if (row != null && row.cells.get(index).netWorthDelta < 0) {
                            setStyle("-fx-background-color: #fff3e0; -fx-text-fill: #e65100; -fx-alignment: CENTER; -fx-font-size: 10px;");
                        } else {
                            setStyle("-fx-alignment: CENTER; -fx-font-size: 10px;");
                        }
                    }
                }
            });
            yearColumn.setPrefWidth(140);
            resultsTable.getColumns().add(yearColumn);
        }

        resultsTable.setItems(FXCollections.observableArrayList(result.rows));
        resultsTable.setFixedCellSize(80);
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
    private static class FilingStatusOption {
        final FilingStatus filingStatus;
        final String label;

        FilingStatusOption(FilingStatus filingStatus, String label) {
            this.filingStatus = filingStatus;
            this.label = label;
        }
    }

    private static class ComparisonCell {
        final int year;
        final double netWorth;
        final double netWorthDelta;
        final double cumulativeTaxes;
        final double rothBalance;
        final boolean hasShortfall;
        final Integer shortfallYear;

        ComparisonCell(int year, double netWorth, double netWorthDelta,
                       double cumulativeTaxes, double rothBalance,
                       boolean hasShortfall, Integer shortfallYear) {
            this.year = year;
            this.netWorth = netWorth;
            this.netWorthDelta = netWorthDelta;
            this.cumulativeTaxes = cumulativeTaxes;
            this.rothBalance = rothBalance;
            this.hasShortfall = hasShortfall;
            this.shortfallYear = shortfallYear;
        }
    }

    private static class ComparisonRow {
        final String scenarioName;
        final Double bracketThreshold;
        final List<ComparisonCell> cells;

        ComparisonRow(String scenarioName, Double bracketThreshold, List<ComparisonCell> cells) {
            this.scenarioName = scenarioName;
            this.bracketThreshold = bracketThreshold;
            this.cells = cells;
        }
    }

    private static class ComparisonResult {
        final int firstDataYear;
        final List<Integer> milestoneYears;
        final List<ComparisonRow> rows;

        ComparisonResult(int firstDataYear, List<Integer> milestoneYears, List<ComparisonRow> rows) {
            this.firstDataYear = firstDataYear;
            this.milestoneYears = milestoneYears;
            this.rows = rows;
        }
    }
}
