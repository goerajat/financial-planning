package rg.financialplanning.strategy;

import org.junit.Test;
import org.junit.Before;
import rg.financialplanning.model.IndividualYearlySummary;
import rg.financialplanning.model.Person;
import rg.financialplanning.model.YearlySummary;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class RMDOptimizationStrategyTest {

    private RMDOptimizationStrategy strategy;

    @Before
    public void setUp() {
        strategy = new RMDOptimizationStrategy();
    }

    private YearlySummary createYearlySummary(int year, Map<String, IndividualYearlySummary> individuals) {
        return new YearlySummary(year, 0, 0, 0, 0, 0, 0, 0, 0, 0, individuals);
    }

    // ===== optimize tests =====

    @Test
    public void testOptimize_nullCurrentSummary() {
        // Should not throw exception
        strategy.optimize(null, null);
    }

    @Test
    public void testOptimize_noIndividuals() {
        YearlySummary current = createYearlySummary(2025, new HashMap<>());
        strategy.optimize(null, current);
        assertEquals(0.0, current.rmdWithdrawals(), 0.001);
    }

    @Test
    public void testOptimize_personTooYoungForRmd() {
        // Person born 1965 will be 60 in 2025 - below RMD age
        Person person = new Person("John", 1965);
        IndividualYearlySummary individual = new IndividualYearlySummary(person, 2025, 0, 500000, 0, 0, 0);

        Map<String, IndividualYearlySummary> individuals = new HashMap<>();
        individuals.put("John", individual);

        YearlySummary current = createYearlySummary(2025, individuals);
        strategy.optimize(null, current);

        assertEquals(0.0, individual.rmdWithdrawals(), 0.001);
        assertEquals(0.0, current.rmdWithdrawals(), 0.001);
    }

    @Test
    public void testOptimize_personAtRmdAge_born1952() {
        // Person born 1952, RMD starts at 73 - in 2025 they are 73
        Person person = new Person("John", 1952);
        IndividualYearlySummary individual = new IndividualYearlySummary(person, 2025, 0, 500000, 0, 0, 0);

        // Verify name is set correctly
        assertEquals("John", individual.name());

        Map<String, IndividualYearlySummary> individuals = new HashMap<>();
        individuals.put(individual.name(), individual);

        // Create previous year summary with qualified assets - use individual.name() as key for consistency
        IndividualYearlySummary prevIndividual = new IndividualYearlySummary(person, 2024, 0, 500000, 0, 0, 0);
        Map<String, IndividualYearlySummary> prevIndividuals = new HashMap<>();
        prevIndividuals.put(individual.name(), prevIndividual);
        YearlySummary previous = createYearlySummary(2024, prevIndividuals);

        // Verify the lookup works
        assertNotNull(previous.getIndividualSummary("John"));
        assertEquals(500000, previous.getIndividualSummary("John").qualifiedAssets(), 0.01);

        YearlySummary current = createYearlySummary(2025, individuals);
        strategy.optimize(previous, current);

        // RMD at 73: 500000 / 26.5 = 18867.92
        double expectedRmd = 500000 / 26.5;
        assertEquals(expectedRmd, individual.rmdWithdrawals(), 0.01);
        assertEquals(expectedRmd, current.rmdWithdrawals(), 0.01);
    }

    @Test
    public void testOptimize_personAtRmdAge_born1960() {
        // Person born 1960, RMD starts at 75 - in 2035 they are 75
        Person person = new Person("John", 1960);
        IndividualYearlySummary individual = new IndividualYearlySummary(person, 2035, 0, 500000, 0, 0, 0);

        Map<String, IndividualYearlySummary> individuals = new HashMap<>();
        individuals.put("John", individual);

        // Create previous year summary
        IndividualYearlySummary prevIndividual = new IndividualYearlySummary(person, 2034, 0, 500000, 0, 0, 0);
        Map<String, IndividualYearlySummary> prevIndividuals = new HashMap<>();
        prevIndividuals.put("John", prevIndividual);
        YearlySummary previous = createYearlySummary(2034, prevIndividuals);

        YearlySummary current = createYearlySummary(2035, individuals);
        strategy.optimize(previous, current);

        // RMD at 75: 500000 / 24.6 = 20325.20
        double expectedRmd = 500000 / 24.6;
        assertEquals(expectedRmd, individual.rmdWithdrawals(), 0.01);
    }

    @Test
    public void testOptimize_noPreviousSummary() {
        // Person at RMD age but no previous summary - RMD should be 0
        Person person = new Person("John", 1952);
        IndividualYearlySummary individual = new IndividualYearlySummary(person, 2025, 0, 500000, 0, 0, 0);

        Map<String, IndividualYearlySummary> individuals = new HashMap<>();
        individuals.put("John", individual);

        YearlySummary current = createYearlySummary(2025, individuals);
        strategy.optimize(null, current);

        assertEquals(0.0, individual.rmdWithdrawals(), 0.001);
    }

    @Test
    public void testOptimize_noQualifiedAssets() {
        Person person = new Person("John", 1952);
        IndividualYearlySummary individual = new IndividualYearlySummary(person, 2025, 0, 0, 200000, 100000, 0);

        Map<String, IndividualYearlySummary> individuals = new HashMap<>();
        individuals.put("John", individual);

        // Previous year with no qualified assets
        IndividualYearlySummary prevIndividual = new IndividualYearlySummary(person, 2024, 0, 0, 200000, 100000, 0);
        Map<String, IndividualYearlySummary> prevIndividuals = new HashMap<>();
        prevIndividuals.put("John", prevIndividual);
        YearlySummary previous = createYearlySummary(2024, prevIndividuals);

        YearlySummary current = createYearlySummary(2025, individuals);
        strategy.optimize(previous, current);

        assertEquals(0.0, individual.rmdWithdrawals(), 0.001);
    }

    @Test
    public void testOptimize_multipleIndividuals() {
        // Two people at RMD age
        Person john = new Person("John", 1952);
        Person jane = new Person("Jane", 1950);

        IndividualYearlySummary johnSummary = new IndividualYearlySummary(john, 2025, 0, 500000, 0, 0, 0);
        IndividualYearlySummary janeSummary = new IndividualYearlySummary(jane, 2025, 0, 300000, 0, 0, 0);

        Map<String, IndividualYearlySummary> individuals = new HashMap<>();
        individuals.put(john.name(), johnSummary);
        individuals.put(jane.name(), janeSummary);

        // Previous year - use person.name() as key to match lookup
        IndividualYearlySummary prevJohn = new IndividualYearlySummary(john, 2024, 0, 500000, 0, 0, 0);
        IndividualYearlySummary prevJane = new IndividualYearlySummary(jane, 2024, 0, 300000, 0, 0, 0);
        Map<String, IndividualYearlySummary> prevIndividuals = new HashMap<>();
        prevIndividuals.put(john.name(), prevJohn);
        prevIndividuals.put(jane.name(), prevJane);
        YearlySummary previous = createYearlySummary(2024, prevIndividuals);

        YearlySummary current = createYearlySummary(2025, individuals);
        strategy.optimize(previous, current);

        double johnRmd = 500000 / 26.5; // Age 73
        double janeRmd = 300000 / 24.6; // Age 75

        assertEquals(johnRmd, johnSummary.rmdWithdrawals(), 0.01);
        assertEquals(janeRmd, janeSummary.rmdWithdrawals(), 0.01);
        assertEquals(johnRmd + janeRmd, current.rmdWithdrawals(), 0.01);
    }

    @Test
    public void testOptimize_nullPerson() {
        IndividualYearlySummary individual = new IndividualYearlySummary(null, 2025, 0, 500000, 0, 0, 0);

        Map<String, IndividualYearlySummary> individuals = new HashMap<>();
        individuals.put("Unknown", individual);

        YearlySummary current = createYearlySummary(2025, individuals);
        strategy.optimize(null, current);

        assertEquals(0.0, individual.rmdWithdrawals(), 0.001);
    }

    // ===== calculateRmd tests =====

    @Test
    public void testCalculateRmd_underAge() {
        Person person = new Person("John", 1970);
        double rmd = strategy.calculateRmd(person, 2025, 500000); // Age 55
        assertEquals(0.0, rmd, 0.001);
    }

    @Test
    public void testCalculateRmd_atRmdAge() {
        Person person = new Person("John", 1952);
        double rmd = strategy.calculateRmd(person, 2025, 500000); // Age 73
        assertEquals(500000 / 26.5, rmd, 0.01);
    }

    @Test
    public void testCalculateRmd_nullPerson() {
        double rmd = strategy.calculateRmd(null, 2025, 500000);
        assertEquals(0.0, rmd, 0.001);
    }

    @Test
    public void testCalculateRmd_zeroAssets() {
        Person person = new Person("John", 1952);
        double rmd = strategy.calculateRmd(person, 2025, 0);
        assertEquals(0.0, rmd, 0.001);
    }

    @Test
    public void testCalculateRmd_negativeAssets() {
        Person person = new Person("John", 1952);
        double rmd = strategy.calculateRmd(person, 2025, -10000);
        assertEquals(0.0, rmd, 0.001);
    }

    // ===== getRmdStartAge tests =====

    @Test
    public void testGetRmdStartAge_born1950() {
        Person person = new Person("John", 1950);
        assertEquals(72, strategy.getRmdStartAge(person));
    }

    @Test
    public void testGetRmdStartAge_born1955() {
        Person person = new Person("John", 1955);
        assertEquals(73, strategy.getRmdStartAge(person));
    }

    @Test
    public void testGetRmdStartAge_born1960() {
        Person person = new Person("John", 1960);
        assertEquals(75, strategy.getRmdStartAge(person));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetRmdStartAge_nullPersonThrowsException() {
        strategy.getRmdStartAge(null);
    }

    // ===== getFirstRmdYear tests =====

    @Test
    public void testGetFirstRmdYear_born1950() {
        Person person = new Person("John", 1950);
        assertEquals(1950 + 72, strategy.getFirstRmdYear(person));
    }

    @Test
    public void testGetFirstRmdYear_born1955() {
        Person person = new Person("John", 1955);
        assertEquals(1955 + 73, strategy.getFirstRmdYear(person));
    }

    @Test
    public void testGetFirstRmdYear_born1960() {
        Person person = new Person("John", 1960);
        assertEquals(1960 + 75, strategy.getFirstRmdYear(person));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetFirstRmdYear_nullPersonThrowsException() {
        strategy.getFirstRmdYear(null);
    }

    // ===== Strategy metadata tests =====

    @Test
    public void testGetStrategyName() {
        assertEquals("RMD Optimization", strategy.getStrategyName());
    }

    @Test
    public void testGetDescription() {
        String description = strategy.getDescription();
        assertNotNull(description);
        assertTrue(description.contains("RMD"));
        assertTrue(description.contains("Required Minimum Distribution"));
    }
}
