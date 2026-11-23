package rg.financialplanning.web.db;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import rg.financialplanning.web.dto.HealthPremiumEstimateDTO;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.*;

public class PremiumEstimateRepositoryTest {

    private Path dir;
    private Database db;
    private PremiumEstimateRepository repo;

    @Before
    public void open() throws IOException {
        dir = Files.createTempDirectory("planner-pe");
        db = new Database(dir.resolve("p.db"));
        repo = new PremiumEstimateRepository(db);
    }

    @After
    public void close() throws IOException {
        db.close();
        try (var files = Files.walk(dir)) {
            files.sorted((a, b) -> b.compareTo(a)).forEach(p -> p.toFile().delete());
        }
    }

    private static HealthPremiumEstimateDTO done(String name, double pre, String at) {
        return new HealthPremiumEstimateDTO(name, "DONE", "SILVER", at, at, 1234, "NJ", "New Jersey", 2032, 2037, 2038,
                pre, 235, 6.0, "## Notes\n\nText.", List.of("https://kff.org", "https://cms.gov"), null);
    }

    @Test
    public void newestPerPersonComesBackWithEveryField() {
        assertEquals(Optional.empty(), repo.latest("A"));
        repo.record(done("A", 1400, "2026-11-22T10:00:00Z"), "prompt one");
        repo.record(done("A", 1500, "2026-11-22T11:00:00Z"), "prompt two");
        repo.record(done("B", 900, "2026-11-22T11:00:00Z"), "prompt three");

        HealthPremiumEstimateDTO a = repo.latest("a").orElseThrow();
        assertEquals("A", a.personName());
        assertEquals(1500, a.preMedicarePremiumMonthly(), 0);
        assertEquals(235, a.medicarePlanPremiumMonthly(), 0);
        assertEquals(6.0, a.medicalInflationPct(), 0);
        assertEquals("New Jersey", a.stateName());
        assertEquals(2032, a.gapStart());
        assertEquals(2038, a.medicareYear());
        assertEquals(1234, a.durationMs());
        assertEquals(List.of("https://kff.org", "https://cms.gov"), a.sources());
        assertEquals("## Notes\n\nText.", a.notes());
        assertEquals(900, repo.latest("B").orElseThrow().preMedicarePremiumMonthly(), 0);
    }

    @Test
    public void errorsAreKeptToo() {
        HealthPremiumEstimateDTO err = new HealthPremiumEstimateDTO("A", "ERROR", "GOLD", "t", "t", 5, "NJ", "New Jersey",
                0, 0, 2038, 0, 0, 0, null, List.of(), "claude exited 1");
        repo.record(err, "p");
        HealthPremiumEstimateDTO back = repo.latest("A").orElseThrow();
        assertEquals("ERROR", back.status());
        assertEquals("GOLD", back.planTier());
        assertEquals("claude exited 1", back.error());
        assertTrue(back.sources().isEmpty());
        assertNull(back.notes());
    }
}
