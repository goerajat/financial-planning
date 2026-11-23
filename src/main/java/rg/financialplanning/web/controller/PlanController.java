package rg.financialplanning.web.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import rg.financialplanning.web.dto.FinancialPlanDTO;
import rg.financialplanning.web.dto.RateDTO;
import rg.financialplanning.web.dto.SensitivityAnalysisRequestDTO;
import rg.financialplanning.web.dto.SensitivityAnalysisResponseDTO;
import rg.financialplanning.web.dto.RothComparisonRequestDTO;
import rg.financialplanning.web.dto.RothComparisonResponseDTO;
import rg.financialplanning.web.dto.StateScenarioRequestDTO;
import rg.financialplanning.web.dto.StateScenarioResponseDTO;
import rg.financialplanning.web.dto.ScenarioPdfRequestDTO;
import rg.financialplanning.web.service.FinancialPlanService;
import rg.financialplanning.web.aggregator.AggregatorException;
import rg.financialplanning.web.aggregator.BalanceRefreshService;
import rg.financialplanning.web.dto.RefreshBalancesResponseDTO;
import rg.financialplanning.web.dto.YearlySummaryDTO;
import rg.financialplanning.web.dto.RefreshRunDTO;
import rg.financialplanning.web.db.PlanRepository;
import rg.financialplanning.web.db.RefreshHistoryRepository;

import java.io.IOException;
import java.time.Instant;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/plan")
public class PlanController {

    private final FinancialPlanService planService;
    private final BalanceRefreshService balanceRefreshService;
    private final PlanRepository planRepository;
    private final RefreshHistoryRepository history;

    public PlanController(FinancialPlanService planService, BalanceRefreshService balanceRefreshService,
                          PlanRepository planRepository, RefreshHistoryRepository history) {
        this.planService = planService;
        this.balanceRefreshService = balanceRefreshService;
        this.planRepository = planRepository;
        this.history = history;
    }

    /** The stored plan; empty until something is saved. */
    @GetMapping
    public ResponseEntity<FinancialPlanDTO> getPlan() {
        return ResponseEntity.ok(planRepository.load());
    }

    /** Replaces the stored plan. Entry ids are kept as sent; missing ones are assigned. */
    @PutMapping
    public ResponseEntity<FinancialPlanDTO> putPlan(@RequestBody FinancialPlanDTO planData) {
        planRepository.save(planData);
        return ResponseEntity.ok(planData);
    }

    @GetMapping("/history")
    public ResponseEntity<List<RefreshRunDTO>> history() {
        return ResponseEntity.ok(history.runs());
    }

    @GetMapping("/history/{id}")
    public ResponseEntity<RefreshRunDTO> historyRun(@PathVariable("id") long id) {
        RefreshRunDTO run = history.run(id);
        return run == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(run);
    }

    @PostMapping("/generate")
    public ResponseEntity<byte[]> generatePdf(@RequestBody FinancialPlanDTO planData) {
        try {
            byte[] pdfBytes = planService.generatePdf(planData);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", "financial_plan.pdf");
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(pdfBytes);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/export")
    public ResponseEntity<String> exportToJson(@RequestBody FinancialPlanDTO planData) {
        String json = planService.exportAsJson(planData);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setContentDispositionFormData("attachment", "financial_plan.json");
        return ResponseEntity.ok()
                .headers(headers)
                .body(json);
    }

    @PostMapping("/import")
    public ResponseEntity<FinancialPlanDTO> importFromJson(@RequestBody String json) {
        try {
            FinancialPlanDTO planData = planService.importFromJson(json);
            planRepository.save(planData);
            return ResponseEntity.ok(planData);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/upload")
    public ResponseEntity<FinancialPlanDTO> uploadFile(@RequestParam("file") MultipartFile file) {
        try {
            String json = new String(file.getBytes(), StandardCharsets.UTF_8);
            FinancialPlanDTO planData = planService.importFromJson(json);
            planRepository.save(planData);
            return ResponseEntity.ok(planData);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/summary")
    public ResponseEntity<List<YearlySummaryDTO>> summary(@RequestBody FinancialPlanDTO planData) {
        return ResponseEntity.ok(planService.generateSummaries(planData));
    }

    @PostMapping("/refresh-balances")
    public ResponseEntity<?> refreshBalances(@RequestBody FinancialPlanDTO planData) {
        try {
            RefreshBalancesResponseDTO result = balanceRefreshService.refresh(planData);
            planRepository.save(result.plan);
            history.record(result.report, planService.generateSummaries(result.plan), Instant.now());
            return ResponseEntity.ok(result);
        } catch (AggregatorException e) {
            return ResponseEntity.status(502).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/default-rates")
    public ResponseEntity<List<RateDTO>> getDefaultRates() {
        return ResponseEntity.ok(planService.getDefaultRates());
    }

    @PostMapping("/sensitivity-analysis")
    public ResponseEntity<SensitivityAnalysisResponseDTO> runSensitivityAnalysis(
            @RequestBody SensitivityAnalysisRequestDTO request) {
        try {
            SensitivityAnalysisResponseDTO response = planService.runSensitivityAnalysis(request);
            return ResponseEntity.ok(response);
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/roth-comparison")
    public ResponseEntity<RothComparisonResponseDTO> runRothComparison(
            @RequestBody RothComparisonRequestDTO request) {
        try {
            RothComparisonResponseDTO response = planService.runRothConversionComparison(request);
            return ResponseEntity.ok(response);
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/state-scenario")
    public ResponseEntity<StateScenarioResponseDTO> runStateScenarioAnalysis(
            @RequestBody StateScenarioRequestDTO request) {
        try {
            StateScenarioResponseDTO response = planService.runStateScenarioAnalysis(request);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/generate-scenario")
    public ResponseEntity<byte[]> generateScenarioPdf(@RequestBody ScenarioPdfRequestDTO request) {
        try {
            byte[] pdfBytes = planService.generateScenarioPdf(request);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            String filename = request.getScenarioName() != null
                    ? "financial_plan_" + request.getScenarioName().replaceAll("[^a-zA-Z0-9]", "_") + ".pdf"
                    : "financial_plan_scenario.pdf";
            headers.setContentDispositionFormData("attachment", filename);
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(pdfBytes);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
