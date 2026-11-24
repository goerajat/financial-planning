package rg.financialplanning.web.explain;

import rg.financialplanning.model.IndividualYearlySummary;
import rg.financialplanning.model.Person;
import rg.financialplanning.model.YearlySummary;
import rg.financialplanning.web.dto.FinancialPlanDTO;
import rg.financialplanning.web.dto.StateByYearDTO;
import rg.financialplanning.web.health.HealthPremiumPrompt;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The projection retold as events: what started, stopped or was decided in
 * each year, read off the finished summaries plus the notes the strategies
 * left. This is what lets an answer name the rule that moved a figure instead
 * of guessing it from the table.
 */
public final class ExplainEvents {

    private static final int MEDICARE_AGE = 65;
    private static final int LOOKBACK_YEARS = 2;
    private static final int MAX_SUGGESTIONS = 4;

    private ExplainEvents() {}

    /** One line per year that had something to say; people appear by the names the plan uses. */
    public static List<String> lines(YearlySummary[] summaries, FinancialPlanDTO plan) {
        List<String> out = new ArrayList<>();
        for (int i = 0; i < summaries.length; i++) {
            YearlySummary y = summaries[i];
            YearlySummary prev = i > 0 ? summaries[i - 1] : null;
            List<String> ev = new ArrayList<>();
            int year = y.year();

            if (prev != null && prev.totalIncome() > 0 && y.totalIncome() == 0) {
                ev.add("Wages stop; " + prev.year() + " was the last year with wages.");
            }
            String move = stateMove(plan, year, prev != null);
            if (move != null) ev.add(move);

            for (IndividualYearlySummary ind : y.individualSummaries().values()) {
                IndividualYearlySummary before = prev == null ? null : prev.getIndividualSummary(ind.name());
                Person person = ind.person();
                String who = ind.name();
                int age = person == null ? 0 : person.getAgeInYear(year);
                double prevSs = before == null ? 0 : before.socialSecurityBenefits();
                if (ind.socialSecurityBenefits() > 0 && prevSs == 0) {
                    ev.add("Social Security starts for " + who + " at age " + age + ": "
                            + money(ind.socialSecurityBenefits()) + " a year.");
                }
                double prevRmd = before == null ? 0 : before.rmdWithdrawals();
                if (ind.rmdWithdrawals() > 0 && prevRmd == 0) {
                    ev.add("Required minimum distributions start for " + who + " at age " + age + ": "
                            + money(ind.rmdWithdrawals()) + " this year.");
                }
                if (person != null && age == MEDICARE_AGE) {
                    ev.add(who + " turns " + MEDICARE_AGE + " and Medicare premiums start.");
                }
            }

            boolean anyoneOnMedicare = y.individualSummaries().values().stream()
                    .anyMatch(ind -> ind.person() != null && ind.person().getAgeInYear(year) >= MEDICARE_AGE);
            if (y.healthPremiums() > 0 && (prev == null || prev.healthPremiums() == 0) && !anyoneOnMedicare) {
                ev.add("Health cover is now bought privately because nobody has wages: "
                        + money(y.healthPremiums()) + " this year.");
            }
            if (y.irmaaSurcharge() > 0 && (prev == null || prev.irmaaSurcharge() == 0)) {
                ev.add("A Medicare IRMAA surcharge of " + money(y.irmaaSurcharge())
                        + " begins, set by the household income of " + (year - LOOKBACK_YEARS) + ".");
            } else if (prev != null && prev.irmaaSurcharge() > 0 && y.irmaaSurcharge() == 0) {
                ev.add("The IRMAA surcharge ends.");
            }
            if (prev != null && prev.mortgageBalance() > 0 && y.mortgageBalance() == 0) {
                ev.add("The mortgage is paid off this year.");
            }
            if (y.oneOffProceeds() > 0) {
                ev.add("One-off proceeds of " + money(y.oneOffProceeds()) + " arrive.");
            }

            List<String> drawn = new ArrayList<>();
            if (y.nonQualifiedWithdrawals() > 0) drawn.add(money(y.nonQualifiedWithdrawals()) + " from the taxable accounts");
            if (y.qualifiedWithdrawals() > 0) {
                drawn.add(money(y.qualifiedWithdrawals()) + " from the retirement accounts"
                        + (hasConversion(y) ? " (including the Roth conversion)" : ""));
            }
            if (y.rothWithdrawals() > 0) drawn.add(money(y.rothWithdrawals()) + " from Roth");
            if (y.cashWithdrawals() > 0) drawn.add(money(y.cashWithdrawals()) + " from cash");
            if (!drawn.isEmpty()) ev.add("Drawn to cover the year: " + String.join(", ", drawn) + ".");
            if (y.nonQualifiedContributions() > 0) {
                ev.add("Surplus of " + money(y.nonQualifiedContributions()) + " saved into the taxable accounts.");
            }
            if (y.deficit() > 0) {
                ev.add("Shortfall of " + money(y.deficit()) + ": every account is empty and the year cannot be covered.");
            }

            ev.addAll(y.notes());
            if (!ev.isEmpty()) out.add(year + ": " + String.join(" ", ev));
        }
        return out;
    }

    /** Questions this projection itself raises, in the order they happen, at most four. */
    public static List<String> suggestions(YearlySummary[] summaries) {
        List<String> q = new ArrayList<>();
        YearlySummary prev = null;
        for (YearlySummary y : summaries) {
            if (q.size() >= MAX_SUGGESTIONS) break;
            int year = y.year();
            if (prev != null && prev.totalIncome() > 0 && y.totalIncome() == 0) {
                add(q, "Why do withdrawals jump in " + year + "?");
            }
            if (hasConversion(y) && (prev == null || !hasConversion(prev))) {
                add(q, "Why does Roth increase in " + year + "?");
            }
            if (y.totalSocialSecurity() > 0 && (prev == null || prev.totalSocialSecurity() == 0)) {
                add(q, "What changes when Social Security starts in " + year + "?");
            }
            if (y.irmaaSurcharge() > 0 && (prev == null || prev.irmaaSurcharge() == 0)) {
                add(q, "Why does an IRMAA surcharge appear in " + year + "?");
            }
            if (y.rmdWithdrawals() > 0 && (prev == null || prev.rmdWithdrawals() == 0)) {
                add(q, "What are the required distributions from " + year + "?");
            }
            if (prev != null && prev.mortgageBalance() > 0 && y.mortgageBalance() == 0) {
                add(q, "What happens when the mortgage is paid off in " + year + "?");
            }
            if (y.deficit() > 0 && (prev == null || prev.deficit() == 0)) {
                add(q, "Why does the plan run short in " + year + "?");
            }
            prev = y;
        }
        if (q.size() < 2 && summaries.length > 0) {
            add(q, "How does net worth change over the plan?");
            add(q, "What are the biggest costs in the first ten years?");
        }
        return q.size() > MAX_SUGGESTIONS ? q.subList(0, MAX_SUGGESTIONS) : q;
    }

    private static void add(List<String> q, String s) {
        if (q.size() < MAX_SUGGESTIONS && !q.contains(s)) q.add(s);
    }

    static boolean hasConversion(YearlySummary y) {
        return y.notes().stream().anyMatch(n -> n.startsWith("Roth conversion:"));
    }

    private static String stateMove(FinancialPlanDTO plan, int year, boolean hasPrevious) {
        if (plan == null || plan.getStatesByYear() == null) return null;
        String now = stateFor(plan.getStatesByYear(), year);
        String before = stateFor(plan.getStatesByYear(), year - 1);
        if (now == null || !hasPrevious || now.equals(before)) return null;
        return "Move to " + HealthPremiumPrompt.displayName(now) + ".";
    }

    private static String stateFor(List<StateByYearDTO> states, int year) {
        for (StateByYearDTO s : states) {
            if (s.getStartYear() <= year && year <= s.getEndYear()) return s.getState();
        }
        return null;
    }

    /** Round money the way the table shows it: thousands, or millions past one. */
    public static String money(double v) {
        double a = Math.abs(v);
        if (a >= 1_000_000) return String.format(Locale.US, "%s$%.1fM", v < 0 ? "-" : "", a / 1_000_000);
        return String.format(Locale.US, "%s$%dk", v < 0 ? "-" : "", Math.round(a / 1000));
    }
}
