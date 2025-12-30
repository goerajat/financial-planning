package rg.financialplanning.strategy;

import rg.financialplanning.calculator.FederalTaxCalculator;
import rg.financialplanning.calculator.NJStateTaxCalculator;
import rg.financialplanning.model.FilingStatus;
import rg.financialplanning.model.IndividualYearlySummary;
import rg.financialplanning.model.Person;
import rg.financialplanning.model.YearlySummary;

/**
 * Tax optimization strategy that performs Roth conversions to minimize lifetime taxes.
 *
 * A Roth conversion moves money from a traditional/qualified retirement account (pre-tax)
 * to a Roth account (post-tax). The converted amount is taxed as ordinary income in the
 * year of conversion, but future growth and qualified withdrawals are tax-free.
 *
 * This strategy uses "tax bracket filling" - converting enough to fill up lower tax brackets
 * without pushing into significantly higher ones. This is beneficial when:
 * - Current income is lower than expected future income
 * - You want to reduce future RMDs
 * - You expect tax rates to increase
 * - You want tax-free income in retirement
 *
 * The strategy calculates the optimal conversion amount based on:
 * - Current taxable income (income + social security benefits + RMD withdrawals)
 * - Target tax bracket threshold
 * - Available qualified assets to convert
 *
 * Age eligibility requirement:
 * - Roth conversions are only performed for individuals aged 59 or older
 * - This ensures penalty-free qualified withdrawals (withdrawals before age 59½
 *   are subject to a 10% early withdrawal penalty)
 */
public class RothConversionOptimizationStrategy implements TaxOptimizationStrategy {

    /**
     * Minimum age for penalty-free qualified retirement account withdrawals.
     * Withdrawals before age 59½ are subject to a 10% early withdrawal penalty.
     * We use 59 as the threshold (conservative, slightly before 59½).
     */
    public static final int QUALIFIED_WITHDRAWAL_MIN_AGE = 59;

    private final FederalTaxCalculator federalTaxCalculator;
    private final NJStateTaxCalculator stateTaxCalculator;
    private final FilingStatus filingStatus;
    private final double targetBracketThreshold;

    /**
     * Default federal tax bracket thresholds for Married Filing Jointly (2026 projected).
     * These represent the top of each bracket.
     */
    public static final double MFJ_12_PERCENT_BRACKET = 96_950;
    public static final double MFJ_22_PERCENT_BRACKET = 206_700;
    public static final double MFJ_24_PERCENT_BRACKET = 394_600;

    /**
     * Default federal tax bracket thresholds for Single filers (2026 projected).
     */
    public static final double SINGLE_12_PERCENT_BRACKET = 48_475;
    public static final double SINGLE_22_PERCENT_BRACKET = 103_350;
    public static final double SINGLE_24_PERCENT_BRACKET = 197_300;

    /**
     * Creates a Roth conversion strategy with default settings.
     * Defaults to Married Filing Jointly and filling up to the 22% bracket.
     */
    public RothConversionOptimizationStrategy() {
        this(FilingStatus.MARRIED_FILING_JOINTLY, MFJ_22_PERCENT_BRACKET);
    }

    /**
     * Creates a Roth conversion strategy with the specified filing status.
     * Uses the 22% bracket threshold for the given filing status.
     *
     * @param filingStatus the filing status
     */
    public RothConversionOptimizationStrategy(FilingStatus filingStatus) {
        this(filingStatus, getDefault22PercentBracket(filingStatus));
    }

    /**
     * Creates a Roth conversion strategy with custom settings.
     *
     * @param filingStatus the filing status for tax calculations
     * @param targetBracketThreshold the income threshold to fill up to (e.g., top of 22% bracket)
     */
    public RothConversionOptimizationStrategy(FilingStatus filingStatus, double targetBracketThreshold) {
        this.federalTaxCalculator = new FederalTaxCalculator();
        this.stateTaxCalculator = new NJStateTaxCalculator();
        this.filingStatus = filingStatus;
        this.targetBracketThreshold = targetBracketThreshold;
    }

    private static double getDefault22PercentBracket(FilingStatus filingStatus) {
        return switch (filingStatus) {
            case SINGLE, MARRIED_FILING_SEPARATELY -> SINGLE_22_PERCENT_BRACKET;
            case MARRIED_FILING_JOINTLY, HEAD_OF_HOUSEHOLD -> MFJ_22_PERCENT_BRACKET;
        };
    }

    @Override
    public void optimize(YearlySummary previousYearlySummary, YearlySummary currentYearlySummary) {
        if (currentYearlySummary == null) {
            return;
        }

        // Calculate combined taxable income for married filing jointly
        double combinedTaxableIncome = calculateCombinedTaxableIncome(currentYearlySummary);

        // Calculate room in the target bracket based on combined income
        double roomInBracket = targetBracketThreshold - combinedTaxableIncome;

        if (roomInBracket <= 0) {
            // Already at or above target bracket, no conversion recommended
            updateYearlySummaryAssets(currentYearlySummary);
            return;
        }

        // Distribute conversion opportunity among individuals based on available qualified assets
        double remainingRoom = roomInBracket;
        int currentYear = currentYearlySummary.year();

        for (IndividualYearlySummary individual : currentYearlySummary.individualSummaries().values()) {
            if (remainingRoom <= 0) {
                break;
            }

            // Check if individual is eligible for penalty-free qualified withdrawals
            if (!isEligibleForQualifiedWithdrawal(individual, currentYear)) {
                continue;
            }

            double availableToConvert = individual.qualifiedAssets();
            if (availableToConvert <= 0) {
                continue;
            }

            // Convert the lesser of remaining room or available assets
            double conversionAmount = Math.min(remainingRoom, availableToConvert);

            // Apply the conversion at individual level
            applyConversion(individual, conversionAmount);

            remainingRoom -= conversionAmount;
        }

        // Update yearly summary totals
        updateYearlySummaryAssets(currentYearlySummary);
    }

    /**
     * Calculates the combined taxable income across all individuals.
     * Used for married filing jointly where tax brackets apply to combined household income.
     *
     * @param summary the yearly summary containing all individuals
     * @return the combined taxable income
     */
    private double calculateCombinedTaxableIncome(YearlySummary summary) {
        double combinedIncome = 0.0;

        for (IndividualYearlySummary individual : summary.individualSummaries().values()) {
            combinedIncome += calculateIndividualTaxableIncome(individual);
        }

        return combinedIncome;
    }

    /**
     * Calculates the taxable income for an individual.
     * Includes income, taxable portion of social security, RMD withdrawals, and qualified withdrawals.
     *
     * @param individual the individual yearly summary
     * @return the individual's taxable income contribution
     */
    private double calculateIndividualTaxableIncome(IndividualYearlySummary individual) {
        double income = individual.income();

        // Social security benefits - approximately 85% is taxable for higher earners
        double taxableSocialSecurity = individual.socialSecurityBenefits() * 0.85;

        // RMD withdrawals are fully taxable
        double rmdWithdrawals = individual.rmdWithdrawals();

        // Qualified withdrawals (from traditional IRA/401k) are fully taxable
        double qualifiedWithdrawals = individual.qualifiedWithdrawals();

        return income + taxableSocialSecurity + rmdWithdrawals + qualifiedWithdrawals;
    }

    /**
     * Checks if an individual is eligible for penalty-free qualified retirement account withdrawals.
     * Individuals must be at least 59½ years old to avoid the 10% early withdrawal penalty.
     *
     * @param individual the individual yearly summary
     * @param year the current year
     * @return true if the individual is eligible for penalty-free withdrawals
     */
    private boolean isEligibleForQualifiedWithdrawal(IndividualYearlySummary individual, int year) {
        Person person = individual.person();
        if (person == null) {
            return false;
        }

        int age = person.getAgeInYear(year);
        return age >= QUALIFIED_WITHDRAWAL_MIN_AGE;
    }

    /**
     * Applies a Roth conversion by moving assets from qualified to Roth.
     * Calculates the tax cost of the conversion and funds it by:
     * 1. First reducing non-qualified contributions (individual level)
     * 2. Then reducing the roth contributions (meaning less goes to Roth)
     *
     * @param individual the individual yearly summary
     * @param amount the amount to convert
     */
    private void applyConversion(IndividualYearlySummary individual, double amount) {
        if (amount <= 0) {
            return;
        }

        // Calculate current taxable income for this individual
        double currentTaxableIncome = calculateIndividualTaxableIncome(individual);

        // Calculate the tax cost of the conversion
        double taxCost = calculateConversionTaxCost(amount, currentTaxableIncome);

        // Reduce qualified assets and track withdrawal
        double currentQualified = individual.qualifiedAssets();
        individual.setQualifiedAssets(currentQualified - amount);
        double currentQualifiedWithdrawals = individual.qualifiedWithdrawals();
        individual.setQualifiedWithdrawals(currentQualifiedWithdrawals + amount);

        // Fund the tax cost - first from non-qualified contributions
        double remainingTaxCost = taxCost;
        double currentNonQualContributions = individual.nonQualifiedContributions();

        if (currentNonQualContributions > 0) {
            double fundedFromNonQual = Math.min(remainingTaxCost, currentNonQualContributions);
            individual.setNonQualifiedContributions(currentNonQualContributions - fundedFromNonQual);
            remainingTaxCost -= fundedFromNonQual;
        }

        // Calculate actual Roth contribution (conversion amount minus remaining tax cost)
        double actualRothContribution = amount - remainingTaxCost;

        // Increase Roth assets and track contribution
        double currentRoth = individual.rothAssets();
        individual.setRothAssets(currentRoth + actualRothContribution);
        double currentRothContributions = individual.rothContributions();
        individual.setRothContributions(currentRothContributions + actualRothContribution);
    }

    /**
     * Updates the yearly summary's aggregate asset totals based on individual summaries.
     *
     * @param summary the yearly summary to update
     */
    private void updateYearlySummaryAssets(YearlySummary summary) {
        double totalQualified = 0.0;
        double totalRoth = 0.0;
        double totalQualifiedWithdrawals = 0.0;
        double totalRothContributions = 0.0;
        double totalNonQualifiedContributions = 0.0;

        for (IndividualYearlySummary individual : summary.individualSummaries().values()) {
            totalQualified += individual.qualifiedAssets();
            totalRoth += individual.rothAssets();
            totalQualifiedWithdrawals += individual.qualifiedWithdrawals();
            totalRothContributions += individual.rothContributions();
            totalNonQualifiedContributions += individual.nonQualifiedContributions();
        }

        summary.setQualifiedAssets(totalQualified);
        summary.setRothAssets(totalRoth);
        summary.setQualifiedWithdrawals(totalQualifiedWithdrawals);
        summary.setRothContributions(totalRothContributions);
        summary.setNonQualifiedContributions(totalNonQualifiedContributions);
    }

    /**
     * Calculates the optimal Roth conversion amount without applying it.
     * Useful for projections and planning.
     *
     * @param currentTaxableIncome the current taxable income before conversion
     * @param availableQualifiedAssets the qualified assets available to convert
     * @return the recommended conversion amount
     */
    public double calculateOptimalConversion(double currentTaxableIncome, double availableQualifiedAssets) {
        double roomInBracket = targetBracketThreshold - currentTaxableIncome;

        if (roomInBracket <= 0 || availableQualifiedAssets <= 0) {
            return 0.0;
        }

        return Math.min(roomInBracket, availableQualifiedAssets);
    }

    /**
     * Calculates the tax cost of a Roth conversion.
     *
     * @param conversionAmount the amount to convert
     * @param currentTaxableIncome the current taxable income before conversion
     * @return the estimated federal and state tax on the conversion
     */
    public double calculateConversionTaxCost(double conversionAmount, double currentTaxableIncome) {
        if (conversionAmount <= 0) {
            return 0.0;
        }

        // Calculate tax with and without conversion
        double taxWithoutConversion = federalTaxCalculator.calculateTax(currentTaxableIncome, filingStatus)
                + stateTaxCalculator.calculateTax(currentTaxableIncome, filingStatus);

        double incomeWithConversion = currentTaxableIncome + conversionAmount;
        double taxWithConversion = federalTaxCalculator.calculateTax(incomeWithConversion, filingStatus)
                + stateTaxCalculator.calculateTax(incomeWithConversion, filingStatus);

        return taxWithConversion - taxWithoutConversion;
    }

    /**
     * Gets the marginal tax rate at the current income level.
     *
     * @param taxableIncome the taxable income
     * @return the combined federal and state marginal rate
     */
    public double getMarginalTaxRate(double taxableIncome) {
        double federalRate = federalTaxCalculator.getMarginalTaxRate(taxableIncome, filingStatus);
        double stateRate = stateTaxCalculator.getMarginalTaxRate(taxableIncome, filingStatus);
        return federalRate + stateRate;
    }

    public FilingStatus getFilingStatus() {
        return filingStatus;
    }

    public double getTargetBracketThreshold() {
        return targetBracketThreshold;
    }

    @Override
    public String getStrategyName() {
        return "Roth Conversion Optimization";
    }

    @Override
    public String getDescription() {
        return String.format(
                "Performs strategic Roth conversions to fill lower tax brackets. " +
                "Converts qualified (pre-tax) assets to Roth (post-tax) up to the %.0f income threshold " +
                "(top of target bracket). This pays taxes now at lower rates to avoid higher taxes later, " +
                "reduces future RMDs, and provides tax-free growth and withdrawals in retirement.",
                targetBracketThreshold);
    }
}
