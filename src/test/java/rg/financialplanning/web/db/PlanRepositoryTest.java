package rg.financialplanning.web.db;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import rg.financialplanning.web.dto.FinancialEntryDTO;
import rg.financialplanning.web.dto.FinancialPlanDTO;
import rg.financialplanning.web.dto.PersonDTO;
import rg.financialplanning.web.dto.StateByYearDTO;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class PlanRepositoryTest {

    private Path dir;
    private Database db;
    private PlanRepository repo;

    @Before
    public void open() throws IOException {
        dir = Files.createTempDirectory("planner-db");
        db = new Database(dir.resolve("planner.db"));
        repo = new PlanRepository(db);
    }

    @After
    public void close() throws IOException {
        db.close();
        try (var files = Files.walk(dir)) {
            files.sorted((a, b) -> b.compareTo(a)).forEach(p -> p.toFile().delete());
        }
    }

    @Test
    public void emptyUntilSaved() {
        assertFalse(repo.exists());
        FinancialPlanDTO plan = repo.load();
        assertTrue(plan.getPersons().isEmpty());
        assertTrue(plan.getEntries().isEmpty());
        assertTrue(plan.getRates().isEmpty());
        assertTrue(plan.getStatesByYear().isEmpty());
        assertNull(repo.updatedAt());
        assertEquals(4, db.schemaVersion());
    }

    @Test
    public void roundTripsEveryField() {
        FinancialPlanDTO plan = new FinancialPlanDTO();
        PersonDTO pa = new PersonDTO("A", 1970, true);
        pa.setSocialSecurityFraMonthly(3200.5);
        pa.setSocialSecurityClaimAge(70);
        pa.setPreMedicarePremiumMonthly(1400);
        pa.setMedicarePlanPremiumMonthly(250);
        plan.getPersons().add(pa);
        plan.getPersons().add(new PersonDTO("B", 1972));
        FinancialEntryDTO e1 = new FinancialEntryDTO("A", "INCOME", "Salary", 100000, 2026, 2030);
        e1.setId(7L);
        FinancialEntryDTO e2 = new FinancialEntryDTO("B", "QUALIFIED", "401k (as of 2026-11-21)", 250000, 2026, 2056);
        e2.setId(3L);
        e2.setSource("aggregator:42");
        FinancialEntryDTO e3 = new FinancialEntryDTO("A", "EXPENSE", "Living", 60000, 2026, 2056);
        FinancialEntryDTO e4 = new FinancialEntryDTO("A", "MORTGAGE", "House", 750000, 2026, 2056);
        e4.setAnnualPayment(87600);
        plan.getEntries().add(e1);
        plan.getEntries().add(e2);
        plan.getEntries().add(e3);
        plan.getEntries().add(e4);
        plan.setRates(Map.of("QUALIFIED", 6.5, "CASH", 2.0));
        plan.getStatesByYear().add(new StateByYearDTO("NJ", 2026, 2030));
        plan.getStatesByYear().add(new StateByYearDTO("FL", 2031, 2056));

        repo.save(plan);
        assertTrue(repo.exists());
        assertNotNull(repo.updatedAt());
        FinancialPlanDTO back = repo.load();

        assertEquals(2, back.getPersons().size());
        assertEquals("A", back.getPersons().get(0).getName());
        assertTrue(back.getPersons().get(0).isSelfEmployed());
        assertEquals(1972, back.getPersons().get(1).getYearOfBirth());
        assertFalse(back.getPersons().get(1).isSelfEmployed());
        assertEquals(3200.5, back.getPersons().get(0).getSocialSecurityFraMonthly(), 0);
        assertEquals(70, back.getPersons().get(0).getSocialSecurityClaimAge());
        assertEquals(1400, back.getPersons().get(0).getPreMedicarePremiumMonthly(), 0);
        assertEquals(250, back.getPersons().get(0).getMedicarePlanPremiumMonthly(), 0);
        assertEquals(0, back.getPersons().get(1).getSocialSecurityClaimAge());

        assertEquals(4, back.getEntries().size());
        assertEquals(Long.valueOf(7), back.getEntries().get(0).getId());
        assertEquals("Salary", back.getEntries().get(0).getDescription());
        assertEquals(Long.valueOf(3), back.getEntries().get(1).getId());
        assertEquals("aggregator:42", back.getEntries().get(1).getSource());
        assertEquals(250000, back.getEntries().get(1).getValue());
        assertEquals("an entry without an id gets one above the highest", Long.valueOf(8), back.getEntries().get(2).getId());
        assertNull(back.getEntries().get(2).getSource());
        assertEquals(0, back.getEntries().get(2).getAnnualPayment(), 0);
        assertEquals(87600, back.getEntries().get(3).getAnnualPayment(), 0);

        assertEquals(6.5, back.getRates().get("QUALIFIED"), 0);
        assertEquals(2.0, back.getRates().get("CASH"), 0);
        assertEquals(2, back.getStatesByYear().size());
        assertEquals("FL", back.getStatesByYear().get(1).getState());
        assertEquals(2031, back.getStatesByYear().get(1).getStartYear());
    }

    @Test
    public void saveReplacesRatherThanAppends() {
        FinancialPlanDTO plan = new FinancialPlanDTO();
        plan.getPersons().add(new PersonDTO("A", 1970));
        plan.getEntries().add(new FinancialEntryDTO("A", "INCOME", "Salary", 1, 2026, 2030));
        plan.getEntries().add(new FinancialEntryDTO("A", "INCOME", "Bonus", 1, 2026, 2030));
        repo.save(plan);

        FinancialPlanDTO smaller = new FinancialPlanDTO();
        smaller.getPersons().add(new PersonDTO("A", 1970));
        smaller.getEntries().add(new FinancialEntryDTO("A", "INCOME", "Salary", 2, 2026, 2030));
        repo.save(smaller);

        FinancialPlanDTO back = repo.load();
        assertEquals(1, back.getEntries().size());
        assertEquals(2, back.getEntries().get(0).getValue());
        assertEquals(List.of("A"), back.getPersons().stream().map(PersonDTO::getName).toList());
    }

    @Test
    public void survivesReopen() {
        FinancialPlanDTO plan = new FinancialPlanDTO();
        plan.getPersons().add(new PersonDTO("A", 1970));
        repo.save(plan);
        db.close();

        db = new Database(dir.resolve("planner.db"));
        repo = new PlanRepository(db);
        assertEquals(1, repo.load().getPersons().size());
    }
}
