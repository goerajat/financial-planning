package rg.financialplanning.web.dto;

import java.util.ArrayList;
import java.util.List;

public class RefreshReportDTO {

    public static class ImportedRow {
        public long accountId;
        public String personName;
        public String itemType;
        public String description;
        public int value;
        public double actual;
        public String asOf;
        public boolean stale;
    }

    public static class SkippedRow {
        public String account;
        public String reason;

        public SkippedRow() {
        }

        public SkippedRow(String account, String reason) {
            this.account = account;
            this.reason = reason;
        }
    }

    public int year;
    public List<ImportedRow> imported = new ArrayList<>();
    public List<SkippedRow> skipped = new ArrayList<>();
    public List<String> unknownPersons = new ArrayList<>();
    public List<FinancialEntryDTO> handEnteredAssetRows = new ArrayList<>();
}
