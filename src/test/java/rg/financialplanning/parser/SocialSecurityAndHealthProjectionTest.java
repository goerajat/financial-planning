package rg.financialplanning.parser;

import org.junit.Test;
import rg.financialplanning.calculator.MedicarePremiumCalculator;
import rg.financialplanning.model.FinancialEntry;
import rg.financialplanning.model.ItemType;
import rg.financialplanning.model.Person;
import rg.financialplanning.model.YearlySummary;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class SocialSecurityAndHealthProjectionTest {

    private static final double TOL = 0.5;

    private static Map<ItemType, Double> rates(double cola, double medical) {
        Map<ItemType, Double> rates = new HashMap<>();
        rates.put(ItemType.SOCIAL_SECURITY_BENEFITS, cola);
        rates.put(ItemType.HEALTH_PREMIUM, medical);
        rates.put(ItemType.EXPENSE, 3.0);
        return rates;
    }

    private static YearlySummary[] project(List<FinancialEntry> entries, Map<String, Person> persons, Map<ItemType, Double> rates) {
        FinancialDataProcessor processor = new FinancialDataProcessor();
        processor.setEntries(entries);
        return processor.generateYearlySummaries(rates, persons);
    }

    private static YearlySummary year(YearlySummary[] s, int year) {
        for (YearlySummary y : s) if (y.year() == year) return y;
        throw new AssertionError("no year " + year);
    }

    @Test
    public void benefitStartsAtTheClaimingAgeWithColaFromThePlanStart() {
        // Born 1973, claims at 70 in 2043; PIA 3,000 a month in 2026 dollars, COLA 2%.
        Person a = new Person("A", 1973, false, 3000, 70, 0, 0);
        Map<String, Person> persons = Map.of("A", a);
        List<FinancialEntry> entries = List.of(new FinancialEntry("A", ItemType.EXPENSE, "Living", 50000, 2026, 2050));
        YearlySummary[] s = project(entries, persons, rates(2.0, 0));

        assertEquals(0, year(s, 2042).totalSocialSecurity(), TOL);
        double expected2043 = 3000 * 12 * 1.24 * Math.pow(1.02, 2043 - 2026);
        assertEquals(expected2043, year(s, 2043).totalSocialSecurity(), TOL);
        assertEquals(expected2043 * 1.02, year(s, 2044).totalSocialSecurity(), TOL);
        assertEquals(expected2043, year(s, 2043).individualSummaries().get("A").socialSecurityBenefits(), TOL);
    }

    @Test
    public void earlyClaimIsReducedAndDefaultClaimAgeIsFra() {
        Person early = new Person("E", 1973, false, 3000, 62, 0, 0);
        Person fra = new Person("F", 1973, false, 3000, 0, 0, 0);
        Map<String, Person> persons = new LinkedHashMap<>();
        persons.put("E", early);
        persons.put("F", fra);
        List<FinancialEntry> entries = List.of(new FinancialEntry("E", ItemType.EXPENSE, "Living", 50000, 2026, 2045));
        YearlySummary[] s = project(entries, persons, rates(0, 0));

        // E claims at 62 in 2035 and gets 70% of PIA; F waits for 67 in 2040.
        assertEquals(3000 * 12 * 0.70, year(s, 2035).individualSummaries().get("E").socialSecurityBenefits(), TOL);
        assertNull(year(s, 2035).individualSummaries().get("F"));
        assertEquals(3000 * 12, year(s, 2040).individualSummaries().get("F").socialSecurityBenefits(), TOL);
    }

    @Test
    public void spousalTopUpOnceTheWorkerHasClaimed() {
        // Worker PIA 4,000; spouse's own PIA 1,200. Both claim at 67 (2040 and 2041).
        Person worker = new Person("W", 1973, false, 4000, 67, 0, 0);
        Person spouse = new Person("S", 1974, false, 1200, 67, 0, 0);
        Map<String, Person> persons = new LinkedHashMap<>();
        persons.put("W", worker);
        persons.put("S", spouse);
        List<FinancialEntry> entries = List.of(new FinancialEntry("W", ItemType.EXPENSE, "Living", 50000, 2026, 2045));
        YearlySummary[] s = project(entries, persons, rates(0, 0));

        assertEquals(4000 * 12, year(s, 2040).individualSummaries().get("W").socialSecurityBenefits(), TOL);
        // Spouse claims in 2041: half of 4,000 beats her own 1,200.
        assertEquals(2000 * 12, year(s, 2041).individualSummaries().get("S").socialSecurityBenefits(), TOL);
        assertEquals(6000 * 12, year(s, 2041).totalSocialSecurity(), TOL);
    }

    @Test
    public void spousalIsReducedWhenTheSpouseClaimsEarly() {
        Person worker = new Person("W", 1973, false, 4000, 67, 0, 0);
        Person spouse = new Person("S", 1974, false, 0, 62, 0, 0);
        Map<String, Person> persons = new LinkedHashMap<>();
        persons.put("W", worker);
        persons.put("S", spouse);
        List<FinancialEntry> entries = List.of(new FinancialEntry("W", ItemType.EXPENSE, "Living", 50000, 2026, 2045));
        YearlySummary[] s = project(entries, persons, rates(0, 0));

        // S is 62 in 2036 but W has not claimed, so nothing until W does in 2040.
        assertNull(year(s, 2037).individualSummaries().get("S"));
        assertEquals(4000 * 12 * 0.325, year(s, 2040).individualSummaries().get("S").socialSecurityBenefits(), TOL);
    }

    @Test
    public void handEnteredSocialSecurityRowsStillCount() {
        Person a = new Person("A", 1973);
        List<FinancialEntry> entries = List.of(
                new FinancialEntry("A", ItemType.SOCIAL_SECURITY_BENEFITS, "SSA", 30000, 2040, 2045),
                new FinancialEntry("A", ItemType.EXPENSE, "Living", 50000, 2026, 2045));
        YearlySummary[] s = project(entries, Map.of("A", a), rates(0, 0));
        assertEquals(30000, year(s, 2040).totalSocialSecurity(), TOL);
        assertEquals(0, year(s, 2039).totalSocialSecurity(), TOL);
    }

    @Test
    public void pre65CoverOnlyInYearsWithoutWagesAndStopsAtMedicare() {
        // Born 1973: 65 in 2038. Wages through 2031, so cover is charged 2032..2037.
        Person a = new Person("A", 1973, false, 0, 0, 1500, 300);
        List<FinancialEntry> entries = List.of(
                new FinancialEntry("A", ItemType.INCOME, "Salary", 200000, 2026, 2031),
                new FinancialEntry("A", ItemType.EXPENSE, "Living", 50000, 2026, 2045));
        YearlySummary[] s = project(entries, Map.of("A", a), rates(0, 5.0));

        assertEquals(0, year(s, 2031).healthPremiums(), TOL);
        assertEquals(1500 * 12 * Math.pow(1.05, 2032 - 2026), year(s, 2032).healthPremiums(), TOL);
        assertEquals(1500 * 12 * Math.pow(1.05, 2037 - 2026), year(s, 2037).healthPremiums(), TOL);

        // From 65: standard Part B grown from 2026 plus the plan premium grown from the plan start; no IRMAA at this income.
        double growth = Math.pow(1.05, 2038 - 2026);
        double expected = 202.90 * 12 * growth + 300 * 12 * growth;
        assertEquals(expected, year(s, 2038).healthPremiums(), TOL);
        assertEquals(0, year(s, 2038).irmaaSurcharge(), TOL);
        // Premiums are a cash outflow.
        assertTrue(year(s, 2038).totalCashOutflows() >= expected);
    }

    @Test
    public void irmaaComesFromTheMagiTwoYearsEarlier() {
        // Born 1961: 65 in 2026 would need history, so start the plan in 2024 with a spike in 2027 only.
        Person a = new Person("A", 1961, false, 0, 0, 0, 0);
        Person b = new Person("B", 1961, false, 0, 0, 0, 0);
        Map<String, Person> persons = new LinkedHashMap<>();
        persons.put("A", a);
        persons.put("B", b);
        List<FinancialEntry> entries = List.of(
                new FinancialEntry("A", ItemType.INCOME, "Low", 100000, 2024, 2032),
                new FinancialEntry("B", ItemType.INCOME, "Bonus", 250000, 2027, 2027),
                new FinancialEntry("A", ItemType.EXPENSE, "Living", 50000, 2024, 2032));
        YearlySummary[] s = project(entries, persons, rates(0, 0));

        // 2028 uses 2026 MAGI (100k): no surcharge for either person.
        assertEquals(0, year(s, 2028).irmaaSurcharge(), TOL);
        // 2029 uses 2027 MAGI (350k). The 2029 tier-3 floor is 342k indexed three years (~368k),
        // so this is tier 2 for a couple: both pay the tier-2 surcharge.
        double tier2 = (206.40 + 37.50) * 12;
        assertEquals(2 * tier2, year(s, 2029).irmaaSurcharge(), TOL);
        assertEquals(350000, year(s, 2029).lookbackMagi(), 1);
        // 2030 uses 2028 MAGI: back to none.
        assertEquals(0, year(s, 2030).irmaaSurcharge(), TOL);
        assertEquals(2 * MedicarePremiumCalculator.annualPartB(2030, 0), year(s, 2030).healthPremiums(), TOL);
    }
}
