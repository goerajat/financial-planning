package rg.financialplanning.ui;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import rg.financialplanning.export.FinancialPlanInputs;
import rg.financialplanning.export.PdfExporter;
import rg.financialplanning.model.FinancialEntry;
import rg.financialplanning.model.ItemType;
import rg.financialplanning.model.Person;
import rg.financialplanning.model.YearlySummary;
import rg.financialplanning.parser.FinancialDataProcessor;
import rg.financialplanning.ui.model.ObservableFinancialEntry;
import rg.financialplanning.ui.model.ObservableItemTypeRate;
import rg.financialplanning.ui.model.ObservablePerson;
import rg.financialplanning.ui.util.DataPersistence;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Main controller for the Financial Planner application.
 * Manages the tabbed interface and coordinates data between tabs.
 */
public class MainController {

    private final Stage primaryStage;
    private final BorderPane root;
    private final TabPane tabPane;

    // Observable lists for data binding
    private final ObservableList<ObservablePerson> persons = FXCollections.observableArrayList();
    private final ObservableList<ObservableFinancialEntry> entries = FXCollections.observableArrayList();
    private final ObservableList<ObservableItemTypeRate> rates = FXCollections.observableArrayList();

    // Tab controllers
    private PersonsTabController personsTabController;
    private EntriesTabController entriesTabController;
    private RatesTabController ratesTabController;
    private ResultsTabController resultsTabController;

    // Status
    private Label statusLabel;
    private ProgressIndicator progressIndicator;

    // Generated PDF file path
    private File lastGeneratedPdf;

    // Last saved/loaded file
    private File currentFile;

    public MainController(Stage primaryStage) {
        this.primaryStage = primaryStage;
        this.root = new BorderPane();
        this.tabPane = new TabPane();

        initializeDefaultRates();
        initializeTabs();
        initializeBottomBar();

        root.setCenter(tabPane);
    }

    private void initializeDefaultRates() {
        // Initialize with default rates for each item type
        rates.add(new ObservableItemTypeRate(ItemType.INCOME, 3.0));
        rates.add(new ObservableItemTypeRate(ItemType.EXPENSE, 3.0));
        rates.add(new ObservableItemTypeRate(ItemType.NON_QUALIFIED, 6.0));
        rates.add(new ObservableItemTypeRate(ItemType.QUALIFIED, 6.0));
        rates.add(new ObservableItemTypeRate(ItemType.ROTH, 6.0));
        rates.add(new ObservableItemTypeRate(ItemType.CASH, 2.0));
        rates.add(new ObservableItemTypeRate(ItemType.REAL_ESTATE, 3.0));
        rates.add(new ObservableItemTypeRate(ItemType.LIFE_INSURANCE_BENEFIT, 0.0));
        rates.add(new ObservableItemTypeRate(ItemType.SOCIAL_SECURITY_BENEFITS, 2.0));
        rates.add(new ObservableItemTypeRate(ItemType.ROTH_CONTRIBUTION, 0.0));
        rates.add(new ObservableItemTypeRate(ItemType.QUALIFIED_CONTRIBUTION, 0.0));
        rates.add(new ObservableItemTypeRate(ItemType.LIFE_INSURANCE_CONTRIBUTION, 0.0));
        rates.add(new ObservableItemTypeRate(ItemType.MORTGAGE, 6.5));
        rates.add(new ObservableItemTypeRate(ItemType.MORTGAGE_REPAYMENT, 0.0));
    }

    private void initializeTabs() {
        // Persons Tab
        personsTabController = new PersonsTabController(persons);
        Tab personsTab = new Tab("Persons", personsTabController.getRoot());
        personsTab.setClosable(false);

        // Financial Entries Tab
        entriesTabController = new EntriesTabController(entries, persons);
        Tab entriesTab = new Tab("Financial Entries", entriesTabController.getRoot());
        entriesTab.setClosable(false);

        // Rates Tab
        ratesTabController = new RatesTabController(rates);
        Tab ratesTab = new Tab("Rates", ratesTabController.getRoot());
        ratesTab.setClosable(false);

        // Results Tab
        resultsTabController = new ResultsTabController(primaryStage);
        Tab resultsTab = new Tab("Results", resultsTabController.getRoot());
        resultsTab.setClosable(false);

        tabPane.getTabs().addAll(personsTab, entriesTab, ratesTab, resultsTab);
    }

    private void initializeBottomBar() {
        HBox bottomBar = new HBox(10);
        bottomBar.setPadding(new Insets(10, 15, 10, 15));
        bottomBar.setAlignment(Pos.CENTER_LEFT);
        bottomBar.setStyle("-fx-background-color: #f0f0f0; -fx-border-color: #ccc; -fx-border-width: 1 0 0 0;");

        // File operations buttons
        Button loadButton = new Button("Load");
        loadButton.setStyle("-fx-padding: 8 15;");
        loadButton.setOnAction(e -> loadData());

        Button saveButton = new Button("Save");
        saveButton.setStyle("-fx-padding: 8 15;");
        saveButton.setOnAction(e -> saveData(false));

        Button saveAsButton = new Button("Save As...");
        saveAsButton.setStyle("-fx-padding: 8 15;");
        saveAsButton.setOnAction(e -> saveData(true));

        // Separator
        Separator separator = new Separator();
        separator.setOrientation(javafx.geometry.Orientation.VERTICAL);

        Button generateButton = new Button("Generate Plan");
        generateButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 20;");
        generateButton.setOnAction(e -> generatePlan());

        progressIndicator = new ProgressIndicator();
        progressIndicator.setMaxSize(24, 24);
        progressIndicator.setVisible(false);

        statusLabel = new Label("Ready");
        statusLabel.setStyle("-fx-text-fill: #666;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label helpLabel = new Label("Load/Save your data, then Generate Plan");
        helpLabel.setStyle("-fx-text-fill: #888; -fx-font-style: italic;");

        bottomBar.getChildren().addAll(loadButton, saveButton, saveAsButton, separator,
                generateButton, progressIndicator, statusLabel, spacer, helpLabel);
        root.setBottom(bottomBar);
    }

    private void generatePlan() {
        if (entries.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "No Data", "Please add at least one financial entry before generating a plan.");
            return;
        }

        statusLabel.setText("Generating plan...");
        progressIndicator.setVisible(true);

        // Create background service for PDF generation
        Service<File> generateService = new Service<>() {
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

                        // Process financial data
                        FinancialDataProcessor processor = new FinancialDataProcessor();
                        processor.setEntries(financialEntries);

                        YearlySummary[] summaries = processor.generateYearlySummaries(percentageRates, personsByName);

                        // Generate PDF
                        File tempPdf = File.createTempFile("financial_plan_", ".pdf");
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

        generateService.setOnSucceeded(e -> {
            lastGeneratedPdf = generateService.getValue();
            statusLabel.setText("Plan generated successfully!");
            progressIndicator.setVisible(false);

            // Display in results tab
            resultsTabController.displayPdf(lastGeneratedPdf);

            // Switch to results tab
            tabPane.getSelectionModel().select(3);
        });

        generateService.setOnFailed(e -> {
            statusLabel.setText("Error generating plan");
            progressIndicator.setVisible(false);
            Throwable ex = generateService.getException();
            showAlert(Alert.AlertType.ERROR, "Generation Failed",
                    "Failed to generate financial plan:\n" + ex.getMessage());
            ex.printStackTrace();
        });

        generateService.start();
    }

    private void saveData(boolean saveAs) {
        File fileToSave = currentFile;

        if (saveAs || fileToSave == null) {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Save Financial Plan Data");
            fileChooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("Financial Plan Files", "*.fpd")
            );
            fileChooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("JSON Files", "*.json")
            );
            if (currentFile != null) {
                fileChooser.setInitialDirectory(currentFile.getParentFile());
                fileChooser.setInitialFileName(currentFile.getName());
            } else {
                fileChooser.setInitialFileName("financial_plan.fpd");
            }

            fileToSave = fileChooser.showSaveDialog(primaryStage);
        }

        if (fileToSave != null) {
            try {
                DataPersistence.saveToFile(fileToSave, persons, entries, rates);
                currentFile = fileToSave;
                statusLabel.setText("Saved: " + fileToSave.getName());
                updateWindowTitle();
            } catch (IOException ex) {
                showAlert(Alert.AlertType.ERROR, "Save Failed",
                        "Failed to save data: " + ex.getMessage());
            }
        }
    }

    private void loadData() {
        // Confirm if there's unsaved data
        if (!persons.isEmpty() || !entries.isEmpty()) {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Load Data");
            confirm.setHeaderText(null);
            confirm.setContentText("Loading will replace current data. Continue?");
            if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
                return;
            }
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Load Financial Plan Data");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Financial Plan Files", "*.fpd")
        );
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("JSON Files", "*.json")
        );
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("All Files", "*.*")
        );

        File file = fileChooser.showOpenDialog(primaryStage);
        if (file != null) {
            try {
                DataPersistence.loadFromFile(file, persons, entries, rates);
                currentFile = file;
                statusLabel.setText("Loaded: " + file.getName());
                updateWindowTitle();

                // Refresh the rates table
                ratesTabController.getRoot().getScene().getWindow().sizeToScene();

            } catch (IOException ex) {
                showAlert(Alert.AlertType.ERROR, "Load Failed",
                        "Failed to load data: " + ex.getMessage());
            } catch (Exception ex) {
                showAlert(Alert.AlertType.ERROR, "Load Failed",
                        "Invalid file format: " + ex.getMessage());
            }
        }
    }

    private void updateWindowTitle() {
        String title = "Financial Planner";
        if (currentFile != null) {
            title += " - " + currentFile.getName();
        }
        primaryStage.setTitle(title);
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public BorderPane getRoot() {
        return root;
    }

    public ObservableList<ObservablePerson> getPersons() {
        return persons;
    }

    public ObservableList<ObservableFinancialEntry> getEntries() {
        return entries;
    }

    public ObservableList<ObservableItemTypeRate> getRates() {
        return rates;
    }
}
