package rg.financialplanning.web.db;

import rg.financialplanning.web.dto.FinancialEntryDTO;
import rg.financialplanning.web.dto.FinancialPlanDTO;
import rg.financialplanning.web.dto.PersonDTO;
import rg.financialplanning.web.dto.StateByYearDTO;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Stores one named plan. {@link #save} replaces the plan's rows wholesale in a
 * transaction, which is the simplest thing that keeps the browser's whole-plan
 * state and the database in step.
 */
public class PlanRepository {

    public static final String DEFAULT_PLAN = "default";

    private final Database db;

    public PlanRepository(Database db) {
        this.db = db;
    }

    /** The plan, or an empty one (no persons, entries, rates or states) if none was saved yet. */
    public FinancialPlanDTO load() {
        return load(DEFAULT_PLAN);
    }

    public FinancialPlanDTO load(String name) {
        return db.read(c -> {
            Long planId = planId(c, name);
            FinancialPlanDTO plan = new FinancialPlanDTO();
            if (planId == null) return plan;

            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT name, year_of_birth, self_employed, ss_fra_monthly, ss_claim_age, "
                            + "pre_medicare_premium_monthly, medicare_plan_premium_monthly "
                            + "FROM persons WHERE plan_id = ? ORDER BY position")) {
                ps.setLong(1, planId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        PersonDTO p = new PersonDTO(rs.getString(1), rs.getInt(2), rs.getInt(3) != 0);
                        p.setSocialSecurityFraMonthly(rs.getDouble(4));
                        p.setSocialSecurityClaimAge(rs.getInt(5));
                        p.setPreMedicarePremiumMonthly(rs.getDouble(6));
                        p.setMedicarePlanPremiumMonthly(rs.getDouble(7));
                        plan.getPersons().add(p);
                    }
                }
            }
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT id, person_name, item_type, description, value, start_year, end_year, source, "
                            + "annual_payment FROM entries WHERE plan_id = ? ORDER BY position")) {
                ps.setLong(1, planId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        FinancialEntryDTO e = new FinancialEntryDTO(rs.getString(2), rs.getString(3), rs.getString(4),
                                rs.getInt(5), rs.getInt(6), rs.getInt(7));
                        e.setId(rs.getLong(1));
                        e.setSource(rs.getString(8));
                        e.setAnnualPayment(rs.getDouble(9));
                        plan.getEntries().add(e);
                    }
                }
            }
            try (PreparedStatement ps = c.prepareStatement("SELECT item_type, rate FROM rates WHERE plan_id = ?")) {
                ps.setLong(1, planId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) plan.getRates().put(rs.getString(1), rs.getDouble(2));
                }
            }
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT state, start_year, end_year FROM states_by_year WHERE plan_id = ? ORDER BY position")) {
                ps.setLong(1, planId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        plan.getStatesByYear().add(new StateByYearDTO(rs.getString(1), rs.getInt(2), rs.getInt(3)));
                    }
                }
            }
            return plan;
        });
    }

    public boolean exists() {
        return db.read(c -> planId(c, DEFAULT_PLAN) != null);
    }

    public void save(FinancialPlanDTO plan) {
        save(DEFAULT_PLAN, plan);
    }

    public void save(String name, FinancialPlanDTO plan) {
        db.inTransaction(c -> {
            long planId = ensurePlan(c, name);
            for (String table : List.of("persons", "entries", "rates", "states_by_year")) {
                try (PreparedStatement ps = c.prepareStatement("DELETE FROM " + table + " WHERE plan_id = ?")) {
                    ps.setLong(1, planId);
                    ps.executeUpdate();
                }
            }

            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO persons(plan_id, position, name, year_of_birth, self_employed, ss_fra_monthly, "
                            + "ss_claim_age, pre_medicare_premium_monthly, medicare_plan_premium_monthly) "
                            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
                int pos = 0;
                for (PersonDTO p : nn(plan.getPersons())) {
                    ps.setLong(1, planId);
                    ps.setInt(2, pos++);
                    ps.setString(3, p.getName());
                    ps.setInt(4, p.getYearOfBirth());
                    ps.setInt(5, p.isSelfEmployed() ? 1 : 0);
                    ps.setDouble(6, p.getSocialSecurityFraMonthly());
                    ps.setInt(7, p.getSocialSecurityClaimAge());
                    ps.setDouble(8, p.getPreMedicarePremiumMonthly());
                    ps.setDouble(9, p.getMedicarePlanPremiumMonthly());
                    ps.addBatch();
                }
                ps.executeBatch();
            }

            long nextId = 1;
            for (FinancialEntryDTO e : nn(plan.getEntries())) {
                if (e.getId() != null && e.getId() >= nextId) nextId = e.getId() + 1;
            }
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO entries(plan_id, id, position, person_name, item_type, description, value, "
                            + "start_year, end_year, source, annual_payment) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
                int pos = 0;
                for (FinancialEntryDTO e : nn(plan.getEntries())) {
                    long id = e.getId() == null ? nextId++ : e.getId();
                    e.setId(id);
                    ps.setLong(1, planId);
                    ps.setLong(2, id);
                    ps.setInt(3, pos++);
                    ps.setString(4, e.getPersonName());
                    ps.setString(5, e.getItemType());
                    ps.setString(6, e.getDescription() == null ? "" : e.getDescription());
                    ps.setInt(7, e.getValue());
                    ps.setInt(8, e.getStartYear());
                    ps.setInt(9, e.getEndYear());
                    ps.setString(10, e.getSource());
                    ps.setDouble(11, e.getAnnualPayment());
                    ps.addBatch();
                }
                ps.executeBatch();
            }

            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO rates(plan_id, item_type, rate) VALUES (?, ?, ?)")) {
                for (Map.Entry<String, Double> r : nn(plan.getRates()).entrySet()) {
                    if (r.getValue() == null) continue;
                    ps.setLong(1, planId);
                    ps.setString(2, r.getKey());
                    ps.setDouble(3, r.getValue());
                    ps.addBatch();
                }
                ps.executeBatch();
            }

            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO states_by_year(plan_id, position, state, start_year, end_year) VALUES (?, ?, ?, ?, ?)")) {
                int pos = 0;
                for (StateByYearDTO s : nn(plan.getStatesByYear())) {
                    ps.setLong(1, planId);
                    ps.setInt(2, pos++);
                    ps.setString(3, s.getState());
                    ps.setInt(4, s.getStartYear());
                    ps.setInt(5, s.getEndYear());
                    ps.addBatch();
                }
                ps.executeBatch();
            }

            try (PreparedStatement ps = c.prepareStatement("UPDATE plans SET updated_at = ? WHERE id = ?")) {
                ps.setString(1, Instant.now().toString());
                ps.setLong(2, planId);
                ps.executeUpdate();
            }
            return null;
        });
    }

    /** When the default plan was last written, ISO instant, or null if never. */
    public String updatedAt() {
        return db.read(c -> {
            try (PreparedStatement ps = c.prepareStatement("SELECT updated_at FROM plans WHERE name = ?")) {
                ps.setString(1, DEFAULT_PLAN);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? rs.getString(1) : null;
                }
            }
        });
    }

    static Long planId(Connection c, String name) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT id FROM plans WHERE name = ?")) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getLong(1) : null;
            }
        }
    }

    static long ensurePlan(Connection c, String name) throws SQLException {
        Long id = planId(c, name);
        if (id != null) return id;
        String now = Instant.now().toString();
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO plans(name, created_at, updated_at) VALUES (?, ?, ?)", Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, name);
            ps.setString(2, now);
            ps.setString(3, now);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getLong(1);
            }
        }
    }

    private static <T> List<T> nn(List<T> list) {
        return list == null ? new ArrayList<>() : list;
    }

    private static <K, V> Map<K, V> nn(Map<K, V> map) {
        return map == null ? new HashMap<>() : map;
    }
}
