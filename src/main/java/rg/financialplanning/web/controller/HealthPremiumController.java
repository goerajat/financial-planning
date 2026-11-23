package rg.financialplanning.web.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import rg.financialplanning.web.dto.FinancialPlanDTO;
import rg.financialplanning.web.dto.HealthPremiumEstimateDTO;
import rg.financialplanning.web.health.HealthPremiumEstimateService;
import rg.financialplanning.web.health.HealthPremiumPrompt;

import java.util.Locale;
import java.util.Map;

/**
 * Premium research through the local claude CLI.
 *
 * <pre>
 * POST /api/plan/health-premiums/estimate  {personName, planTier, plan}  → 202 with the RUNNING status
 * GET  /api/plan/health-premiums?person=X                               → latest status, or NONE
 * </pre>
 */
@RestController
@RequestMapping("/api/plan/health-premiums")
public class HealthPremiumController {

    public static class EstimateRequest {
        public String personName;
        public String planTier;
        public FinancialPlanDTO plan;
    }

    private final HealthPremiumEstimateService service;

    public HealthPremiumController(HealthPremiumEstimateService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<HealthPremiumEstimateDTO> status(@RequestParam("person") String person) {
        return ResponseEntity.ok(service.status(person));
    }

    @PostMapping("/estimate")
    public ResponseEntity<?> estimate(@RequestBody EstimateRequest req) {
        if (req == null || req.personName == null || req.personName.isBlank() || req.plan == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "personName and plan are required."));
        }
        HealthPremiumPrompt.Tier tier;
        try {
            tier = req.planTier == null ? HealthPremiumPrompt.Tier.SILVER
                    : HealthPremiumPrompt.Tier.valueOf(req.planTier.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "planTier must be SILVER or GOLD."));
        }
        try {
            return ResponseEntity.status(HttpStatus.ACCEPTED).body(service.request(req.plan, req.personName, tier));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
