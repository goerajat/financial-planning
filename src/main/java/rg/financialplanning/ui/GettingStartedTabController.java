package rg.financialplanning.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

/**
 * Controller for the Getting Started tab.
 * Provides introduction and instructions for users.
 */
public class GettingStartedTabController {

    private final VBox root;

    public GettingStartedTabController() {
        this.root = new VBox(10);
        initializeUI();
    }

    private void initializeUI() {
        root.setPadding(new Insets(0));

        // Create scrollable content
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent;");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        VBox content = new VBox(25);
        content.setPadding(new Insets(20));
        content.setMaxWidth(900);

        // Welcome Header
        Label headerLabel = new Label("Welcome to Financial Planner");
        headerLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        Label subtitleLabel = new Label("Plan your financial future with comprehensive projections, tax analysis, and scenario comparisons.");
        subtitleLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #666;");
        subtitleLabel.setWrapText(true);

        VBox headerSection = new VBox(8, headerLabel, subtitleLabel);

        // Getting Started Section
        VBox gettingStartedSection = createGettingStartedSection();

        // Entry Types Section
        VBox entryTypesSection = createEntryTypesSection();

        // Advanced Analysis Section
        VBox advancedSection = createAdvancedAnalysisSection();

        // Tips Section
        VBox tipsSection = createTipsSection();

        content.getChildren().addAll(headerSection, gettingStartedSection, entryTypesSection, advancedSection, tipsSection);
        scrollPane.setContent(content);
        root.getChildren().add(scrollPane);
    }

    private VBox createGettingStartedSection() {
        VBox section = new VBox(15);

        Label sectionTitle = createSectionTitle("Getting Started");
        Label intro = new Label("Follow these steps to create your financial plan:");
        intro.setStyle("-fx-text-fill: #444;");

        VBox stepsContainer = new VBox(10);

        stepsContainer.getChildren().addAll(
                createStep("1", "Add Persons",
                        "Start by adding yourself and any family members. Enter names, birth years, and self-employment status."),
                createStep("2", "Add Financial Entries",
                        "Enter your income sources, expenses, assets, and contributions. Specify the person, type, amount, and the years each entry applies."),
                createStep("3", "Configure Rates",
                        "Adjust growth rates for different asset types and inflation rates for income/expenses. Default values are provided."),
                createStep("4", "Set State Residency",
                        "Define which state you'll live in for each year range. This affects state tax calculations."),
                createStep("5", "Generate Results",
                        "View your financial projections as a detailed PDF report showing year-by-year cash flow, assets, and net worth.")
        );

        section.getChildren().addAll(sectionTitle, intro, stepsContainer);
        return section;
    }

    private HBox createStep(String number, String title, String description) {
        HBox step = new HBox(15);
        step.setPadding(new Insets(12));
        step.setStyle("-fx-background-color: #f8f9fa; -fx-background-radius: 8; -fx-border-color: #4a90d9; -fx-border-width: 0 0 0 4; -fx-border-radius: 8 0 0 8;");
        step.setAlignment(Pos.TOP_LEFT);

        // Number circle
        StackPane numberCircle = new StackPane();
        numberCircle.setMinSize(32, 32);
        numberCircle.setMaxSize(32, 32);
        numberCircle.setStyle("-fx-background-color: #4a90d9; -fx-background-radius: 16;");
        Label numberLabel = new Label(number);
        numberLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px;");
        numberCircle.getChildren().add(numberLabel);

        // Content
        VBox stepContent = new VBox(4);
        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #333;");
        Label descLabel = new Label(description);
        descLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 13px;");
        descLabel.setWrapText(true);
        stepContent.getChildren().addAll(titleLabel, descLabel);
        HBox.setHgrow(stepContent, Priority.ALWAYS);

        step.getChildren().addAll(numberCircle, stepContent);
        return step;
    }

    private VBox createEntryTypesSection() {
        VBox section = new VBox(15);

        Label sectionTitle = createSectionTitle("Entry Types Explained");

        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(15);

        // Income & Expenses card
        VBox incomeCard = createTypeCard("Income & Expenses",
                "Income - Salary, wages, business income\n" +
                "Expense - Living costs, bills, discretionary spending\n" +
                "One-off Proceeds - One-time income like property sales or inheritance");

        // Investment Accounts card
        VBox investmentCard = createTypeCard("Investment Accounts",
                "Qualified Assets - 401(k), Traditional IRA (pre-tax)\n" +
                "Roth IRA - After-tax retirement savings\n" +
                "Non-Qualified - Taxable brokerage accounts\n" +
                "Cash - Savings, checking, money market");

        // Contributions card
        VBox contributionsCard = createTypeCard("Contributions",
                "Qualified Contribution - Annual 401(k)/IRA contributions\n" +
                "Roth Contribution - Annual Roth IRA contributions\n" +
                "Life Insurance Premium - Insurance policy payments");

        // Other Assets card
        VBox otherCard = createTypeCard("Other Assets",
                "Real Estate - Property values\n" +
                "Mortgage - Home loan balance\n" +
                "Social Security - Expected SS benefits\n" +
                "Life Insurance Benefit - Death benefit amounts");

        grid.add(incomeCard, 0, 0);
        grid.add(investmentCard, 1, 0);
        grid.add(contributionsCard, 0, 1);
        grid.add(otherCard, 1, 1);

        // Make columns equal width
        ColumnConstraints col1 = new ColumnConstraints();
        col1.setPercentWidth(50);
        ColumnConstraints col2 = new ColumnConstraints();
        col2.setPercentWidth(50);
        grid.getColumnConstraints().addAll(col1, col2);

        section.getChildren().addAll(sectionTitle, grid);
        return section;
    }

    private VBox createTypeCard(String title, String items) {
        VBox card = new VBox(8);
        card.setPadding(new Insets(15));
        card.setStyle("-fx-background-color: #f8f9fa; -fx-background-radius: 8; -fx-border-color: #e9ecef; -fx-border-width: 1; -fx-border-radius: 8;");

        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #2c3e50;");

        VBox itemsBox = new VBox(4);
        for (String item : items.split("\n")) {
            String[] parts = item.split(" - ", 2);
            HBox itemRow = new HBox(5);
            itemRow.setAlignment(Pos.TOP_LEFT);

            Label bullet = new Label("\u2022");
            bullet.setStyle("-fx-text-fill: #4a90d9; -fx-font-size: 12px;");

            TextFlow textFlow = new TextFlow();
            Text boldPart = new Text(parts[0]);
            boldPart.setStyle("-fx-font-weight: bold; -fx-font-size: 12px;");
            Text normalPart = new Text(parts.length > 1 ? " - " + parts[1] : "");
            normalPart.setStyle("-fx-font-size: 12px;");
            textFlow.getChildren().addAll(boldPart, normalPart);

            itemRow.getChildren().addAll(bullet, textFlow);
            itemsBox.getChildren().add(itemRow);
        }

        card.getChildren().addAll(titleLabel, itemsBox);
        return card;
    }

    private VBox createAdvancedAnalysisSection() {
        VBox section = new VBox(15);

        Label sectionTitle = createSectionTitle("Advanced Analysis");

        HBox featuresBox = new HBox(15);

        VBox sensitivityCard = createFeatureCard("Sensitivity Analysis",
                "See how changes in growth rates or inflation affect your long-term projections. Identify potential risks and opportunities.");

        VBox rothCard = createFeatureCard("Roth Conversion Comparison",
                "Compare scenarios with different Roth conversion strategies to optimize your tax situation over time.");

        VBox stateCard = createFeatureCard("State Scenario Analysis",
                "Compare the financial impact of living in different states, including state income tax differences.");

        HBox.setHgrow(sensitivityCard, Priority.ALWAYS);
        HBox.setHgrow(rothCard, Priority.ALWAYS);
        HBox.setHgrow(stateCard, Priority.ALWAYS);

        featuresBox.getChildren().addAll(sensitivityCard, rothCard, stateCard);
        section.getChildren().addAll(sectionTitle, featuresBox);
        return section;
    }

    private VBox createFeatureCard(String title, String description) {
        VBox card = new VBox(8);
        card.setPadding(new Insets(15));
        card.setStyle("-fx-background-color: #f8f9fa; -fx-background-radius: 8; -fx-border-color: #e9ecef; -fx-border-width: 1; -fx-border-radius: 8;");

        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #2c3e50;");

        Label descLabel = new Label(description);
        descLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 12px;");
        descLabel.setWrapText(true);

        card.getChildren().addAll(titleLabel, descLabel);
        return card;
    }

    private VBox createTipsSection() {
        VBox section = new VBox(10);
        section.setPadding(new Insets(20));
        section.setStyle("-fx-background-color: #fff8e6; -fx-background-radius: 8; -fx-border-color: #ffe4a0; -fx-border-width: 1; -fx-border-radius: 8;");

        Label sectionTitle = new Label("Tips");
        sectionTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #2c3e50; -fx-border-color: #f0ad4e; -fx-border-width: 0 0 2 0; -fx-padding: 0 0 8 0;");

        VBox tipsList = new VBox(8);
        tipsList.setPadding(new Insets(10, 0, 0, 0));

        tipsList.getChildren().addAll(
                createTipItem("Use Save to store your data as a file for backup"),
                createTipItem("Use Load to restore a previously saved plan"),
                createTipItem("Entry values should be annual amounts (the system handles projections)"),
                createTipItem("Set end year to a far future date (e.g., 2100) for ongoing income/expenses")
        );

        section.getChildren().addAll(sectionTitle, tipsList);
        return section;
    }

    private HBox createTipItem(String text) {
        HBox item = new HBox(8);
        item.setAlignment(Pos.TOP_LEFT);

        Label bullet = new Label("\u2022");
        bullet.setStyle("-fx-text-fill: #f0ad4e; -fx-font-size: 14px;");

        Label textLabel = new Label(text);
        textLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 13px;");
        textLabel.setWrapText(true);

        item.getChildren().addAll(bullet, textLabel);
        return item;
    }

    private Label createSectionTitle(String text) {
        Label title = new Label(text);
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #2c3e50; -fx-border-color: #4a90d9; -fx-border-width: 0 0 2 0; -fx-padding: 0 0 8 0;");
        return title;
    }

    public VBox getRoot() {
        return root;
    }
}
