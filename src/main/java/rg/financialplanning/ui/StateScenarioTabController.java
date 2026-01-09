package rg.financialplanning.ui;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.*;
import rg.financialplanning.calculator.StateTaxCalculatorFactory;
import rg.financialplanning.export.FinancialPlanInputs;
import rg.financialplanning.export.PdfExporter;
import rg.financialplanning.model.FilingStatus;
import rg.financialplanning.model.FinancialEntry;
import rg.financialplanning.model.ItemType;
import rg.financialplanning.model.Person;
import rg.financialplanning.model.State;
import rg.financialplanning.model.StateByYear;
import rg.financialplanning.model.YearlySummary;
import rg.financialplanning.parser.FinancialDataProcessor;
import rg.financialplanning.strategy.CompositeTaxOptimizationStrategy;
import rg.financialplanning.ui.model.ObservableFinancialEntry;
import rg.financialplanning.ui.model.ObservableItemTypeRate;
import rg.financialplanning.ui.model.ObservablePerson;
import rg.financialplanning.ui.model.ObservableStateByYear;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.text.NumberFormat;
import java.time.Year;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Controller for the State Residence Scenario Comparison tab.
 * Allows users to define and compare different state residence scenarios.
 */
public class StateScenarioTabController {

    private final VBox root;
    private final ObservableList<ObservablePerson> persons;
    private final ObservableList<ObservableFinancialEntry> entries;
    private final ObservableList<ObservableItemTypeRate> rates;
    private final ObservableList<ObservableStateByYear> statesByYear;

    // Scenario management
    private final ObservableList<StateScenario> scenarios = FXCollections.observableArrayList();
    private ListView<StateScenario> scenarioListView;

    // Input controls
    private ComboBox<FilingStatusOption> filingStatusComboBox;
    private Spinner<Integer> yearIncrementSpinner;
    private Button runButton;
    private Button addScenarioButton;
    private ProgressIndicator progressIndicator;
    private Label statusLabel;

    // Results table
    private TableView<ScenarioRow> resultsTable;
    private Label resultsHeaderLabel;

    // PDF generation status
    private Label pdfStatusLabel;
    private boolean isPdfGenerating = false;

    // Filing status options
    private static final List<FilingStatusOption> FILING_STATUS_OPTIONS = List.of(
        new FilingStatusOption(FilingStatus.MARRIED_FILING_JOINTLY, "Married Filing Jointly"),
        new FilingStatusOption(FilingStatus.SINGLE, "Single"),
        new FilingStatusOption(FilingStatus.MARRIED_FILING_SEPARATELY, "Married Filing Separately"),
        new FilingStatusOption(FilingStatus.HEAD_OF_HOUSEHOLD, "Head of Household")
    );

    private final NumberFormat currencyFormat;
    private final int currentYear;

    public StateScenarioTabController(ObservableList<ObservablePerson> persons,
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
        this.currentYear = Year.now().getValue();

        initializeUI();
    }

    private void initializeUI() {
        root.setPadding(new Insets(15));

        // Header
        Label headerLabel = new Label("State Residence Scenario Comparison");
        headerLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        Label descriptionLabel = new Label(
            "Compare different state residence scenarios to see how relocating affects your taxes and net worth. " +
            "Define multiple scenarios with different state sequences and compare their long-term outcomes. " +
            "Click any row to generate a PDF for that scenario."
        );
        descriptionLabel.setStyle("-fx-text-fill: #666;");
        descriptionLabel.setWrapText(true);

        // Scenario builder section
        VBox scenarioBuilder = createScenarioBuilder();

        // PDF status label
        pdfStatusLabel = new Label("");
        pdfStatusLabel.setStyle("-fx-text-fill: #1565c0; -fx-background-color: #e3f2fd; -fx-padding: 8 12; -fx-background-radius: 4;");
        pdfStatusLabel.setVisible(false);
        pdfStatusLabel.setManaged(false);

        // Input controls
        HBox inputBox = createInputBox();

        // Results section
        resultsHeaderLabel = new Label("Results");
        resultsHeaderLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
        resultsHeaderLabel.setVisible(false);

        resultsTable = new TableView<>();
        resultsTable.setPlaceholder(new Label("Add at least 2 scenarios and run comparison to see results"));
        resultsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        VBox.setVgrow(resultsTable, Priority.ALWAYS);

        // Add row click handler for PDF generation
        resultsTable.setRowFactory(tv -> {
            TableRow<ScenarioRow> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (!row.isEmpty() && event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 1) {
                    int rowIndex = row.getIndex();
                    ScenarioRow clickedRow = row.getItem();
                    generatePdfForScenario(clickedRow, rowIndex);
                }
            });
            row.setStyle("-fx-cursor: hand;");
            return row;
        });

        // Legend
        HBox legend = createLegend();

        // Notes
        VBox notes = createNotes();

        root.getChildren().addAll(headerLabel, descriptionLabel, scenarioBuilder, inputBox, pdfStatusLabel,
                                   resultsHeaderLabel, resultsTable, legend, notes);
    }

    private VBox createScenarioBuilder() {
        VBox builder = new VBox(10);
        builder.setPadding(new Insets(15));
        builder.setStyle("-fx-background-color: #f9f9f9; -fx-background-radius: 8;");

        Label title = new Label("Scenarios (0/5)");
        title.setStyle("-fx-font-weight: bold;");

        scenarioListView = new ListView<>(scenarios);
        scenarioListView.setPrefHeight(220);
        scenarioListView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(StateScenario item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    HBox cell = new HBox(10);
                    cell.setAlignment(Pos.CENTER_LEFT);

                    int index = getIndex();
                    String indexStr = (index + 1) + ". ";
                    String baselineStr = index == 0 ? " (baseline)" : "";

                    VBox info = new VBox(2);
                    Label nameLabel = new Label(indexStr + item.name + baselineStr);
                    nameLabel.setStyle("-fx-font-weight: bold;");
                    Label descLabel = new Label(item.getDescription());
                    descLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #666;");
                    info.getChildren().addAll(nameLabel, descLabel);

                    Region spacer = new Region();
                    HBox.setHgrow(spacer, Priority.ALWAYS);

                    Button editBtn = new Button("Edit");
                    editBtn.setStyle("-fx-font-size: 11px;");
                    editBtn.setOnAction(e -> editScenario(index));

                    Button deleteBtn = new Button("Delete");
                    deleteBtn.setStyle("-fx-font-size: 11px;");
                    deleteBtn.setOnAction(e -> deleteScenario(index));

                    cell.getChildren().addAll(info, spacer, editBtn, deleteBtn);
                    setGraphic(cell);
                }
            }
        });

        scenarios.addListener((javafx.collections.ListChangeListener<StateScenario>) c -> {
            title.setText("Scenarios (" + scenarios.size() + "/5)");
            addScenarioButton.setDisable(scenarios.size() >= 5);
            runButton.setDisable(scenarios.size() < 2 || entries.isEmpty());
        });

        addScenarioButton = new Button("+ Add Scenario");
        addScenarioButton.setStyle("-fx-background-color: #e8f5e9; -fx-text-fill: #2e7d32;");
        addScenarioButton.setOnAction(e -> addScenario());

        Label hint = new Label("Add at least 2 scenarios to run a comparison.");
        hint.setStyle("-fx-font-size: 11px; -fx-text-fill: #666; -fx-font-style: italic;");

        builder.getChildren().addAll(title, scenarioListView, addScenarioButton, hint);

        return builder;
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
        runButton.setDisable(true);

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

        HBox baselineItem = createLegendItem("#f5f5f5", "#cccccc", "Baseline (First Scenario)");
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

        Label note1 = new Label("- Net Worth: Total assets minus liabilities at each milestone year.");
        Label note2 = new Label("- Delta: Difference in net worth compared to the first scenario (baseline).");
        Label note3 = new Label("- Tax Savings: Cumulative state tax savings compared to baseline.");
        Label note4 = new Label("- Florida (FL): Has no state income tax, which can lead to significant savings.");

        note1.setStyle("-fx-font-size: 11px; -fx-text-fill: #555;");
        note2.setStyle("-fx-font-size: 11px; -fx-text-fill: #555;");
        note3.setStyle("-fx-font-size: 11px; -fx-text-fill: #555;");
        note4.setStyle("-fx-font-size: 11px; -fx-text-fill: #555;");

        notes.getChildren().addAll(notesHeader, note1, note2, note3, note4);
        return notes;
    }

    private void addScenario() {
        showScenarioDialog(null, -1);
    }

    private void editScenario(int index) {
        if (index >= 0 && index < scenarios.size()) {
            showScenarioDialog(scenarios.get(index), index);
        }
    }

    private void deleteScenario(int index) {
        if (index >= 0 && index < scenarios.size()) {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Delete Scenario");
            confirm.setHeaderText(null);
            confirm.setContentText("Are you sure you want to delete this scenario?");
            confirm.showAndWait().ifPresent(response -> {
                if (response == ButtonType.OK) {
                    scenarios.remove(index);
                }
            });
        }
    }

    private void showScenarioDialog(StateScenario existingScenario, int editIndex) {
        Dialog<StateScenario> dialog = new Dialog<>();
        dialog.setTitle(existingScenario == null ? "Add Scenario" : "Edit Scenario");
        dialog.setHeaderText(null);

        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        // Scenario name
        TextField nameField = new TextField();
        nameField.setPromptText("e.g., Move to FL in 2030");
        nameField.setPrefWidth(250);
        if (existingScenario != null) {
            nameField.setText(existingScenario.name);
        }

        grid.add(new Label("Scenario Name:"), 0, 0);
        grid.add(nameField, 1, 0);

        // State ranges
        Label rangesLabel = new Label("State Residence Periods:");
        rangesLabel.setStyle("-fx-font-weight: bold;");
        grid.add(rangesLabel, 0, 1, 2, 1);

        VBox rangesBox = new VBox(5);
        List<StateRangeEditor> rangeEditors = new ArrayList<>();

        Runnable addRange = () -> {
            StateRangeEditor editor = new StateRangeEditor(currentYear);
            rangeEditors.add(editor);
            rangesBox.getChildren().add(editor.getNode());
            editor.setOnRemove(() -> {
                rangeEditors.remove(editor);
                rangesBox.getChildren().remove(editor.getNode());
            });
        };

        // Initialize with existing data or default
        if (existingScenario != null && !existingScenario.statesByYear.isEmpty()) {
            for (StateByYear sby : existingScenario.statesByYear) {
                StateRangeEditor editor = new StateRangeEditor(currentYear);
                editor.setState(sby.state());
                editor.setStartYear(sby.startYear());
                editor.setEndYear(sby.endYear());
                rangeEditors.add(editor);
                rangesBox.getChildren().add(editor.getNode());
                editor.setOnRemove(() -> {
                    rangeEditors.remove(editor);
                    rangesBox.getChildren().remove(editor.getNode());
                });
            }
        } else {
            addRange.run();
        }

        Button addRangeBtn = new Button("+ Add State Period");
        addRangeBtn.setStyle("-fx-font-size: 11px;");
        addRangeBtn.setOnAction(e -> addRange.run());

        VBox rangesContainer = new VBox(10, rangesBox, addRangeBtn);
        grid.add(rangesContainer, 0, 2, 2, 1);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                String name = nameField.getText().trim();
                if (name.isEmpty()) {
                    showAlert("Validation Error", "Please enter a scenario name.");
                    return null;
                }
                if (rangeEditors.isEmpty()) {
                    showAlert("Validation Error", "Please add at least one state period.");
                    return null;
                }

                List<StateByYear> ranges = rangeEditors.stream()
                        .map(editor -> new StateByYear(editor.getState(), editor.getStartYear(), editor.getEndYear()))
                        .collect(Collectors.toList());

                return new StateScenario(name, ranges);
            }
            return null;
        });

        dialog.showAndWait().ifPresent(scenario -> {
            if (editIndex >= 0) {
                scenarios.set(editIndex, scenario);
            } else {
                scenarios.add(scenario);
            }
        });
    }

    private void runComparison() {
        if (entries.isEmpty()) {
            showAlert("No Data", "Please add some financial entries before running the comparison.");
            return;
        }

        if (scenarios.size() < 2) {
            showAlert("Not Enough Scenarios", "Please add at least 2 scenarios to compare.");
            return;
        }

        FilingStatusOption selectedFilingStatus = filingStatusComboBox.getValue();
        int yearIncrement = yearIncrementSpinner.getValue();

        runButton.setDisable(true);
        progressIndicator.setVisible(true);
        statusLabel.setText("Analyzing...");

        Service<ScenarioResult> comparisonService = new Service<>() {
            @Override
            protected Task<ScenarioResult> createTask() {
                return new Task<>() {
                    @Override
                    protected ScenarioResult call() {
                        return performComparison(selectedFilingStatus.filingStatus, yearIncrement);
                    }
                };
            }
        };

        comparisonService.setOnSucceeded(e -> {
            ScenarioResult result = comparisonService.getValue();
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
            showAlert("Comparison Failed", "Failed to run state scenario comparison:\n" + ex.getMessage());
            ex.printStackTrace();
        });

        comparisonService.start();
    }

    private ScenarioResult performComparison(FilingStatus filingStatus, int yearIncrement) {
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

        // Generate baseline summaries from first scenario
        StateScenario baselineScenario = scenarios.get(0);
        FinancialDataProcessor baselineProcessor = new FinancialDataProcessor();
        baselineProcessor.setEntries(financialEntries);
        baselineProcessor.setTaxOptimizationStrategy(
                new CompositeTaxOptimizationStrategy(filingStatus, baselineScenario.statesByYear)
        );
        YearlySummary[] baselineSummaries = baselineProcessor.generateYearlySummaries(baseRates, personsByName);

        // Build rows for each scenario
        List<ScenarioRow> rows = new ArrayList<>();

        for (int i = 0; i < scenarios.size(); i++) {
            StateScenario scenario = scenarios.get(i);
            rows.add(buildScenarioRow(
                    scenario.name,
                    scenario.statesByYear,
                    scenario.getDescription(),
                    financialEntries,
                    baseRates,
                    personsByName,
                    filingStatus,
                    milestoneYears,
                    firstDataYear,
                    baselineSummaries,
                    i == 0 ? baselineSummaries : null
            ));
        }

        return new ScenarioResult(firstDataYear, milestoneYears, rows);
    }

    private ScenarioRow buildScenarioRow(
            String scenarioName,
            List<StateByYear> statesByYear,
            String description,
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
            FinancialDataProcessor processor = new FinancialDataProcessor();
            processor.setEntries(entries);
            processor.setTaxOptimizationStrategy(
                    new CompositeTaxOptimizationStrategy(filingStatus, statesByYear)
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
        List<ScenarioCell> cells = new ArrayList<>();
        for (int milestoneYear : milestoneYears) {
            int index = milestoneYear - firstDataYear;
            if (index >= 0 && index < summaries.length) {
                YearlySummary summary = summaries[index];
                YearlySummary baseline = baselineSummaries[index];

                // Calculate cumulative taxes
                double cumulativeTaxes = 0;
                double cumulativeStateTaxes = 0;
                double baselineCumulativeStateTaxes = 0;
                for (int i = 0; i <= index; i++) {
                    cumulativeTaxes += summaries[i].federalIncomeTax() + summaries[i].stateIncomeTax();
                    cumulativeStateTaxes += summaries[i].stateIncomeTax();
                    baselineCumulativeStateTaxes += baselineSummaries[i].stateIncomeTax();
                }

                double stateTaxSavings = baselineCumulativeStateTaxes - cumulativeStateTaxes;

                // Get active state for this year
                State state = StateTaxCalculatorFactory.getStateForYear(milestoneYear, statesByYear);
                String activeState = state.getDisplayName();

                boolean hasShortfall = firstShortfallYear != null && firstShortfallYear <= milestoneYear;

                cells.add(new ScenarioCell(
                        milestoneYear,
                        summary.netWorth(),
                        summary.netWorth() - baseline.netWorth(),
                        cumulativeTaxes,
                        cumulativeStateTaxes,
                        stateTaxSavings,
                        activeState,
                        hasShortfall,
                        hasShortfall ? firstShortfallYear : null
                ));
            }
        }

        return new ScenarioRow(scenarioName, description, cells);
    }

    private void displayResults(ScenarioResult result) {
        resultsHeaderLabel.setText("State Residence Scenario Comparison (First Data Year: " + result.firstDataYear + ")");
        resultsHeaderLabel.setVisible(true);

        // Clear existing columns
        resultsTable.getColumns().clear();

        // Scenario column
        TableColumn<ScenarioRow, String> scenarioColumn = new TableColumn<>("Scenario");
        scenarioColumn.setCellValueFactory(cellData -> {
            ScenarioRow row = cellData.getValue();
            return new SimpleStringProperty(row.scenarioName + "\n" + row.description);
        });
        scenarioColumn.setPrefWidth(200);
        scenarioColumn.setStyle("-fx-alignment: CENTER-LEFT;");
        resultsTable.getColumns().add(scenarioColumn);

        // Year columns
        for (int i = 0; i < result.milestoneYears.size(); i++) {
            final int index = i;
            int year = result.milestoneYears.get(i);
            int yearsFromStart = year - result.firstDataYear;

            TableColumn<ScenarioRow, String> yearColumn = new TableColumn<>(year + "\n(+" + yearsFromStart + " yrs)");
            yearColumn.setCellValueFactory(cellData -> {
                ScenarioCell cell = cellData.getValue().cells.get(index);
                if (cell.hasShortfall) {
                    return new SimpleStringProperty("Shortfall\n(Year " + cell.shortfallYear + ")");
                } else {
                    String deltaPrefix = cell.netWorthDelta >= 0 ? "+" : "";
                    String savingsPrefix = cell.stateTaxSavings >= 0 ? "+" : "";
                    return new SimpleStringProperty(
                        currencyFormat.format(cell.netWorth) + "\n" +
                        deltaPrefix + currencyFormat.format(cell.netWorthDelta) + "\n" +
                        "Tax savings: " + savingsPrefix + currencyFormat.format(cell.stateTaxSavings) + "\n" +
                        cell.activeState
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
                        ScenarioRow row = rowIndex >= 0 && rowIndex < resultsTable.getItems().size()
                                ? resultsTable.getItems().get(rowIndex)
                                : null;

                        if (item.startsWith("Shortfall")) {
                            setStyle("-fx-background-color: #ffebee; -fx-text-fill: #c62828; -fx-font-weight: bold; -fx-alignment: CENTER; -fx-font-size: 10px;");
                        } else if (rowIndex == 0) {
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
            yearColumn.setPrefWidth(150);
            resultsTable.getColumns().add(yearColumn);
        }

        resultsTable.setItems(FXCollections.observableArrayList(result.rows));
        resultsTable.setFixedCellSize(80);
    }

    private void generatePdfForScenario(ScenarioRow scenarioRow, int rowIndex) {
        if (isPdfGenerating) {
            return;
        }

        // Get the corresponding StateScenario
        if (rowIndex < 0 || rowIndex >= scenarios.size()) {
            showAlert("Error", "Could not find scenario data for this row.");
            return;
        }

        StateScenario scenario = scenarios.get(rowIndex);
        isPdfGenerating = true;
        String scenarioName = scenario.name;
        pdfStatusLabel.setText("Generating PDF for \"" + scenarioName + "\"...");
        pdfStatusLabel.setVisible(true);
        pdfStatusLabel.setManaged(true);

        FilingStatus filingStatus = filingStatusComboBox.getValue().filingStatus;
        List<StateByYear> scenarioStatesByYear = scenario.statesByYear;

        Service<File> pdfService = new Service<>() {
            @Override
            protected Task<File> createTask() {
                return new Task<>() {
                    @Override
                    protected File call() throws Exception {
                        // Convert observable data to domain models
                        List<FinancialEntry> financialEntries = entries.stream()
                                .map(ObservableFinancialEntry::toFinancialEntry)
                                .collect(Collectors.toList());

                        List<Person> personList = persons.stream()
                                .map(ObservablePerson::toPerson)
                                .collect(Collectors.toList());

                        Map<String, Person> personsByName = personList.stream()
                                .collect(Collectors.toMap(Person::name, p -> p, (a, b) -> a));

                        Map<ItemType, Double> percentageRates = rates.stream()
                                .collect(Collectors.toMap(
                                        ObservableItemTypeRate::getItemType,
                                        ObservableItemTypeRate::getRate
                                ));

                        // Process financial data with scenario-specific state configuration
                        FinancialDataProcessor processor = new FinancialDataProcessor();
                        processor.setEntries(financialEntries);
                        processor.setTaxOptimizationStrategy(
                                new CompositeTaxOptimizationStrategy(filingStatus, scenarioStatesByYear)
                        );

                        YearlySummary[] summaries = processor.generateYearlySummaries(percentageRates, personsByName);

                        // Generate PDF
                        String safeScenarioName = scenarioName.replaceAll("[^a-zA-Z0-9]", "_");
                        File tempPdf = File.createTempFile("financial_plan_" + safeScenarioName + "_", ".pdf");
                        tempPdf.deleteOnExit();

                        FinancialPlanInputs inputs = new FinancialPlanInputs(
                                financialEntries,
                                personList,
                                percentageRates
                        );

                        PdfExporter exporter = new PdfExporter();
                        exporter.exportYearlySummariesToPdf(summaries, inputs, tempPdf.getAbsolutePath());

                        return tempPdf;
                    }
                };
            }
        };

        pdfService.setOnSucceeded(e -> {
            isPdfGenerating = false;
            pdfStatusLabel.setVisible(false);
            pdfStatusLabel.setManaged(false);

            File pdfFile = pdfService.getValue();
            try {
                if (Desktop.isDesktopSupported()) {
                    Desktop.getDesktop().open(pdfFile);
                } else {
                    showAlert("PDF Generated", "PDF saved to: " + pdfFile.getAbsolutePath());
                }
            } catch (IOException ex) {
                showAlert("Error", "Failed to open PDF: " + ex.getMessage());
            }
        });

        pdfService.setOnFailed(e -> {
            isPdfGenerating = false;
            pdfStatusLabel.setVisible(false);
            pdfStatusLabel.setManaged(false);
            Throwable ex = pdfService.getException();
            showAlert("PDF Generation Failed", "Failed to generate PDF:\n" + ex.getMessage());
            ex.printStackTrace();
        });

        pdfService.start();
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

    private static class StateScenario {
        final String name;
        final List<StateByYear> statesByYear;

        StateScenario(String name, List<StateByYear> statesByYear) {
            this.name = name;
            this.statesByYear = statesByYear;
        }

        String getDescription() {
            if (statesByYear == null || statesByYear.isEmpty()) {
                return "NJ (default)";
            }
            return statesByYear.stream()
                    .map(sby -> sby.state().name() + " " + sby.startYear() + "-" + sby.endYear())
                    .collect(Collectors.joining(", "));
        }
    }

    private static class StateRangeEditor {
        private final HBox node;
        private final ComboBox<State> stateCombo;
        private final Spinner<Integer> startYearSpinner;
        private final Spinner<Integer> endYearSpinner;
        private Runnable onRemove;

        StateRangeEditor(int currentYear) {
            node = new HBox(10);
            node.setAlignment(Pos.CENTER_LEFT);

            stateCombo = new ComboBox<>(FXCollections.observableArrayList(State.values()));
            stateCombo.setConverter(new javafx.util.StringConverter<>() {
                @Override
                public String toString(State state) {
                    return state != null ? state.getDisplayName() : "";
                }

                @Override
                public State fromString(String string) {
                    return Arrays.stream(State.values())
                            .filter(s -> s.getDisplayName().equals(string))
                            .findFirst()
                            .orElse(State.NJ);
                }
            });
            stateCombo.getSelectionModel().select(State.NJ);
            stateCombo.setPrefWidth(120);

            startYearSpinner = new Spinner<>(1900, 2100, currentYear, 1);
            startYearSpinner.setEditable(true);
            startYearSpinner.setPrefWidth(80);

            endYearSpinner = new Spinner<>(1900, 2100, currentYear + 25, 1);
            endYearSpinner.setEditable(true);
            endYearSpinner.setPrefWidth(80);

            Button removeBtn = new Button("Remove");
            removeBtn.setStyle("-fx-font-size: 11px; -fx-background-color: #ffebee; -fx-text-fill: #c62828;");
            removeBtn.setOnAction(e -> {
                if (onRemove != null) {
                    onRemove.run();
                }
            });

            node.getChildren().addAll(
                    stateCombo,
                    new Label("from"),
                    startYearSpinner,
                    new Label("to"),
                    endYearSpinner,
                    removeBtn
            );
        }

        HBox getNode() {
            return node;
        }

        State getState() {
            return stateCombo.getValue();
        }

        void setState(State state) {
            stateCombo.getSelectionModel().select(state);
        }

        int getStartYear() {
            return startYearSpinner.getValue();
        }

        void setStartYear(int year) {
            startYearSpinner.getValueFactory().setValue(year);
        }

        int getEndYear() {
            return endYearSpinner.getValue();
        }

        void setEndYear(int year) {
            endYearSpinner.getValueFactory().setValue(year);
        }

        void setOnRemove(Runnable onRemove) {
            this.onRemove = onRemove;
        }
    }

    private static class ScenarioCell {
        final int year;
        final double netWorth;
        final double netWorthDelta;
        final double cumulativeTaxes;
        final double cumulativeStateTaxes;
        final double stateTaxSavings;
        final String activeState;
        final boolean hasShortfall;
        final Integer shortfallYear;

        ScenarioCell(int year, double netWorth, double netWorthDelta,
                     double cumulativeTaxes, double cumulativeStateTaxes,
                     double stateTaxSavings, String activeState,
                     boolean hasShortfall, Integer shortfallYear) {
            this.year = year;
            this.netWorth = netWorth;
            this.netWorthDelta = netWorthDelta;
            this.cumulativeTaxes = cumulativeTaxes;
            this.cumulativeStateTaxes = cumulativeStateTaxes;
            this.stateTaxSavings = stateTaxSavings;
            this.activeState = activeState;
            this.hasShortfall = hasShortfall;
            this.shortfallYear = shortfallYear;
        }
    }

    private static class ScenarioRow {
        final String scenarioName;
        final String description;
        final List<ScenarioCell> cells;

        ScenarioRow(String scenarioName, String description, List<ScenarioCell> cells) {
            this.scenarioName = scenarioName;
            this.description = description;
            this.cells = cells;
        }
    }

    private static class ScenarioResult {
        final int firstDataYear;
        final List<Integer> milestoneYears;
        final List<ScenarioRow> rows;

        ScenarioResult(int firstDataYear, List<Integer> milestoneYears, List<ScenarioRow> rows) {
            this.firstDataYear = firstDataYear;
            this.milestoneYears = milestoneYears;
            this.rows = rows;
        }
    }
}
