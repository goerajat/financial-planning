package rg.financialplanning.web.db;

import rg.financialplanning.web.dto.RefreshReportDTO;
import rg.financialplanning.web.dto.RefreshRunDTO;
import rg.financialplanning.web.dto.YearlySummaryDTO;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Every aggregator refresh is kept: the balances it brought in and the yearly
 * projection the plan made with them. Reading the two back side by side is
 * what a plan-vs-actual view needs.
 */
public class RefreshHistoryRepository {

    private final Database db;

    public RefreshHistoryRepository(Database db) {
        this.db = db;
    }

    public long record(RefreshReportDTO report, List<YearlySummaryDTO> projection, Instant at) {
        return record(PlanRepository.DEFAULT_PLAN, report, projection, at);
    }

    public long record(String planName, RefreshReportDTO report, List<YearlySummaryDTO> projection, Instant at) {
        return db.inTransaction(c -> {
            long planId = PlanRepository.ensurePlan(c, planName);
            long runId;
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO refresh_runs(plan_id, refreshed_at, plan_year, imported_count, skipped_count) "
                            + "VALUES (?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS)) {
                ps.setLong(1, planId);
                ps.setString(2, at.toString());
                ps.setInt(3, report.year);
                ps.setInt(4, report.imported.size());
                ps.setInt(5, report.skipped.size());
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    keys.next();
                    runId = keys.getLong(1);
                }
            }
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO refresh_balances(run_id, account_id, person_name, item_type, description, actual, "
                            + "as_of, stale, projected) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
                for (RefreshReportDTO.ImportedRow r : report.imported) {
                    ps.setLong(1, runId);
                    ps.setLong(2, r.accountId);
                    ps.setString(3, r.personName);
                    ps.setString(4, r.itemType);
                    ps.setString(5, r.description);
                    ps.setDouble(6, r.actual);
                    ps.setString(7, r.asOf);
                    ps.setInt(8, r.stale ? 1 : 0);
                    ps.setDouble(9, r.value);
                    ps.addBatch();
                }
                ps.executeBatch();
            }
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO projection_history(run_id, year, net_worth, total_assets, qualified, non_qualified, "
                            + "roth, cash, real_estate, life_insurance_cash, life_insurance_benefits, mortgage, income, "
                            + "social_security, expenses, taxes, withdrawals, rmd, deficit, "
                            + "mortgage_interest, mortgage_principal, mortgage_extra, health_premiums, irmaa) "
                            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
                for (YearlySummaryDTO s : projection) {
                    ps.setLong(1, runId);
                    ps.setInt(2, s.year);
                    ps.setDouble(3, s.netWorth);
                    ps.setDouble(4, s.totalAssets);
                    ps.setDouble(5, s.qualifiedAssets);
                    ps.setDouble(6, s.nonQualifiedAssets);
                    ps.setDouble(7, s.rothAssets);
                    ps.setDouble(8, s.cash);
                    ps.setDouble(9, s.realEstate);
                    ps.setDouble(10, s.lifeInsuranceCash);
                    ps.setDouble(11, s.lifeInsuranceBenefits);
                    ps.setDouble(12, s.mortgageBalance);
                    ps.setDouble(13, s.totalIncome);
                    ps.setDouble(14, s.totalSocialSecurity);
                    ps.setDouble(15, s.totalExpenses);
                    ps.setDouble(16, s.totalTaxes);
                    ps.setDouble(17, s.totalWithdrawals);
                    ps.setDouble(18, s.rmdWithdrawals);
                    ps.setDouble(19, s.deficit);
                    ps.setDouble(20, s.mortgageInterest);
                    ps.setDouble(21, s.mortgagePrincipal);
                    ps.setDouble(22, s.mortgageRepayment);
                    ps.setDouble(23, s.healthPremiums);
                    ps.setDouble(24, s.irmaaSurcharge);
                    ps.addBatch();
                }
                ps.executeBatch();
            }
            return runId;
        });
    }

    /** Runs newest first, with the totals of what each brought in. */
    public List<RefreshRunDTO> runs() {
        return db.read(c -> {
            List<RefreshRunDTO> out = new ArrayList<>();
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT r.id, r.refreshed_at, r.plan_year, r.imported_count, r.skipped_count, "
                            + "COALESCE((SELECT SUM(actual) FROM refresh_balances b WHERE b.run_id = r.id), 0), "
                            + "COALESCE((SELECT SUM(projected) FROM refresh_balances b WHERE b.run_id = r.id), 0), "
                            + "(SELECT net_worth FROM projection_history p WHERE p.run_id = r.id AND p.year = r.plan_year) "
                            + "FROM refresh_runs r ORDER BY r.id DESC")) {
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        RefreshRunDTO run = new RefreshRunDTO();
                        run.id = rs.getLong(1);
                        run.refreshedAt = rs.getString(2);
                        run.planYear = rs.getInt(3);
                        run.importedCount = rs.getInt(4);
                        run.skippedCount = rs.getInt(5);
                        run.actualTotal = rs.getDouble(6);
                        run.projectedTotal = rs.getDouble(7);
                        double nw = rs.getDouble(8);
                        run.netWorthPlanYear = rs.wasNull() ? null : nw;
                        out.add(run);
                    }
                }
            }
            return out;
        });
    }

    public RefreshRunDTO run(long id) {
        for (RefreshRunDTO r : runs()) {
            if (r.id == id) {
                r.balances = balances(id);
                r.projection = projection(id);
                return r;
            }
        }
        return null;
    }

    public List<RefreshRunDTO.Balance> balances(long runId) {
        return db.read(c -> {
            List<RefreshRunDTO.Balance> out = new ArrayList<>();
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT account_id, person_name, item_type, description, actual, as_of, stale, projected "
                            + "FROM refresh_balances WHERE run_id = ? ORDER BY id")) {
                ps.setLong(1, runId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        RefreshRunDTO.Balance b = new RefreshRunDTO.Balance();
                        b.accountId = rs.getLong(1);
                        b.personName = rs.getString(2);
                        b.itemType = rs.getString(3);
                        b.description = rs.getString(4);
                        b.actual = rs.getDouble(5);
                        b.asOf = rs.getString(6);
                        b.stale = rs.getInt(7) != 0;
                        b.projected = rs.getDouble(8);
                        out.add(b);
                    }
                }
            }
            return out;
        });
    }

    public List<YearlySummaryDTO> projection(long runId) {
        return db.read(c -> {
            List<YearlySummaryDTO> out = new ArrayList<>();
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT year, net_worth, total_assets, qualified, non_qualified, roth, cash, real_estate, "
                            + "life_insurance_cash, life_insurance_benefits, mortgage, income, social_security, "
                            + "expenses, taxes, withdrawals, rmd, deficit, mortgage_interest, mortgage_principal, "
                            + "mortgage_extra, health_premiums, irmaa FROM projection_history WHERE run_id = ? ORDER BY year")) {
                ps.setLong(1, runId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        YearlySummaryDTO s = new YearlySummaryDTO();
                        s.year = rs.getInt(1);
                        s.netWorth = rs.getDouble(2);
                        s.totalAssets = rs.getDouble(3);
                        s.qualifiedAssets = rs.getDouble(4);
                        s.nonQualifiedAssets = rs.getDouble(5);
                        s.rothAssets = rs.getDouble(6);
                        s.cash = rs.getDouble(7);
                        s.realEstate = rs.getDouble(8);
                        s.lifeInsuranceCash = rs.getDouble(9);
                        s.lifeInsuranceBenefits = rs.getDouble(10);
                        s.mortgageBalance = rs.getDouble(11);
                        s.totalIncome = rs.getDouble(12);
                        s.totalSocialSecurity = rs.getDouble(13);
                        s.totalExpenses = rs.getDouble(14);
                        s.totalTaxes = rs.getDouble(15);
                        s.totalWithdrawals = rs.getDouble(16);
                        s.rmdWithdrawals = rs.getDouble(17);
                        s.deficit = rs.getDouble(18);
                        s.mortgageInterest = rs.getDouble(19);
                        s.mortgagePrincipal = rs.getDouble(20);
                        s.mortgageRepayment = rs.getDouble(21);
                        s.mortgagePayment = s.mortgageInterest + s.mortgagePrincipal;
                        s.healthPremiums = rs.getDouble(22);
                        s.irmaaSurcharge = rs.getDouble(23);
                        out.add(s);
                    }
                }
            }
            return out;
        });
    }
}
