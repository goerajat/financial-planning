package rg.financialplanning.web.aggregator;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import rg.financialplanning.web.dto.FinancialPlanDTO;
import rg.financialplanning.web.dto.RefreshBalancesResponseDTO;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;

@Service
public class BalanceRefreshService {

    private final Path configPath;
    private final AggregatorClient client = new AggregatorClient();
    private final BalanceRefresher refresher = new BalanceRefresher();

    public BalanceRefreshService(@Value("${aggregator.config:}") String configPath) {
        this.configPath = configPath == null || configPath.isBlank()
                ? AggregatorConfig.defaultPath() : Path.of(configPath);
    }

    public RefreshBalancesResponseDTO refresh(FinancialPlanDTO plan) {
        AggregatorConfig cfg;
        try {
            cfg = AggregatorConfig.load(configPath);
        } catch (IOException e) {
            throw new AggregatorException("Could not read " + configPath + ": " + e.getMessage(), e);
        }
        return refresher.refresh(plan, client.fetchBalances(cfg.baseUrl()), cfg, LocalDate.now(BalanceRefresher.ZONE));
    }
}
