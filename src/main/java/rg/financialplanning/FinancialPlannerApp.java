package rg.financialplanning;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import rg.financialplanning.ui.MainController;

/**
 * JavaFX Application entry point for the Financial Planner.
 * Provides a graphical interface for entering financial data and generating PDF reports.
 */
public class FinancialPlannerApp extends Application {

    @Override
    public void start(Stage primaryStage) {
        MainController mainController = new MainController(primaryStage);

        Scene scene = new Scene(mainController.getRoot(), 1200, 800);
        scene.getStylesheets().add(getClass().getResource("/styles/main.css") != null
            ? getClass().getResource("/styles/main.css").toExternalForm()
            : "");

        primaryStage.setTitle("Financial Planner");
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(900);
        primaryStage.setMinHeight(600);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
