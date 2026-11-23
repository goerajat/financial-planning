package rg.financialplanning.web.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import rg.financialplanning.web.db.Database;
import rg.financialplanning.web.db.PlanRepository;
import rg.financialplanning.web.db.PremiumEstimateRepository;
import rg.financialplanning.web.db.RefreshHistoryRepository;

import java.nio.file.Path;

@Configuration
public class DatabaseConfig {

    @Bean(destroyMethod = "close")
    public Database database(@Value("${planner.db:}") String dbPath) {
        return dbPath == null || dbPath.isBlank() ? new Database() : new Database(Path.of(dbPath));
    }

    @Bean
    public PlanRepository planRepository(Database db) {
        return new PlanRepository(db);
    }

    @Bean
    public RefreshHistoryRepository refreshHistoryRepository(Database db) {
        return new RefreshHistoryRepository(db);
    }

    @Bean
    public PremiumEstimateRepository premiumEstimateRepository(Database db) {
        return new PremiumEstimateRepository(db);
    }
}
