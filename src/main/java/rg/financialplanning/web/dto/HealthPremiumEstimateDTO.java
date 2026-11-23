package rg.financialplanning.web.dto;

import java.util.List;

/**
 * One person's latest premium estimate as the UI sees it. {@code status} is
 * NONE (never run), RUNNING, DONE or ERROR; the figures are set only for DONE.
 */
public record HealthPremiumEstimateDTO(
        String personName,
        String status,
        String planTier,
        String requestedAt,
        String completedAt,
        long durationMs,
        String state,
        String stateName,
        int gapStart,
        int gapEnd,
        int medicareYear,
        double preMedicarePremiumMonthly,
        double medicarePlanPremiumMonthly,
        double medicalInflationPct,
        String notes,
        List<String> sources,
        String error) {

    public static HealthPremiumEstimateDTO none(String personName) {
        return new HealthPremiumEstimateDTO(personName, "NONE", null, null, null, 0, null, null, 0, 0, 0,
                0, 0, 0, null, List.of(), null);
    }

    public HealthPremiumEstimateDTO withStatus(String newStatus, String newRequestedAt) {
        return new HealthPremiumEstimateDTO(personName, newStatus, planTier, newRequestedAt, completedAt, durationMs,
                state, stateName, gapStart, gapEnd, medicareYear, preMedicarePremiumMonthly, medicarePlanPremiumMonthly,
                medicalInflationPct, notes, sources, error);
    }
}
