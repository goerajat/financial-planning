package rg.financialplanning.model;

import rg.financialplanning.calculator.RMDCalculator;
import rg.financialplanning.calculator.SocialSecurityTaxCalculator;

import java.util.Objects;

/**
 * Class representing the financial summary for a specific individual in a specific year.
 */
public class IndividualYearlySummary {
    private final Person person;
    private final String name;
    private final int year;
    private final double income;
    private double qualifiedAssets;
    private double nonQualifiedAssets;
    private double rothAssets;
    private final double socialSecurityBenefits;
    private double qualifiedWithdrawals;
    private double nonQualifiedWithdrawals;
    private double rothWithdrawals;
    private double rmdWithdrawals;
    private double socialSecurityTax;
    private double rothContributions;
    private double qualifiedContributions;
    private double nonQualifiedContributions;
    private double cashWithdrawals;
    private double deficit;

    public IndividualYearlySummary(Person person, int year, double income,
                                   double qualifiedAssets, double nonQualifiedAssets,
                                   double rothAssets, double socialSecurityBenefits) {
        this.person = person;
        this.name = person != null ? person.name() : null;
        this.year = year;
        this.income = income;
        this.qualifiedAssets = qualifiedAssets;
        this.nonQualifiedAssets = nonQualifiedAssets;
        this.rothAssets = rothAssets;
        this.socialSecurityBenefits = socialSecurityBenefits;
        this.qualifiedWithdrawals = 0.0;
        this.nonQualifiedWithdrawals = 0.0;
        this.rothWithdrawals = 0.0;
        this.rmdWithdrawals = 0.0;
        this.socialSecurityTax = 0.0;
        this.rothContributions = 0.0;
        this.qualifiedContributions = 0.0;
        this.nonQualifiedContributions = 0.0;
        this.cashWithdrawals = 0.0;
        this.deficit = 0.0;
    }

    public Person person() {
        return person;
    }

    public String name() {
        return name;
    }

    public int year() {
        return year;
    }

    public double income() {
        return income;
    }

    public double qualifiedAssets() {
        return qualifiedAssets;
    }

    public double nonQualifiedAssets() {
        return nonQualifiedAssets;
    }

    public double rothAssets() {
        return rothAssets;
    }

    public double socialSecurityBenefits() {
        return socialSecurityBenefits;
    }

    public double qualifiedWithdrawals() {
        return qualifiedWithdrawals;
    }

    public double nonQualifiedWithdrawals() {
        return nonQualifiedWithdrawals;
    }

    public double rothWithdrawals() {
        return rothWithdrawals;
    }

    public void setQualifiedWithdrawals(double qualifiedWithdrawals) {
        this.qualifiedWithdrawals = qualifiedWithdrawals;
    }

    public void setNonQualifiedWithdrawals(double nonQualifiedWithdrawals) {
        this.nonQualifiedWithdrawals = nonQualifiedWithdrawals;
    }

    public void setRothWithdrawals(double rothWithdrawals) {
        this.rothWithdrawals = rothWithdrawals;
    }

    public double rmdWithdrawals() {
        return rmdWithdrawals;
    }

    /**
     * Sets the RMD withdrawal amount.
     * The withdrawal is capped at the available qualified assets, and qualified assets
     * are reduced by the withdrawal amount.
     *
     * @param rmdWithdrawals the RMD withdrawal amount
     */
    public void setRmdWithdrawals(double rmdWithdrawals) {
        // Cap withdrawal at available qualified assets
        double actualWithdrawal = Math.min(rmdWithdrawals, this.qualifiedAssets);
        this.rmdWithdrawals = actualWithdrawal;
        // Reduce qualified assets by the withdrawal amount
        this.qualifiedAssets -= actualWithdrawal;
    }

    /**
     * Calculates and sets the RMD withdrawal based on age, birth year, and previous year's qualified assets.
     * The withdrawal is capped at available qualified assets and reduces them accordingly.
     *
     * @param age the age of the individual at the end of the distribution year
     * @param birthYear the birth year of the individual
     * @param previousYearQualifiedAssets the qualified assets balance at the end of the previous year
     * @return the actual RMD withdrawal amount (may be less than calculated if insufficient assets)
     */
    public double calculateRmdWithdrawals(int age, int birthYear, double previousYearQualifiedAssets) {
        RMDCalculator rmdCalculator = new RMDCalculator();
        if (rmdCalculator.isRMDRequired(age, birthYear)) {
            double calculatedRmd = rmdCalculator.calculateRMD(age, previousYearQualifiedAssets);
            setRmdWithdrawals(calculatedRmd);
            return this.rmdWithdrawals; // Return actual withdrawal after capping
        }
        setRmdWithdrawals(0.0);
        return 0.0;
    }

    public void setQualifiedAssets(double qualifiedAssets) {
        this.qualifiedAssets = qualifiedAssets;
    }

    public void setNonQualifiedAssets(double nonQualifiedAssets) {
        this.nonQualifiedAssets = nonQualifiedAssets;
    }

    public void setRothAssets(double rothAssets) {
        this.rothAssets = rothAssets;
    }

    public double socialSecurityTax() {
        return socialSecurityTax;
    }

    public void setSocialSecurityTax(double socialSecurityTax) {
        this.socialSecurityTax = socialSecurityTax;
    }

    public double rothContributions() {
        return rothContributions;
    }

    public void setRothContributions(double rothContributions) {
        this.rothContributions = rothContributions;
    }

    public double qualifiedContributions() {
        return qualifiedContributions;
    }

    public void setQualifiedContributions(double qualifiedContributions) {
        this.qualifiedContributions = qualifiedContributions;
    }

    public double nonQualifiedContributions() {
        return nonQualifiedContributions;
    }

    public void setNonQualifiedContributions(double nonQualifiedContributions) {
        this.nonQualifiedContributions = nonQualifiedContributions;
    }

    public double cashWithdrawals() {
        return cashWithdrawals;
    }

    public void setCashWithdrawals(double cashWithdrawals) {
        this.cashWithdrawals = cashWithdrawals;
    }

    public double deficit() {
        return deficit;
    }

    public void setDeficit(double deficit) {
        this.deficit = deficit;
    }

    /**
     * Calculates and sets the Social Security tax based on income and filing status.
     * Uses the individual's income to calculate total Social Security.
     *
     * @param isSelfEmployed the filing status for Additional Medicare Tax threshold
     * @return the calculated social security tax
     */
    public double calculateSocialSecurityTax(boolean isSelfEmployed) {
        SocialSecurityTaxCalculator calculator = new SocialSecurityTaxCalculator();
        double tax = calculator.calculateSocialSecurityTax(income, isSelfEmployed);
        this.socialSecurityTax = tax;
        return tax;
    }

    public double totalWithdrawals() {
        return qualifiedWithdrawals + nonQualifiedWithdrawals + rothWithdrawals;
    }

    public double totalAssets() {
        return qualifiedAssets + nonQualifiedAssets + rothAssets;
    }

    public double totalIncome() {
        return income + socialSecurityBenefits;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        IndividualYearlySummary that = (IndividualYearlySummary) o;
        return year == that.year &&
                Double.compare(that.income, income) == 0 &&
                Double.compare(that.qualifiedAssets, qualifiedAssets) == 0 &&
                Double.compare(that.nonQualifiedAssets, nonQualifiedAssets) == 0 &&
                Double.compare(that.rothAssets, rothAssets) == 0 &&
                Double.compare(that.socialSecurityBenefits, socialSecurityBenefits) == 0 &&
                Double.compare(that.qualifiedWithdrawals, qualifiedWithdrawals) == 0 &&
                Double.compare(that.nonQualifiedWithdrawals, nonQualifiedWithdrawals) == 0 &&
                Double.compare(that.rothWithdrawals, rothWithdrawals) == 0 &&
                Double.compare(that.rmdWithdrawals, rmdWithdrawals) == 0 &&
                Double.compare(that.socialSecurityTax, socialSecurityTax) == 0 &&
                Double.compare(that.rothContributions, rothContributions) == 0 &&
                Double.compare(that.qualifiedContributions, qualifiedContributions) == 0 &&
                Double.compare(that.nonQualifiedContributions, nonQualifiedContributions) == 0 &&
                Double.compare(that.cashWithdrawals, cashWithdrawals) == 0 &&
                Double.compare(that.deficit, deficit) == 0 &&
                Objects.equals(person, that.person) &&
                Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(person, name, year, income, qualifiedAssets, nonQualifiedAssets, rothAssets,
                socialSecurityBenefits, qualifiedWithdrawals, nonQualifiedWithdrawals, rothWithdrawals,
                rmdWithdrawals, socialSecurityTax, rothContributions, qualifiedContributions,
                nonQualifiedContributions, cashWithdrawals, deficit);
    }

    @Override
    public String toString() {
        return String.format("%s Year %d: Income=%.2f, SS=%.2f, Qualified=%.2f, NonQualified=%.2f, Roth=%.2f, " +
                "QualWd=%.2f, NonQualWd=%.2f, RothWd=%.2f, RMD=%.2f, SSTax=%.2f, " +
                "RothContrib=%.2f, QualContrib=%.2f, NonQualContrib=%.2f, CashWd=%.2f, Deficit=%.2f",
            name, year, income, socialSecurityBenefits, qualifiedAssets, nonQualifiedAssets, rothAssets,
            qualifiedWithdrawals, nonQualifiedWithdrawals, rothWithdrawals, rmdWithdrawals, socialSecurityTax,
            rothContributions, qualifiedContributions, nonQualifiedContributions, cashWithdrawals, deficit);
    }
}
