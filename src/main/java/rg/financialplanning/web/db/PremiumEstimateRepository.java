package rg.financialplanning.web.db;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import rg.financialplanning.web.dto.HealthPremiumEstimateDTO;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Every premium estimate a person asked for is kept, with the prompt that
 * produced it, so the figures in the plan can be traced back to their sources.
 * The UI reads the newest one per person.
 */
public class PremiumEstimateRepository {

    private static final Gson GSON = new Gson();

    private final Database db;

    public PremiumEstimateRepository(Database db) {
        this.db = db;
    }

    public long record(HealthPremiumEstimateDTO e, String prompt) {
        return record(PlanRepository.DEFAULT_PLAN, e, prompt);
    }

    public long record(String planName, HealthPremiumEstimateDTO e, String prompt) {
        return db.inTransaction(c -> {
            long planId = PlanRepository.ensurePlan(c, planName);
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO premium_estimates(plan_id, person_key, person_name, plan_tier, requested_at, completed_at, "
                            + "status, state, gap_start, gap_end, medicare_year, pre_medicare_monthly, medicare_plan_monthly, "
                            + "medical_inflation_pct, notes, sources, prompt, error, duration_ms) "
                            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    java.sql.Statement.RETURN_GENERATED_KEYS)) {
                ps.setLong(1, planId);
                ps.setString(2, key(e.personName()));
                ps.setString(3, e.personName());
                ps.setString(4, e.planTier());
                ps.setString(5, e.requestedAt());
                ps.setString(6, e.completedAt());
                ps.setString(7, e.status());
                ps.setString(8, e.state());
                ps.setInt(9, e.gapStart());
                ps.setInt(10, e.gapEnd());
                ps.setInt(11, e.medicareYear());
                ps.setDouble(12, e.preMedicarePremiumMonthly());
                ps.setDouble(13, e.medicarePlanPremiumMonthly());
                ps.setDouble(14, e.medicalInflationPct());
                ps.setString(15, e.notes());
                ps.setString(16, GSON.toJson(e.sources() == null ? List.of() : e.sources()));
                ps.setString(17, prompt);
                ps.setString(18, e.error());
                ps.setLong(19, e.durationMs());
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    keys.next();
                    return keys.getLong(1);
                }
            }
        });
    }

    public Optional<HealthPremiumEstimateDTO> latest(String personName) {
        return latest(PlanRepository.DEFAULT_PLAN, personName);
    }

    public Optional<HealthPremiumEstimateDTO> latest(String planName, String personName) {
        return db.read(c -> {
            Long planId = PlanRepository.planId(c, planName);
            if (planId == null) return Optional.empty();
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT person_name, status, plan_tier, requested_at, completed_at, duration_ms, state, gap_start, "
                            + "gap_end, medicare_year, pre_medicare_monthly, medicare_plan_monthly, medical_inflation_pct, "
                            + "notes, sources, error FROM premium_estimates WHERE plan_id = ? AND person_key = ? "
                            + "ORDER BY id DESC LIMIT 1")) {
                ps.setLong(1, planId);
                ps.setString(2, key(personName));
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) return Optional.empty();
                    List<String> sources = GSON.fromJson(rs.getString("sources"),
                            new TypeToken<List<String>>() {}.getType());
                    String state = rs.getString("state");
                    return Optional.of(new HealthPremiumEstimateDTO(
                            rs.getString("person_name"),
                            rs.getString("status"),
                            rs.getString("plan_tier"),
                            rs.getString("requested_at"),
                            rs.getString("completed_at"),
                            rs.getLong("duration_ms"),
                            state,
                            rg.financialplanning.web.health.HealthPremiumPrompt.displayName(state),
                            rs.getInt("gap_start"),
                            rs.getInt("gap_end"),
                            rs.getInt("medicare_year"),
                            rs.getDouble("pre_medicare_monthly"),
                            rs.getDouble("medicare_plan_monthly"),
                            rs.getDouble("medical_inflation_pct"),
                            rs.getString("notes"),
                            sources == null ? List.of() : sources,
                            rs.getString("error")));
                }
            }
        });
    }

    static String key(String personName) {
        return personName == null ? "" : personName.trim().toLowerCase(Locale.ROOT);
    }
}
