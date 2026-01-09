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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/plan")
public class PlanController {

    private final FinancialPlanService planService;

    public PlanController(FinancialPlanService planService) {
        this.planService = planService;
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
            return ResponseEntity.ok(planData);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
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
