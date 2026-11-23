package rg.financialplanning.web.aggregator;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/** One row of the aggregator's {@code GET /api/balances}. */
public record AggregatorBalance(
        long accountId,
        String institutionName,
        String accountName,
        String accountType,
        String holderNames,
        BigDecimal amount,
        String currency,
        Instant capturedAt,
        LocalDate closedOn
) {
    /** Institution without the em-dash holder suffix the aggregator adds to per-login institutions. */
    public String institutionShortName() {
        if (institutionName == null) return "";
        int i = institutionName.indexOf(" — ");
        return i > 0 ? institutionName.substring(0, i) : institutionName;
    }
}
