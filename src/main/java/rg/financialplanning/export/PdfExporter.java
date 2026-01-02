package rg.financialplanning.export;

import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.AreaBreak;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.AreaBreakType;
import com.itextpdf.layout.properties.HorizontalAlignment;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.StackedXYAreaRenderer2;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.DefaultTableXYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import rg.financialplanning.model.FinancialEntry;
import rg.financialplanning.model.IndividualYearlySummary;
import rg.financialplanning.model.ItemType;
import rg.financialplanning.model.Person;
import rg.financialplanning.model.YearlySummary;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.ToDoubleFunction;

/**
 * Exports yearly summaries to PDF format with organized sections:
 * - Current Inputs and Assumptions
 * - Net Worth Projections (5-year intervals)
 * - Cash Inflows (income, withdrawals, social security)
 * - Cash Outflows (expenses, contributions, taxes, mortgage payments)
 * - Assets (qualified, non-qualified, roth, cash, real estate, life insurance)
 * - Liabilities (mortgage balance)
 * - Net Worth
 *
 * Tables are paginated horizontally to ensure year columns fit within page boundaries.
 */
public class PdfExporter {

    private static final int MAX_YEARS_PER_PAGE = 10;
    private static final DeviceRgb SECTION_HEADER_COLOR = new DeviceRgb(41, 128, 185);
    private static final DeviceRgb ROW_ALTERNATE_COLOR = new DeviceRgb(245, 245, 245);
    private static final DeviceRgb TOTAL_ROW_COLOR = new DeviceRgb(180, 198, 220);
    private static final DeviceRgb SUBSECTION_COLOR = new DeviceRgb(52, 73, 94);
    private static final DeviceRgb NET_WORTH_COLOR = new DeviceRgb(144, 175, 197);

    public void exportYearlySummariesToPdf(YearlySummary[] summaries, String filePath) throws IOException {
        exportYearlySummariesToPdf(summaries, null, Path.of(filePath));
    }

    public void exportYearlySummariesToPdf(YearlySummary[] summaries, Path filePath) throws IOException {
        exportYearlySummariesToPdf(summaries, null, filePath);
    }

    public void exportYearlySummariesToPdf(YearlySummary[] summaries, FinancialPlanInputs inputs, String filePath) throws IOException {
        exportYearlySummariesToPdf(summaries, inputs, Path.of(filePath));
    }

    public void exportYearlySummariesToPdf(YearlySummary[] summaries, FinancialPlanInputs inputs, Path filePath) throws IOException {
        if (summaries == null || summaries.length == 0) {
            throw new IllegalArgumentException("Summaries array cannot be null or empty");
        }

        // Collect all unique individual names across all years
        Set<String> allNames = new LinkedHashSet<>();
        for (YearlySummary summary : summaries) {
            if (summary != null && summary.individualSummaries() != null) {
                allNames.addAll(summary.individualSummaries().keySet());
            }
        }

        try (PdfWriter writer = new PdfWriter(filePath.toFile());
             PdfDocument pdf = new PdfDocument(writer);
             Document document = new Document(pdf, PageSize.LETTER.rotate())) {

            document.setMargins(20, 20, 20, 20);

            // Section 1: Current Inputs and Assumptions (if inputs provided)
            if (inputs != null) {
                addInputsAndAssumptionsSection(document, inputs);
                document.add(new AreaBreak(AreaBreakType.NEXT_PAGE));
            }

            // Section 2: Net Worth Projections (5-year intervals)
            addNetWorthProjectionsSection(document, summaries);
            document.add(new AreaBreak(AreaBreakType.NEXT_PAGE));

            // Section 3: Cash Flows and Assets/Liabilities (paginated yearly tables)
            int totalYears = summaries.length;
            int totalPages = (totalYears + MAX_YEARS_PER_PAGE - 1) / MAX_YEARS_PER_PAGE;

            for (int pageIndex = 0; pageIndex < totalPages; pageIndex++) {
                if (pageIndex > 0) {
                    document.add(new AreaBreak(AreaBreakType.NEXT_PAGE));
                }

                int startIndex = pageIndex * MAX_YEARS_PER_PAGE;
                int endIndex = Math.min(startIndex + MAX_YEARS_PER_PAGE, totalYears);
                YearlySummary[] pageSummaries = Arrays.copyOfRange(summaries, startIndex, endIndex);

                // Title with page info
                PdfFont boldFont = PdfFontFactory.createFont("Helvetica-Bold");
                String titleText = "Cash Flows and Balance Sheet";
                if (totalPages > 1) {
                    int startYear = pageSummaries[0].year();
                    int endYear = pageSummaries[pageSummaries.length - 1].year();
                    titleText += " (Years " + startYear + "-" + endYear + ")";
                }
                Paragraph title = new Paragraph(titleText)
                        .setFont(boldFont)
                        .setFontSize(18)
                        .setTextAlignment(TextAlignment.CENTER)
                        .setMarginBottom(20);
                document.add(title);

                // Build table for this page's years
                Table table = buildTableForYears(pageSummaries, allNames);
                document.add(table);
            }
        }
    }

    private void addInputsAndAssumptionsSection(Document document, FinancialPlanInputs inputs) throws IOException {
        PdfFont boldFont = PdfFontFactory.createFont("Helvetica-Bold");

        // Title
        Paragraph title = new Paragraph("Current Inputs and Assumptions")
                .setFont(boldFont)
                .setFontSize(18)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(20);
        document.add(title);

        // Persons Section
        if (!inputs.persons().isEmpty()) {
            addInputSubsectionHeader(document, "Persons");
            Table personsTable = new Table(UnitValue.createPercentArray(new float[]{2f, 1f, 1f}));
            personsTable.setWidth(UnitValue.createPercentValue(60));
            personsTable.setFontSize(9);

            addTableHeader(personsTable, "Name", "Year of Birth", "Current Age");
            int rowNum = 0;
            for (Person person : inputs.persons()) {
                addInputRow(personsTable, rowNum++, person.name(),
                        String.valueOf(person.yearOfBirth()),
                        String.valueOf(person.getCurrentAge()));
            }
            document.add(personsTable);
            document.add(new Paragraph("\n"));
        }

        // Income Section
        List<FinancialEntry> incomeEntries = filterEntriesByType(inputs.entries(), ItemType.INCOME);
        if (!incomeEntries.isEmpty()) {
            addInputSubsectionHeader(document, "Income");
            Table incomeTable = createEntriesTable();
            int rowNum = 0;
            for (FinancialEntry entry : incomeEntries) {
                addEntryRow(incomeTable, rowNum++, entry);
            }
            document.add(incomeTable);
            document.add(new Paragraph("\n"));
        }

        // Expenses Section
        List<FinancialEntry> expenseEntries = filterEntriesByType(inputs.entries(), ItemType.EXPENSE);
        if (!expenseEntries.isEmpty()) {
            addInputSubsectionHeader(document, "Expenses");
            Table expenseTable = createEntriesTable();
            int rowNum = 0;
            for (FinancialEntry entry : expenseEntries) {
                addEntryRow(expenseTable, rowNum++, entry);
            }
            document.add(expenseTable);
            document.add(new Paragraph("\n"));
        }

        // Assets Section
        List<FinancialEntry> assetEntries = filterEntriesByTypes(inputs.entries(),
                ItemType.QUALIFIED, ItemType.NON_QUALIFIED, ItemType.ROTH, ItemType.CASH,
                ItemType.REAL_ESTATE, ItemType.LIFE_INSURANCE_BENEFIT);
        if (!assetEntries.isEmpty()) {
            addInputSubsectionHeader(document, "Assets");
            Table assetTable = createAssetEntriesTable();
            int rowNum = 0;
            for (FinancialEntry entry : assetEntries) {
                addAssetEntryRow(assetTable, rowNum++, entry);
            }
            document.add(assetTable);
            document.add(new Paragraph("\n"));
        }

        // Mortgage Section
        List<FinancialEntry> mortgageEntries = filterEntriesByTypes(inputs.entries(),
                ItemType.MORTGAGE, ItemType.MORTGAGE_REPAYMENT);
        if (!mortgageEntries.isEmpty()) {
            addInputSubsectionHeader(document, "Mortgage");
            Table mortgageTable = new Table(UnitValue.createPercentArray(new float[]{2f, 1.5f, 1.5f, 1f, 1f}));
            mortgageTable.setWidth(UnitValue.createPercentValue(80));
            mortgageTable.setFontSize(9);

            addTableHeader(mortgageTable, "Type", "Amount/Payment", "Description", "Start Year", "End Year");
            int rowNum = 0;
            for (FinancialEntry entry : mortgageEntries) {
                String typeLabel = entry.item() == ItemType.MORTGAGE ? "Mortgage Balance" : "Extra Principal";
                addInputRow(mortgageTable, rowNum++,
                        typeLabel,
                        formatCurrency(entry.value()),
                        entry.description() != null ? entry.description() : "-",
                        String.valueOf(entry.startYear()),
                        String.valueOf(entry.endYear()));
            }
            document.add(mortgageTable);
            document.add(new Paragraph("\n"));
        }

        // Social Security Section
        List<FinancialEntry> ssEntries = filterEntriesByType(inputs.entries(), ItemType.SOCIAL_SECURITY_BENEFITS);
        if (!ssEntries.isEmpty()) {
            addInputSubsectionHeader(document, "Social Security Benefits");
            Table ssTable = createEntriesTable();
            int rowNum = 0;
            for (FinancialEntry entry : ssEntries) {
                addEntryRow(ssTable, rowNum++, entry);
            }
            document.add(ssTable);
            document.add(new Paragraph("\n"));
        }

        // Contributions Section
        List<FinancialEntry> contribEntries = filterEntriesByTypes(inputs.entries(),
                ItemType.ROTH_CONTRIBUTION, ItemType.QUALIFIED_CONTRIBUTION, ItemType.LIFE_INSURANCE_CONTRIBUTION);
        if (!contribEntries.isEmpty()) {
            addInputSubsectionHeader(document, "Contributions");
            Table contribTable = createAssetEntriesTable();
            int rowNum = 0;
            for (FinancialEntry entry : contribEntries) {
                addAssetEntryRow(contribTable, rowNum++, entry);
            }
            document.add(contribTable);
            document.add(new Paragraph("\n"));
        }

        // Rate Assumptions Section
        if (!inputs.percentageRates().isEmpty()) {
            addInputSubsectionHeader(document, "Rate Assumptions");
            Table ratesTable = new Table(UnitValue.createPercentArray(new float[]{3f, 1f}));
            ratesTable.setWidth(UnitValue.createPercentValue(50));
            ratesTable.setFontSize(9);

            addTableHeader(ratesTable, "Item Type", "Annual Rate");
            int rowNum = 0;
            for (Map.Entry<ItemType, Double> rate : inputs.percentageRates().entrySet()) {
                String rateLabel = formatRateLabel(rate.getKey());
                addInputRow(ratesTable, rowNum++, rateLabel, String.format("%.2f%%", rate.getValue()));
            }
            document.add(ratesTable);
            document.add(new Paragraph("\n"));
        }

        // Tax Assumptions (informational)
        addInputSubsectionHeader(document, "Tax Calculation Notes");
        Paragraph taxNotes = new Paragraph()
                .setFontSize(9)
                .add("- Federal and State income taxes are calculated based on taxable income\n")
                .add("- Social Security and Medicare taxes apply to earned income\n")
                .add("- Capital gains taxes apply to investment withdrawals from non-qualified accounts\n")
                .add("- RMD (Required Minimum Distribution) applies to qualified accounts after age 73");
        document.add(taxNotes);
    }

    private void addNetWorthProjectionsSection(Document document, YearlySummary[] summaries) throws IOException {
        PdfFont boldFont = PdfFontFactory.createFont("Helvetica-Bold");

        // Title
        Paragraph title = new Paragraph("Net Worth Projections")
                .setFont(boldFont)
                .setFontSize(18)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(20);
        document.add(title);

        // Add the chart first
        Image chartImage = createNetWorthChart(summaries);
        chartImage.setHorizontalAlignment(HorizontalAlignment.CENTER);
        chartImage.setMarginBottom(20);
        document.add(chartImage);

        // Build list of years at 5-year intervals for the table
        List<YearlySummary> projectionYears = new ArrayList<>();
        int startYear = summaries[0].year();

        for (int i = 0; i < summaries.length; i++) {
            int yearOffset = summaries[i].year() - startYear;
            if (yearOffset % 5 == 0) {
                projectionYears.add(summaries[i]);
            }
        }
        // Always include the last year if not already included
        YearlySummary lastSummary = summaries[summaries.length - 1];
        if (!projectionYears.contains(lastSummary)) {
            projectionYears.add(lastSummary);
        }

        // Create table
        int columnCount = 1 + projectionYears.size();
        float[] columnWidths = new float[columnCount];
        columnWidths[0] = 3f;
        for (int i = 1; i < columnCount; i++) {
            columnWidths[i] = 1f;
        }

        Table table = new Table(UnitValue.createPercentArray(columnWidths));
        table.setWidth(UnitValue.createPercentValue(100));
        table.setFontSize(9);

        // Header row with years
        Cell headerCell = new Cell()
                .add(new Paragraph("Item"))
                .setBackgroundColor(SECTION_HEADER_COLOR)
                .setFontColor(ColorConstants.WHITE)
                .setTextAlignment(TextAlignment.CENTER)
                .setBold();
        table.addHeaderCell(headerCell);

        for (YearlySummary summary : projectionYears) {
            Cell yearCell = new Cell()
                    .add(new Paragraph(String.valueOf(summary.year())))
                    .setBackgroundColor(SECTION_HEADER_COLOR)
                    .setFontColor(ColorConstants.WHITE)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setBold();
            table.addHeaderCell(yearCell);
        }

        // Assets section
        addProjectionSectionHeader(table, "ASSETS", columnCount);
        int rowNum = 0;
        addProjectionRow(table, "Qualified Assets", projectionYears, YearlySummary::qualifiedAssets, rowNum++);
        addProjectionRow(table, "Non-Qualified Assets", projectionYears, YearlySummary::nonQualifiedAssets, rowNum++);
        addProjectionRow(table, "Roth Assets", projectionYears, YearlySummary::rothAssets, rowNum++);
        addProjectionRow(table, "Cash", projectionYears, YearlySummary::cash, rowNum++);
        addProjectionRow(table, "Real Estate", projectionYears, YearlySummary::realEstate, rowNum++);
        addProjectionRow(table, "Life Insurance Benefits", projectionYears, YearlySummary::lifeInsuranceBenefits, rowNum++);
        addProjectionSummaryRow(table, "TOTAL ASSETS", projectionYears, YearlySummary::totalAssets);

        // Liabilities section
        addProjectionSectionHeader(table, "LIABILITIES", columnCount);
        addProjectionRow(table, "Outstanding Mortgage", projectionYears, YearlySummary::mortgageBalance, rowNum++);
        addProjectionSummaryRow(table, "TOTAL LIABILITIES", projectionYears, YearlySummary::mortgageBalance);

        // Net Worth
        addProjectionSectionHeader(table, "NET WORTH", columnCount);
        addProjectionSummaryRow(table, "NET WORTH", projectionYears, YearlySummary::netWorth);

        document.add(table);
    }

    private Image createNetWorthChart(YearlySummary[] summaries) throws IOException {
        // Create dataset for stacked area chart (assets)
        DefaultTableXYDataset assetDataset = new DefaultTableXYDataset();

        XYSeries qualifiedSeries = new XYSeries("Qualified", false, false);
        XYSeries nonQualifiedSeries = new XYSeries("Non-Qualified", false, false);
        XYSeries rothSeries = new XYSeries("Roth", false, false);
        XYSeries cashSeries = new XYSeries("Cash", false, false);
        XYSeries realEstateSeries = new XYSeries("Real Estate", false, false);
        XYSeries lifeInsuranceSeries = new XYSeries("Life Insurance", false, false);

        for (YearlySummary summary : summaries) {
            int year = summary.year();
            qualifiedSeries.add(year, summary.qualifiedAssets());
            nonQualifiedSeries.add(year, summary.nonQualifiedAssets());
            rothSeries.add(year, summary.rothAssets());
            cashSeries.add(year, summary.cash());
            realEstateSeries.add(year, summary.realEstate());
            lifeInsuranceSeries.add(year, summary.lifeInsuranceBenefits());
        }

        assetDataset.addSeries(qualifiedSeries);
        assetDataset.addSeries(nonQualifiedSeries);
        assetDataset.addSeries(rothSeries);
        assetDataset.addSeries(cashSeries);
        assetDataset.addSeries(realEstateSeries);
        assetDataset.addSeries(lifeInsuranceSeries);

        // Create dataset for line chart (liabilities and net worth)
        DefaultTableXYDataset lineDataset = new DefaultTableXYDataset();

        XYSeries liabilitiesSeries = new XYSeries("Liabilities", false, false);
        XYSeries netWorthSeries = new XYSeries("Net Worth", false, false);

        for (YearlySummary summary : summaries) {
            int year = summary.year();
            liabilitiesSeries.add(year, summary.mortgageBalance());
            netWorthSeries.add(year, summary.netWorth());
        }

        lineDataset.addSeries(liabilitiesSeries);
        lineDataset.addSeries(netWorthSeries);

        // Create axes with larger fonts
        NumberAxis domainAxis = new NumberAxis("Year");
        domainAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
        domainAxis.setAutoRangeIncludesZero(false);
        domainAxis.setLabelFont(new Font("SansSerif", Font.BOLD, 18));
        domainAxis.setTickLabelFont(new Font("SansSerif", Font.PLAIN, 16));
        // Remove commas from year labels
        domainAxis.setNumberFormatOverride(new java.text.DecimalFormat("0"));

        NumberAxis rangeAxis = new NumberAxis("Amount ($)");
        // Format large numbers as abbreviated (e.g., $1M, $500K)
        rangeAxis.setNumberFormatOverride(new java.text.NumberFormat() {
            @Override
            public StringBuffer format(double number, StringBuffer toAppendTo, java.text.FieldPosition pos) {
                if (Math.abs(number) >= 1_000_000) {
                    return toAppendTo.append(String.format("$%.1fM", number / 1_000_000));
                } else if (Math.abs(number) >= 1_000) {
                    return toAppendTo.append(String.format("$%.0fK", number / 1_000));
                } else {
                    return toAppendTo.append(String.format("$%.0f", number));
                }
            }
            @Override
            public StringBuffer format(long number, StringBuffer toAppendTo, java.text.FieldPosition pos) {
                return format((double) number, toAppendTo, pos);
            }
            @Override
            public Number parse(String source, java.text.ParsePosition parsePosition) {
                return null;
            }
        });
        rangeAxis.setLabelFont(new Font("SansSerif", Font.BOLD, 18));
        rangeAxis.setTickLabelFont(new Font("SansSerif", Font.PLAIN, 16));

        // Create stacked area renderer for assets with semi-transparent colors
        StackedXYAreaRenderer2 areaRenderer = new StackedXYAreaRenderer2();
        areaRenderer.setSeriesPaint(0, new Color(46, 139, 87, 200));   // Qualified - Sea Green
        areaRenderer.setSeriesPaint(1, new Color(65, 105, 225, 200));  // Non-Qualified - Royal Blue
        areaRenderer.setSeriesPaint(2, new Color(255, 165, 0, 200));   // Roth - Orange
        areaRenderer.setSeriesPaint(3, new Color(147, 112, 219, 200)); // Cash - Medium Purple
        areaRenderer.setSeriesPaint(4, new Color(210, 105, 30, 200));  // Real Estate - Chocolate
        areaRenderer.setSeriesPaint(5, new Color(60, 179, 113, 200));  // Life Insurance - Medium Sea Green
        // Add outlines to area segments
        areaRenderer.setSeriesOutlinePaint(0, new Color(46, 139, 87).darker());
        areaRenderer.setSeriesOutlinePaint(1, new Color(65, 105, 225).darker());
        areaRenderer.setSeriesOutlinePaint(2, new Color(255, 165, 0).darker());
        areaRenderer.setSeriesOutlinePaint(3, new Color(147, 112, 219).darker());
        areaRenderer.setSeriesOutlinePaint(4, new Color(210, 105, 30).darker());
        areaRenderer.setSeriesOutlinePaint(5, new Color(60, 179, 113).darker());
        areaRenderer.setOutline(true);

        // Create the plot with stacked area dataset
        XYPlot plot = new XYPlot(assetDataset, domainAxis, rangeAxis, areaRenderer);
        plot.setBackgroundPaint(new Color(250, 250, 250));
        plot.setDomainGridlinePaint(new Color(200, 200, 200));
        plot.setRangeGridlinePaint(new Color(200, 200, 200));
        plot.setDomainGridlineStroke(new BasicStroke(0.5f));
        plot.setRangeGridlineStroke(new BasicStroke(0.5f));
        plot.setOutlinePaint(Color.DARK_GRAY);
        plot.setOutlineStroke(new BasicStroke(1.0f));

        // Add line dataset as second dataset
        plot.setDataset(1, lineDataset);

        // Create line renderer for liabilities and net worth with bold lines (no markers)
        XYLineAndShapeRenderer lineRenderer = new XYLineAndShapeRenderer(true, false);
        // Liabilities - Bright Red
        lineRenderer.setSeriesPaint(0, new Color(220, 20, 60));
        lineRenderer.setSeriesStroke(0, new BasicStroke(6.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        // Net Worth - Black with bold line
        lineRenderer.setSeriesPaint(1, Color.BLACK);
        lineRenderer.setSeriesStroke(1, new BasicStroke(7.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        plot.setRenderer(1, lineRenderer);

        // Set rendering order so lines appear on top of areas
        plot.setDatasetRenderingOrder(org.jfree.chart.plot.DatasetRenderingOrder.FORWARD);

        // Create the chart with legend
        JFreeChart chart = new JFreeChart(null, JFreeChart.DEFAULT_TITLE_FONT, plot, true);
        chart.setBackgroundPaint(Color.WHITE);

        // Customize legend with larger font
        chart.getLegend().setItemFont(new Font("SansSerif", Font.BOLD, 16));
        chart.getLegend().setBackgroundPaint(new Color(255, 255, 255, 240));
        chart.getLegend().setFrame(new org.jfree.chart.block.BlockBorder(Color.GRAY));
        chart.getLegend().setPadding(new org.jfree.chart.ui.RectangleInsets(10, 10, 10, 10));

        // Create the chart image at higher resolution
        int width = 1800;
        int height = 900;
        BufferedImage bufferedImage = chart.createBufferedImage(width, height);

        // Convert to byte array
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(bufferedImage, "png", baos);
        byte[] imageBytes = baos.toByteArray();

        // Create iText Image
        return new Image(ImageDataFactory.create(imageBytes));
    }

    private Table buildTableForYears(YearlySummary[] summaries, Set<String> allNames) throws IOException {
        // Calculate column count: 1 for row labels + years
        int columnCount = 1 + summaries.length;
        float[] columnWidths = new float[columnCount];
        columnWidths[0] = 3f; // Wider first column for labels
        for (int i = 1; i < columnCount; i++) {
            columnWidths[i] = 1f;
        }

        Table table = new Table(UnitValue.createPercentArray(columnWidths));
        table.setWidth(UnitValue.createPercentValue(100));
        table.setFontSize(7);

        // Header row with years
        addHeaderRow(table, summaries);

        // Age rows for each individual
        int rowNum = 0;
        for (String name : allNames) {
            addAgeRow(table, name, summaries, rowNum++);
        }

        // === CASH INFLOWS SECTION ===
        addSectionHeader(table, "CASH INFLOWS", columnCount);

        // Income
        addTotalRow(table, "Total Income", summaries, YearlySummary::totalIncome, rowNum++);
        for (String name : allNames) {
            addIndividualRow(table, name + " Income", summaries, name,
                    IndividualYearlySummary::income, rowNum++);
        }

        // Withdrawals
        addTotalRow(table, "Total RMD Withdrawals", summaries, YearlySummary::rmdWithdrawals, rowNum++);
        for (String name : allNames) {
            addIndividualRow(table, name + " RMD Withdrawals", summaries, name,
                    IndividualYearlySummary::rmdWithdrawals, rowNum++);
        }

        addTotalRow(table, "Total Qualified Withdrawals", summaries, YearlySummary::qualifiedWithdrawals, rowNum++);
        for (String name : allNames) {
            addIndividualRow(table, name + " Qualified Withdrawals", summaries, name,
                    IndividualYearlySummary::qualifiedWithdrawals, rowNum++);
        }

        addTotalRow(table, "Total Non-Qualified Withdrawals", summaries, YearlySummary::nonQualifiedWithdrawals, rowNum++);
        for (String name : allNames) {
            addIndividualRow(table, name + " Non-Qualified Withdrawals", summaries, name,
                    IndividualYearlySummary::nonQualifiedWithdrawals, rowNum++);
        }

        addTotalRow(table, "Total Roth Withdrawals", summaries, YearlySummary::rothWithdrawals, rowNum++);
        for (String name : allNames) {
            addIndividualRow(table, name + " Roth Withdrawals", summaries, name,
                    IndividualYearlySummary::rothWithdrawals, rowNum++);
        }

        addTotalRow(table, "Total Cash Withdrawals", summaries, YearlySummary::cashWithdrawals, rowNum++);
        for (String name : allNames) {
            addIndividualRow(table, name + " Cash Withdrawals", summaries, name,
                    IndividualYearlySummary::cashWithdrawals, rowNum++);
        }

        // Social Security Benefits
        addTotalRow(table, "Total Social Security Benefits", summaries, YearlySummary::totalSocialSecurity, rowNum++);
        for (String name : allNames) {
            addIndividualRow(table, name + " Social Security Benefits", summaries, name,
                    IndividualYearlySummary::socialSecurityBenefits, rowNum++);
        }

        // Total Cash Inflows
        addSummaryRow(table, "TOTAL CASH INFLOWS", summaries, YearlySummary::totalCashInflows, columnCount);

        // === CASH OUTFLOWS SECTION ===
        addSectionHeader(table, "CASH OUTFLOWS", columnCount);

        // Expenses
        addTotalRow(table, "Total Expenses", summaries, YearlySummary::totalExpenses, rowNum++);

        // Contributions
        addTotalRow(table, "Total Roth Contributions", summaries, YearlySummary::rothContributions, rowNum++);
        for (String name : allNames) {
            addIndividualRow(table, name + " Roth Contributions", summaries, name,
                    IndividualYearlySummary::rothContributions, rowNum++);
        }

        addTotalRow(table, "Total Qualified Contributions", summaries, YearlySummary::qualifiedContributions, rowNum++);
        for (String name : allNames) {
            addIndividualRow(table, name + " Qualified Contributions", summaries, name,
                    IndividualYearlySummary::qualifiedContributions, rowNum++);
        }

        addTotalRow(table, "Total Non-Qualified Contributions", summaries, YearlySummary::nonQualifiedContributions, rowNum++);
        for (String name : allNames) {
            addIndividualRow(table, name + " Non-Qualified Contributions", summaries, name,
                    IndividualYearlySummary::nonQualifiedContributions, rowNum++);
        }

        // Taxes
        addTotalRow(table, "Total Taxes", summaries, YearlySummary::totalTaxes, rowNum++);
        addDataRow(table, "Federal Income Tax", summaries, YearlySummary::federalIncomeTax, rowNum++);
        addDataRow(table, "State Income Tax", summaries, YearlySummary::stateIncomeTax, rowNum++);
        addDataRow(table, "Capital Gains Tax", summaries, YearlySummary::capitalGainsTax, rowNum++);
        addDataRow(table, "Social Security Tax", summaries, YearlySummary::socialSecurityTax, rowNum++);
        addDataRow(table, "Medicare Tax", summaries, YearlySummary::medicareTax, rowNum++);

        // Mortgage Payments
        addTotalRow(table, "Total Mortgage Payments", summaries,
                s -> s.mortgagePayment() + s.mortgageRepayment(), rowNum++);
        addDataRow(table, "Mortgage Payment", summaries, YearlySummary::mortgagePayment, rowNum++);
        addDataRow(table, "Mortgage Repayment (Extra Principal)", summaries, YearlySummary::mortgageRepayment, rowNum++);

        // Total Cash Outflows
        addSummaryRow(table, "TOTAL CASH OUTFLOWS", summaries, YearlySummary::totalCashOutflows, columnCount);

        // Deficit
        addDataRow(table, "Deficit", summaries, YearlySummary::deficit, rowNum++);

        // === ASSETS SECTION ===
        addSectionHeader(table, "ASSETS", columnCount);

        addTotalRow(table, "Total Qualified Assets", summaries, YearlySummary::qualifiedAssets, rowNum++);
        for (String name : allNames) {
            addIndividualRow(table, name + " Qualified Assets", summaries, name,
                    IndividualYearlySummary::qualifiedAssets, rowNum++);
        }

        addTotalRow(table, "Total Non-Qualified Assets", summaries, YearlySummary::nonQualifiedAssets, rowNum++);
        for (String name : allNames) {
            addIndividualRow(table, name + " Non-Qualified Assets", summaries, name,
                    IndividualYearlySummary::nonQualifiedAssets, rowNum++);
        }

        addTotalRow(table, "Total Roth Assets", summaries, YearlySummary::rothAssets, rowNum++);
        for (String name : allNames) {
            addIndividualRow(table, name + " Roth Assets", summaries, name,
                    IndividualYearlySummary::rothAssets, rowNum++);
        }

        addTotalRow(table, "Total Cash", summaries, YearlySummary::cash, rowNum++);
        addTotalRow(table, "Total Real Estate", summaries, YearlySummary::realEstate, rowNum++);
        addTotalRow(table, "Total Life Insurance Benefits", summaries, YearlySummary::lifeInsuranceBenefits, rowNum++);

        // Total Assets
        addSummaryRow(table, "TOTAL ASSETS", summaries, YearlySummary::totalAssets, columnCount);

        // === LIABILITIES SECTION ===
        addSectionHeader(table, "LIABILITIES", columnCount);

        addTotalRow(table, "Total Liabilities", summaries, YearlySummary::mortgageBalance, rowNum++);
        addDataRow(table, "Outstanding Mortgage Balance", summaries, YearlySummary::mortgageBalance, rowNum++);

        // === NET WORTH SECTION ===
        addSectionHeader(table, "NET WORTH", columnCount);

        addSummaryRow(table, "NET WORTH", summaries, YearlySummary::netWorth, columnCount);

        return table;
    }

    // ===== Helper methods for Inputs and Assumptions section =====

    private void addInputSubsectionHeader(Document document, String title) throws IOException {
        PdfFont boldFont = PdfFontFactory.createFont("Helvetica-Bold");
        Paragraph header = new Paragraph(title)
                .setFont(boldFont)
                .setFontSize(12)
                .setFontColor(SUBSECTION_COLOR)
                .setMarginTop(10)
                .setMarginBottom(5);
        document.add(header);
    }

    private Table createEntriesTable() {
        Table table = new Table(UnitValue.createPercentArray(new float[]{1.5f, 2f, 1.5f, 1f, 1f}));
        table.setWidth(UnitValue.createPercentValue(80));
        table.setFontSize(9);
        addTableHeader(table, "Person", "Description", "Annual Amount", "Start Year", "End Year");
        return table;
    }

    private Table createAssetEntriesTable() {
        Table table = new Table(UnitValue.createPercentArray(new float[]{1.5f, 1.5f, 2f, 1.5f, 1f, 1f}));
        table.setWidth(UnitValue.createPercentValue(90));
        table.setFontSize(9);
        addTableHeader(table, "Person", "Type", "Description", "Amount", "Start Year", "End Year");
        return table;
    }

    private void addTableHeader(Table table, String... headers) {
        for (String header : headers) {
            Cell cell = new Cell()
                    .add(new Paragraph(header))
                    .setBackgroundColor(SECTION_HEADER_COLOR)
                    .setFontColor(ColorConstants.WHITE)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setBold();
            table.addHeaderCell(cell);
        }
    }

    private void addInputRow(Table table, int rowNum, String... values) {
        DeviceRgb bgColor = (rowNum % 2 == 0) ? null : ROW_ALTERNATE_COLOR;
        for (String value : values) {
            Cell cell = new Cell()
                    .add(new Paragraph(value))
                    .setTextAlignment(TextAlignment.LEFT);
            if (bgColor != null) cell.setBackgroundColor(bgColor);
            table.addCell(cell);
        }
    }

    private void addEntryRow(Table table, int rowNum, FinancialEntry entry) {
        addInputRow(table, rowNum,
                entry.name() != null ? entry.name() : "-",
                entry.description() != null ? entry.description() : "-",
                formatCurrency(entry.value()),
                String.valueOf(entry.startYear()),
                String.valueOf(entry.endYear()));
    }

    private void addAssetEntryRow(Table table, int rowNum, FinancialEntry entry) {
        addInputRow(table, rowNum,
                entry.name() != null ? entry.name() : "-",
                formatItemType(entry.item()),
                entry.description() != null ? entry.description() : "-",
                formatCurrency(entry.value()),
                String.valueOf(entry.startYear()),
                String.valueOf(entry.endYear()));
    }

    private List<FinancialEntry> filterEntriesByType(List<FinancialEntry> entries, ItemType type) {
        List<FinancialEntry> result = new ArrayList<>();
        for (FinancialEntry entry : entries) {
            if (entry.item() == type) {
                result.add(entry);
            }
        }
        return result;
    }

    private List<FinancialEntry> filterEntriesByTypes(List<FinancialEntry> entries, ItemType... types) {
        List<FinancialEntry> result = new ArrayList<>();
        Set<ItemType> typeSet = Set.of(types);
        for (FinancialEntry entry : entries) {
            if (typeSet.contains(entry.item())) {
                result.add(entry);
            }
        }
        return result;
    }

    private String formatItemType(ItemType type) {
        return switch (type) {
            case QUALIFIED -> "Qualified (401k/IRA)";
            case NON_QUALIFIED -> "Non-Qualified";
            case ROTH -> "Roth";
            case CASH -> "Cash";
            case REAL_ESTATE -> "Real Estate";
            case LIFE_INSURANCE_BENEFIT -> "Life Insurance";
            case ROTH_CONTRIBUTION -> "Roth Contribution";
            case QUALIFIED_CONTRIBUTION -> "Qualified Contribution";
            case LIFE_INSURANCE_CONTRIBUTION -> "Life Ins Contribution";
            default -> type.name();
        };
    }

    private String formatRateLabel(ItemType type) {
        return switch (type) {
            case INCOME -> "Income Growth Rate";
            case EXPENSE -> "Expense Inflation Rate";
            case QUALIFIED -> "Qualified Assets Growth Rate";
            case NON_QUALIFIED -> "Non-Qualified Assets Growth Rate";
            case ROTH -> "Roth Assets Growth Rate";
            case CASH -> "Cash Growth Rate";
            case REAL_ESTATE -> "Real Estate Appreciation Rate";
            case MORTGAGE -> "Mortgage Interest Rate";
            case SOCIAL_SECURITY_BENEFITS -> "Social Security COLA";
            default -> type.name() + " Rate";
        };
    }

    private String formatCurrency(double value) {
        return String.format("$%,.0f", value);
    }

    // ===== Helper methods for Net Worth Projections section =====

    private void addProjectionSectionHeader(Table table, String sectionName, int columnCount) {
        Cell sectionCell = new Cell(1, columnCount)
                .add(new Paragraph(sectionName))
                .setBackgroundColor(SUBSECTION_COLOR)
                .setFontColor(ColorConstants.WHITE)
                .setTextAlignment(TextAlignment.LEFT)
                .setBold()
                .setPaddingLeft(5);
        table.addCell(sectionCell);
    }

    private void addProjectionRow(Table table, String label, List<YearlySummary> summaries,
                                   ToDoubleFunction<YearlySummary> valueExtractor, int rowNum) {
        DeviceRgb bgColor = (rowNum % 2 == 0) ? null : ROW_ALTERNATE_COLOR;

        Cell labelCell = new Cell()
                .add(new Paragraph(label))
                .setTextAlignment(TextAlignment.LEFT)
                .setPaddingLeft(10);
        if (bgColor != null) labelCell.setBackgroundColor(bgColor);
        table.addCell(labelCell);

        for (YearlySummary summary : summaries) {
            Cell valueCell = new Cell()
                    .add(new Paragraph(formatCurrency(valueExtractor.applyAsDouble(summary))))
                    .setTextAlignment(TextAlignment.RIGHT);
            if (bgColor != null) valueCell.setBackgroundColor(bgColor);
            table.addCell(valueCell);
        }
    }

    private void addProjectionSummaryRow(Table table, String label, List<YearlySummary> summaries,
                                          ToDoubleFunction<YearlySummary> valueExtractor) {
        Cell labelCell = new Cell()
                .add(new Paragraph(label))
                .setBackgroundColor(TOTAL_ROW_COLOR)
                .setTextAlignment(TextAlignment.LEFT)
                .setBold()
                .setPaddingLeft(5);
        table.addCell(labelCell);

        for (YearlySummary summary : summaries) {
            Cell valueCell = new Cell()
                    .add(new Paragraph(formatCurrency(valueExtractor.applyAsDouble(summary))))
                    .setBackgroundColor(TOTAL_ROW_COLOR)
                    .setTextAlignment(TextAlignment.RIGHT)
                    .setBold();
            table.addCell(valueCell);
        }
    }

    // ===== Existing helper methods for yearly tables =====

    private void addHeaderRow(Table table, YearlySummary[] summaries) {
        Cell headerCell = new Cell()
                .add(new Paragraph("Item"))
                .setBackgroundColor(SECTION_HEADER_COLOR)
                .setFontColor(ColorConstants.WHITE)
                .setTextAlignment(TextAlignment.CENTER)
                .setBold();
        table.addHeaderCell(headerCell);

        for (YearlySummary summary : summaries) {
            if (summary != null) {
                Cell yearCell = new Cell()
                        .add(new Paragraph(String.valueOf(summary.year())))
                        .setBackgroundColor(SECTION_HEADER_COLOR)
                        .setFontColor(ColorConstants.WHITE)
                        .setTextAlignment(TextAlignment.CENTER)
                        .setBold();
                table.addHeaderCell(yearCell);
            }
        }
    }

    private void addSectionHeader(Table table, String sectionName, int columnCount) {
        Cell sectionCell = new Cell(1, columnCount)
                .add(new Paragraph(sectionName))
                .setBackgroundColor(SUBSECTION_COLOR)
                .setFontColor(ColorConstants.WHITE)
                .setTextAlignment(TextAlignment.LEFT)
                .setBold()
                .setPaddingLeft(5);
        table.addCell(sectionCell);
    }

    private void addTotalRow(Table table, String label, YearlySummary[] summaries,
                              ToDoubleFunction<YearlySummary> valueExtractor, int rowNum) throws IOException {
        PdfFont boldFont = PdfFontFactory.createFont("Helvetica-Bold");
        Border topBorder = new SolidBorder(ColorConstants.GRAY, 0.5f);

        Cell labelCell = new Cell()
                .add(new Paragraph(label).setFont(boldFont))
                .setBackgroundColor(TOTAL_ROW_COLOR)
                .setTextAlignment(TextAlignment.LEFT)
                .setPaddingLeft(10)
                .setBorderTop(topBorder)
                .setBorderBottom(Border.NO_BORDER)
                .setBorderLeft(Border.NO_BORDER)
                .setBorderRight(Border.NO_BORDER);
        table.addCell(labelCell);

        for (YearlySummary summary : summaries) {
            if (summary != null) {
                Cell valueCell = new Cell()
                        .add(new Paragraph(formatValue(valueExtractor.applyAsDouble(summary))).setFont(boldFont))
                        .setBackgroundColor(TOTAL_ROW_COLOR)
                        .setTextAlignment(TextAlignment.RIGHT)
                        .setBorderTop(topBorder)
                        .setBorderBottom(Border.NO_BORDER)
                        .setBorderLeft(Border.NO_BORDER)
                        .setBorderRight(Border.NO_BORDER);
                table.addCell(valueCell);
            }
        }
    }

    private void addDataRow(Table table, String label, YearlySummary[] summaries,
                             ToDoubleFunction<YearlySummary> valueExtractor, int rowNum) {
        DeviceRgb bgColor = (rowNum % 2 == 0) ? null : ROW_ALTERNATE_COLOR;

        Cell labelCell = new Cell()
                .add(new Paragraph(label))
                .setTextAlignment(TextAlignment.LEFT)
                .setPaddingLeft(10)
                .setBorder(Border.NO_BORDER);
        if (bgColor != null) labelCell.setBackgroundColor(bgColor);
        table.addCell(labelCell);

        for (YearlySummary summary : summaries) {
            if (summary != null) {
                Cell valueCell = new Cell()
                        .add(new Paragraph(formatValue(valueExtractor.applyAsDouble(summary))))
                        .setTextAlignment(TextAlignment.RIGHT)
                        .setBorder(Border.NO_BORDER);
                if (bgColor != null) valueCell.setBackgroundColor(bgColor);
                table.addCell(valueCell);
            }
        }
    }

    private void addIndividualRow(Table table, String label, YearlySummary[] summaries,
                                   String individualName,
                                   ToDoubleFunction<IndividualYearlySummary> valueExtractor, int rowNum) {
        DeviceRgb bgColor = (rowNum % 2 == 0) ? null : ROW_ALTERNATE_COLOR;

        Cell labelCell = new Cell()
                .add(new Paragraph("  " + label))
                .setTextAlignment(TextAlignment.LEFT)
                .setPaddingLeft(20)
                .setBorder(Border.NO_BORDER);
        if (bgColor != null) labelCell.setBackgroundColor(bgColor);
        table.addCell(labelCell);

        for (YearlySummary summary : summaries) {
            double value = 0.0;
            if (summary != null) {
                IndividualYearlySummary individual = summary.getIndividualSummary(individualName);
                if (individual != null) {
                    value = valueExtractor.applyAsDouble(individual);
                }
            }
            Cell valueCell = new Cell()
                    .add(new Paragraph(formatValue(value)))
                    .setTextAlignment(TextAlignment.RIGHT)
                    .setBorder(Border.NO_BORDER);
            if (bgColor != null) valueCell.setBackgroundColor(bgColor);
            table.addCell(valueCell);
        }
    }

    private void addAgeRow(Table table, String name, YearlySummary[] summaries, int rowNum) {
        DeviceRgb bgColor = (rowNum % 2 == 0) ? null : ROW_ALTERNATE_COLOR;

        Cell labelCell = new Cell()
                .add(new Paragraph(name + " Age"))
                .setTextAlignment(TextAlignment.LEFT)
                .setPaddingLeft(10)
                .setBorder(Border.NO_BORDER);
        if (bgColor != null) labelCell.setBackgroundColor(bgColor);
        table.addCell(labelCell);

        for (YearlySummary summary : summaries) {
            String ageStr = "";
            if (summary != null) {
                IndividualYearlySummary individual = summary.getIndividualSummary(name);
                if (individual != null && individual.person() != null) {
                    ageStr = String.valueOf(individual.person().getAgeInYear(summary.year()));
                }
            }
            Cell valueCell = new Cell()
                    .add(new Paragraph(ageStr))
                    .setTextAlignment(TextAlignment.RIGHT)
                    .setBorder(Border.NO_BORDER);
            if (bgColor != null) valueCell.setBackgroundColor(bgColor);
            table.addCell(valueCell);
        }
    }

    private void addSummaryRow(Table table, String label, YearlySummary[] summaries,
                                ToDoubleFunction<YearlySummary> valueExtractor, int columnCount) throws IOException {
        PdfFont boldFont = PdfFontFactory.createFont("Helvetica-Bold");

        // Use darker color for NET WORTH row
        DeviceRgb bgColor = label.contains("NET WORTH") ? NET_WORTH_COLOR : TOTAL_ROW_COLOR;

        Cell labelCell = new Cell()
                .add(new Paragraph(label).setFont(boldFont))
                .setBackgroundColor(bgColor)
                .setTextAlignment(TextAlignment.LEFT)
                .setPaddingLeft(5);
        table.addCell(labelCell);

        for (YearlySummary summary : summaries) {
            if (summary != null) {
                Cell valueCell = new Cell()
                        .add(new Paragraph(formatValue(valueExtractor.applyAsDouble(summary))).setFont(boldFont))
                        .setBackgroundColor(bgColor)
                        .setTextAlignment(TextAlignment.RIGHT);
                table.addCell(valueCell);
            }
        }
    }

    private String formatValue(double value) {
        if (value == 0.0) {
            return "-";
        }
        return String.format("%,.0f", value);
    }
}
