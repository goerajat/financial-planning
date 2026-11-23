package rg.financialplanning.web.health;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import rg.financialplanning.web.claude.ClaudeCliRunner;
import rg.financialplanning.web.db.Database;
import rg.financialplanning.web.db.PremiumEstimateRepository;
import rg.financialplanning.web.dto.FinancialEntryDTO;
import rg.financialplanning.web.dto.FinancialPlanDTO;
import rg.financialplanning.web.dto.HealthPremiumEstimateDTO;
import rg.financialplanning.web.dto.PersonDTO;
import rg.financialplanning.web.dto.StateByYearDTO;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

public class HealthPremiumEstimateServiceTest {

    private Path dir;
    private Database db;
    private PremiumEstimateRepository repo;

    /** Stands in for the CLI: blocks until released, counts starts, answers a fixed text. */
    private static final class FakeRunner extends ClaudeCliRunner {
        final CountDownLatch release = new CountDownLatch(1);
        final AtomicInteger starts = new AtomicInteger();
        volatile String prompt;
        volatile String answer = "Found it.\n{\"preMedicarePremiumMonthly\": 1500, \"medicarePlanPremiumMonthly\": 235, "
                + "\"medicalInflationPct\": 6, \"sources\": [\"https://kff.org\"]}";
        volatile boolean fail;

        FakeRunner() {
            super("fake", 1000);
        }

        @Override
        public Result run(String prompt, String allowedTools, int maxTurns) {
            starts.incrementAndGet();
            this.prompt = prompt;
            try {
                release.await(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return fail ? new Result(false, null, "claude exited 1: limit reached", 5) : new Result(true, answer, null, 7);
        }
    }

    @Before
    public void open() throws IOException {
        dir = Files.createTempDirectory("planner-hp");
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

    private static FinancialPlanDTO plan() {
        FinancialPlanDTO plan = new FinancialPlanDTO();
        plan.setPersons(new ArrayList<>(List.of(new PersonDTO("A", 1973), new PersonDTO("B", 1974))));
        FinancialEntryDTO e = new FinancialEntryDTO();
        e.setPersonName("A");
        e.setItemType("INCOME");
        e.setDescription("Salary");
        e.setValue(100000);
        e.setStartYear(2026);
        e.setEndYear(2030);
        plan.setEntries(new ArrayList<>(List.of(e)));
        plan.setStatesByYear(new ArrayList<>(List.of(new StateByYearDTO("NJ", 2026, 2070))));
        return plan;
    }

    private static HealthPremiumEstimateDTO awaitDone(HealthPremiumEstimateService svc, String person) throws Exception {
        for (int i = 0; i < 100; i++) {
            HealthPremiumEstimateDTO s = svc.status(person);
            if (!"RUNNING".equals(s.status())) return s;
            Thread.sleep(20);
        }
        throw new AssertionError("still running");
    }

    @Test
    public void runsOncePerPersonAndStoresTheAnswer() throws Exception {
        FakeRunner runner = new FakeRunner();
        HealthPremiumEstimateService svc = new HealthPremiumEstimateService(repo, runner);

        HealthPremiumEstimateDTO first = svc.request(plan(), "A", HealthPremiumPrompt.Tier.SILVER);
        HealthPremiumEstimateDTO second = svc.request(plan(), "a", HealthPremiumPrompt.Tier.GOLD);
        assertEquals("RUNNING", first.status());
        assertEquals("SILVER", second.planTier());
        assertEquals("RUNNING", svc.status("A").status());
        assertEquals(2031, first.gapStart());
        assertEquals("New Jersey", first.stateName());

        runner.release.countDown();
        HealthPremiumEstimateDTO done = awaitDone(svc, "A");
        assertEquals(1, runner.starts.get());
        assertEquals("DONE", done.status());
        assertEquals(1500, done.preMedicarePremiumMonthly(), 0);
        assertEquals(235, done.medicarePlanPremiumMonthly(), 0);
        assertEquals(6, done.medicalInflationPct(), 0);
        assertEquals("Found it.", done.notes());
        assertEquals(List.of("https://kff.org"), done.sources());
        assertTrue(runner.prompt.contains("born 1973"));
        assertFalse(runner.prompt.contains("Person: A"));

        // Stored: a fresh service over the same store sees it.
        HealthPremiumEstimateDTO reloaded = new HealthPremiumEstimateService(repo, new FakeRunner()).status("A");
        assertEquals("DONE", reloaded.status());
        assertEquals(1500, reloaded.preMedicarePremiumMonthly(), 0);
        assertEquals("NONE", svc.status("B").status());
    }

    @Test
    public void failureIsStoredAsErrorAndCanBeRetried() throws Exception {
        FakeRunner runner = new FakeRunner();
        runner.fail = true;
        HealthPremiumEstimateService svc = new HealthPremiumEstimateService(repo, runner);
        svc.request(plan(), "A", HealthPremiumPrompt.Tier.SILVER);
        runner.release.countDown();
        HealthPremiumEstimateDTO err = awaitDone(svc, "A");
        assertEquals("ERROR", err.status());
        assertTrue(err.error().contains("limit reached"));

        runner.fail = false;
        HealthPremiumEstimateDTO again = svc.request(plan(), "A", HealthPremiumPrompt.Tier.SILVER);
        assertEquals("RUNNING", again.status());
        assertEquals("DONE", awaitDone(svc, "A").status());
        assertEquals(2, runner.starts.get());
    }

    @Test
    public void unparseableAnswerIsAnError() throws Exception {
        FakeRunner runner = new FakeRunner();
        runner.answer = "Sorry, no figures.";
        HealthPremiumEstimateService svc = new HealthPremiumEstimateService(repo, runner);
        svc.request(plan(), "A", HealthPremiumPrompt.Tier.SILVER);
        runner.release.countDown();
        HealthPremiumEstimateDTO err = awaitDone(svc, "A");
        assertEquals("ERROR", err.status());
        assertTrue(err.error().contains("no JSON object"));
    }

    @Test
    public void planWithoutAStateIsRejectedBeforeAnyRun() {
        FakeRunner runner = new FakeRunner();
        HealthPremiumEstimateService svc = new HealthPremiumEstimateService(repo, runner);
        FinancialPlanDTO plan = plan();
        plan.setStatesByYear(new ArrayList<>());
        try {
            svc.request(plan, "A", HealthPremiumPrompt.Tier.SILVER);
            fail();
        } catch (IllegalArgumentException expected) {
            assertEquals(0, runner.starts.get());
            assertEquals("NONE", svc.status("A").status());
        }
    }
}
