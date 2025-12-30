package rg.financialplanning.model;

import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;

public class IndividualYearlySummaryTest {

    private Person person;
    private IndividualYearlySummary summary;

    @Before
    public void setUp() {
        person = new Person("John Doe", 1960);
        summary = new IndividualYearlySummary(person, 2025, 100000, 500000, 200000, 150000, 30000);
    }

    @Test
    public void testConstructor_initializesFieldsCorrectly() {
        assertEquals(person, summary.person());
        assertEquals("John Doe", summary.name());
        assertEquals(2025, summary.year());
        assertEquals(100000, summary.income(), 0.001);
        assertEquals(500000, summary.qualifiedAssets(), 0.001);
        assertEquals(200000, summary.nonQualifiedAssets(), 0.001);
        assertEquals(150000, summary.rothAssets(), 0.001);
        assertEquals(30000, summary.socialSecurityBenefits(), 0.001);
    }

    @Test
    public void testConstructor_initializesWithdrawalsToZero() {
        assertEquals(0.0, summary.qualifiedWithdrawals(), 0.001);
        assertEquals(0.0, summary.nonQualifiedWithdrawals(), 0.001);
        assertEquals(0.0, summary.rothWithdrawals(), 0.001);
        assertEquals(0.0, summary.rmdWithdrawals(), 0.001);
        assertEquals(0.0, summary.cashWithdrawals(), 0.001);
    }

    @Test
    public void testConstructor_initializesContributionsToZero() {
        assertEquals(0.0, summary.rothContributions(), 0.001);
        assertEquals(0.0, summary.qualifiedContributions(), 0.001);
        assertEquals(0.0, summary.nonQualifiedContributions(), 0.001);
    }

    @Test
    public void testConstructor_initializesOtherFieldsToZero() {
        assertEquals(0.0, summary.socialSecurityTax(), 0.001);
        assertEquals(0.0, summary.deficit(), 0.001);
    }

    @Test
    public void testConstructor_nullPerson() {
        IndividualYearlySummary nullPersonSummary = new IndividualYearlySummary(null, 2025, 100000, 500000, 200000, 150000, 30000);
        assertNull(nullPersonSummary.person());
        assertNull(nullPersonSummary.name());
    }

    @Test
    public void testSetQualifiedWithdrawals() {
        summary.setQualifiedWithdrawals(25000);
        assertEquals(25000, summary.qualifiedWithdrawals(), 0.001);
    }

    @Test
    public void testSetNonQualifiedWithdrawals() {
        summary.setNonQualifiedWithdrawals(15000);
        assertEquals(15000, summary.nonQualifiedWithdrawals(), 0.001);
    }

    @Test
    public void testSetRothWithdrawals() {
        summary.setRothWithdrawals(10000);
        assertEquals(10000, summary.rothWithdrawals(), 0.001);
    }

    @Test
    public void testSetRmdWithdrawals_reducesQualifiedAssets() {
        double originalQualified = summary.qualifiedAssets();
        summary.setRmdWithdrawals(20000);
        assertEquals(20000, summary.rmdWithdrawals(), 0.001);
        assertEquals(originalQualified - 20000, summary.qualifiedAssets(), 0.001);
    }

    @Test
    public void testSetRmdWithdrawals_cappedAtAvailableQualifiedAssets() {
        double originalQualified = summary.qualifiedAssets();
        summary.setRmdWithdrawals(originalQualified + 100000);
        assertEquals(originalQualified, summary.rmdWithdrawals(), 0.001);
        assertEquals(0.0, summary.qualifiedAssets(), 0.001);
    }

    @Test
    public void testSetQualifiedAssets() {
        summary.setQualifiedAssets(600000);
        assertEquals(600000, summary.qualifiedAssets(), 0.001);
    }

    @Test
    public void testSetNonQualifiedAssets() {
        summary.setNonQualifiedAssets(250000);
        assertEquals(250000, summary.nonQualifiedAssets(), 0.001);
    }

    @Test
    public void testSetRothAssets() {
        summary.setRothAssets(200000);
        assertEquals(200000, summary.rothAssets(), 0.001);
    }

    @Test
    public void testSetSocialSecurityTax() {
        summary.setSocialSecurityTax(5000);
        assertEquals(5000, summary.socialSecurityTax(), 0.001);
    }

    @Test
    public void testSetRothContributions() {
        summary.setRothContributions(7000);
        assertEquals(7000, summary.rothContributions(), 0.001);
    }

    @Test
    public void testSetQualifiedContributions() {
        summary.setQualifiedContributions(22500);
        assertEquals(22500, summary.qualifiedContributions(), 0.001);
    }

    @Test
    public void testSetNonQualifiedContributions() {
        summary.setNonQualifiedContributions(50000);
        assertEquals(50000, summary.nonQualifiedContributions(), 0.001);
    }

    @Test
    public void testSetCashWithdrawals() {
        summary.setCashWithdrawals(10000);
        assertEquals(10000, summary.cashWithdrawals(), 0.001);
    }

    @Test
    public void testSetDeficit() {
        summary.setDeficit(5000);
        assertEquals(5000, summary.deficit(), 0.001);
    }

    @Test
    public void testTotalWithdrawals() {
        summary.setQualifiedWithdrawals(10000);
        summary.setNonQualifiedWithdrawals(20000);
        summary.setRothWithdrawals(5000);
        assertEquals(35000, summary.totalWithdrawals(), 0.001);
    }

    @Test
    public void testTotalAssets() {
        assertEquals(500000 + 200000 + 150000, summary.totalAssets(), 0.001);
    }

    @Test
    public void testTotalIncome() {
        assertEquals(100000 + 30000, summary.totalIncome(), 0.001);
    }

    @Test
    public void testCalculateRmdWithdrawals_underAge() {
        Person youngPerson = new Person("Young", 1980);
        IndividualYearlySummary youngSummary = new IndividualYearlySummary(youngPerson, 2025, 100000, 500000, 200000, 150000, 0);
        double rmd = youngSummary.calculateRmdWithdrawals(45, 1980, 500000);
        assertEquals(0.0, rmd, 0.001);
    }

    @Test
    public void testCalculateRmdWithdrawals_atRmdAge() {
        // Person born in 1952 (RMD starts at 73), in year 2025 they are 73
        Person rmdAgePerson = new Person("RmdAge", 1952);
        IndividualYearlySummary rmdSummary = new IndividualYearlySummary(rmdAgePerson, 2025, 0, 500000, 0, 0, 30000);
        double rmd = rmdSummary.calculateRmdWithdrawals(73, 1952, 500000);
        assertTrue(rmd > 0);
        // RMD at 73 with life expectancy factor of 26.5: 500000 / 26.5 = 18867.92
        assertEquals(500000 / 26.5, rmd, 0.01);
    }

    @Test
    public void testEquals_sameValues() {
        IndividualYearlySummary summary2 = new IndividualYearlySummary(person, 2025, 100000, 500000, 200000, 150000, 30000);
        assertEquals(summary, summary2);
    }

    @Test
    public void testEquals_differentIncome() {
        IndividualYearlySummary summary2 = new IndividualYearlySummary(person, 2025, 120000, 500000, 200000, 150000, 30000);
        assertNotEquals(summary, summary2);
    }

    @Test
    public void testHashCode_sameForEqualObjects() {
        IndividualYearlySummary summary2 = new IndividualYearlySummary(person, 2025, 100000, 500000, 200000, 150000, 30000);
        assertEquals(summary.hashCode(), summary2.hashCode());
    }

    @Test
    public void testToString_containsKeyInfo() {
        String result = summary.toString();
        assertTrue(result.contains("John Doe"));
        assertTrue(result.contains("2025"));
        assertTrue(result.contains("100000"));
    }
}
