package rg.financialplanning.web.health;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import rg.financialplanning.web.claude.ClaudeCliRunner;
import rg.financialplanning.web.db.PremiumEstimateRepository;
import rg.financialplanning.web.dto.FinancialPlanDTO;
import rg.financialplanning.web.dto.HealthPremiumEstimateDTO;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Runs the premium research for one person on a background thread and keeps
 * the answer. One run per person at a time: the CLI is metered and shared with
 * whoever is using it interactively, so a double click must not fork a second
 * process. {@link #request} never blocks on the CLI; the UI polls {@link #status}.
 */
@Service
public class HealthPremiumEstimateService {

    private static final Logger log = LoggerFactory.getLogger(HealthPremiumEstimateService.class);
    private static final String ALLOWED_TOOLS = "WebSearch,WebFetch";
    private static final ZoneId ZONE = ZoneId.of("America/New_York");

    private final PremiumEstimateRepository repo;
    private final ClaudeCliRunner runner;
    private final ExecutorService pool;
    private final Map<String, HealthPremiumEstimateDTO> running = new ConcurrentHashMap<>();

    @Autowired
    public HealthPremiumEstimateService(PremiumEstimateRepository repo,
                                        @Value("${claude.executable:}") String executable) {
        this(repo, new ClaudeCliRunner(executable, ClaudeCliRunner.DEFAULT_TIMEOUT_MS));
    }

    public HealthPremiumEstimateService(PremiumEstimateRepository repo, ClaudeCliRunner runner) {
        this.repo = repo;
        this.runner = runner;
        this.pool = Executors.newFixedThreadPool(2, r -> {
            Thread t = new Thread(r, "premium-estimate");
            t.setDaemon(true);
            return t;
        });
    }

    /** Newest estimate for a person: a run in flight wins over a stored one. */
    public HealthPremiumEstimateDTO status(String personName) {
        HealthPremiumEstimateDTO inFlight = running.get(key(personName));
        if (inFlight != null) return inFlight;
        Optional<HealthPremiumEstimateDTO> stored = repo.latest(personName);
        return stored.orElse(HealthPremiumEstimateDTO.none(personName));
    }

    /**
     * Start a run for the person unless one is already going. The plan comes from
     * the caller, not the store, so an unsaved year of birth or state still counts.
     *
     * @throws IllegalArgumentException when the plan lacks what the prompt needs
     */
    public HealthPremiumEstimateDTO request(FinancialPlanDTO plan, String personName, HealthPremiumPrompt.Tier tier) {
        HealthPremiumPrompt.Inputs in = HealthPremiumPrompt.inputs(plan, personName, tier, LocalDate.now(ZONE).getYear());
        String k = key(in.personName());
        String now = Instant.now().toString();
        HealthPremiumEstimateDTO pending = new HealthPremiumEstimateDTO(in.personName(), "RUNNING", tier.name(), now, null,
                0, in.hasGap() ? in.preMedicareState() : in.medicareState(),
                in.hasGap() ? in.preMedicareStateName() : in.medicareStateName(),
                in.gapStart(), in.gapEnd(), in.medicareYear(), 0, 0, 0, null, List.of(), null);
        if (running.putIfAbsent(k, pending) != null) return running.get(k);
        String prompt = HealthPremiumPrompt.build(in);
        try {
            pool.submit(() -> run(k, pending, prompt));
        } catch (RuntimeException e) {
            running.remove(k);
            throw e;
        }
        return pending;
    }

    private void run(String k, HealthPremiumEstimateDTO pending, String prompt) {
        HealthPremiumEstimateDTO result;
        try {
            log.info("Premium estimate starting for {}", pending.personName());
            ClaudeCliRunner.Result r = runner.run(prompt, ALLOWED_TOOLS, ClaudeCliRunner.DEFAULT_MAX_TURNS);
            if (r.ok()) {
                HealthPremiumEstimateParser.Parsed p = HealthPremiumEstimateParser.parse(r.output());
                result = new HealthPremiumEstimateDTO(pending.personName(), "DONE", pending.planTier(),
                        pending.requestedAt(), Instant.now().toString(), r.durationMs(), pending.state(), pending.stateName(),
                        pending.gapStart(), pending.gapEnd(), pending.medicareYear(),
                        p.preMedicareMonthly(), p.medicarePlanMonthly(), p.medicalInflationPct(), p.notes(), p.sources(), null);
            } else {
                result = failed(pending, r.error(), r.durationMs());
            }
        } catch (Exception e) {
            log.warn("Premium estimate for {} failed: {}", pending.personName(), e.toString());
            result = failed(pending, e.getMessage() == null ? e.toString() : e.getMessage(), 0);
        }
        try {
            repo.record(result, prompt);
        } catch (RuntimeException e) {
            log.warn("Could not store the premium estimate for {}: {}", pending.personName(), e.toString());
        } finally {
            running.remove(k);
        }
        log.info("Premium estimate for {}: {}", pending.personName(), result.status());
    }

    private static HealthPremiumEstimateDTO failed(HealthPremiumEstimateDTO pending, String error, long durationMs) {
        return new HealthPremiumEstimateDTO(pending.personName(), "ERROR", pending.planTier(), pending.requestedAt(),
                Instant.now().toString(), durationMs, pending.state(), pending.stateName(), pending.gapStart(),
                pending.gapEnd(), pending.medicareYear(), 0, 0, 0, null, List.of(), error);
    }

    private static String key(String personName) {
        return personName == null ? "" : personName.trim().toLowerCase(Locale.ROOT);
    }
}
