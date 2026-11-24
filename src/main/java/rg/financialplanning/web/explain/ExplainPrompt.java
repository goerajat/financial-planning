package rg.financialplanning.web.explain;

import rg.financialplanning.model.YearlySummary;
import rg.financialplanning.web.dto.ExplanationDTO;
import rg.financialplanning.web.dto.FinancialPlanDTO;
import rg.financialplanning.web.dto.PersonDTO;
import rg.financialplanning.web.dto.StateByYearDTO;
import rg.financialplanning.web.health.HealthPremiumPrompt;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The prompt that answers one question about a projection. It carries the
 * table the Results tab shows, the planner's rules in plain words, the log of
 * what the planner did each year, and the thread so far. People are called
 * "the older spouse" and "the younger spouse": no name leaves the planner.
 */
public final class ExplainPrompt {

    static final int MAX_PRIOR = 6;
    static final int MAX_WORDS = 120;

    private static final String RULEBOOK = loadRulebook();

    private static final String[][] RATE_LABELS = {
            {"QUALIFIED", "retirement accounts"},
            {"NON_QUALIFIED", "taxable accounts"},
            {"ROTH", "Roth"},
            {"CASH", "cash"},
            {"REAL_ESTATE", "real estate"},
            {"LIFE_INSURANCE_CASH", "life insurance cash value"},
            {"INCOME", "wages rise"},
            {"EXPENSE", "living expenses rise"},
            {"SOCIAL_SECURITY_BENEFITS", "Social Security COLA"},
            {"HEALTH_PREMIUM", "medical inflation"},
            {"MORTGAGE", "mortgage interest rate"},
    };

    private ExplainPrompt() {}

    /** Stand-ins for the people in the plan, oldest first, and the mapping back. */
    public static final class Labels {
        private final LinkedHashMap<String, String> nameToLabel = new LinkedHashMap<>();

        public static Labels of(List<PersonDTO> persons) {
            Labels out = new Labels();
            if (persons == null) return out;
            List<PersonDTO> sorted = new ArrayList<>();
            for (PersonDTO p : persons) if (p != null && p.getName() != null && !p.getName().isBlank()) sorted.add(p);
            sorted.sort(Comparator.comparingInt(PersonDTO::getYearOfBirth));
            int n = sorted.size();
            for (int i = 0; i < n; i++) {
                String label;
                if (n == 1) label = "person";
                else if (n == 2) {
                    boolean tie = sorted.get(0).getYearOfBirth() == sorted.get(1).getYearOfBirth();
                    label = tie ? (i == 0 ? "first spouse" : "second spouse") : (i == 0 ? "older spouse" : "younger spouse");
                } else {
                    label = "person " + (char) ('A' + i);
                }
                out.nameToLabel.put(sorted.get(i).getName().trim(), label);
            }
            return out;
        }

        public String label(String name) {
            return name == null ? null : nameToLabel.get(name.trim());
        }

        public List<String> labels() {
            return new ArrayList<>(nameToLabel.values());
        }

        /** Names replaced by "the <label>", longest names first so one is never found inside another. */
        public String toPrompt(String text) {
            if (text == null || nameToLabel.isEmpty()) return text;
            List<String> names = new ArrayList<>(nameToLabel.keySet());
            names.sort(Comparator.comparingInt(String::length).reversed());
            String out = text;
            for (String name : names) {
                Pattern p = Pattern.compile("(?i)(?<![\\p{L}\\p{N}])" + Pattern.quote(name) + "(?![\\p{L}\\p{N}])");
                out = p.matcher(out).replaceAll(Matcher.quoteReplacement("the " + nameToLabel.get(name)));
            }
            return out;
        }

        /** Labels back to names for display, "the" dropped and the possessive kept. */
        public String toDisplay(String text) {
            if (text == null || nameToLabel.isEmpty()) return text;
            String out = text;
            for (Map.Entry<String, String> e : nameToLabel.entrySet()) {
                Pattern p = Pattern.compile("(?i)\\b(?:the\\s+)?" + Pattern.quote(e.getValue()) + "\\b");
                out = p.matcher(out).replaceAll(Matcher.quoteReplacement(e.getKey()));
            }
            return out;
        }
    }

    public static String build(FinancialPlanDTO plan, YearlySummary[] summaries, List<ExplanationDTO> prior,
                               String question, Labels labels) {
        StringBuilder sb = new StringBuilder();
        sb.append("You are explaining a household's long-range financial projection to the people it belongs to. ")
          .append("They are not accountants or programmers.\n\n")
          .append("Answer the question at the end using only the material here: the projection table, the rules the ")
          .append("planner follows, and the planner's own log of what it did each year. Write in plain English, at most ")
          .append(MAX_WORDS).append(" words, as one or two short paragraphs: first what the figure is doing, then the rule ")
          .append("or event that moved it, then one sentence on what it means for them. No headings, no bullet points, no ")
          .append("code, no field or column names other than the table's own headings, no mention of this material or of ")
          .append("being an AI. Round money to thousands (for example $32k) or millions ($1.2M); the table is in thousands ")
          .append("of dollars. Call people exactly what the material calls them (").append(String.join(", ", labels.labels()))
          .append("). If the material does not show the reason, say what the table shows and that the reason is not in ")
          .append("the projection; do not guess.\n\n");

        sb.append("HOUSEHOLD\n");
        int firstYear = summaries.length > 0 ? summaries[0].year() : java.time.Year.now().getValue();
        List<PersonDTO> persons = plan.getPersons() == null ? List.of() : plan.getPersons();
        for (PersonDTO p : persons) {
            String label = labels.label(p.getName());
            if (label == null) continue;
            sb.append("- The ").append(label).append(": age ").append(firstYear - p.getYearOfBirth()).append(" in ")
              .append(firstYear).append(", claims Social Security at ")
              .append(p.getSocialSecurityClaimAge() > 0 ? "age " + p.getSocialSecurityClaimAge() : "full retirement age")
              .append(p.getSocialSecurityFraMonthly() > 0 ? "" : " (no benefit record of their own)").append(".\n");
        }
        sb.append("- ").append(persons.size() >= 2 ? "Married, one joint tax return." : "Single filer.").append('\n');
        String states = stateSpans(plan.getStatesByYear());
        if (!states.isEmpty()) sb.append("- State of residence: ").append(states).append(".\n");
        String rates = rateLine(plan.getRates());
        if (!rates.isEmpty()) sb.append("- Yearly assumptions: ").append(rates).append(".\n");
        sb.append('\n');

        sb.append("HOW THE PLANNER WORKS\n").append(RULEBOOK.trim()).append("\n\n");

        sb.append("WHAT THE PLANNER DID EACH YEAR\n");
        for (String line : ExplainEvents.lines(summaries, plan)) sb.append(labels.toPrompt(line)).append('\n');
        sb.append('\n');

        sb.append("PROJECTION (thousands of dollars; balances at the end of each year, flows during the year)\n");
        sb.append(table(summaries)).append('\n');

        List<ExplanationDTO> answered = new ArrayList<>();
        if (prior != null) for (ExplanationDTO e : prior) if ("DONE".equals(e.status()) && e.answer() != null) answered.add(e);
        if (answered.size() > MAX_PRIOR) answered = answered.subList(answered.size() - MAX_PRIOR, answered.size());
        if (!answered.isEmpty()) {
            sb.append("EARLIER IN THIS CONVERSATION\n");
            for (ExplanationDTO e : answered) {
                sb.append("Q: ").append(labels.toPrompt(e.question().trim())).append('\n')
                  .append("A: ").append(labels.toPrompt(e.answer().trim())).append("\n\n");
            }
        }

        sb.append("QUESTION\n").append(labels.toPrompt(question.trim())).append('\n');
        return sb.toString();
    }

    /** The same columns as the Results tab, in the same order, so the answer and the screen agree. */
    static String table(YearlySummary[] summaries) {
        StringBuilder sb = new StringBuilder();
        sb.append("Year,Net worth,Qualified,Non-qualified,Roth,Cash,Other,Mortgage balance,Wages,Social Security,")
          .append("Withdrawals,of which RMD,Living,Taxes,Health,of which IRMAA,Mtg interest,Mtg principal,Shortfall\n");
        for (YearlySummary y : summaries) {
            sb.append(y.year()).append(',')
              .append(k(y.netWorth())).append(',')
              .append(k(y.qualifiedAssets())).append(',')
              .append(k(y.nonQualifiedAssets())).append(',')
              .append(k(y.rothAssets())).append(',')
              .append(k(y.cash())).append(',')
              .append(k(y.realEstate() + y.lifeInsuranceCash() + y.lifeInsuranceBenefits())).append(',')
              .append(k(y.mortgageBalance())).append(',')
              .append(k(y.totalIncome())).append(',')
              .append(k(y.totalSocialSecurity())).append(',')
              .append(k(y.totalWithdrawals())).append(',')
              .append(k(y.rmdWithdrawals())).append(',')
              .append(k(y.totalExpenses())).append(',')
              .append(k(y.totalTaxes())).append(',')
              .append(k(y.healthPremiums())).append(',')
              .append(k(y.irmaaSurcharge())).append(',')
              .append(k(y.mortgageInterest())).append(',')
              .append(k(y.mortgagePrincipal() + y.mortgageRepayment())).append(',')
              .append(k(y.deficit())).append('\n');
        }
        return sb.toString();
    }

    private static String k(double v) {
        return Long.toString(Math.round(v / 1000));
    }

    private static String stateSpans(List<StateByYearDTO> states) {
        if (states == null || states.isEmpty()) return "";
        List<String> parts = new ArrayList<>();
        for (StateByYearDTO s : states) {
            parts.add(HealthPremiumPrompt.displayName(s.getState()) + " " + s.getStartYear() + "-" + s.getEndYear());
        }
        return String.join("; ", parts);
    }

    private static String rateLine(Map<String, Double> rates) {
        if (rates == null) return "";
        List<String> parts = new ArrayList<>();
        for (String[] r : RATE_LABELS) {
            Double v = rates.get(r[0]);
            if (v == null) continue;
            parts.add(r[1] + " " + trimPct(v) + "%");
        }
        return String.join(", ", parts);
    }

    private static String trimPct(double v) {
        String s = String.format(Locale.US, "%.2f", v);
        return s.replaceAll("0+$", "").replaceAll("\\.$", "");
    }

    private static String loadRulebook() {
        try (InputStream in = ExplainPrompt.class.getResourceAsStream("/explain-rulebook.md")) {
            if (in == null) throw new IllegalStateException("explain-rulebook.md missing from classpath");
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
