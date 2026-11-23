package rg.financialplanning.web.dto;

import rg.financialplanning.model.YearlySummary;

/** One projected year, flattened for the web client's results table. */
public class YearlySummaryDTO {
    public int year;
    public double totalIncome;
    public double totalExpenses;
    public double totalSocialSecurity;
    public double totalTaxes;
    public double totalWithdrawals;
    public double rmdWithdrawals;
    public double deficit;
    public double qualifiedAssets;
    public double nonQualifiedAssets;
    public double rothAssets;
    public double cash;
    public double realEstate;
    public double lifeInsuranceCash;
    public double lifeInsuranceBenefits;
    public double mortgageBalance;
    public double mortgagePayment;
    public double mortgageInterest;
    public double mortgagePrincipal;
    public double mortgageRepayment;
    public double healthPremiums;
    public double irmaaSurcharge;
    public double totalAssets;
    public double netWorth;

    public static YearlySummaryDTO from(YearlySummary s) {
        YearlySummaryDTO d = new YearlySummaryDTO();
        d.year = s.year();
        d.totalIncome = s.totalIncome();
        d.totalExpenses = s.totalExpenses();
        d.totalSocialSecurity = s.totalSocialSecurity();
        d.totalTaxes = s.totalTaxes();
        d.totalWithdrawals = s.totalWithdrawals();
        d.rmdWithdrawals = s.rmdWithdrawals();
        d.deficit = s.deficit();
        d.qualifiedAssets = s.qualifiedAssets();
        d.nonQualifiedAssets = s.nonQualifiedAssets();
        d.rothAssets = s.rothAssets();
        d.cash = s.cash();
        d.realEstate = s.realEstate();
        d.lifeInsuranceCash = s.lifeInsuranceCash();
        d.lifeInsuranceBenefits = s.lifeInsuranceBenefits();
        d.mortgageBalance = s.mortgageBalance();
        d.mortgagePayment = s.mortgagePayment();
        d.mortgageInterest = s.mortgageInterest();
        d.mortgagePrincipal = s.mortgagePrincipal();
        d.mortgageRepayment = s.mortgageRepayment();
        d.healthPremiums = s.healthPremiums();
        d.irmaaSurcharge = s.irmaaSurcharge();
        d.totalAssets = s.totalAssets();
        d.netWorth = s.netWorth();
        return d;
    }
}
