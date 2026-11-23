package rg.financialplanning.web.health;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import java.util.ArrayList;
import java.util.List;

/**
 * Pulls the figures out of the model's answer: the last JSON object in the text
 * that carries the premium keys. Everything before it is the explanation.
 */
public final class HealthPremiumEstimateParser {

    public record Parsed(double preMedicareMonthly, double medicarePlanMonthly, double medicalInflationPct,
                         List<String> sources, String notes) {}

    private HealthPremiumEstimateParser() {}

    public static Parsed parse(String output) {
        if (output == null || output.isBlank()) throw new IllegalArgumentException("Empty answer.");
        String text = output.trim();
        int searchEnd = text.length();
        while (true) {
            int close = text.lastIndexOf('}', searchEnd - 1);
            if (close < 0) throw new IllegalArgumentException("The answer has no JSON object with the premium figures.");
            int open = matchingOpen(text, close);
            if (open >= 0) {
                String candidate = text.substring(open, close + 1);
                JsonObject obj = tryObject(candidate);
                if (obj != null && obj.has("medicarePlanPremiumMonthly")) {
                    String notes = fromFirstHeading(stripFence(text.substring(0, open)).trim());
                    return new Parsed(
                            number(obj, "preMedicarePremiumMonthly"),
                            number(obj, "medicarePlanPremiumMonthly"),
                            number(obj, "medicalInflationPct"),
                            strings(obj.get("sources")),
                            notes);
                }
            }
            searchEnd = close;
        }
    }

    private static int matchingOpen(String text, int close) {
        int depth = 0;
        boolean inString = false;
        for (int i = close; i >= 0; i--) {
            char c = text.charAt(i);
            if (c == '"' && (i == 0 || text.charAt(i - 1) != '\\')) inString = !inString;
            if (inString) continue;
            if (c == '}') depth++;
            else if (c == '{' && --depth == 0) return i;
        }
        return -1;
    }

    private static JsonObject tryObject(String candidate) {
        try {
            JsonElement el = JsonParser.parseString(candidate);
            return el.isJsonObject() ? el.getAsJsonObject() : null;
        } catch (JsonSyntaxException | IllegalStateException e) {
            return null;
        }
    }

    private static double number(JsonObject obj, String key) {
        JsonElement el = obj.get(key);
        if (el == null || el.isJsonNull()) return 0;
        if (el.isJsonPrimitive() && el.getAsJsonPrimitive().isNumber()) return el.getAsDouble();
        String s = el.getAsString().replaceAll("[^0-9.\\-]", "");
        if (s.isEmpty()) return 0;
        return Double.parseDouble(s);
    }

    private static List<String> strings(JsonElement el) {
        List<String> out = new ArrayList<>();
        if (el == null || !el.isJsonArray()) return out;
        JsonArray arr = el.getAsJsonArray();
        for (JsonElement e : arr) {
            if (e.isJsonPrimitive()) out.add(e.getAsString());
        }
        return out;
    }

    /** Text before the first markdown heading is the model talking to itself; the answer starts at the heading. */
    static String fromFirstHeading(String notes) {
        int idx = -1;
        if (notes.startsWith("#")) idx = 0;
        else {
            int nl = notes.indexOf("\n#");
            if (nl >= 0) idx = nl + 1;
        }
        return idx < 0 ? notes : notes.substring(idx).trim();
    }

    /** A trailing "```json" opener left behind when the fence was asked away but came anyway. */
    private static String stripFence(String s) {
        String t = s.stripTrailing();
        if (t.endsWith("```json") || t.endsWith("```")) {
            int idx = t.lastIndexOf("```");
            t = t.substring(0, idx);
        }
        return t;
    }
}
