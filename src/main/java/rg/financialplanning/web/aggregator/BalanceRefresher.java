package rg.financialplanning.web.aggregator;

import rg.financialplanning.model.ItemType;
import rg.financialplanning.web.dto.FinancialEntryDTO;
import rg.financialplanning.web.dto.FinancialPlanDTO;
import rg.financialplanning.web.dto.PersonDTO;
import rg.financialplanning.web.dto.RefreshBalancesResponseDTO;
import rg.financialplanning.web.dto.RefreshReportDTO;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Replaces the aggregator-sourced entries of a plan with fresh balances.
 *
 * The planner treats an asset entry's value as the balance at the end of its start year
 * (seeded ungrown, grown from the following year), so each balance is compounded at the
 * plan's rate for its bucket from its capture date to 31 December and written as a
 * current-year entry.
 */
public class BalanceRefresher {

    public static final ZoneId ZONE = ZoneId.of("America/New_York");

    private static final Set<String> REFRESHABLE_TYPES = Set.of(
            "QUALIFIED", "NON_QUALIFIED", "ROTH", "CASH", "LIFE_INSURANCE_CASH");

    public RefreshBalancesResponseDTO refresh(FinancialPlanDTO plan, List<AggregatorBalance> balances,
                                              AggregatorConfig cfg, LocalDate today) {
        int year = today.getYear();
        LocalDate yearEnd = LocalDate.of(year, 12, 31);
        Map<String, Double> rates = plan.getRates() == null ? Map.of() : plan.getRates();
        List<FinancialEntryDTO> existing = plan.getEntries() == null ? List.of() : plan.getEntries();

        List<FinancialEntryDTO> kept = new ArrayList<>();
        long maxId = 0;
        int maxEndYear = Integer.MIN_VALUE;
        for (FinancialEntryDTO e : existing) {
            if (e.getEndYear() > maxEndYear) maxEndYear = e.getEndYear();
            if (isAggregatorRow(e)) continue;
            kept.add(e);
            if (e.getId() != null && e.getId() > maxId) maxId = e.getId();
        }
        int endYear = maxEndYear == Integer.MIN_VALUE ? year + 30 : Math.max(maxEndYear, year);

        Set<String> planPersons = new LinkedHashSet<>();
        if (plan.getPersons() != null) {
            for (PersonDTO p : plan.getPersons()) planPersons.add(p.getName());
        }

        RefreshReportDTO report = new RefreshReportDTO();
        report.year = year;
        Set<String> unknownPersons = new TreeSet<>();
        List<FinancialEntryDTO> imported = new ArrayList<>();

        for (AggregatorBalance b : balances) {
            String label = (b.institutionShortName() + " " + (b.accountName() == null ? "" : b.accountName())).trim();
            AggregatorConfig.AccountOverride ov = cfg.accountOverride(b.accountId());
            if (ov != null && Boolean.TRUE.equals(ov.skip)) {
                report.skipped.add(new RefreshReportDTO.SkippedRow(label, "skipped by config"));
                continue;
            }
            if (b.closedOn() != null) {
                report.skipped.add(new RefreshReportDTO.SkippedRow(label, "closed " + b.closedOn()));
                continue;
            }
            if (b.amount() == null) {
                report.skipped.add(new RefreshReportDTO.SkippedRow(label, "no balance"));
                continue;
            }
            String itemType = ov != null && ov.itemType != null && !ov.itemType.isBlank()
                    ? ov.itemType : cfg.itemTypeFor(b.accountType());
            if (itemType == null) {
                report.skipped.add(new RefreshReportDTO.SkippedRow(label,
                        "no item type for account type " + b.accountType() + " (add to typeMap or accounts)"));
                continue;
            }
            try {
                ItemType.valueOf(itemType);
            } catch (IllegalArgumentException ex) {
                report.skipped.add(new RefreshReportDTO.SkippedRow(label, "unknown item type " + itemType + " in config"));
                continue;
            }
            if (b.amount().signum() < 0) {
                report.skipped.add(new RefreshReportDTO.SkippedRow(label, "negative balance"));
                continue;
            }

            List<String> persons = new ArrayList<>();
            if (ov != null && ov.person != null && !ov.person.isBlank()) {
                persons.add(ov.person);
            } else {
                List<String> unmapped = new ArrayList<>();
                for (String holder : splitHolders(b.holderNames())) {
                    String p = cfg.personFor(holder);
                    if (p == null) {
                        unmapped.add(holder);
                    } else if (!persons.contains(p)) {
                        persons.add(p);
                    }
                }
                if (persons.isEmpty()) {
                    report.skipped.add(new RefreshReportDTO.SkippedRow(label, unmapped.isEmpty()
                            ? "no holder on the account"
                            : "holder not mapped: " + String.join(", ", unmapped)));
                    continue;
                }
            }
            for (String p : persons) {
                if (!planPersons.contains(p)) unknownPersons.add(p);
            }

            LocalDate capturedDate = b.capturedAt() == null ? today : b.capturedAt().atZone(ZONE).toLocalDate();
            double rate = rates.getOrDefault(itemType, 0.0);
            double projected = projectToYearEnd(b.amount(), rate, capturedDate, yearEnd, today.lengthOfYear());
            boolean stale = b.capturedAt() != null
                    && ChronoUnit.DAYS.between(capturedDate, today) > cfg.staleDays();

            double share = projected / persons.size();
            for (String person : persons) {
                FinancialEntryDTO e = new FinancialEntryDTO(person, itemType,
                        label + " (as of " + capturedDate + ")",
                        (int) Math.round(share), year, endYear);
                e.setSource(AggregatorConfig.SOURCE_PREFIX + b.accountId());
                e.setId(++maxId);
                imported.add(e);

                RefreshReportDTO.ImportedRow row = new RefreshReportDTO.ImportedRow();
                row.accountId = b.accountId();
                row.personName = person;
                row.itemType = itemType;
                row.description = e.getDescription();
                row.value = e.getValue();
                row.actual = b.amount().doubleValue() / persons.size();
                row.asOf = capturedDate.toString();
                row.stale = stale;
                report.imported.add(row);
            }
        }

        for (FinancialEntryDTO e : kept) {
            if (REFRESHABLE_TYPES.contains(e.getItemType()) && e.getStartYear() <= year) {
                report.handEnteredAssetRows.add(e);
            }
        }
        report.unknownPersons.addAll(unknownPersons);

        List<FinancialEntryDTO> entries = new ArrayList<>(kept);
        entries.addAll(imported);
        FinancialPlanDTO out = new FinancialPlanDTO(plan.getPersons(), entries, plan.getRates(), plan.getStatesByYear());
        return new RefreshBalancesResponseDTO(out, report);
    }

    public static boolean isAggregatorRow(FinancialEntryDTO e) {
        return e.getSource() != null && e.getSource().startsWith(AggregatorConfig.SOURCE_PREFIX);
    }

    static List<String> splitHolders(String holderNames) {
        List<String> out = new ArrayList<>();
        if (holderNames == null) return out;
        for (String part : holderNames.split(",")) {
            String t = part.trim();
            if (!t.isEmpty()) out.add(t);
        }
        return out;
    }

    /** Compound at ratePercent per year over the fraction of the year left after {@code from}. */
    static double projectToYearEnd(BigDecimal amount, double ratePercent, LocalDate from, LocalDate yearEnd, int daysInYear) {
        long days = Math.max(0, ChronoUnit.DAYS.between(from, yearEnd));
        double fraction = days / (double) daysInYear;
        return amount.doubleValue() * Math.pow(1 + ratePercent / 100.0, fraction);
    }
}
