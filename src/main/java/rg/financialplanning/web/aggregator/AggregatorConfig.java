package rg.financialplanning.web.aggregator;

import com.google.gson.Gson;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Mapping from the finance aggregator's accounts to plan entries. Lives outside the
 * repository (default {@code ~/.financial-planning/aggregator.json}) because it names
 * people and accounts.
 */
public class AggregatorConfig {

    public static final String SOURCE_PREFIX = "aggregator:";

    private static final Map<String, String> DEFAULT_TYPE_MAP = new LinkedHashMap<>();

    static {
        DEFAULT_TYPE_MAP.put("IRA", "QUALIFIED");
        DEFAULT_TYPE_MAP.put("R_401K", "QUALIFIED");
        DEFAULT_TYPE_MAP.put("R_403B", "QUALIFIED");
        DEFAULT_TYPE_MAP.put("ROTH_IRA", "ROTH");
        DEFAULT_TYPE_MAP.put("BROKERAGE", "NON_QUALIFIED");
        DEFAULT_TYPE_MAP.put("CHECKING", "CASH");
        DEFAULT_TYPE_MAP.put("SAVINGS", "CASH");
        DEFAULT_TYPE_MAP.put("CASH", "CASH");
    }

    public static class AccountOverride {
        public Boolean skip;
        public String itemType;
        public String person;
    }

    private String baseUrl = "http://127.0.0.1:8765";
    private int staleDays = 3;
    private Map<String, String> persons = new HashMap<>();
    private Map<String, String> typeMap = new HashMap<>();
    private Map<String, AccountOverride> accounts = new HashMap<>();

    public static Path defaultPath() {
        return Path.of(System.getProperty("user.home"), ".financial-planning", "aggregator.json");
    }

    public static AggregatorConfig load(Path path) throws IOException {
        if (!Files.exists(path)) {
            throw new AggregatorException("Aggregator config not found at " + path
                    + ". Create it with baseUrl and a persons map (aggregator holder name -> plan person name).");
        }
        return fromJson(Files.readString(path));
    }

    public static AggregatorConfig fromJson(String json) {
        AggregatorConfig cfg = new Gson().fromJson(json, AggregatorConfig.class);
        if (cfg == null) {
            throw new AggregatorException("Aggregator config is empty");
        }
        if (cfg.persons == null) cfg.persons = new HashMap<>();
        if (cfg.typeMap == null) cfg.typeMap = new HashMap<>();
        if (cfg.accounts == null) cfg.accounts = new HashMap<>();
        if (cfg.baseUrl == null || cfg.baseUrl.isBlank()) cfg.baseUrl = "http://127.0.0.1:8765";
        return cfg;
    }

    public String baseUrl() {
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    public int staleDays() {
        return staleDays;
    }

    public static String normalizeHolder(String holder) {
        return holder == null ? "" : holder.trim().replaceAll("\\s+", " ").toUpperCase(Locale.ROOT);
    }

    /** Plan person for an aggregator holder name, or null when unmapped. */
    public String personFor(String holder) {
        String key = normalizeHolder(holder);
        for (Map.Entry<String, String> e : persons.entrySet()) {
            if (normalizeHolder(e.getKey()).equals(key)) {
                return e.getValue();
            }
        }
        return null;
    }

    /** Plan item type for an aggregator account type, or null when unmapped. */
    public String itemTypeFor(String accountType) {
        if (accountType == null) return null;
        if (typeMap.containsKey(accountType)) {
            String override = typeMap.get(accountType);
            return override == null || override.isBlank() ? null : override;
        }
        return DEFAULT_TYPE_MAP.get(accountType);
    }

    public AccountOverride accountOverride(long accountId) {
        return accounts.get(Long.toString(accountId));
    }

    public Map<String, String> persons() {
        return persons;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public void setStaleDays(int staleDays) {
        this.staleDays = staleDays;
    }

    public void setPersons(Map<String, String> persons) {
        this.persons = persons;
    }

    public void setTypeMap(Map<String, String> typeMap) {
        this.typeMap = typeMap;
    }

    public void setAccounts(Map<String, AccountOverride> accounts) {
        this.accounts = accounts;
    }
}
