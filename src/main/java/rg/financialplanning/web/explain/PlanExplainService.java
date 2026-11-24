package rg.financialplanning.web.explain;

import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import rg.financialplanning.model.YearlySummary;
import rg.financialplanning.web.claude.ClaudeCliRunner;
import rg.financialplanning.web.db.ExplanationRepository;
import rg.financialplanning.web.dto.ExplainThreadDTO;
import rg.financialplanning.web.dto.ExplanationDTO;
import rg.financialplanning.web.dto.FinancialPlanDTO;
import rg.financialplanning.web.service.FinancialPlanService;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Answers questions about a projection through the local {@code claude} CLI,
 * one at a time for the whole planner: the CLI is metered and shared with
 * whoever is using it interactively. {@link #ask} never blocks on the CLI; the
 * panel polls {@link #status}. A question already answered on the same plan
 * version is served from the store and starts nothing.
 */
@Service
public class PlanExplainService {

    private static final Logger log = LoggerFactory.getLogger(PlanExplainService.class);
    static final long TIMEOUT_MS = 90_000;
    public static final int MAX_QUESTION_CHARS = 400;
    private static final int EARLIER_SHOWN = 8;
    private static final Gson GSON = new Gson();

    private final ExplanationRepository repo;
    private final FinancialPlanService plans;
    private final ClaudeCliRunner runner;
    private final ExecutorService pool;
    private final AtomicReference<InFlight> inFlight = new AtomicReference<>();

    private record InFlight(String planHash, ExplanationDTO pending) {}

    /** Another question is being answered right now. */
    public static final class BusyException extends RuntimeException {
        public BusyException() {
            super("One question at a time. The current one should finish within a minute or two.");
        }
    }

    @Autowired
    public PlanExplainService(ExplanationRepository repo, FinancialPlanService plans,
                              @Value("${claude.executable:}") String executable) {
        this(repo, plans, new ClaudeCliRunner(executable, TIMEOUT_MS));
    }

    public PlanExplainService(ExplanationRepository repo, FinancialPlanService plans, ClaudeCliRunner runner) {
        this.repo = repo;
        this.plans = plans;
        this.runner = runner;
        this.pool = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "plan-explain");
            t.setDaemon(true);
            return t;
        });
    }

    /** The thread for this plan as it stands, with the questions the projection suggests. */
    public ExplainThreadDTO thread(FinancialPlanDTO plan) {
        String hash = planHash(plan);
        return build(hash, ExplainEvents.suggestions(plans.project(plan)));
    }

    /** The thread for a plan version, without suggestions; what the panel polls while a run is going. */
    public ExplainThreadDTO status(String planHash) {
        return build(planHash, null);
    }

    /**
     * Start answering unless the answer is already in the thread.
     *
     * @throws IllegalArgumentException for an empty or over-long question
     * @throws BusyException when a run is already going, for any plan version
     */
    public ExplainThreadDTO ask(FinancialPlanDTO plan, String question) {
        String q = clean(question);
        if (q.isEmpty()) throw new IllegalArgumentException("Type a question first.");
        if (q.length() > MAX_QUESTION_CHARS) {
            throw new IllegalArgumentException("Keep the question under " + MAX_QUESTION_CHARS + " characters.");
        }
        String hash = planHash(plan);
        String key = key(q);
        YearlySummary[] summaries = plans.project(plan);
        List<String> suggestions = ExplainEvents.suggestions(summaries);
        if (repo.answered(hash, key).isPresent()) return build(hash, suggestions);

        ExplanationDTO pending = new ExplanationDTO(0, q, null, "RUNNING", Instant.now().toString(), null, 0, null);
        if (!inFlight.compareAndSet(null, new InFlight(hash, pending))) throw new BusyException();
        try {
            ExplainPrompt.Labels labels = ExplainPrompt.Labels.of(plan.getPersons());
            String prompt = ExplainPrompt.build(plan, summaries, repo.thread(hash), q, labels);
            pool.submit(() -> run(hash, key, pending, prompt, labels));
        } catch (RuntimeException e) {
            inFlight.set(null);
            throw e;
        }
        return build(hash, suggestions);
    }

    private void run(String hash, String key, ExplanationDTO pending, String prompt, ExplainPrompt.Labels labels) {
        ExplanationDTO result;
        try {
            log.info("Explaining: {}", pending.question());
            ClaudeCliRunner.Result r = runner.runWithoutTools(prompt);
            if (r.ok()) {
                result = new ExplanationDTO(0, pending.question(), labels.toDisplay(tidy(r.output())), "DONE",
                        pending.askedAt(), Instant.now().toString(), r.durationMs(), null);
            } else {
                result = failed(pending, r.error(), r.durationMs());
            }
        } catch (Exception e) {
            log.warn("Explanation failed: {}", e.toString());
            result = failed(pending, e.getMessage() == null ? e.toString() : e.getMessage(), 0);
        }
        try {
            repo.record(result, hash, key, prompt);
        } catch (RuntimeException e) {
            log.warn("Could not store the explanation: {}", e.toString());
        } finally {
            inFlight.set(null);
        }
        log.info("Explanation {} in {} ms", result.status(), result.durationMs());
    }

    private ExplainThreadDTO build(String hash, List<String> suggestions) {
        List<ExplanationDTO> messages = new ArrayList<>(repo.thread(hash));
        InFlight f = inFlight.get();
        boolean running = f != null && f.planHash().equals(hash);
        if (running) messages.add(f.pending());
        return new ExplainThreadDTO(hash, running, messages, suggestions, repo.earlier(hash, EARLIER_SHOWN));
    }

    private static ExplanationDTO failed(ExplanationDTO pending, String error, long durationMs) {
        return new ExplanationDTO(0, pending.question(), null, "ERROR", pending.askedAt(), Instant.now().toString(),
                durationMs, error);
    }

    /** A short stable fingerprint of the plan's inputs; rates sorted so map order cannot change it. */
    public static String planHash(FinancialPlanDTO plan) {
        FinancialPlanDTO canonical = new FinancialPlanDTO(plan.getPersons(), plan.getEntries(),
                plan.getRates() == null ? new TreeMap<>() : new TreeMap<>(plan.getRates()), plan.getStatesByYear());
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] d = md.digest(GSON.toJson(canonical).getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 8; i++) sb.append(String.format("%02x", d[i]));
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    static String clean(String q) {
        return q == null ? "" : q.trim().replaceAll("\\s+", " ");
    }

    /** The question as a lookup key: case, spacing and trailing punctuation do not make a new question. */
    static String key(String q) {
        return clean(q).toLowerCase(Locale.ROOT).replaceAll("[?.!\\s]+$", "");
    }

    /** The model's text without a code fence or a leading label; the panel renders plain paragraphs. */
    static String tidy(String out) {
        String s = out == null ? "" : out.trim();
        s = s.replaceAll("(?m)^```[a-z]*\\s*$", "").trim();
        s = s.replaceFirst("(?i)^(answer|explanation)\\s*:\\s*", "");
        return s;
    }
}
