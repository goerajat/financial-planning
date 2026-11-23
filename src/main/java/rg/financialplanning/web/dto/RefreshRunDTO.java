package rg.financialplanning.web.dto;

import java.util.List;

/** One recorded aggregator refresh; balances and projection are filled only for a single-run read. */
public class RefreshRunDTO {

    public static class Balance {
        public long accountId;
        public String personName;
        public String itemType;
        public String description;
        public double actual;
        public String asOf;
        public boolean stale;
        public double projected;
    }

    public long id;
    public String refreshedAt;
    public int planYear;
    public int importedCount;
    public int skippedCount;
    public double actualTotal;
    public double projectedTotal;
    public Double netWorthPlanYear;
    public List<Balance> balances;
    public List<YearlySummaryDTO> projection;
}
