package rg.financialplanning.strategy;

import rg.financialplanning.calculator.FederalTaxCalculator;
import rg.financialplanning.calculator.LongTermCapitalGainsCalculator;
import rg.financialplanning.calculator.NJStateTaxCalculator;
import rg.financialplanning.calculator.SocialSecurityTaxCalculator;

import static rg.financialplanning.calculator.LongTermCapitalGainsCalculator.DEFAULT_COST_BASIS_FACTOR;
import rg.financialplanning.model.FilingStatus;
import rg.financialplanning.model.IndividualYearlySummary;
import rg.financialplanning.model.Person;
import rg.financialplanning.model.YearlySummary;

import java.util.Collection;

/**
 * Strategy to manage expenses and allocate surplus income or handle deficits.
 *
 * This strategy calculates the net income after all taxes and compares it to expenses.
 *
 * Net income calculation (cash flow basis):
 * - Total income (wages, etc.)
 * + RMD withdrawals (taxable income from retirement accounts)
 * + Qualified withdrawals (taxable income from retirement accounts)
 * + 100% of Social Security benefits (full amount received)
 * - Federal income tax (calculated on taxable income with 85% SS)
 * - NJ state income tax
 * - Social Security tax
 * - Medicare tax
 *
 * Note: Taxes are calculated using 85% of Social Security as taxable income,
 * but the full 100% of Social Security is included in cash flow calculations.
 *
 * If surplus (net income > expenses):
 * - Surplus is distributed equally to all individuals' non-qualified investment accounts
 *
 * If deficit (expenses > net income):
 * Withdrawals are made in the following priority order:
 * 1. Non-qualified assets - adjusted for capital gains taxes (federal 20% + NJ ordinary income on gains)
 * 2. Qualified assets - adjusted for federal and state income taxes (ordinary income rates)
 * 3. Roth assets - tax-free withdrawals
 * 4. Cash - no tax implications
 * 5. Any remaining deficit is tracked in the summary
 */
public class ExpenseManagementStrategy implements TaxOptimizationStrategy {

    /**
     * Minimum age for penalty-free qualified retirement account withdrawals.
     * Withdrawals before age 59½ are subject to a 10% early withdrawal penalty.
     */
    public static final int QUALIFIED_WITHDRAWAL_MIN_AGE = 59;

    private final FederalTaxCalculator federalTaxCalculator;
    private final NJStateTaxCalculator stateTaxCalculator;
    private final SocialSecurityTaxCalculator socialSecurityTaxCalculator;
    private final LongTermCapitalGainsCalculator capitalGainsCalculator;
    private final FilingStatus filingStatus;

    /**
     * Creates an expense management strategy with default filing status (Married Filing Jointly).
     */
    public ExpenseManagementStrategy() {
        this(FilingStatus.MARRIED_FILING_JOINTLY);
    }

    /**
     * Creates an expense management strategy with the specified filing status.
     *
     * @param filingStatus the filing status for tax calculations
     */
    public ExpenseManagementStrategy(FilingStatus filingStatus) {
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

        // Calculate gross taxable income (including taxable withdrawals and 85% of SS for tax purposes)
        double grossTaxableIncome = calculateGrossIncome(currentYearlySummary);

        // Calculate all taxes based on taxable income
        double federalTax = federalTaxCalculator.calculateTax(grossTaxableIncome, filingStatus);
        double stateTax = stateTaxCalculator.calculateTax(grossTaxableIncome, filingStatus);

        // Social Security and Medicare taxes are based on earned income only
        double earnedIncome = currentYearlySummary.totalIncome();
        double socialSecurityTax = socialSecurityTaxCalculator.calculateSocialSecurityTax(earnedIncome, false);
        double medicareTax = socialSecurityTaxCalculator.calculateMedicareTax(earnedIncome, false);
        double additionalMedicareTax = socialSecurityTaxCalculator.calculateAdditionalMedicareTax(earnedIncome, filingStatus);

        double totalTaxes = federalTax + stateTax + socialSecurityTax + medicareTax + additionalMedicareTax;

        // Calculate total cash income (includes FULL social security benefits, not just taxable portion)
        double totalCashIncome = calculateTotalCashIncome(currentYearlySummary);

        // Calculate net income after taxes (based on actual cash received, not taxable income)
        double netIncomeAfterTaxes = totalCashIncome - totalTaxes;

        // Get total expenses
        double totalExpenses = currentYearlySummary.totalExpenses();

        // Get mortgage payment
        double mortgagePayment = currentYearlySummary.mortgagePayment();

        // Calculate surplus (including mortgage payment as an outflow)
        double surplus = netIncomeAfterTaxes - totalExpenses - mortgagePayment - currentYearlySummary.rothContributions();

        if (surplus < 0) {
            // Deficit - need to withdraw from assets in order of priority
            double deficit = -surplus;
            handleDeficit(currentYearlySummary, deficit, grossTaxableIncome);
        } else if (surplus > 0) {
            // Distribute surplus equally among all individuals' non-qualified assets
            distributeSurplusToNonQualifiedAssets(currentYearlySummary, surplus);
        }

        // Update yearly summary totals
        updateYearlySummaryAssets(currentYearlySummary);
    }

    /**
     * Calculates the gross taxable income for the year.
     * Includes earned income, RMD withdrawals, qualified withdrawals, and 85% of Social Security.
     * Used for tax calculation purposes.
     *
     * @param summary the yearly summary
     * @return the gross taxable income
     */
    private double calculateGrossIncome(YearlySummary summary) {
        double totalIncome = summary.totalIncome();
        double rmdWithdrawals = summary.rmdWithdrawals();
        double qualifiedWithdrawals = summary.qualifiedWithdrawals();

        // Social Security benefits - approximately 85% taxable for higher earners
        double taxableSocialSecurity = summary.totalSocialSecurity() * 0.85;

        return totalIncome + rmdWithdrawals + qualifiedWithdrawals + taxableSocialSecurity;
    }

    /**
     * Calculates the total cash income for the year.
     * Includes earned income, RMD withdrawals, qualified withdrawals, and FULL Social Security benefits.
     * Used for cash flow/surplus calculation purposes.
     *
     * @param summary the yearly summary
     * @return the total cash income
     */
    private double calculateTotalCashIncome(YearlySummary summary) {
        double totalIncome = summary.totalIncome();
        double rmdWithdrawals = summary.rmdWithdrawals();
        double qualifiedWithdrawals = summary.qualifiedWithdrawals();

        // Full Social Security benefits (100%) for cash flow purposes
        double socialSecurityBenefits = summary.totalSocialSecurity();

        return totalIncome + rmdWithdrawals + qualifiedWithdrawals + socialSecurityBenefits - summary.qualifiedContributions();
    }

    /**
     * Distributes the surplus equally among all individuals' non-qualified assets.
     *
     * @param summary the yearly summary
     * @param surplus the surplus amount to distribute
     */
    private void distributeSurplusToNonQualifiedAssets(YearlySummary summary, double surplus) {
        Collection<IndividualYearlySummary> individuals = summary.individualSummaries().values();

        if (individuals.isEmpty()) {
            return;
        }

        // Distribute equally among all individuals
        double surplusPerIndividual = surplus / individuals.size();

        for (IndividualYearlySummary individual : individuals) {
            // Increase non-qualified assets
            double currentNonQualified = individual.nonQualifiedAssets();
            individual.setNonQualifiedAssets(currentNonQualified + surplusPerIndividual);

            // Track the contribution
            double currentContributions = individual.nonQualifiedContributions();
            individual.setNonQualifiedContributions(currentContributions + surplusPerIndividual);
        }

        // Update the yearly summary's non-qualified contributions
        double totalContributions = 0.0;
        for (IndividualYearlySummary individual : individuals) {
            totalContributions += individual.nonQualifiedContributions();
        }
        summary.setNonQualifiedContributions(totalContributions);
    }

    /**
     * Handles a deficit by withdrawing from assets in priority order.
     *
     * Withdrawal order:
     * 1. Non-qualified assets (adjusted for capital gains taxes)
     * 2. Qualified assets (adjusted for federal and state income taxes)
     * 3. Roth assets (tax-free withdrawals)
     * 4. Cash (no tax)
     * 5. Any remaining deficit is tracked in the summary
     *
     * @param summary the yearly summary
     * @param deficit the deficit amount to cover (positive number)
     * @param currentTaxableIncome the current taxable income for determining marginal rates
     */
    private void handleDeficit(YearlySummary summary, double deficit, double currentTaxableIncome) {
        Collection<IndividualYearlySummary> individuals = summary.individualSummaries().values();

        if (individuals.isEmpty()) {
            summary.setDeficit(deficit);
            return;
        }

        double remainingDeficit = deficit;

        // Step 1: Withdraw from non-qualified assets (with capital gains tax adjustment)
        if (remainingDeficit > 0) {
            remainingDeficit = withdrawFromNonQualifiedAssets(summary, individuals, remainingDeficit, currentTaxableIncome);
        }

        // Step 2: Withdraw from qualified assets (with federal and state income tax adjustment)
        if (remainingDeficit > 0) {
            remainingDeficit = withdrawFromQualifiedAssets(summary, individuals, remainingDeficit, currentTaxableIncome);
        }

        // Step 3: Withdraw from Roth assets (tax-free)
        if (remainingDeficit > 0) {
            remainingDeficit = withdrawFromRothAssets(summary, individuals, remainingDeficit);
        }

        // Step 4: Withdraw from cash (no tax)
        if (remainingDeficit > 0) {
            remainingDeficit = withdrawFromCash(summary, remainingDeficit);
        }

        // Step 5: Track any remaining deficit
        if (remainingDeficit > 0) {
            summary.setDeficit(remainingDeficit);
            // Distribute deficit tracking equally among individuals
            double deficitPerIndividual = remainingDeficit / individuals.size();
            for (IndividualYearlySummary individual : individuals) {
                individual.setDeficit(individual.deficit() + deficitPerIndividual);
            }
        }
    }

    /**
     * Withdraws from non-qualified assets to cover a deficit.
     * Adjusts for capital gains taxes (federal 20% and NJ ordinary income rates on gains).
     * Withdraws as much as possible from each individual until the deficit is covered.
     *
     * @param summary the yearly summary
     * @param individuals the collection of individual summaries
     * @param deficit the deficit amount to cover
     * @param currentTaxableIncome the current taxable income for marginal rate calculation
     * @return the remaining deficit after withdrawal
     */
    private double withdrawFromNonQualifiedAssets(YearlySummary summary,
            Collection<IndividualYearlySummary> individuals, double deficit, double currentTaxableIncome) {

        // Calculate the gross withdrawal needed to net the deficit amount
        // Net = Gross - Federal Capital Gains Tax - NJ State Tax
        // Federal CG Tax = Gross * (1 - cost_basis) * federal_cg_rate = Gross * 0.75 * 0.20 = Gross * 0.15
        // NJ State Tax = Gross * (1 - cost_basis) * nj_marginal_rate = Gross * 0.75 * nj_marginal_rate
        // Net = Gross * (1 - 0.15 - 0.75 * nj_marginal_rate)
        // Gross = Net / (1 - 0.15 - 0.75 * nj_marginal_rate)

        double gainFactor = 1.0 - DEFAULT_COST_BASIS_FACTOR; // 0.75 - portion that is taxable gain
        double federalCapGainsRate = capitalGainsCalculator.getCapitalGainsRate(); // 0.20
        double njMarginalRate = stateTaxCalculator.getMarginalTaxRate(currentTaxableIncome, filingStatus);

        double federalTaxFactor = gainFactor * federalCapGainsRate; // 0.75 * 0.20 = 0.15
        double njTaxFactor = gainFactor * njMarginalRate; // 0.75 * nj_marginal_rate

        double netFactor = 1.0 - federalTaxFactor - njTaxFactor;
        if (netFactor <= 0) {
            netFactor = 0.01;
        }

        double remainingGrossNeeded = deficit / netFactor;
        double totalGrossWithdrawn = 0.0;

        // Withdraw as much as possible from each individual
        for (IndividualYearlySummary individual : individuals) {
            if (remainingGrossNeeded <= 0) {
                break;
            }

            double available = individual.nonQualifiedAssets();
            if (available <= 0) {
                continue;
            }

            // Withdraw as much as needed or available
            double withdrawal = Math.min(remainingGrossNeeded, available);

            individual.setNonQualifiedAssets(available - withdrawal);
            individual.setNonQualifiedWithdrawals(individual.nonQualifiedWithdrawals() + withdrawal);

            totalGrossWithdrawn += withdrawal;
            remainingGrossNeeded -= withdrawal;
        }

        // Update the yearly summary's non-qualified withdrawals
        double totalWithdrawals = 0.0;
        for (IndividualYearlySummary individual : individuals) {
            totalWithdrawals += individual.nonQualifiedWithdrawals();
        }
        summary.setNonQualifiedWithdrawals(totalWithdrawals);

        double netProceeds = totalGrossWithdrawn * netFactor;
        return Math.max(0, deficit - netProceeds);
    }

    /**
     * Withdraws from qualified assets to cover a deficit.
     * Only withdraws from individuals who are age-eligible (59+) for penalty-free withdrawals.
     * Adjusts for federal and state income taxes (ordinary income rates).
     *
     * @param summary the yearly summary
     * @param individuals the collection of individual summaries
     * @param deficit the deficit amount to cover
     * @param currentTaxableIncome the current taxable income for marginal rate calculation
     * @return the remaining deficit after withdrawal
     */
    private double withdrawFromQualifiedAssets(YearlySummary summary,
            Collection<IndividualYearlySummary> individuals, double deficit, double currentTaxableIncome) {

        // Qualified withdrawals are taxed as ordinary income
        // Net = Gross - Federal Tax - State Tax
        // Net = Gross * (1 - federal_marginal_rate - state_marginal_rate)
        // Gross = Net / (1 - federal_marginal_rate - state_marginal_rate)

        double federalMarginalRate = federalTaxCalculator.getMarginalTaxRate(currentTaxableIncome, filingStatus);
        double stateMarginalRate = stateTaxCalculator.getMarginalTaxRate(currentTaxableIncome, filingStatus);

        double netFactor = 1.0 - federalMarginalRate - stateMarginalRate;
        if (netFactor <= 0) {
            netFactor = 0.01;
        }

        int currentYear = summary.year();
        double remainingGrossNeeded = deficit / netFactor;
        double totalGrossWithdrawn = 0.0;

        // Withdraw as much as possible from each eligible individual
        for (IndividualYearlySummary individual : individuals) {
            if (remainingGrossNeeded <= 0) {
                break;
            }

            // Check age eligibility for penalty-free withdrawal
            if (!isEligibleForQualifiedWithdrawal(individual, currentYear)) {
                continue;
            }

            double available = individual.qualifiedAssets();
            if (available <= 0) {
                continue;
            }

            // Withdraw as much as needed or available
            double withdrawal = Math.min(remainingGrossNeeded, available);

            individual.setQualifiedAssets(available - withdrawal);
            individual.setQualifiedWithdrawals(individual.qualifiedWithdrawals() + withdrawal);

            totalGrossWithdrawn += withdrawal;
            remainingGrossNeeded -= withdrawal;
        }

        // Update the yearly summary's qualified withdrawals and assets
        double totalWithdrawals = 0.0;
        double totalQualifiedAssets = 0.0;
        for (IndividualYearlySummary individual : individuals) {
            totalWithdrawals += individual.qualifiedWithdrawals();
            totalQualifiedAssets += individual.qualifiedAssets();
        }
        summary.setQualifiedWithdrawals(totalWithdrawals);
        summary.setQualifiedAssets(totalQualifiedAssets);

        double netProceeds = totalGrossWithdrawn * netFactor;
        return Math.max(0, deficit - netProceeds);
    }

    /**
     * Checks if an individual is eligible for penalty-free qualified retirement account withdrawals.
     * Individuals must be at least 59 years old to avoid the 10% early withdrawal penalty.
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
     * Withdraws from Roth assets to cover a deficit.
     * Roth withdrawals are tax-free (assuming qualified distribution).
     * Withdraws as much as possible from each individual until the deficit is covered.
     *
     * @param summary the yearly summary
     * @param individuals the collection of individual summaries
     * @param deficit the deficit amount to cover
     * @return the remaining deficit after withdrawal
     */
    private double withdrawFromRothAssets(YearlySummary summary,
            Collection<IndividualYearlySummary> individuals, double deficit) {

        // Roth withdrawals are tax-free, so gross = net
        double remainingNeeded = deficit;
        double totalWithdrawn = 0.0;

        // Withdraw as much as possible from each individual
        for (IndividualYearlySummary individual : individuals) {
            if (remainingNeeded <= 0) {
                break;
            }

            double available = individual.rothAssets();
            if (available <= 0) {
                continue;
            }

            // Withdraw as much as needed or available
            double withdrawal = Math.min(remainingNeeded, available);

            individual.setRothAssets(available - withdrawal);
            individual.setRothWithdrawals(individual.rothWithdrawals() + withdrawal);

            totalWithdrawn += withdrawal;
            remainingNeeded -= withdrawal;
        }

        // Update the yearly summary's Roth withdrawals and assets
        double totalWithdrawals = 0.0;
        double totalRothAssets = 0.0;
        for (IndividualYearlySummary individual : individuals) {
            totalWithdrawals += individual.rothWithdrawals();
            totalRothAssets += individual.rothAssets();
        }
        summary.setRothWithdrawals(totalWithdrawals);
        summary.setRothAssets(totalRothAssets);

        return Math.max(0, deficit - totalWithdrawn);
    }

    /**
     * Withdraws from cash to cover a deficit.
     * Cash withdrawals have no tax implications.
     *
     * @param summary the yearly summary
     * @param deficit the deficit amount to cover
     * @return the remaining deficit after withdrawal
     */
    private double withdrawFromCash(YearlySummary summary, double deficit) {
        double availableCash = summary.cash();

        if (availableCash <= 0) {
            return deficit;
        }

        double withdrawal = Math.min(deficit, availableCash);

        // Update cash and track withdrawal
        summary.setCash(availableCash - withdrawal);
        summary.setCashWithdrawals(summary.cashWithdrawals() + withdrawal);

        return Math.max(0, deficit - withdrawal);
    }

    /**
     * Updates the yearly summary's aggregate asset totals based on individual summaries.
     *
     * @param summary the yearly summary to update
     */
    private void updateYearlySummaryAssets(YearlySummary summary) {
        double totalNonQualified = 0.0;

        for (IndividualYearlySummary individual : summary.individualSummaries().values()) {
            totalNonQualified += individual.nonQualifiedAssets();
        }

        summary.setNonQualifiedAssets(totalNonQualified);
    }

    /**
     * Calculates the surplus for a given yearly summary without applying it.
     * Useful for projections and reporting.
     *
     * @param summary the yearly summary
     * @return the surplus amount (positive) or deficit (negative)
     */
    public double calculateSurplus(YearlySummary summary) {
        if (summary == null) {
            return 0.0;
        }

        // Use taxable income for tax calculation
        double grossTaxableIncome = calculateGrossIncome(summary);

        double federalTax = federalTaxCalculator.calculateTax(grossTaxableIncome, filingStatus);
        double stateTax = stateTaxCalculator.calculateTax(grossTaxableIncome, filingStatus);

        double earnedIncome = summary.totalIncome();
        double socialSecurityTax = socialSecurityTaxCalculator.calculateSocialSecurityTax(earnedIncome, false);
        double medicareTax = socialSecurityTaxCalculator.calculateMedicareTax(earnedIncome, false);
        double additionalMedicareTax = socialSecurityTaxCalculator.calculateAdditionalMedicareTax(earnedIncome, filingStatus);

        double totalTaxes = federalTax + stateTax + socialSecurityTax + medicareTax + additionalMedicareTax;

        // Use total cash income (with full SS benefits) for net income calculation
        double totalCashIncome = calculateTotalCashIncome(summary);
        double netIncomeAfterTaxes = totalCashIncome - totalTaxes;

        return netIncomeAfterTaxes - summary.totalExpenses();
    }

    /**
     * Calculates detailed tax breakdown for a yearly summary.
     *
     * @param summary the yearly summary
     * @return array containing [federalTax, stateTax, socialSecurityTax, medicareTax, totalTaxes]
     */
    public double[] calculateTaxBreakdown(YearlySummary summary) {
        if (summary == null) {
            return new double[]{0, 0, 0, 0, 0};
        }

        double grossIncome = calculateGrossIncome(summary);

        double federalTax = federalTaxCalculator.calculateTax(grossIncome, filingStatus);
        double stateTax = stateTaxCalculator.calculateTax(grossIncome, filingStatus);

        double earnedIncome = summary.totalIncome();
        double socialSecurityTax = socialSecurityTaxCalculator.calculateSocialSecurityTax(earnedIncome, false);
        double medicareTax = socialSecurityTaxCalculator.calculateMedicareTax(earnedIncome, false);
        double additionalMedicareTax = socialSecurityTaxCalculator.calculateAdditionalMedicareTax(earnedIncome, filingStatus);

        double totalFicaTax = socialSecurityTax + medicareTax + additionalMedicareTax;
        double totalTaxes = federalTax + stateTax + totalFicaTax;

        return new double[]{federalTax, stateTax, socialSecurityTax, medicareTax + additionalMedicareTax, totalTaxes};
    }

    public FilingStatus getFilingStatus() {
        return filingStatus;
    }

    @Override
    public String getStrategyName() {
        return "Expense Management";
    }

    @Override
    public String getDescription() {
        return "Manages expenses by calculating net income after federal tax, NJ state tax, " +
                "Social Security tax, and Medicare tax. If net income exceeds expenses, " +
                "the surplus is distributed equally to all individuals' non-qualified " +
                "investment accounts. If there is a deficit, withdrawals are made in order: " +
                "(1) non-qualified assets (adjusted for capital gains taxes), " +
                "(2) qualified assets (adjusted for income taxes), " +
                "(3) Roth assets (tax-free), (4) cash. Any remaining deficit is tracked.";
    }
}
