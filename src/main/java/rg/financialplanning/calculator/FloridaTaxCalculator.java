package rg.financialplanning.calculator;

import rg.financialplanning.model.FilingStatus;
import rg.financialplanning.model.YearlySummary;

/**
 * Calculates Florida state income tax.
 * Florida does not have a state income tax, so this calculator always returns 0.
 */
public class FloridaTaxCalculator implements TaxCalculator {

    /**
     * Calculates the Florida state income tax for the given yearly summary.
     * Florida has no state income tax, so this always returns 0.
     *
     * @param summary the yearly financial summary
     * @param filingStatus the filing status (not used as FL has no income tax)
     * @return 0 (Florida has no state income tax)
     */
    @Override
    public double calculateTax(YearlySummary summary, FilingStatus filingStatus) {
        return 0;
    }

    /**
     * Calculates the Florida ordinary/taxable income from a yearly summary.
     * Since Florida has no income tax, this returns 0.
     *
     * @param summary the yearly financial summary
     * @return 0 (Florida has no state income tax)
     */
    @Override
    public double calculateOrdinaryIncome(YearlySummary summary) {
        return 0;
    }

    /**
     * Calculates the Florida state income tax for the given taxable income.
     * Florida has no state income tax, so this always returns 0.
     *
     * @param taxableIncome the taxable income (not used)
     * @param filingStatus the filing status (not used)
     * @return 0 (Florida has no state income tax)
     */
    public double calculateTax(double taxableIncome, FilingStatus filingStatus) {
        return 0;
    }

    /**
     * Returns the effective tax rate for Florida.
     * Florida has no state income tax, so this always returns 0.
     *
     * @param taxableIncome the taxable income (not used)
     * @param filingStatus the filing status (not used)
     * @return 0 (Florida has no state income tax)
     */
    public double getEffectiveTaxRate(double taxableIncome, FilingStatus filingStatus) {
        return 0;
    }

    /**
     * Returns the marginal tax rate for Florida.
     * Florida has no state income tax, so this always returns 0.
     *
     * @param taxableIncome the taxable income (not used)
     * @param filingStatus the filing status (not used)
     * @return 0 (Florida has no state income tax)
     */
    public double getMarginalTaxRate(double taxableIncome, FilingStatus filingStatus) {
        return 0;
    }
}
