package rg.financialplanning.web.db;

import rg.financialplanning.web.dto.ExplanationDTO;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Every question asked about a projection, with the prompt that answered it.
 * Rows are keyed by the plan version they were asked about, so the panel shows
 * one thread per version and a changed plan starts clean.
 */
public class ExplanationRepository {

    private final Database db;

    public ExplanationRepository(Database db) {
        this.db = db;
    }

    public long record(ExplanationDTO e, String planHash, String questionKey, String prompt) {
        return record(PlanRepository.DEFAULT_PLAN, e, planHash, questionKey, prompt);
    }

    public long record(String planName, ExplanationDTO e, String planHash, String questionKey, String prompt) {
        return db.inTransaction(c -> {
            long planId = PlanRepository.ensurePlan(c, planName);
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO explanations(plan_id, plan_hash, question_key, question, answer, prompt, status, error, "
                            + "asked_at, answered_at, duration_ms) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    java.sql.Statement.RETURN_GENERATED_KEYS)) {
                ps.setLong(1, planId);
                ps.setString(2, planHash);
                ps.setString(3, questionKey);
                ps.setString(4, e.question());
                ps.setString(5, e.answer());
                ps.setString(6, prompt);
                ps.setString(7, e.status());
                ps.setString(8, e.error());
                ps.setString(9, e.askedAt());
                ps.setString(10, e.answeredAt());
                ps.setLong(11, e.durationMs());
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    keys.next();
                    return keys.getLong(1);
                }
            }
        });
    }

    /** The thread for one plan version, oldest first, errors included so the panel can offer a retry. */
    public List<ExplanationDTO> thread(String planHash) {
        return db.read(c -> {
            List<ExplanationDTO> out = new ArrayList<>();
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT id, question, answer, status, asked_at, answered_at, duration_ms, error FROM explanations "
                            + "WHERE plan_hash = ? ORDER BY id")) {
                ps.setString(1, planHash);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) out.add(row(rs));
                }
            }
            return out;
        });
    }

    /** The newest answer already given to this question on this plan version. */
    public Optional<ExplanationDTO> answered(String planHash, String questionKey) {
        return db.read(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT id, question, answer, status, asked_at, answered_at, duration_ms, error FROM explanations "
                            + "WHERE plan_hash = ? AND question_key = ? AND status = 'DONE' ORDER BY id DESC LIMIT 1")) {
                ps.setString(1, planHash);
                ps.setString(2, questionKey);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? Optional.of(row(rs)) : Optional.empty();
                }
            }
        });
    }

    /** Answers given about other versions of the plan, newest first. */
    public List<ExplanationDTO> earlier(String planHash, int limit) {
        return db.read(c -> {
            List<ExplanationDTO> out = new ArrayList<>();
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT id, question, answer, status, asked_at, answered_at, duration_ms, error FROM explanations "
                            + "WHERE plan_hash <> ? AND status = 'DONE' ORDER BY id DESC LIMIT ?")) {
                ps.setString(1, planHash);
                ps.setInt(2, limit);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) out.add(row(rs));
                }
            }
            return out;
        });
    }

    private static ExplanationDTO row(ResultSet rs) throws java.sql.SQLException {
        return new ExplanationDTO(
                rs.getLong("id"),
                rs.getString("question"),
                rs.getString("answer"),
                rs.getString("status"),
                rs.getString("asked_at"),
                rs.getString("answered_at"),
                rs.getLong("duration_ms"),
                rs.getString("error"));
    }
}
