package rg.financialplanning.calculator;

import rg.financialplanning.model.FilingStatus;
import rg.financialplanning.model.YearlySummary;

/**
 * Interface for tax calculators that compute tax based on yearly financial summaries.
 * Each implementation can define its own method for calculating ordinary/taxable income
 * from the YearlySummary.
 */
public interface TaxCalculator {

    /**
     * Calculates the tax for the given yearly summary and filing status.
     * The implementation determines how to calculate taxable income from the summary.
     *
     * @param summary the yearly financial summary
     * @param filingStatus the filing status
     * @return the calculated tax amount
     */
    double calculateTax(YearlySummary summary, FilingStatus filingStatus);

    /**
     * Calculates the taxable/ordinary income from a yearly summary.
     * Each tax jurisdiction may have different rules for what constitutes taxable income.
     *
     * @param summary the yearly financial summary
     * @return the calculated ordinary/taxable income
     */
    double calculateOrdinaryIncome(YearlySummary summary);
}
