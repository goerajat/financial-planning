package rg.financialplanning.strategy;

import rg.financialplanning.calculator.FederalTaxCalculator;
import rg.financialplanning.calculator.LongTermCapitalGainsCalculator;
import rg.financialplanning.calculator.SocialSecurityTaxCalculator;
import rg.financialplanning.calculator.StateTaxCalculatorFactory;
import rg.financialplanning.calculator.TaxCalculator;
import rg.financialplanning.model.FilingStatus;
import rg.financialplanning.model.State;
import rg.financialplanning.model.StateByYear;
import rg.financialplanning.model.YearlySummary;

import java.util.ArrayList;
import java.util.List;

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
 * - State income tax (state tax based on year-specific configuration)
 * - Social Security tax (6.2% of earned income up to wage base)
 * - Medicare tax (1.45% of all earned income, plus additional 0.9% for high earners)
 * - Capital gains tax (on non-qualified withdrawals, assuming appreciation)
 */
public class TaxCalculationStrategy implements TaxOptimizationStrategy {

    private final FederalTaxCalculator federalTaxCalculator;
    private final SocialSecurityTaxCalculator socialSecurityTaxCalculator;
    private final LongTermCapitalGainsCalculator capitalGainsCalculator;
    private final FilingStatus filingStatus;
    private final List<StateByYear> statesByYear;

    /**
     * Creates a tax calculation strategy with default filing status (Married Filing Jointly)
     * and default state (NJ).
     */
    public TaxCalculationStrategy() {
        this(FilingStatus.MARRIED_FILING_JOINTLY);
    }

    /**
     * Creates a tax calculation strategy with the specified filing status and default state (NJ).
     *
     * @param filingStatus the filing status for tax calculations
     */
    public TaxCalculationStrategy(FilingStatus filingStatus) {
        this(filingStatus, new ArrayList<>());
    }

    /**
     * Creates a tax calculation strategy with the specified filing status and state configuration.
     *
     * @param filingStatus the filing status for tax calculations
     * @param statesByYear list of state configurations by year (empty list defaults to NJ)
     */
    public TaxCalculationStrategy(FilingStatus filingStatus, List<StateByYear> statesByYear) {
        this.federalTaxCalculator = new FederalTaxCalculator();
        this.socialSecurityTaxCalculator = new SocialSecurityTaxCalculator();
        this.capitalGainsCalculator = new LongTermCapitalGainsCalculator();
        this.filingStatus = filingStatus;
        this.statesByYear = statesByYear != null ? new ArrayList<>(statesByYear) : new ArrayList<>();
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

        // State income tax (based on year-specific configuration)
        TaxCalculator stateTaxCalculator = StateTaxCalculatorFactory.getCalculatorForYear(summary.year(), statesByYear);
        double stateIncomeTax = stateTaxCalculator.calculateTax(summary, filingStatus);
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

        // Social Security benefits - approximately 85% taxable for higher earners
        double taxableSocialSecurity = summary.totalSocialSecurity() * 0.85;

        return totalIncome + rmdWithdrawals + qualifiedWithdrawals + taxableSocialSecurity - summary.qualifiedContributions();
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

        // State capital gains tax (some states tax as ordinary income)
        TaxCalculator stateTaxCalculator = StateTaxCalculatorFactory.getCalculatorForYear(summary.year(), statesByYear);
        double ordinaryIncome = calculateOrdinaryIncome(summary);

        // Get the state marginal rate - for FL this will be 0, for NJ/NY it's ordinary income rate
        double stateMarginalRate = 0.0;
        State currentState = StateTaxCalculatorFactory.getStateForYear(summary.year(), statesByYear);
        if (currentState == State.NJ || currentState == State.NY) {
            // NJ and NY tax capital gains as ordinary income
            if (stateTaxCalculator instanceof rg.financialplanning.calculator.NJStateTaxCalculator njCalc) {
                stateMarginalRate = njCalc.getMarginalTaxRate(ordinaryIncome, filingStatus);
            } else if (stateTaxCalculator instanceof rg.financialplanning.calculator.NYStateTaxCalculator nyCalc) {
                stateMarginalRate = nyCalc.getMarginalTaxRate(ordinaryIncome, filingStatus);
            }
        }
        double stateCapGainsTax = capitalGain * stateMarginalRate;

        return federalCapGainsTax + stateCapGainsTax;
    }

    /**
     * Returns the filing status used for tax calculations.
     *
     * @return the filing status
     */
    public FilingStatus getFilingStatus() {
        return filingStatus;
    }

    /**
     * Returns the state-by-year configuration.
     *
     * @return the list of state configurations
     */
    public List<StateByYear> getStatesByYear() {
        return new ArrayList<>(statesByYear);
    }

    @Override
    public String getStrategyName() {
        return "Tax Calculation";
    }

    @Override
    public String getDescription() {
        return "Calculates and stores all taxes in the YearlySummary after all other " +
                "optimization strategies have completed. Taxes include federal income tax, " +
                "state income tax (configurable by year), Social Security tax, Medicare tax, " +
                "and capital gains tax on non-qualified withdrawals.";
    }
}
