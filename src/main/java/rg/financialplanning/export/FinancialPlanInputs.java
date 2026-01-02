package rg.financialplanning.export;

import rg.financialplanning.model.FinancialEntry;
import rg.financialplanning.model.ItemType;
import rg.financialplanning.model.Person;

import java.util.List;
import java.util.Map;

/**
 * Container for all inputs used in generating a financial plan.
 * Used by PdfExporter to generate the "Current Inputs and Assumptions" section.
 */
public record FinancialPlanInputs(
    List<FinancialEntry> entries,
    List<Person> persons,
    Map<ItemType, Double> percentageRates
) {
    public FinancialPlanInputs {
        entries = entries != null ? List.copyOf(entries) : List.of();
        persons = persons != null ? List.copyOf(persons) : List.of();
        percentageRates = percentageRates != null ? Map.copyOf(percentageRates) : Map.of();
    }
}
