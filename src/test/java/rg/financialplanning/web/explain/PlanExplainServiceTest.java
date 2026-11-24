package rg.financialplanning.web.explain;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import rg.financialplanning.web.claude.ClaudeCliRunner;
import rg.financialplanning.web.db.Database;
import rg.financialplanning.web.db.ExplanationRepository;
import rg.financialplanning.web.dto.ExplainThreadDTO;
import rg.financialplanning.web.dto.ExplanationDTO;
import rg.financialplanning.web.dto.FinancialEntryDTO;
import rg.financialplanning.web.dto.FinancialPlanDTO;
import rg.financialplanning.web.dto.PersonDTO;
import rg.financialplanning.web.dto.StateByYearDTO;
import rg.financialplanning.web.service.FinancialPlanService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

public class PlanExplainServiceTest {

    private Path dir;
    private Database db;
    private ExplanationRepository repo;

    private static final class FakeRunner extends ClaudeCliRunner {
        final CountDownLatch release = new CountDownLatch(1);
        final AtomicInteger starts = new AtomicInteger();
        volatile String prompt;
        volatile String answer = "The older spouse moved $32k to Roth. The younger spouse was not yet 59.";
        volatile boolean fail;

        FakeRunner() {
            super("fake", 1000);
        }

        @Override
        public Result runWithoutTools(String prompt) {
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
        dir = Files.createTempDirectory("planner-explain");
        db = new Database(dir.resolve("p.db"));
        repo = new ExplanationRepository(db);
    }

    @After
    public void close() throws IOException {
        db.close();
        try (var files = Files.walk(dir)) {
            files.sorted((a, b) -> b.compareTo(a)).forEach(p -> p.toFile().delete());
        }
    }

    private static FinancialPlanDTO plan(Map<String, Double> rates) {
        FinancialPlanDTO plan = new FinancialPlanDTO();
        plan.setPersons(new ArrayList<>(List.of(new PersonDTO("Avery", 1973), new PersonDTO("Blake", 1975))));
        FinancialEntryDTO income = new FinancialEntryDTO();
        income.setPersonName("Avery");
        income.setItemType("INCOME");
        income.setDescription("Salary");
        income.setValue(300000);
        income.setStartYear(2026);
        income.setEndYear(2030);
        FinancialEntryDTO ira = new FinancialEntryDTO();
        ira.setPersonName("Avery");
        ira.setItemType("QUALIFIED");
        ira.setDescription("IRA");
        ira.setValue(900000);
        ira.setStartYear(2026);
        ira.setEndYear(2040);
        plan.setEntries(new ArrayList<>(List.of(income, ira)));
        plan.setStatesByYear(new ArrayList<>(List.of(new StateByYearDTO("NJ", 2026, 2040))));
        plan.setRates(rates);
        return plan;
    }

    private static FinancialPlanDTO plan() {
        Map<String, Double> rates = new LinkedHashMap<>();
        rates.put("QUALIFIED", 8.0);
        rates.put("EXPENSE", 3.0);
        return plan(rates);
    }

    private static ExplanationDTO last(ExplainThreadDTO t) {
        return t.messages().get(t.messages().size() - 1);
    }

    private static ExplainThreadDTO awaitIdle(PlanExplainService svc, String hash) throws Exception {
        for (int i = 0; i < 200; i++) {
            ExplainThreadDTO t = svc.status(hash);
            if (!t.running()) return t;
            Thread.sleep(20);
        }
        throw new AssertionError("still running");
    }

    @Test
    public void oneQuestionAtATimeNamesStayHomeAndTheAnswerIsKept() throws Exception {
        FakeRunner runner = new FakeRunner();
        PlanExplainService svc = new PlanExplainService(repo, new FinancialPlanService(), runner);

        ExplainThreadDTO t = svc.ask(plan(), "Why does Roth increase in 2031?");
        assertTrue(t.running());
        assertEquals("RUNNING", last(t).status());
        assertEquals("Why does Roth increase in 2031?", last(t).question());
        assertNotNull(t.suggestions());
        assertTrue(t.suggestions().contains("Why do withdrawals jump in 2031?"));

        try {
            svc.ask(plan(), "What about taxes?");
            fail("second run should be refused");
        } catch (PlanExplainService.BusyException expected) {
            // one at a time
        }
        assertTrue(svc.status(t.planHash()).running());
        assertNull(svc.status(t.planHash()).suggestions());

        runner.release.countDown();
        ExplainThreadDTO done = awaitIdle(svc, t.planHash());
        assertEquals(1, runner.starts.get());
        assertEquals("DONE", last(done).status());
        assertEquals("Avery moved $32k to Roth. Blake was not yet 59.", last(done).answer());
        assertFalse(runner.prompt, runner.prompt.contains("Avery"));
        assertFalse(runner.prompt, runner.prompt.contains("Blake"));
        assertTrue(runner.prompt.contains("the older spouse"));
        assertTrue(runner.prompt.endsWith("QUESTION\nWhy does Roth increase in 2031?\n"));

        // The same question again, however it is typed, is served from the store.
        ExplainThreadDTO again = svc.ask(plan(), "  why does roth increase in 2031 ");
        assertFalse(again.running());
        assertEquals(1, again.messages().size());
        assertEquals(1, runner.starts.get());

        // A changed plan is a new thread, with the old answer under "earlier".
        Map<String, Double> rates = new LinkedHashMap<>();
        rates.put("QUALIFIED", 7.0);
        ExplainThreadDTO fresh = svc.thread(plan(rates));
        assertNotEquals(t.planHash(), fresh.planHash());
        assertTrue(fresh.messages().isEmpty());
        assertEquals(1, fresh.earlier().size());
    }

    @Test
    public void failuresAreRecordedAndFreeTheSlot() throws Exception {
        FakeRunner runner = new FakeRunner();
        runner.fail = true;
        runner.release.countDown();
        PlanExplainService svc = new PlanExplainService(repo, new FinancialPlanService(), runner);

        ExplainThreadDTO t = svc.ask(plan(), "Why do taxes fall?");
        ExplainThreadDTO done = awaitIdle(svc, t.planHash());
        assertEquals("ERROR", last(done).status());
        assertTrue(last(done).error().contains("limit reached"));

        runner.fail = false;
        ExplainThreadDTO retry = svc.ask(plan(), "Why do taxes fall?");
        assertTrue(retry.running());
        assertEquals(2, runner.starts.get());
    }

    @Test
    public void planHashIgnoresRateOrderAndEmptyQuestionsAreRefused() {
        Map<String, Double> a = new LinkedHashMap<>();
        a.put("QUALIFIED", 8.0);
        a.put("EXPENSE", 3.0);
        Map<String, Double> b = new LinkedHashMap<>();
        b.put("EXPENSE", 3.0);
        b.put("QUALIFIED", 8.0);
        assertEquals(PlanExplainService.planHash(plan(a)), PlanExplainService.planHash(plan(b)));
        assertEquals(16, PlanExplainService.planHash(plan(a)).length());

        PlanExplainService svc = new PlanExplainService(repo, new FinancialPlanService(), new FakeRunner());
        try {
            svc.ask(plan(), "   ");
            fail("blank question");
        } catch (IllegalArgumentException expected) {
            assertEquals("Type a question first.", expected.getMessage());
        }
        assertEquals("why does roth increase in 2031", PlanExplainService.key("  Why does  Roth increase in 2031?? "));
    }
}
