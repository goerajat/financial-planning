package rg.financialplanning.strategy;

import rg.financialplanning.model.IndividualYearlySummary;
import rg.financialplanning.model.Person;
import rg.financialplanning.model.YearlySummary;

import java.util.Collection;

/**
 * Strategy to manage expenses and allocate surplus income or handle deficits.
 *
 * This strategy uses the YearlySummary's cash flow methods to determine surplus/deficit:
 * - Surplus = totalCashInflows() - totalCashOutflows()
 *
 * If surplus (positive):
 * - Surplus is distributed equally to all individuals' non-qualified investment accounts
 *
 * If deficit (negative):
 * Withdrawals are made in the following priority order:
 * 1. Non-qualified assets
 * 2. Qualified assets
 * 3. Roth assets
 * 4. Cash
 * 5. Any remaining deficit is tracked in the summary
 *
 * Note: Tax calculations are handled by TaxCalculationStrategy in the optimization loop.
 * This strategy simply withdraws at face value; taxes are recalculated after each iteration.
 */
public class ExpenseManagementStrategy implements TaxOptimizationStrategy {

    /**
     * Minimum age for penalty-free qualified retirement account withdrawals.
     * Withdrawals before age 59½ are subject to a 10% early withdrawal penalty.
     */
    public static final int QUALIFIED_WITHDRAWAL_MIN_AGE = 59;

    /**
     * Creates an expense management strategy.
     */
    public ExpenseManagementStrategy() {
    }

    @Override
    public void optimize(YearlySummary previousYearlySummary, YearlySummary currentYearlySummary) {
        if (currentYearlySummary == null) {
            return;
        }

        // Calculate surplus using YearlySummary's cash flow methods
        // Positive = surplus, Negative = deficit
        double surplus = currentYearlySummary.totalCashInflows() - currentYearlySummary.totalCashOutflows();

        if (surplus < 0) {
            // Deficit - need to withdraw from assets in order of priority
            double deficit = -surplus;
            handleDeficit(currentYearlySummary, deficit);
        } else if (surplus > 0) {
            // Distribute surplus equally among all individuals' non-qualified assets
            distributeSurplusToNonQualifiedAssets(currentYearlySummary, surplus);
        }

        // Update yearly summary totals
        updateYearlySummaryAssets(currentYearlySummary);
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
     * 1. Non-qualified assets
     * 2. Qualified assets
     * 3. Roth assets
     * 4. Cash
     * 5. Any remaining deficit is tracked in the summary
     *
     * @param summary the yearly summary
     * @param deficit the deficit amount to cover (positive number)
     */
    private void handleDeficit(YearlySummary summary, double deficit) {
        Collection<IndividualYearlySummary> individuals = summary.individualSummaries().values();

        if (individuals.isEmpty()) {
            summary.setDeficit(deficit);
            return;
        }

        double remainingDeficit = deficit;

        // Step 1: Withdraw from non-qualified assets
        if (remainingDeficit > 0) {
            remainingDeficit = withdrawFromNonQualifiedAssets(summary, individuals, remainingDeficit);
        }

        // Step 2: Withdraw from qualified assets
        if (remainingDeficit > 0) {
            remainingDeficit = withdrawFromQualifiedAssets(summary, individuals, remainingDeficit);
        }

        // Step 3: Withdraw from Roth assets
        if (remainingDeficit > 0) {
            remainingDeficit = withdrawFromRothAssets(summary, individuals, remainingDeficit);
        }

        // Step 4: Withdraw from cash
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
     * Withdraws the exact deficit amount at face value.
     *
     * @param summary the yearly summary
     * @param individuals the collection of individual summaries
     * @param deficit the deficit amount to cover
     * @return the remaining deficit after withdrawal
     */
    private double withdrawFromNonQualifiedAssets(YearlySummary summary,
            Collection<IndividualYearlySummary> individuals, double deficit) {

        double remainingNeeded = deficit;

        // Withdraw as much as possible from each individual
        for (IndividualYearlySummary individual : individuals) {
            if (remainingNeeded <= 0) {
                break;
            }

            double available = individual.nonQualifiedAssets();
            if (available <= 0) {
                continue;
            }

            // Withdraw as much as needed or available
            double withdrawal = Math.min(remainingNeeded, available);

            individual.setNonQualifiedAssets(available - withdrawal);
            individual.setNonQualifiedWithdrawals(individual.nonQualifiedWithdrawals() + withdrawal);

            remainingNeeded -= withdrawal;
        }

        // Update the yearly summary's non-qualified withdrawals
        double totalWithdrawals = 0.0;
        for (IndividualYearlySummary individual : individuals) {
            totalWithdrawals += individual.nonQualifiedWithdrawals();
        }
        summary.setNonQualifiedWithdrawals(totalWithdrawals);

        return remainingNeeded;
    }

    /**
     * Withdraws from qualified assets to cover a deficit.
     * Only withdraws from individuals who are age-eligible (59+) for penalty-free withdrawals.
     * Withdraws the exact deficit amount at face value.
     *
     * @param summary the yearly summary
     * @param individuals the collection of individual summaries
     * @param deficit the deficit amount to cover
     * @return the remaining deficit after withdrawal
     */
    private double withdrawFromQualifiedAssets(YearlySummary summary,
            Collection<IndividualYearlySummary> individuals, double deficit) {

        int currentYear = summary.year();
        double remainingNeeded = deficit;

        // Withdraw as much as possible from each eligible individual
        for (IndividualYearlySummary individual : individuals) {
            if (remainingNeeded <= 0) {
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
            double withdrawal = Math.min(remainingNeeded, available);

            individual.setQualifiedAssets(available - withdrawal);
            individual.setQualifiedWithdrawals(individual.qualifiedWithdrawals() + withdrawal);

            remainingNeeded -= withdrawal;
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

        return remainingNeeded;
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

        double remainingNeeded = deficit;

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

        return remainingNeeded;
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

    @Override
    public String getStrategyName() {
        return "Expense Management";
    }

    @Override
    public String getDescription() {
        return "Manages expenses by calculating surplus/deficit from cash inflows minus outflows. " +
                "If surplus exists, it is distributed equally to all individuals' non-qualified " +
                "investment accounts. If there is a deficit, withdrawals are made in order: " +
                "(1) non-qualified assets, (2) qualified assets, (3) Roth assets, (4) cash. " +
                "Any remaining deficit is tracked.";
    }
}
