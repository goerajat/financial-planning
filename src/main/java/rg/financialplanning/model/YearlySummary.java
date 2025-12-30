package rg.financialplanning.model;

import java.util.Map;
import java.util.Objects;

/**
 * Class representing the combined financial summary for a specific year across all individuals.
 */
public class YearlySummary {
    private final int year;
    private final double totalIncome;
    private final double totalExpenses;
    private double qualifiedAssets;
    private double nonQualifiedAssets;
    private double rothAssets;
    private double cash;
    private final double realEstate;
    private final double lifeInsuranceBenefits;
    private final double totalSocialSecurity;
    private final Map<String, IndividualYearlySummary> individualSummaries;
    private double qualifiedWithdrawals;
    private double nonQualifiedWithdrawals;
    private double rothWithdrawals;
    private double rmdWithdrawals;
    private double rothContributions;
    private double qualifiedContributions;
    private double nonQualifiedContributions;
    private double cashWithdrawals;
    private double deficit;
    private double federalIncomeTax;
    private double stateIncomeTax;
    private double capitalGainsTax;
    private double socialSecurityTax;
    private double medicareTax;

    public YearlySummary(int year, double totalIncome, double totalExpenses,
                         double qualifiedAssets, double nonQualifiedAssets,
                         double rothAssets, double cash, double realEstate,
                         double lifeInsuranceBenefits, double totalSocialSecurity,
                         Map<String, IndividualYearlySummary> individualSummaries) {
        this.year = year;
        this.totalIncome = totalIncome;
        this.totalExpenses = totalExpenses;
        this.qualifiedAssets = qualifiedAssets;
        this.nonQualifiedAssets = nonQualifiedAssets;
        this.rothAssets = rothAssets;
        this.cash = cash;
        this.realEstate = realEstate;
        this.lifeInsuranceBenefits = lifeInsuranceBenefits;
        this.totalSocialSecurity = totalSocialSecurity;
        this.individualSummaries = individualSummaries != null ? Map.copyOf(individualSummaries) : Map.of();
        this.qualifiedWithdrawals = 0.0;
        this.nonQualifiedWithdrawals = 0.0;
        this.rothWithdrawals = 0.0;
        this.rmdWithdrawals = 0.0;
        this.rothContributions = 0.0;
        this.qualifiedContributions = 0.0;
        this.nonQualifiedContributions = 0.0;
        this.cashWithdrawals = 0.0;
        this.deficit = 0.0;
        this.federalIncomeTax = 0.0;
        this.stateIncomeTax = 0.0;
        this.capitalGainsTax = 0.0;
        this.socialSecurityTax = 0.0;
        this.medicareTax = 0.0;
    }

    public int year() {
        return year;
    }

    public double totalIncome() {
        return totalIncome;
    }

    public double totalExpenses() {
        return totalExpenses;
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

    public double cash() {
        return cash;
    }

    public double realEstate() {
        return realEstate;
    }

    public double lifeInsuranceBenefits() {
        return lifeInsuranceBenefits;
    }

    public double totalSocialSecurity() {
        return totalSocialSecurity;
    }

    public Map<String, IndividualYearlySummary> individualSummaries() {
        return individualSummaries;
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

    public void setQualifiedAssets(double qualifiedAssets) {
        this.qualifiedAssets = qualifiedAssets;
    }

    public void setNonQualifiedAssets(double nonQualifiedAssets) {
        this.nonQualifiedAssets = nonQualifiedAssets;
    }

    public void setRothAssets(double rothAssets) {
        this.rothAssets = rothAssets;
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

    public void setCash(double cash) {
        this.cash = cash;
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

    public double federalIncomeTax() {
        return federalIncomeTax;
    }

    public void setFederalIncomeTax(double federalIncomeTax) {
        this.federalIncomeTax = federalIncomeTax;
    }

    public double stateIncomeTax() {
        return stateIncomeTax;
    }

    public void setStateIncomeTax(double stateIncomeTax) {
        this.stateIncomeTax = stateIncomeTax;
    }

    public double capitalGainsTax() {
        return capitalGainsTax;
    }

    public void setCapitalGainsTax(double capitalGainsTax) {
        this.capitalGainsTax = capitalGainsTax;
    }

    public double socialSecurityTax() {
        return socialSecurityTax;
    }

    public void setSocialSecurityTax(double socialSecurityTax) {
        this.socialSecurityTax = socialSecurityTax;
    }

    public double medicareTax() {
        return medicareTax;
    }

    public void setMedicareTax(double medicareTax) {
        this.medicareTax = medicareTax;
    }

    public double totalTaxes() {
        return federalIncomeTax + stateIncomeTax + capitalGainsTax + socialSecurityTax + medicareTax;
    }

    public double totalWithdrawals() {
        return qualifiedWithdrawals + nonQualifiedWithdrawals + rothWithdrawals;
    }

    public double totalAssets() {
        return qualifiedAssets + nonQualifiedAssets + rothAssets + cash + realEstate + lifeInsuranceBenefits;
    }

    public double netIncome() {
        return totalIncome - totalExpenses;
    }

    public double netWorth() {
        return totalAssets() + netIncome();
    }

    public IndividualYearlySummary getIndividualSummary(String name) {
        return individualSummaries.get(name);
    }

    public double getIncomeForName(String name) {
        IndividualYearlySummary individual = individualSummaries.get(name);
        return individual != null ? individual.income() : 0.0;
    }

    public double getQualifiedForName(String name) {
        IndividualYearlySummary individual = individualSummaries.get(name);
        return individual != null ? individual.qualifiedAssets() : 0.0;
    }

    public double getNonQualifiedForName(String name) {
        IndividualYearlySummary individual = individualSummaries.get(name);
        return individual != null ? individual.nonQualifiedAssets() : 0.0;
    }

    public double getRothForName(String name) {
        IndividualYearlySummary individual = individualSummaries.get(name);
        return individual != null ? individual.rothAssets() : 0.0;
    }

    public double getSocialSecurityForName(String name) {
        IndividualYearlySummary individual = individualSummaries.get(name);
        return individual != null ? individual.socialSecurityBenefits() : 0.0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        YearlySummary that = (YearlySummary) o;
        return year == that.year &&
                Double.compare(that.totalIncome, totalIncome) == 0 &&
                Double.compare(that.totalExpenses, totalExpenses) == 0 &&
                Double.compare(that.qualifiedAssets, qualifiedAssets) == 0 &&
                Double.compare(that.nonQualifiedAssets, nonQualifiedAssets) == 0 &&
                Double.compare(that.rothAssets, rothAssets) == 0 &&
                Double.compare(that.cash, cash) == 0 &&
                Double.compare(that.realEstate, realEstate) == 0 &&
                Double.compare(that.lifeInsuranceBenefits, lifeInsuranceBenefits) == 0 &&
                Double.compare(that.totalSocialSecurity, totalSocialSecurity) == 0 &&
                Double.compare(that.qualifiedWithdrawals, qualifiedWithdrawals) == 0 &&
                Double.compare(that.nonQualifiedWithdrawals, nonQualifiedWithdrawals) == 0 &&
                Double.compare(that.rothWithdrawals, rothWithdrawals) == 0 &&
                Double.compare(that.rmdWithdrawals, rmdWithdrawals) == 0 &&
                Double.compare(that.rothContributions, rothContributions) == 0 &&
                Double.compare(that.qualifiedContributions, qualifiedContributions) == 0 &&
                Double.compare(that.nonQualifiedContributions, nonQualifiedContributions) == 0 &&
                Double.compare(that.cashWithdrawals, cashWithdrawals) == 0 &&
                Double.compare(that.deficit, deficit) == 0 &&
                Double.compare(that.federalIncomeTax, federalIncomeTax) == 0 &&
                Double.compare(that.stateIncomeTax, stateIncomeTax) == 0 &&
                Double.compare(that.capitalGainsTax, capitalGainsTax) == 0 &&
                Double.compare(that.socialSecurityTax, socialSecurityTax) == 0 &&
                Double.compare(that.medicareTax, medicareTax) == 0 &&
                Objects.equals(individualSummaries, that.individualSummaries);
    }

    @Override
    public int hashCode() {
        return Objects.hash(year, totalIncome, totalExpenses, qualifiedAssets,
                nonQualifiedAssets, rothAssets, cash, realEstate, lifeInsuranceBenefits,
                totalSocialSecurity, qualifiedWithdrawals, nonQualifiedWithdrawals,
                rothWithdrawals, rmdWithdrawals, rothContributions, qualifiedContributions,
                nonQualifiedContributions, cashWithdrawals, deficit,
                federalIncomeTax, stateIncomeTax, capitalGainsTax, socialSecurityTax, medicareTax,
                individualSummaries);
    }

    @Override
    public String toString() {
        return String.format("Year %d: Income=%.2f, Expenses=%.2f, Qualified=%.2f, NonQualified=%.2f, Roth=%.2f, " +
                "Cash=%.2f, RealEstate=%.2f, LifeIns=%.2f, SS=%.2f, QualWd=%.2f, NonQualWd=%.2f, RothWd=%.2f, RMD=%.2f, " +
                "RothContrib=%.2f, QualContrib=%.2f, NonQualContrib=%.2f, CashWd=%.2f, Deficit=%.2f, " +
                "FedTax=%.2f, StateTax=%.2f, CapGainsTax=%.2f, SSTax=%.2f, MedicareTax=%.2f, Net=%.2f",
            year, totalIncome, totalExpenses, qualifiedAssets, nonQualifiedAssets, rothAssets, cash, realEstate,
            lifeInsuranceBenefits, totalSocialSecurity, qualifiedWithdrawals, nonQualifiedWithdrawals, rothWithdrawals,
            rmdWithdrawals, rothContributions, qualifiedContributions, nonQualifiedContributions,
            cashWithdrawals, deficit, federalIncomeTax, stateIncomeTax, capitalGainsTax, socialSecurityTax, medicareTax,
            netIncome());
    }

    public void replan(YearlySummary previousYearlySummary) {


    }
}
