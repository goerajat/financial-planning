package rg.financialplanning.web.aggregator;

import org.junit.Before;
import org.junit.Test;
import rg.financialplanning.web.dto.FinancialEntryDTO;
import rg.financialplanning.web.dto.FinancialPlanDTO;
import rg.financialplanning.web.dto.PersonDTO;
import rg.financialplanning.web.dto.RefreshBalancesResponseDTO;
import rg.financialplanning.web.dto.RefreshReportDTO;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

public class BalanceRefresherTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 21);
    private static final Instant CAPTURED = Instant.parse("2026-11-21T11:32:16Z");

    private BalanceRefresher refresher;
    private AggregatorConfig cfg;
    private FinancialPlanDTO plan;

    @Before
    public void setUp() {
        refresher = new BalanceRefresher();
        cfg = AggregatorConfig.fromJson("{"
                + "\"baseUrl\": \"http://127.0.0.1:8765\","
                + "\"staleDays\": 3,"
                + "\"persons\": {\"JOHN DOE\": \"John\", \"Jane Roe\": \"Jane\"},"
                + "\"typeMap\": {\"HSA\": \"ROTH\"},"
                + "\"accounts\": {"
                + "  \"66\": {\"itemType\": \"LIFE_INSURANCE_CASH\", \"person\": \"John\"},"
                + "  \"99\": {\"skip\": true}"
                + "}}");

        Map<String, Double> rates = new HashMap<>();
        rates.put("QUALIFIED", 6.0);
        rates.put("NON_QUALIFIED", 6.0);
        rates.put("ROTH", 6.0);
        rates.put("CASH", 2.0);
        rates.put("LIFE_INSURANCE_CASH", 4.0);

        List<FinancialEntryDTO> entries = new ArrayList<>();
        FinancialEntryDTO salary = new FinancialEntryDTO("John", "INCOME", "Salary", 100000, 2024, 2040);
        salary.setId(1L);
        entries.add(salary);
        FinancialEntryDTO handIra = new FinancialEntryDTO("John", "QUALIFIED", "Old IRA figure", 500000, 2024, 2040);
        handIra.setId(2L);
        entries.add(handIra);
        FinancialEntryDTO staleAgg = new FinancialEntryDTO("John", "CASH", "Bank Checking (as of 2026-10-01)", 1, 2026, 2040);
        staleAgg.setId(3L);
        staleAgg.setSource("aggregator:36");
        entries.add(staleAgg);

        plan = new FinancialPlanDTO(List.of(new PersonDTO("John", 1970), new PersonDTO("Jane", 1972)), entries, rates);
    }

    private static AggregatorBalance row(long id, String inst, String acct, String type, String holders, String amount) {
        return new AggregatorBalance(id, inst, acct, type, holders, new BigDecimal(amount), "USD", CAPTURED, null);
    }

    @Test
    public void replacesAggregatorRowsAndKeepsHandRows() {
        List<AggregatorBalance> balances = List.of(
                row(36, "Citibank Online", "Checking ...2540", "CHECKING", "JOHN DOE", "10000"));

        RefreshBalancesResponseDTO out = refresher.refresh(plan, balances, cfg, TODAY);

        List<FinancialEntryDTO> entries = out.plan.getEntries();
        assertEquals(3, entries.size());
        assertTrue(entries.stream().anyMatch(e -> "Salary".equals(e.getDescription())));
        assertTrue(entries.stream().anyMatch(e -> "Old IRA figure".equals(e.getDescription())));
        assertFalse(entries.stream().anyMatch(e -> e.getDescription().startsWith("Bank Checking")));

        FinancialEntryDTO imported = entries.stream().filter(BalanceRefresher::isAggregatorRow).findFirst().orElseThrow();
        assertEquals("aggregator:36", imported.getSource());
        assertEquals("John", imported.getPersonName());
        assertEquals("CASH", imported.getItemType());
        assertEquals("Citibank Online Checking ...2540 (as of 2026-11-21)", imported.getDescription());
        assertEquals(2026, imported.getStartYear());
        assertEquals(2040, imported.getEndYear());
        assertEquals(Long.valueOf(3L), imported.getId());

        // 40 days of 365 at 2%: 10000 * 1.02^(40/365)
        assertEquals(Math.round(10000 * Math.pow(1.02, 40.0 / 365)), imported.getValue());

        RefreshReportDTO report = out.report;
        assertEquals(2026, report.year);
        assertEquals(1, report.imported.size());
        assertFalse(report.imported.get(0).stale);
        assertEquals(1, report.handEnteredAssetRows.size());
        assertEquals("Old IRA figure", report.handEnteredAssetRows.get(0).getDescription());
        assertTrue(report.unknownPersons.isEmpty());
    }

    @Test
    public void mapsTypesAndOverrides() {
        List<AggregatorBalance> balances = List.of(
                row(74, "Charles Schwab", "Rollover IRA", "IRA", "JOHN DOE", "1000"),
                row(83, "Broker", "Roth IRA", "ROTH_IRA", "John  Doe", "1000"),
                row(41, "Broker", "Brokerage", "BROKERAGE", "jane roe", "1000"),
                row(58, "Employer", "401(k) Plan", "R_401K", "JOHN DOE", "1000"),
                row(7, "Bank", "HSA", "HSA", "JOHN DOE", "1000"),
                row(66, "Insurer", "Whole Life", "OTHER", "Kid Doe", "1000"),
                row(99, "Broker", "Skipped", "BROKERAGE", "JOHN DOE", "1000"));

        RefreshBalancesResponseDTO out = refresher.refresh(plan, balances, cfg, TODAY);
        Map<Long, FinancialEntryDTO> bySource = out.plan.getEntries().stream()
                .filter(BalanceRefresher::isAggregatorRow)
                .collect(Collectors.toMap(e -> Long.parseLong(e.getSource().substring("aggregator:".length())), e -> e));

        assertEquals("QUALIFIED", bySource.get(74L).getItemType());
        assertEquals("ROTH", bySource.get(83L).getItemType());
        assertEquals("John", bySource.get(83L).getPersonName());
        assertEquals("NON_QUALIFIED", bySource.get(41L).getItemType());
        assertEquals("Jane", bySource.get(41L).getPersonName());
        assertEquals("QUALIFIED", bySource.get(58L).getItemType());
        assertEquals("ROTH", bySource.get(7L).getItemType());
        assertEquals("LIFE_INSURANCE_CASH", bySource.get(66L).getItemType());
        assertEquals("John", bySource.get(66L).getPersonName());
        assertFalse(bySource.containsKey(99L));
        assertEquals(1, out.report.skipped.size());
        assertEquals("skipped by config", out.report.skipped.get(0).reason);
    }

    @Test
    public void skipsWithReasons() {
        List<AggregatorBalance> balances = List.of(
                new AggregatorBalance(1, "Bank", "Closed", "CHECKING", "JOHN DOE", new BigDecimal("5"), "USD", CAPTURED, LocalDate.of(2026, 1, 2)),
                row(2, "Bank", "Card", "CREDIT", "JOHN DOE", "100"),
                row(3, "Broker", "Margin", "BROKERAGE", "JOHN DOE", "-50"),
                row(4, "Insurer", "Policy", "OTHER", "Someone Else", "100"),
                row(5, "Broker", "Stranger", "BROKERAGE", "Someone Else", "100"),
                new AggregatorBalance(6, "Bank", "Empty", "CHECKING", "JOHN DOE", null, null, CAPTURED, null));

        RefreshBalancesResponseDTO out = refresher.refresh(plan, balances, cfg, TODAY);

        assertTrue(out.plan.getEntries().stream().noneMatch(BalanceRefresher::isAggregatorRow));
        Map<String, String> reasons = out.report.skipped.stream()
                .collect(Collectors.toMap(s -> s.account, s -> s.reason));
        assertEquals("closed 2026-01-02", reasons.get("Bank Closed"));
        assertTrue(reasons.get("Bank Card").startsWith("no item type for account type CREDIT"));
        assertEquals("negative balance", reasons.get("Broker Margin"));
        assertTrue(reasons.get("Insurer Policy").startsWith("no item type"));
        assertEquals("holder not mapped: Someone Else", reasons.get("Broker Stranger"));
        assertEquals("no balance", reasons.get("Bank Empty"));
    }

    @Test
    public void splitsJointAccountsAndFlagsUnknownPersonsAndStaleCaptures() {
        Instant old = Instant.parse("2026-11-10T12:00:00Z");
        List<AggregatorBalance> balances = List.of(
                new AggregatorBalance(10, "Bank", "Joint", "SAVINGS", "JOHN DOE, Jane Roe", new BigDecimal("1000"), "USD", old, null),
                row(11, "Bank", "Solo", "SAVINGS", "JOHN DOE", "1000"));
        cfg.persons().put("MYSTERY HOLDER", "Mystery");
        balances = new ArrayList<>(balances);
        balances.add(row(12, "Bank", "Unknown person", "SAVINGS", "Mystery Holder", "1000"));

        RefreshBalancesResponseDTO out = refresher.refresh(plan, balances, cfg, TODAY);

        List<FinancialEntryDTO> joint = out.plan.getEntries().stream()
                .filter(e -> "aggregator:10".equals(e.getSource())).collect(Collectors.toList());
        assertEquals(2, joint.size());
        assertEquals(joint.get(0).getValue(), joint.get(1).getValue());
        int expectedEach = (int) Math.round(1000 * Math.pow(1.02, 51.0 / 365) / 2);
        assertEquals(expectedEach, joint.get(0).getValue());
        assertTrue(out.report.imported.stream().filter(r -> r.accountId == 10).allMatch(r -> r.stale));
        assertTrue(out.report.imported.stream().filter(r -> r.accountId == 11).noneMatch(r -> r.stale));
        assertEquals(List.of("Mystery"), out.report.unknownPersons);
    }

    @Test
    public void emptyPlanGetsThirtyYearHorizon() {
        FinancialPlanDTO empty = new FinancialPlanDTO(List.of(), new ArrayList<>(), new HashMap<>());
        RefreshBalancesResponseDTO out = refresher.refresh(empty,
                List.of(row(1, "Bank", "Checking", "CHECKING", "JOHN DOE", "1000")), cfg, TODAY);
        FinancialEntryDTO e = out.plan.getEntries().get(0);
        assertEquals(2026, e.getStartYear());
        assertEquals(2056, e.getEndYear());
        assertEquals(1000, e.getValue());
        assertEquals(List.of("John"), out.report.unknownPersons);
    }

    @Test
    public void projectionCompoundsForRemainingFraction() {
        double full = BalanceRefresher.projectToYearEnd(new BigDecimal("100"), 6.0,
                LocalDate.of(2026, 12, 31).minusDays(365), LocalDate.of(2026, 12, 31), 365);
        assertEquals(106.0, full, 1e-9);
        double none = BalanceRefresher.projectToYearEnd(new BigDecimal("100"), 6.0,
                LocalDate.of(2026, 12, 31), LocalDate.of(2026, 12, 31), 365);
        assertEquals(100.0, none, 1e-9);
    }

    @Test
    public void parsesAggregatorPayload() {
        String json = "[{\"accountId\":36,\"institutionId\":6,\"institutionName\":\"Citibank Online\","
                + "\"accountName\":\"Checking ...2540\",\"accountType\":\"CHECKING\",\"accountHolderNames\":\"JOHN DOE\","
                + "\"currentNative\":{\"amount\":\"12.34\",\"currency\":\"USD\"},"
                + "\"currentReport\":{\"amount\":\"12.34\",\"currency\":\"USD\"},"
                + "\"available\":null,\"limit\":null,\"capturedAt\":\"2026-11-21T11:32:16.663943400Z\","
                + "\"closedOn\":null},"
                + "{\"accountId\":40,\"institutionName\":\"Bank — JANE ROE\",\"accountName\":\"Old\","
                + "\"accountType\":\"SAVINGS\",\"accountHolderNames\":null,\"currentReport\":null,"
                + "\"capturedAt\":null,\"closedOn\":\"2025-03-01\"}]";

        List<AggregatorBalance> rows = AggregatorClient.parse(json);

        assertEquals(2, rows.size());
        assertEquals(36, rows.get(0).accountId());
        assertEquals(new BigDecimal("12.34"), rows.get(0).amount());
        assertEquals(Instant.parse("2026-11-21T11:32:16.663943400Z"), rows.get(0).capturedAt());
        assertNull(rows.get(0).closedOn());
        assertEquals("Bank", rows.get(1).institutionShortName());
        assertNull(rows.get(1).amount());
        assertEquals(LocalDate.of(2025, 3, 1), rows.get(1).closedOn());
    }
}
