package rg.financialplanning.web.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import rg.financialplanning.web.dto.ExplainThreadDTO;
import rg.financialplanning.web.dto.FinancialPlanDTO;
import rg.financialplanning.web.explain.PlanExplainService;

import java.util.Map;

/**
 * Questions about the projection. {@code thread} and {@code ask} take the plan
 * as the browser has it, saved or not; {@code status} is the poll and starts
 * nothing.
 */
@RestController
@RequestMapping("/api/plan/explain")
public class ExplainController {

    public static class AskRequest {
        public FinancialPlanDTO plan;
        public String question;
    }

    private final PlanExplainService service;

    public ExplainController(PlanExplainService service) {
        this.service = service;
    }

    @PostMapping("/thread")
    public ResponseEntity<ExplainThreadDTO> thread(@RequestBody FinancialPlanDTO plan) {
        return ResponseEntity.ok(service.thread(plan));
    }

    @GetMapping("/status")
    public ResponseEntity<ExplainThreadDTO> status(@RequestParam("plan") String planHash) {
        return ResponseEntity.ok(service.status(planHash));
    }

    @PostMapping("/ask")
    public ResponseEntity<?> ask(@RequestBody AskRequest req) {
        if (req == null || req.plan == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "plan and question are required."));
        }
        try {
            return ResponseEntity.status(HttpStatus.ACCEPTED).body(service.ask(req.plan, req.question));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (PlanExplainService.BusyException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", e.getMessage()));
        }
    }
}
