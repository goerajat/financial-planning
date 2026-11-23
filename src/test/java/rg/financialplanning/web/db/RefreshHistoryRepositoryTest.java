package rg.financialplanning.web.db;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import rg.financialplanning.web.dto.RefreshReportDTO;
import rg.financialplanning.web.dto.RefreshRunDTO;
import rg.financialplanning.web.dto.YearlySummaryDTO;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import static org.junit.Assert.*;

public class RefreshHistoryRepositoryTest {

    private Path dir;
    private Database db;
    private RefreshHistoryRepository repo;

    @Before
    public void open() throws IOException {
        dir = Files.createTempDirectory("planner-db");
        db = new Database(dir.resolve("planner.db"));
        repo = new RefreshHistoryRepository(db);
    }

    @After
    public void close() throws IOException {
        db.close();
        try (var files = Files.walk(dir)) {
            files.sorted((a, b) -> b.compareTo(a)).forEach(p -> p.toFile().delete());
        }
    }

    private static RefreshReportDTO.ImportedRow row(long account, String person, double actual, int projected, boolean stale) {
        RefreshReportDTO.ImportedRow r = new RefreshReportDTO.ImportedRow();
        r.accountId = account;
        r.personName = person;
        r.itemType = "QUALIFIED";
        r.description = "Broker 401k (as of 2026-11-21)";
        r.actual = actual;
        r.value = projected;
        r.asOf = "2026-11-21";
        r.stale = stale;
        return r;
    }

    private static YearlySummaryDTO year(int y, double netWorth) {
        YearlySummaryDTO s = new YearlySummaryDTO();
        s.year = y;
        s.netWorth = netWorth;
        s.totalAssets = netWorth;
        s.qualifiedAssets = netWorth;
        return s;
    }

    @Test
    public void recordsBalancesAndProjectionPerRun() {
        RefreshReportDTO report = new RefreshReportDTO();
        report.year = 2026;
        report.imported.add(row(1, "A", 1000.5, 1010, false));
        report.imported.add(row(2, "B", 500, 505, true));
        report.skipped.add(new RefreshReportDTO.SkippedRow("x", "closed"));
        Instant at = Instant.parse("2026-11-21T15:00:00Z");

        long runId = repo.record(report, List.of(year(2026, 1515), year(2027, 1600)), at);

        List<RefreshRunDTO> runs = repo.runs();
        assertEquals(1, runs.size());
        RefreshRunDTO run = runs.get(0);
        assertEquals(runId, run.id);
        assertEquals(at.toString(), run.refreshedAt);
        assertEquals(2026, run.planYear);
        assertEquals(2, run.importedCount);
        assertEquals(1, run.skippedCount);
        assertEquals(1500.5, run.actualTotal, 1e-9);
        assertEquals(1515, run.projectedTotal, 1e-9);
        assertEquals(1515, run.netWorthPlanYear, 1e-9);
        assertNull("list form carries no detail", run.balances);

        RefreshRunDTO detail = repo.run(runId);
        assertEquals(2, detail.balances.size());
        assertEquals(2, detail.balances.get(1).accountId);
        assertEquals(500, detail.balances.get(1).actual, 0);
        assertEquals(505, detail.balances.get(1).projected, 0);
        assertTrue(detail.balances.get(1).stale);
        assertEquals("2026-11-21", detail.balances.get(1).asOf);
        assertEquals(2, detail.projection.size());
        assertEquals(2027, detail.projection.get(1).year);
        assertEquals(1600, detail.projection.get(1).netWorth, 0);
    }

    @Test
    public void runsAreNewestFirstAndIndependent() {
        RefreshReportDTO first = new RefreshReportDTO();
        first.year = 2026;
        first.imported.add(row(1, "A", 100, 101, false));
        RefreshReportDTO second = new RefreshReportDTO();
        second.year = 2026;
        second.imported.add(row(1, "A", 110, 111, false));

        long a = repo.record(first, List.of(year(2026, 101)), Instant.parse("2026-11-01T00:00:00Z"));
        long b = repo.record(second, List.of(year(2026, 111)), Instant.parse("2026-12-01T00:00:00Z"));

        List<RefreshRunDTO> runs = repo.runs();
        assertEquals(b, runs.get(0).id);
        assertEquals(a, runs.get(1).id);
        assertEquals(110, runs.get(0).actualTotal, 0);
        assertEquals(100, runs.get(1).actualTotal, 0);
        assertNull(repo.run(999));
    }
}
