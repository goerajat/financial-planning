package rg.financialplanning.web.dto;

public class RefreshBalancesResponseDTO {
    public FinancialPlanDTO plan;
    public RefreshReportDTO report;

    public RefreshBalancesResponseDTO() {
    }

    public RefreshBalancesResponseDTO(FinancialPlanDTO plan, RefreshReportDTO report) {
        this.plan = plan;
        this.report = report;
    }
}
