package rg.financialplanning.web.aggregator;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class AggregatorClient {

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    public List<AggregatorBalance> fetchBalances(String baseUrl) {
        HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + "/api/balances?currency=USD"))
                .timeout(Duration.ofSeconds(20))
                .GET()
                .build();
        HttpResponse<String> response;
        try {
            response = http.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException e) {
            throw new AggregatorException("Could not reach the aggregator at " + baseUrl + ": " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AggregatorException("Interrupted while calling the aggregator", e);
        }
        if (response.statusCode() != 200) {
            throw new AggregatorException("Aggregator answered HTTP " + response.statusCode() + " for /api/balances");
        }
        return parse(response.body());
    }

    public static List<AggregatorBalance> parse(String json) {
        JsonElement root = JsonParser.parseString(json);
        if (!root.isJsonArray()) {
            throw new AggregatorException("Unexpected /api/balances payload: not an array");
        }
        List<AggregatorBalance> out = new ArrayList<>();
        JsonArray rows = root.getAsJsonArray();
        for (JsonElement el : rows) {
            JsonObject o = el.getAsJsonObject();
            BigDecimal amount = null;
            String currency = null;
            JsonElement report = o.get("currentReport");
            if (report != null && report.isJsonObject()) {
                JsonObject r = report.getAsJsonObject();
                String amountText = str(r, "amount");
                amount = amountText == null ? null : new BigDecimal(amountText);
                currency = str(r, "currency");
            }
            String capturedAt = str(o, "capturedAt");
            String closedOn = str(o, "closedOn");
            out.add(new AggregatorBalance(
                    o.get("accountId").getAsLong(),
                    str(o, "institutionName"),
                    str(o, "accountName"),
                    str(o, "accountType"),
                    str(o, "accountHolderNames"),
                    amount,
                    currency,
                    capturedAt == null ? null : Instant.parse(capturedAt),
                    closedOn == null ? null : LocalDate.parse(closedOn)));
        }
        return out;
    }

    private static String str(JsonObject o, String key) {
        JsonElement e = o.get(key);
        return e == null || e.isJsonNull() ? null : e.getAsString();
    }
}
