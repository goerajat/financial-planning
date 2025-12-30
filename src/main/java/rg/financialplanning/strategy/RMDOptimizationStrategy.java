package rg.financialplanning.strategy;

import rg.financialplanning.calculator.RMDCalculator;
import rg.financialplanning.model.IndividualYearlySummary;
import rg.financialplanning.model.Person;
import rg.financialplanning.model.YearlySummary;

/**
 * Tax optimization strategy that calculates and applies Required Minimum Distributions (RMDs).
 *
 * This strategy:
 * 1. Calculates RMDs for each individual based on their age and previous year's qualified assets
 * 2. Sets the RMD withdrawal amounts in each IndividualYearlySummary
 * 3. Updates the total RMD withdrawals in the YearlySummary
 *
 * RMD rules:
 * - Born 1950 or earlier: RMDs begin at age 72
 * - Born 1951-1959: RMDs begin at age 73
 * - Born 1960 or later: RMDs begin at age 75
 *
 * The RMD amount is calculated by dividing the previous year-end qualified account balance
 * by the life expectancy factor from the IRS Uniform Lifetime Table.
 */
public class RMDOptimizationStrategy implements TaxOptimizationStrategy {

    private final RMDCalculator rmdCalculator;

    public RMDOptimizationStrategy() {
        this.rmdCalculator = new RMDCalculator();
    }

    @Override
    public void optimize(YearlySummary previousYearlySummary, YearlySummary currentYearlySummary) {
        if (currentYearlySummary == null) {
            return;
        }

        double totalRmdWithdrawals = 0.0;

        // Process each individual in the current year summary
        for (IndividualYearlySummary individual : currentYearlySummary.individualSummaries().values()) {
            double individualRmd = calculateAndSetRmd(individual, previousYearlySummary);
            totalRmdWithdrawals += individualRmd;
        }

        // Update the YearlySummary's qualified assets to reflect the sum of individual qualified assets
        // (since setRmdWithdrawals on individuals reduces their qualified assets)
        updateYearlySummaryAssets(currentYearlySummary);

        // Set the total RMD withdrawals (without reducing qualified assets again since we already synced above)
        setRmdWithdrawalsWithoutAssetReduction(currentYearlySummary, totalRmdWithdrawals);
    }

    /**
     * Updates the YearlySummary's qualified assets to reflect the sum of individual qualified assets.
     */
    private void updateYearlySummaryAssets(YearlySummary summary) {
        double totalQualified = 0.0;
        for (IndividualYearlySummary individual : summary.individualSummaries().values()) {
            totalQualified += individual.qualifiedAssets();
        }
        summary.setQualifiedAssets(totalQualified);
    }

    /**
     * Sets RMD withdrawals on the YearlySummary without reducing qualified assets
     * (because individual asset reductions have already been synced to the summary).
     */
    private void setRmdWithdrawalsWithoutAssetReduction(YearlySummary summary, double rmdWithdrawals) {
        // We need to work around the fact that setRmdWithdrawals reduces qualified assets
        // by temporarily increasing qualified assets, then calling setRmdWithdrawals
        double currentQualified = summary.qualifiedAssets();
        summary.setQualifiedAssets(currentQualified + rmdWithdrawals);
        summary.setRmdWithdrawals(rmdWithdrawals);
        // Now qualified assets should be back to currentQualified
    }

    /**
     * Calculates and sets the RMD for an individual.
     *
     * @param individual the individual yearly summary to update
     * @param previousYearlySummary the previous year's summary (for prior year-end balance)
     * @return the calculated RMD amount
     */
    private double calculateAndSetRmd(IndividualYearlySummary individual, YearlySummary previousYearlySummary) {
        Person person = individual.person();
        if (person == null) {
            individual.setRmdWithdrawals(0.0);
            return 0.0;
        }

        int currentYear = individual.year();
        int age = person.getAgeInYear(currentYear);
        int birthYear = person.yearOfBirth();

        // Check if RMD is required
        if (!rmdCalculator.isRMDRequired(age, birthYear)) {
            individual.setRmdWithdrawals(0.0);
            return 0.0;
        }

        // Get previous year's qualified assets for this individual
        double previousYearQualifiedAssets = getPreviousYearQualifiedAssets(individual.name(), previousYearlySummary);

        // Calculate RMD if there are qualified assets
        if (previousYearQualifiedAssets <= 0) {
            individual.setRmdWithdrawals(0.0);
            return 0.0;
        }

        double rmd = rmdCalculator.calculateRMD(age, previousYearQualifiedAssets);
        individual.setRmdWithdrawals(rmd);

        return rmd;
    }

    /**
     * Gets the previous year's qualified assets for an individual.
     *
     * @param name the individual's name
     * @param previousYearlySummary the previous year's summary
     * @return the previous year-end qualified assets, or 0 if not available
     */
    private double getPreviousYearQualifiedAssets(String name, YearlySummary previousYearlySummary) {
        if (previousYearlySummary == null || name == null) {
            return 0.0;
        }

        IndividualYearlySummary previousIndividual = previousYearlySummary.getIndividualSummary(name);
        if (previousIndividual == null) {
            return 0.0;
        }

        return previousIndividual.qualifiedAssets();
    }

    /**
     * Calculates the RMD for a specific individual without modifying any state.
     * Useful for projections and planning.
     *
     * @param person the person
     * @param year the year for which to calculate RMD
     * @param previousYearQualifiedAssets the previous year-end qualified assets
     * @return the calculated RMD amount, or 0 if RMD is not required
     */
    public double calculateRmd(Person person, int year, double previousYearQualifiedAssets) {
        if (person == null || previousYearQualifiedAssets <= 0) {
            return 0.0;
        }

        int age = person.getAgeInYear(year);
        int birthYear = person.yearOfBirth();

        if (!rmdCalculator.isRMDRequired(age, birthYear)) {
            return 0.0;
        }

        return rmdCalculator.calculateRMD(age, previousYearQualifiedAssets);
    }

    /**
     * Returns the RMD start age for a person based on their birth year.
     *
     * @param person the person
     * @return the age at which RMDs must begin
     */
    public int getRmdStartAge(Person person) {
        if (person == null) {
            throw new IllegalArgumentException("Person cannot be null");
        }
        return rmdCalculator.getRMDStartAge(person.yearOfBirth());
    }

    /**
     * Returns the first year in which RMDs are required for a person.
     *
     * @param person the person
     * @return the first year RMDs are required
     */
    public int getFirstRmdYear(Person person) {
        if (person == null) {
            throw new IllegalArgumentException("Person cannot be null");
        }
        int startAge = rmdCalculator.getRMDStartAge(person.yearOfBirth());
        return person.yearOfBirth() + startAge;
    }

    @Override
    public String getStrategyName() {
        return "RMD Optimization";
    }

    @Override
    public String getDescription() {
        return "Calculates and applies Required Minimum Distributions (RMDs) from qualified " +
                "retirement accounts based on IRS rules. RMDs are mandatory withdrawals that " +
                "must begin at age 72-75 depending on birth year, calculated using the IRS " +
                "Uniform Lifetime Table and the previous year-end account balance.";
    }
}
