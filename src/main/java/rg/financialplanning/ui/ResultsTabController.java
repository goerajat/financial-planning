package rg.financialplanning.ui;

import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

/**
 * Controller for the Results tab.
 * Displays the generated PDF and allows saving to a file.
 */
public class ResultsTabController {

    private final VBox root;
    private final Stage primaryStage;
    private final ScrollPane scrollPane;
    private final VBox pagesContainer;
    private final Label statusLabel;
    private final Button saveButton;
    private final Slider zoomSlider;
    private final Label zoomLabel;

    private File currentPdfFile;
    private List<Image> renderedPages = new ArrayList<>();
    private double currentZoom = 1.0;

    private static final float DEFAULT_DPI = 150f;

    public ResultsTabController(Stage primaryStage) {
        this.primaryStage = primaryStage;
        this.root = new VBox(10);
        this.scrollPane = new ScrollPane();
        this.pagesContainer = new VBox(20);
        this.statusLabel = new Label("No plan generated yet. Use the other tabs to enter data, then click 'Generate Plan'.");
        this.saveButton = new Button("Save PDF");
        this.zoomSlider = new Slider(0.5, 2.0, 1.0);
        this.zoomLabel = new Label("100%");

        initializeUI();
    }

    private void initializeUI() {
        root.setPadding(new Insets(15));

        // Header
        Label headerLabel = new Label("Financial Plan Results");
        headerLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        // Toolbar
        HBox toolbar = createToolbar();

        // PDF viewer
        pagesContainer.setAlignment(Pos.TOP_CENTER);
        pagesContainer.setPadding(new Insets(10));
        pagesContainer.setStyle("-fx-background-color: #e0e0e0;");

        scrollPane.setContent(pagesContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setPannable(true);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        // Status bar
        HBox statusBar = new HBox(10);
        statusBar.setAlignment(Pos.CENTER_LEFT);
        statusBar.setPadding(new Insets(5, 0, 0, 0));
        statusLabel.setStyle("-fx-text-fill: #666;");
        statusBar.getChildren().add(statusLabel);

        // Initially show placeholder
        showPlaceholder();

        root.getChildren().addAll(headerLabel, toolbar, scrollPane, statusBar);
    }

    private HBox createToolbar() {
        HBox toolbar = new HBox(15);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setPadding(new Insets(5, 0, 10, 0));

        // Save button
        saveButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;");
        saveButton.setOnAction(e -> savePdf());
        saveButton.setDisable(true);

        // Zoom controls
        Label zoomTitle = new Label("Zoom:");

        zoomSlider.setShowTickLabels(false);
        zoomSlider.setShowTickMarks(false);
        zoomSlider.setPrefWidth(150);
        zoomSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            currentZoom = newVal.doubleValue();
            zoomLabel.setText(String.format("%.0f%%", currentZoom * 100));
            updatePageDisplay();
        });

        Button zoomFitButton = new Button("Fit Width");
        zoomFitButton.setOnAction(e -> {
            zoomSlider.setValue(1.0);
        });

        Button zoomActualButton = new Button("100%");
        zoomActualButton.setOnAction(e -> {
            zoomSlider.setValue(1.0);
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        toolbar.getChildren().addAll(saveButton, spacer, zoomTitle, zoomSlider, zoomLabel, zoomFitButton);
        return toolbar;
    }

    private void showPlaceholder() {
        pagesContainer.getChildren().clear();

        VBox placeholder = new VBox(20);
        placeholder.setAlignment(Pos.CENTER);
        placeholder.setPadding(new Insets(100));

        Label icon = new Label("\uD83D\uDCC4"); // Document emoji
        icon.setStyle("-fx-font-size: 48px;");

        Label message = new Label("No plan generated yet");
        message.setStyle("-fx-font-size: 16px; -fx-text-fill: #666;");

        Label instructions = new Label("1. Add persons in the 'Persons' tab\n" +
                "2. Add financial entries in the 'Financial Entries' tab\n" +
                "3. Configure rates in the 'Rates' tab\n" +
                "4. Click 'Generate Plan' at the bottom");
        instructions.setStyle("-fx-text-fill: #888;");

        placeholder.getChildren().addAll(icon, message, instructions);
        pagesContainer.getChildren().add(placeholder);
    }

    public void displayPdf(File pdfFile) {
        this.currentPdfFile = pdfFile;
        renderedPages.clear();

        try (PDDocument document = PDDocument.load(pdfFile)) {
            PDFRenderer renderer = new PDFRenderer(document);
            int pageCount = document.getNumberOfPages();

            for (int i = 0; i < pageCount; i++) {
                BufferedImage bufferedImage = renderer.renderImageWithDPI(i, DEFAULT_DPI);
                Image fxImage = SwingFXUtils.toFXImage(bufferedImage, null);
                renderedPages.add(fxImage);
            }

            updatePageDisplay();
            saveButton.setDisable(false);
            statusLabel.setText("Plan generated: " + pageCount + " pages");

        } catch (IOException e) {
            showError("Failed to render PDF: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void updatePageDisplay() {
        pagesContainer.getChildren().clear();

        for (int i = 0; i < renderedPages.size(); i++) {
            Image page = renderedPages.get(i);

            ImageView imageView = new ImageView(page);
            imageView.setPreserveRatio(true);
            imageView.setFitWidth(page.getWidth() * currentZoom);

            // Page container with shadow effect
            VBox pageContainer = new VBox(5);
            pageContainer.setAlignment(Pos.CENTER);
            pageContainer.setStyle("-fx-background-color: white; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.2), 10, 0, 0, 3);");
            pageContainer.setPadding(new Insets(10));

            Label pageLabel = new Label("Page " + (i + 1) + " of " + renderedPages.size());
            pageLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 10px;");

            pageContainer.getChildren().addAll(imageView, pageLabel);
            pagesContainer.getChildren().add(pageContainer);
        }
    }

    private void savePdf() {
        if (currentPdfFile == null || !currentPdfFile.exists()) {
            showError("No PDF file available to save.");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Financial Plan");
        fileChooser.setInitialFileName("FinancialPlan.pdf");
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("PDF Files", "*.pdf")
        );

        File saveFile = fileChooser.showSaveDialog(primaryStage);
        if (saveFile != null) {
            try {
                Files.copy(currentPdfFile.toPath(), saveFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                statusLabel.setText("Plan saved to: " + saveFile.getName());

                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Save Successful");
                alert.setHeaderText(null);
                alert.setContentText("Financial plan saved to:\n" + saveFile.getAbsolutePath());
                alert.showAndWait();

            } catch (IOException e) {
                showError("Failed to save PDF: " + e.getMessage());
            }
        }
    }

    private void showError(String message) {
        statusLabel.setText("Error: " + message);
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public VBox getRoot() {
        return root;
    }
}
