package rg.financialplanning.web.health;

import rg.financialplanning.model.State;
import rg.financialplanning.web.dto.FinancialEntryDTO;
import rg.financialplanning.web.dto.FinancialPlanDTO;
import rg.financialplanning.web.dto.PersonDTO;
import rg.financialplanning.web.dto.StateByYearDTO;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * What the planner already knows about a person's health-cover years, and the
 * research prompt built from it. The prompt carries ages, years and the state of
 * residence; never a name.
 */
public final class HealthPremiumPrompt {

    public static final int MEDICARE_AGE = 65;
    /** Years of Medicare cover the state list covers, so a planned move shows in the prompt. */
    private static final int MEDICARE_HORIZON = 15;

    public enum Tier { SILVER, GOLD }

    /** One state over a span of plan years, for the prompt text. */
    public record StateSpan(String state, String stateName, int startYear, int endYear) {}

    /**
     * @param personWageEnd    last year this person has wages, or 0 when they have none in the plan
     * @param householdWageEnd last year anyone in the plan has wages, or 0
     * @param gapStart         first year pre-Medicare cover is charged, or 0 when there is no gap
     * @param gapEnd           last such year, or 0
     * @param midpointAge      the person's age in the middle of the gap, used to price the plan
     */
    public record Inputs(
            String personName,
            int yearOfBirth,
            int ageNow,
            int householdSize,
            int personWageEnd,
            int householdWageEnd,
            int gapStart,
            int gapEnd,
            int midpointAge,
            int medicareYear,
            String preMedicareState,
            String preMedicareStateName,
            String medicareState,
            String medicareStateName,
            List<StateSpan> states,
            Tier tier) {

        public boolean hasGap() {
            return gapStart > 0 && gapEnd >= gapStart;
        }
    }

    private HealthPremiumPrompt() {}

    public static Inputs inputs(FinancialPlanDTO plan, String personName, Tier tier, int currentYear) {
        PersonDTO person = null;
        for (PersonDTO p : plan.getPersons()) {
            if (p.getName() != null && p.getName().equalsIgnoreCase(personName)) {
                person = p;
                break;
            }
        }
        if (person == null) throw new IllegalArgumentException("No person named " + personName + " in the plan.");
        if (person.getYearOfBirth() < 1900 || person.getYearOfBirth() > currentYear) {
            throw new IllegalArgumentException("Set " + person.getName() + "'s year of birth first.");
        }
        if (plan.getStatesByYear() == null || plan.getStatesByYear().isEmpty()) {
            throw new IllegalArgumentException("Set the state of residence on the States tab first.");
        }

        int personWageEnd = 0;
        int householdWageEnd = 0;
        for (FinancialEntryDTO e : plan.getEntries()) {
            if (!"INCOME".equals(e.getItemType()) || e.getValue() <= 0) continue;
            householdWageEnd = Math.max(householdWageEnd, e.getEndYear());
            if (person.getName().equalsIgnoreCase(e.getPersonName())) {
                personWageEnd = Math.max(personWageEnd, e.getEndYear());
            }
        }

        int medicareYear = person.getYearOfBirth() + MEDICARE_AGE;
        int gapStart = Math.max(currentYear, householdWageEnd + 1);
        int gapEnd = medicareYear - 1;
        boolean hasGap = gapStart <= gapEnd;
        if (!hasGap) {
            gapStart = 0;
            gapEnd = 0;
        }
        int midpointAge = hasGap ? (gapStart + gapEnd) / 2 - person.getYearOfBirth() : 0;

        int from = hasGap ? gapStart : Math.max(currentYear, medicareYear);
        List<StateSpan> spans = spans(plan.getStatesByYear(), from, medicareYear + MEDICARE_HORIZON);
        String preState = hasGap ? stateFor(plan.getStatesByYear(), (gapStart + gapEnd) / 2) : null;
        String medState = stateFor(plan.getStatesByYear(), Math.max(currentYear, medicareYear));

        return new Inputs(
                person.getName(),
                person.getYearOfBirth(),
                currentYear - person.getYearOfBirth(),
                plan.getPersons().size(),
                personWageEnd,
                householdWageEnd,
                gapStart,
                gapEnd,
                midpointAge,
                medicareYear,
                preState,
                displayName(preState),
                medState,
                displayName(medState),
                spans,
                tier);
    }

    public static String build(Inputs in) {
        String tier = in.tier() == Tier.GOLD ? "Gold" : "Silver";
        StringBuilder sb = new StringBuilder();
        sb.append("You are estimating health insurance premiums for one person in a retirement plan. ")
          .append("Use web search for current figures and cite the pages you used. ")
          .append("Today is ").append(LocalDate.now()).append(". ")
          .append("Every amount is per month, for this one person, non-smoker, in today's dollars.\n\n");

        sb.append("Person: born ").append(in.yearOfBirth()).append(", age ").append(in.ageNow()).append(" now. ");
        sb.append("Household of ").append(in.householdSize()).append(" on one tax return.\n");
        sb.append("State of residence: ");
        for (int i = 0; i < in.states().size(); i++) {
            StateSpan s = in.states().get(i);
            if (i > 0) sb.append("; ");
            sb.append(s.stateName()).append(" ").append(s.startYear()).append("-").append(s.endYear());
        }
        sb.append(".\n");
        if (in.personWageEnd() > 0) {
            sb.append("This person's wages end in ").append(in.personWageEnd()).append(". ");
        } else {
            sb.append("This person has no wages in the plan. ");
        }
        if (in.householdWageEnd() > 0) {
            sb.append("The last year anyone in the household has wages is ").append(in.householdWageEnd())
              .append("; employer cover is assumed through that year.\n");
        } else {
            sb.append("Nobody in the household has wages.\n");
        }
        if (in.hasGap()) {
            sb.append("Cover must be bought on the individual market ").append(in.gapStart()).append("-").append(in.gapEnd())
              .append(" (ages ").append(in.gapStart() - in.yearOfBirth()).append("-").append(in.gapEnd() - in.yearOfBirth())
              .append("). Medicare starts ").append(in.medicareYear()).append(".\n\n");
        } else {
            sb.append("There is no gap before Medicare, which starts ").append(in.medicareYear())
              .append(" (or has started).\n\n");
        }

        sb.append("Estimate three figures:\n");
        if (in.hasGap()) {
            sb.append("1. preMedicarePremiumMonthly: the full, unsubsidised monthly premium of the benchmark ")
              .append("(second-lowest-cost) ").append(tier).append(" plan on the ACA marketplace in ")
              .append(in.preMedicareStateName()).append(" for a single ").append(in.midpointAge())
              .append("-year-old, which is this person's age in the middle of the gap, current plan year. ")
              .append("Use a state-wide average across rating areas. Do not apply any premium tax credit.\n");
        } else {
            sb.append("1. preMedicarePremiumMonthly: 0, because there is no gap before Medicare.\n");
        }
        sb.append("2. medicarePlanPremiumMonthly: a standard (not high-deductible) Medigap Plan G at age 65 in ")
          .append(in.medicareStateName()).append(", state average for a new enrollee, plus the average premium of a ")
          .append("stand-alone Part D drug plan in that state. Exclude Part B and any IRMAA surcharge; the plan adds those.\n");
        sb.append("3. medicalInflationPct: one annual growth rate, in percent, to apply to all of these premiums over the next ")
          .append("15 years. Justify it from the Medicare Part B premium history and the trustees' projection, Medigap rate ")
          .append("increase trends, and ACA marketplace premium trends.\n\n");

        sb.append("Write a short markdown explanation first, starting straight away with a heading: the figures, how you ")
          .append("arrived at each, and the sources as links. Do not narrate your process or your searching, do not use tables, ")
          .append("and do not add a separate sources section; the links in the text are enough. ")
          .append("Then, as the very last line, output one JSON object and nothing after it, exactly in this shape:\n")
          .append("{\"preMedicarePremiumMonthly\": 1234, \"medicarePlanPremiumMonthly\": 234, \"medicalInflationPct\": 6.0, ")
          .append("\"sources\": [\"https://...\"]}\n")
          .append("Numbers only in the JSON, no currency symbols, no code fence around it.");
        return sb.toString();
    }

    private static List<StateSpan> spans(List<StateByYearDTO> states, int from, int to) {
        List<StateSpan> out = new ArrayList<>();
        String current = null;
        int start = from;
        for (int y = from; y <= to; y++) {
            String s = stateFor(states, y);
            if (current == null) {
                current = s;
                start = y;
            } else if (!current.equals(s)) {
                out.add(new StateSpan(current, displayName(current), start, y - 1));
                current = s;
                start = y;
            }
        }
        if (current != null) out.add(new StateSpan(current, displayName(current), start, to));
        return out;
    }

    /** The state configured for a year; outside every range, the nearest configured one. */
    static String stateFor(List<StateByYearDTO> states, int year) {
        StateByYearDTO best = null;
        int bestDistance = Integer.MAX_VALUE;
        for (StateByYearDTO s : states) {
            if (s.getState() == null || s.getState().isBlank()) continue;
            if (year >= s.getStartYear() && year <= s.getEndYear()) return s.getState().toUpperCase(Locale.ROOT);
            int distance = year < s.getStartYear() ? s.getStartYear() - year : year - s.getEndYear();
            if (distance < bestDistance) {
                bestDistance = distance;
                best = s;
            }
        }
        if (best == null) throw new IllegalArgumentException("Set the state of residence on the States tab first.");
        return best.getState().toUpperCase(Locale.ROOT);
    }

    public static String displayName(String code) {
        if (code == null) return null;
        try {
            return State.valueOf(code).getDisplayName();
        } catch (IllegalArgumentException e) {
            return code;
        }
    }
}
