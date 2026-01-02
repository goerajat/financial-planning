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
    private double mortgagePayment;
    private double mortgageBalance;
    private double mortgageRepayment;

    public YearlySummary(int year, double totalIncome, double totalExpenses,
                         double qualifiedAssets, double nonQualifiedAssets,
                         double rothAssets, double cash, double realEstate,
                         double lifeInsuranceBenefits, double totalSocialSecurity,
                         double rothContributions,
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
        this.rothContributions = rothContributions;
        this.qualifiedContributions = 0.0;
        this.nonQualifiedContributions = 0.0;
        this.cashWithdrawals = 0.0;
        this.deficit = 0.0;
        this.federalIncomeTax = 0.0;
        this.stateIncomeTax = 0.0;
        this.capitalGainsTax = 0.0;
        this.socialSecurityTax = 0.0;
        this.medicareTax = 0.0;
        this.mortgagePayment = 0.0;
        this.mortgageBalance = 0.0;
        this.mortgageRepayment = 0.0;
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

    public double mortgagePayment() {
        return mortgagePayment;
    }

    public void setMortgagePayment(double mortgagePayment) {
        this.mortgagePayment = mortgagePayment;
    }

    public double mortgageBalance() {
        return mortgageBalance;
    }

    public void setMortgageBalance(double mortgageBalance) {
        this.mortgageBalance = mortgageBalance;
    }

    public double mortgageRepayment() {
        return mortgageRepayment;
    }

    public void setMortgageRepayment(double mortgageRepayment) {
        this.mortgageRepayment = mortgageRepayment;
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

    // ===== Validation Methods =====

    private static final double EPSILON = 0.01; // Tolerance for floating point comparison

    /**
     * Calculates the sum of individual incomes.
     */
    public double sumIndividualIncome() {
        return individualSummaries.values().stream()
                .mapToDouble(IndividualYearlySummary::income)
                .sum();
    }

    /**
     * Calculates the sum of individual RMD withdrawals.
     */
    public double sumIndividualRmdWithdrawals() {
        return individualSummaries.values().stream()
                .mapToDouble(IndividualYearlySummary::rmdWithdrawals)
                .sum();
    }

    /**
     * Calculates the sum of individual Roth withdrawals.
     */
    public double sumIndividualRothWithdrawals() {
        return individualSummaries.values().stream()
                .mapToDouble(IndividualYearlySummary::rothWithdrawals)
                .sum();
    }

    /**
     * Calculates the sum of individual qualified withdrawals.
     */
    public double sumIndividualQualifiedWithdrawals() {
        return individualSummaries.values().stream()
                .mapToDouble(IndividualYearlySummary::qualifiedWithdrawals)
                .sum();
    }

    /**
     * Calculates the sum of individual non-qualified withdrawals.
     */
    public double sumIndividualNonQualifiedWithdrawals() {
        return individualSummaries.values().stream()
                .mapToDouble(IndividualYearlySummary::nonQualifiedWithdrawals)
                .sum();
    }

    /**
     * Calculates the sum of individual social security benefits.
     */
    public double sumIndividualSocialSecurity() {
        return individualSummaries.values().stream()
                .mapToDouble(IndividualYearlySummary::socialSecurityBenefits)
                .sum();
    }

    /**
     * Validates that the sum of individual values equals the corresponding yearly summary totals.
     * Checks: income, RMD withdrawals, Roth withdrawals, qualified withdrawals,
     * non-qualified withdrawals, and social security benefits.
     *
     * @return true if all individual totals match the yearly summary totals
     */
    public boolean validateIndividualTotals() {
        return Math.abs(sumIndividualIncome() - totalIncome) < EPSILON &&
               Math.abs(sumIndividualRmdWithdrawals() - rmdWithdrawals) < EPSILON &&
               Math.abs(sumIndividualRothWithdrawals() - rothWithdrawals) < EPSILON &&
               Math.abs(sumIndividualQualifiedWithdrawals() - qualifiedWithdrawals) < EPSILON &&
               Math.abs(sumIndividualNonQualifiedWithdrawals() - nonQualifiedWithdrawals) < EPSILON &&
               Math.abs(sumIndividualSocialSecurity() - totalSocialSecurity) < EPSILON;
    }

    /**
     * Returns a detailed validation result for individual totals.
     * Useful for debugging when validation fails.
     *
     * @return a string describing any mismatches, or "Valid" if all match
     */
    public String getIndividualTotalsValidationDetails() {
        StringBuilder sb = new StringBuilder();

        double incomeSum = sumIndividualIncome();
        if (Math.abs(incomeSum - totalIncome) >= EPSILON) {
            sb.append(String.format("Income mismatch: sum=%.2f, total=%.2f%n", incomeSum, totalIncome));
        }

        double rmdSum = sumIndividualRmdWithdrawals();
        if (Math.abs(rmdSum - rmdWithdrawals) >= EPSILON) {
            sb.append(String.format("RMD Withdrawals mismatch: sum=%.2f, total=%.2f%n", rmdSum, rmdWithdrawals));
        }

        double rothWdSum = sumIndividualRothWithdrawals();
        if (Math.abs(rothWdSum - rothWithdrawals) >= EPSILON) {
            sb.append(String.format("Roth Withdrawals mismatch: sum=%.2f, total=%.2f%n", rothWdSum, rothWithdrawals));
        }

        double qualWdSum = sumIndividualQualifiedWithdrawals();
        if (Math.abs(qualWdSum - qualifiedWithdrawals) >= EPSILON) {
            sb.append(String.format("Qualified Withdrawals mismatch: sum=%.2f, total=%.2f%n", qualWdSum, qualifiedWithdrawals));
        }

        double nonQualWdSum = sumIndividualNonQualifiedWithdrawals();
        if (Math.abs(nonQualWdSum - nonQualifiedWithdrawals) >= EPSILON) {
            sb.append(String.format("Non-Qualified Withdrawals mismatch: sum=%.2f, total=%.2f%n", nonQualWdSum, nonQualifiedWithdrawals));
        }

        double ssSum = sumIndividualSocialSecurity();
        if (Math.abs(ssSum - totalSocialSecurity) >= EPSILON) {
            sb.append(String.format("Social Security mismatch: sum=%.2f, total=%.2f%n", ssSum, totalSocialSecurity));
        }

        return sb.length() == 0 ? "Valid" : sb.toString().trim();
    }

    /**
     * Calculates total cash inflows: income + social security + all withdrawals.
     */
    public double totalCashInflows() {
        return totalIncome + totalSocialSecurity + rmdWithdrawals + qualifiedWithdrawals +
               nonQualifiedWithdrawals + rothWithdrawals + cashWithdrawals;
    }

    /**
     * Calculates total cash outflows: expenses + taxes + all contributions + mortgage payment + mortgage repayment.
     */
    public double totalCashOutflows() {
        return totalExpenses + totalTaxes() + rothContributions + qualifiedContributions + nonQualifiedContributions + mortgagePayment + mortgageRepayment;
    }

    /**
     * Calculates total contributions: roth + qualified + non-qualified.
     */
    public double totalContributions() {
        return rothContributions + qualifiedContributions + nonQualifiedContributions;
    }

    /**
     * Validates that total cash inflows plus deficit equal total cash outflows.
     * Formula: Income + SS + Withdrawals + Deficit = Expenses + Taxes + Contributions
     * (Deficit fills the gap when inflows are insufficient to cover outflows)
     *
     * @return true if cash flow is balanced
     */
    public boolean validateCashFlow() {
        double inflows = totalCashInflows();
        double outflows = totalCashOutflows();
        return Math.abs(inflows + deficit - outflows) < EPSILON;
    }

    /**
     * Returns a detailed validation result for cash flow.
     * Useful for debugging when validation fails.
     *
     * @return a string describing the cash flow breakdown and any imbalance
     */
    public String getCashFlowValidationDetails() {
        double inflows = totalCashInflows();
        double outflows = totalCashOutflows();
        double difference = inflows + deficit - outflows;

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Cash Inflows: %.2f%n", inflows));
        sb.append(String.format("  Income: %.2f%n", totalIncome));
        sb.append(String.format("  Social Security: %.2f%n", totalSocialSecurity));
        sb.append(String.format("  RMD Withdrawals: %.2f%n", rmdWithdrawals));
        sb.append(String.format("  Qualified Withdrawals: %.2f%n", qualifiedWithdrawals));
        sb.append(String.format("  Non-Qualified Withdrawals: %.2f%n", nonQualifiedWithdrawals));
        sb.append(String.format("  Roth Withdrawals: %.2f%n", rothWithdrawals));
        sb.append(String.format("  Cash Withdrawals: %.2f%n", cashWithdrawals));
        sb.append(String.format("Cash Outflows: %.2f%n", outflows));
        sb.append(String.format("  Expenses: %.2f%n", totalExpenses));
        sb.append(String.format("  Total Taxes: %.2f%n", totalTaxes()));
        sb.append(String.format("  Roth Contributions: %.2f%n", rothContributions));
        sb.append(String.format("  Qualified Contributions: %.2f%n", qualifiedContributions));
        sb.append(String.format("  Non-Qualified Contributions: %.2f%n", nonQualifiedContributions));
        sb.append(String.format("  Mortgage Payment: %.2f%n", mortgagePayment));
        sb.append(String.format("  Mortgage Repayment: %.2f%n", mortgageRepayment));
        sb.append(String.format("Deficit: %.2f%n", deficit));
        sb.append(String.format("Difference (should be 0): %.2f%n", difference));
        sb.append(Math.abs(difference) < EPSILON ? "Status: VALID" : "Status: INVALID");

        return sb.toString();
    }

    /**
     * Validates both individual totals and cash flow.
     *
     * @return true if both validations pass
     */
    public boolean validate() {
        return validateIndividualTotals() && validateCashFlow();
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
                Double.compare(that.mortgagePayment, mortgagePayment) == 0 &&
                Double.compare(that.mortgageBalance, mortgageBalance) == 0 &&
                Double.compare(that.mortgageRepayment, mortgageRepayment) == 0 &&
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
                mortgagePayment, mortgageBalance, mortgageRepayment, individualSummaries);
    }

    @Override
    public String toString() {
        return String.format("Year %d: Income=%.2f, Expenses=%.2f, Qualified=%.2f, NonQualified=%.2f, Roth=%.2f, " +
                "Cash=%.2f, RealEstate=%.2f, LifeIns=%.2f, SS=%.2f, QualWd=%.2f, NonQualWd=%.2f, RothWd=%.2f, RMD=%.2f, " +
                "RothContrib=%.2f, QualContrib=%.2f, NonQualContrib=%.2f, CashWd=%.2f, Deficit=%.2f, " +
                "FedTax=%.2f, StateTax=%.2f, CapGainsTax=%.2f, SSTax=%.2f, MedicareTax=%.2f, " +
                "MortgagePmt=%.2f, MortgageBal=%.2f, MortgageRepay=%.2f, Net=%.2f",
            year, totalIncome, totalExpenses, qualifiedAssets, nonQualifiedAssets, rothAssets, cash, realEstate,
            lifeInsuranceBenefits, totalSocialSecurity, qualifiedWithdrawals, nonQualifiedWithdrawals, rothWithdrawals,
            rmdWithdrawals, rothContributions, qualifiedContributions, nonQualifiedContributions,
            cashWithdrawals, deficit, federalIncomeTax, stateIncomeTax, capitalGainsTax, socialSecurityTax, medicareTax,
            mortgagePayment, mortgageBalance, mortgageRepayment, netIncome());
    }

    public void replan(YearlySummary previousYearlySummary) {


    }
}
