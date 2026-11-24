package rg.financialplanning.web.dto;

/** One question about a projection and its answer; status is RUNNING, DONE or ERROR. */
public record ExplanationDTO(
        long id,
        String question,
        String answer,
        String status,
        String askedAt,
        String answeredAt,
        long durationMs,
        String error) {
}
