package rg.financialplanning.web.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import rg.financialplanning.web.dto.FinancialPlanDTO;
import rg.financialplanning.web.dto.RateDTO;
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
}
