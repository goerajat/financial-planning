package rg.financialplanning.strategy;

import rg.financialplanning.calculator.FederalTaxCalculator;
import rg.financialplanning.calculator.LongTermCapitalGainsCalculator;
import rg.financialplanning.calculator.NJStateTaxCalculator;
import rg.financialplanning.calculator.SocialSecurityTaxCalculator;
import rg.financialplanning.model.FilingStatus;
import rg.financialplanning.model.YearlySummary;

import static rg.financialplanning.calculator.LongTermCapitalGainsCalculator.DEFAULT_COST_BASIS_FACTOR;

/**
 * Strategy to calculate and store all taxes in the YearlySummary.
 *
 * This strategy runs AFTER all other optimization strategies have completed,
 * so it calculates taxes based on the final state of the YearlySummary including:
 * - RMD withdrawals
 * - Qualified withdrawals
 * - Non-qualified withdrawals (capital gains)
 * - Roth conversions
 * - Expense management adjustments
 *
 * Taxes calculated and stored:
 * - Federal income tax (on ordinary income: wages, RMD, qualified withdrawals, 85% SS, Roth conversions)
 * - State income tax (NJ state tax on same ordinary income)
 * - Social Security tax (6.2% of earned income up to wage base)
 * - Medicare tax (1.45% of all earned income, plus additional 0.9% for high earners)
 * - Capital gains tax (on non-qualified withdrawals, assuming appreciation)
 */
public class TaxCalculationStrategy implements TaxOptimizationStrategy {

    private final FederalTaxCalculator federalTaxCalculator;
    private final NJStateTaxCalculator stateTaxCalculator;
    private final SocialSecurityTaxCalculator socialSecurityTaxCalculator;
    private final LongTermCapitalGainsCalculator capitalGainsCalculator;
    private final FilingStatus filingStatus;

    /**
     * Creates a tax calculation strategy with default filing status (Married Filing Jointly).
     */
    public TaxCalculationStrategy() {
        this(FilingStatus.MARRIED_FILING_JOINTLY);
    }

    /**
     * Creates a tax calculation strategy with the specified filing status.
     *
     * @param filingStatus the filing status for tax calculations
     */
    public TaxCalculationStrategy(FilingStatus filingStatus) {
        this.federalTaxCalculator = new FederalTaxCalculator();
        this.stateTaxCalculator = new NJStateTaxCalculator();
        this.socialSecurityTaxCalculator = new SocialSecurityTaxCalculator();
        this.capitalGainsCalculator = new LongTermCapitalGainsCalculator();
        this.filingStatus = filingStatus;
    }

    @Override
    public void optimize(YearlySummary previousYearlySummary, YearlySummary currentYearlySummary) {
        if (currentYearlySummary == null) {
            return;
        }

        // Calculate and store all taxes
        calculateAndStoreTaxes(currentYearlySummary);
    }

    /**
     * Calculates all taxes and stores them in the YearlySummary.
     *
     * @param summary the yearly summary to update with tax calculations
     */
    private void calculateAndStoreTaxes(YearlySummary summary) {
        // Calculate gross taxable income for federal and state income taxes
        double ordinaryIncome = calculateOrdinaryIncome(summary);

        // Federal income tax
        double federalIncomeTax = federalTaxCalculator.calculateTax(ordinaryIncome, filingStatus);
        summary.setFederalIncomeTax(federalIncomeTax);

        // State income tax (NJ)
        double stateIncomeTax = stateTaxCalculator.calculateTax(ordinaryIncome, filingStatus);
        summary.setStateIncomeTax(stateIncomeTax);

        // Social Security and Medicare taxes are based on earned income only
        double earnedIncome = summary.totalIncome();

        double socialSecurityTax = socialSecurityTaxCalculator.calculateSocialSecurityTax(earnedIncome, false);
        summary.setSocialSecurityTax(socialSecurityTax);

        double medicareTax = socialSecurityTaxCalculator.calculateMedicareTax(earnedIncome, false);
        double additionalMedicareTax = socialSecurityTaxCalculator.calculateAdditionalMedicareTax(earnedIncome, filingStatus);
        summary.setMedicareTax(medicareTax + additionalMedicareTax);

        // Capital gains tax on non-qualified withdrawals
        double capitalGainsTax = calculateCapitalGainsTax(summary);
        summary.setCapitalGainsTax(capitalGainsTax);
    }

    /**
     * Calculates the ordinary taxable income for federal and state income taxes.
     *
     * Includes:
     * - Earned income (wages, salary)
     * - RMD withdrawals (taxable as ordinary income)
     * - Qualified withdrawals (taxable as ordinary income)
     * - 85% of Social Security benefits (taxable portion for higher earners)
     * - Roth conversions (taxable as ordinary income)
     *
     * @param summary the yearly summary
     * @return the ordinary taxable income
     */
    private double calculateOrdinaryIncome(YearlySummary summary) {
        double totalIncome = summary.totalIncome();
        double rmdWithdrawals = summary.rmdWithdrawals();
        double qualifiedWithdrawals = summary.qualifiedWithdrawals();

        // Roth conversions are tracked as both qualified withdrawals and Roth contributions
        // The qualified withdrawal portion is already included in qualifiedWithdrawals
        // Roth contributions represent the amount converted, which should be taxed
        double rothConversions = summary.rothContributions();

        // Social Security benefits - approximately 85% taxable for higher earners
        double taxableSocialSecurity = summary.totalSocialSecurity() * 0.85;

        return totalIncome + rmdWithdrawals + qualifiedWithdrawals + rothConversions + taxableSocialSecurity;
    }

    /**
     * Calculates capital gains tax on non-qualified withdrawals.
     *
     * Assumes a portion of non-qualified withdrawals represents capital gains
     * based on the default cost basis factor.
     *
     * @param summary the yearly summary
     * @return the capital gains tax
     */
    private double calculateCapitalGainsTax(YearlySummary summary) {
        double nonQualifiedWithdrawals = summary.nonQualifiedWithdrawals();

        if (nonQualifiedWithdrawals <= 0) {
            return 0.0;
        }

        // Calculate the gain portion (assuming default cost basis factor)
        double gainFactor = 1.0 - DEFAULT_COST_BASIS_FACTOR; // e.g., 0.75 if cost basis is 25%
        double capitalGain = nonQualifiedWithdrawals * gainFactor;

        // Apply capital gains rate (federal long-term rate)
        double federalCapGainsTax = capitalGain * capitalGainsCalculator.getCapitalGainsRate();

        // NJ taxes capital gains as ordinary income
        double ordinaryIncome = calculateOrdinaryIncome(summary);
        double njMarginalRate = stateTaxCalculator.getMarginalTaxRate(ordinaryIncome, filingStatus);
        double njCapGainsTax = capitalGain * njMarginalRate;

        return federalCapGainsTax + njCapGainsTax;
    }

    /**
     * Returns the filing status used for tax calculations.
     *
     * @return the filing status
     */
    public FilingStatus getFilingStatus() {
        return filingStatus;
    }

    @Override
    public String getStrategyName() {
        return "Tax Calculation";
    }

    @Override
    public String getDescription() {
        return "Calculates and stores all taxes in the YearlySummary after all other " +
                "optimization strategies have completed. Taxes include federal income tax, " +
                "NJ state income tax, Social Security tax, Medicare tax, and capital gains tax " +
                "on non-qualified withdrawals.";
    }
}
