package rg.financialplanning.web.dto;

import java.util.List;

/**
 * The conversation about one version of the plan. {@code suggestions} is null
 * on a status poll, which carries no plan to derive them from.
 */
public record ExplainThreadDTO(
        String planHash,
        boolean running,
        List<ExplanationDTO> messages,
        List<String> suggestions,
        List<ExplanationDTO> earlier) {
}
