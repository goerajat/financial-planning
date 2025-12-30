package rg.financialplanning.parser;

import rg.financialplanning.model.ItemType;
import rg.financialplanning.model.ItemTypePercentage;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Parser for reading ItemType percentage data from a CSV file.
 * Expected format: item_type,percentage_increase
 */
public class ItemTypePercentageParser {

    private final Map<ItemType, Double> percentagesByType;

    public ItemTypePercentageParser() {
        this.percentagesByType = new HashMap<>();
    }

    public void loadFromCsv(String filePath) throws IOException {
        loadFromCsv(Path.of(filePath));
    }

    public void loadFromCsv(Path filePath) throws IOException {
        percentagesByType.clear();

        try (BufferedReader reader = Files.newBufferedReader(filePath)) {
            String headerLine = reader.readLine();
            if (headerLine == null) {
                throw new IOException("CSV file is empty");
            }

            String line;
            int lineNumber = 1;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (line.isBlank()) {
                    continue;
                }
                try {
                    ItemTypePercentage itemTypePercentage = parseLine(line);
                    percentagesByType.put(itemTypePercentage.itemType(), itemTypePercentage.percentageIncrease());
                } catch (Exception e) {
                    throw new IOException("Error parsing line " + lineNumber + ": " + e.getMessage(), e);
                }
            }
        }
    }

    private ItemTypePercentage parseLine(String line) {
        String[] parts = line.split(",", -1);
        if (parts.length < 2) {
            throw new IllegalArgumentException("Expected 2 columns (item_type, percentage_increase), found " + parts.length);
        }

        String itemTypeStr = parts[0].trim();
        String percentageStr = parts[1].trim();

        if (itemTypeStr.isEmpty()) {
            throw new IllegalArgumentException("Item type cannot be empty");
        }
        if (percentageStr.isEmpty()) {
            throw new IllegalArgumentException("Percentage increase cannot be empty");
        }

        ItemType itemType = ItemType.fromString(itemTypeStr);
        double percentageIncrease = Double.parseDouble(percentageStr);

        return new ItemTypePercentage(itemType, percentageIncrease);
    }

    public Map<ItemType, Double> getPercentagesByType() {
        return Map.copyOf(percentagesByType);
    }

    public Double getPercentageForType(ItemType itemType) {
        return percentagesByType.get(itemType);
    }

    public double getPercentageForTypeOrDefault(ItemType itemType, double defaultValue) {
        return percentagesByType.getOrDefault(itemType, defaultValue);
    }
}
