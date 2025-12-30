package rg.financialplanning.model;

import org.junit.Test;
import org.junit.Before;
import java.util.HashMap;
import java.util.Map;
import static org.junit.Assert.*;

public class YearlySummaryTest {

    private YearlySummary summary;
    private Map<String, IndividualYearlySummary> individualSummaries;
    private Person person1;
    private Person person2;

    @Before
    public void setUp() {
        person1 = new Person("John", 1960);
        person2 = new Person("Jane", 1965);

        IndividualYearlySummary ind1 = new IndividualYearlySummary(person1, 2025, 100000, 500000, 200000, 100000, 30000);
        IndividualYearlySummary ind2 = new IndividualYearlySummary(person2, 2025, 80000, 400000, 150000, 80000, 25000);

        individualSummaries = new HashMap<>();
        individualSummaries.put("John", ind1);
        individualSummaries.put("Jane", ind2);

        summary = new YearlySummary(2025, 180000, 120000, 900000, 350000, 180000, 50000, 500000, 200000, 55000, individualSummaries);
    }

    @Test
    public void testConstructor_initializesFieldsCorrectly() {
        assertEquals(2025, summary.year());
        assertEquals(180000, summary.totalIncome(), 0.001);
        assertEquals(120000, summary.totalExpenses(), 0.001);
        assertEquals(900000, summary.qualifiedAssets(), 0.001);
        assertEquals(350000, summary.nonQualifiedAssets(), 0.001);
        assertEquals(180000, summary.rothAssets(), 0.001);
        assertEquals(50000, summary.cash(), 0.001);
        assertEquals(500000, summary.realEstate(), 0.001);
        assertEquals(200000, summary.lifeInsuranceBenefits(), 0.001);
        assertEquals(55000, summary.totalSocialSecurity(), 0.001);
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
    public void testConstructor_initializesDeficitToZero() {
        assertEquals(0.0, summary.deficit(), 0.001);
    }

    @Test
    public void testConstructor_initializesTaxesToZero() {
        assertEquals(0.0, summary.federalIncomeTax(), 0.001);
        assertEquals(0.0, summary.stateIncomeTax(), 0.001);
        assertEquals(0.0, summary.capitalGainsTax(), 0.001);
        assertEquals(0.0, summary.socialSecurityTax(), 0.001);
        assertEquals(0.0, summary.medicareTax(), 0.001);
    }

    @Test
    public void testConstructor_nullIndividualSummaries() {
        YearlySummary nullSummary = new YearlySummary(2025, 100000, 50000, 500000, 200000, 100000, 50000, 300000, 100000, 30000, null);
        assertNotNull(nullSummary.individualSummaries());
        assertTrue(nullSummary.individualSummaries().isEmpty());
    }

    @Test
    public void testIndividualSummaries_areImmutable() {
        Map<String, IndividualYearlySummary> summaries = summary.individualSummaries();
        assertNotNull(summaries);
        assertEquals(2, summaries.size());
    }

    @Test
    public void testGetIndividualSummary() {
        IndividualYearlySummary johnSummary = summary.getIndividualSummary("John");
        assertNotNull(johnSummary);
        assertEquals("John", johnSummary.name());
    }

    @Test
    public void testGetIndividualSummary_notFound() {
        IndividualYearlySummary unknownSummary = summary.getIndividualSummary("Unknown");
        assertNull(unknownSummary);
    }

    @Test
    public void testGetIncomeForName() {
        assertEquals(100000, summary.getIncomeForName("John"), 0.001);
        assertEquals(80000, summary.getIncomeForName("Jane"), 0.001);
        assertEquals(0.0, summary.getIncomeForName("Unknown"), 0.001);
    }

    @Test
    public void testGetQualifiedForName() {
        assertEquals(500000, summary.getQualifiedForName("John"), 0.001);
        assertEquals(400000, summary.getQualifiedForName("Jane"), 0.001);
        assertEquals(0.0, summary.getQualifiedForName("Unknown"), 0.001);
    }

    @Test
    public void testGetNonQualifiedForName() {
        assertEquals(200000, summary.getNonQualifiedForName("John"), 0.001);
        assertEquals(150000, summary.getNonQualifiedForName("Jane"), 0.001);
    }

    @Test
    public void testGetRothForName() {
        assertEquals(100000, summary.getRothForName("John"), 0.001);
        assertEquals(80000, summary.getRothForName("Jane"), 0.001);
    }

    @Test
    public void testGetSocialSecurityForName() {
        assertEquals(30000, summary.getSocialSecurityForName("John"), 0.001);
        assertEquals(25000, summary.getSocialSecurityForName("Jane"), 0.001);
    }

    @Test
    public void testSetQualifiedWithdrawals() {
        summary.setQualifiedWithdrawals(50000);
        assertEquals(50000, summary.qualifiedWithdrawals(), 0.001);
    }

    @Test
    public void testSetNonQualifiedWithdrawals() {
        summary.setNonQualifiedWithdrawals(30000);
        assertEquals(30000, summary.nonQualifiedWithdrawals(), 0.001);
    }

    @Test
    public void testSetRothWithdrawals() {
        summary.setRothWithdrawals(20000);
        assertEquals(20000, summary.rothWithdrawals(), 0.001);
    }

    @Test
    public void testSetRmdWithdrawals_reducesQualifiedAssets() {
        double originalQualified = summary.qualifiedAssets();
        summary.setRmdWithdrawals(30000);
        assertEquals(30000, summary.rmdWithdrawals(), 0.001);
        assertEquals(originalQualified - 30000, summary.qualifiedAssets(), 0.001);
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
        summary.setQualifiedAssets(1000000);
        assertEquals(1000000, summary.qualifiedAssets(), 0.001);
    }

    @Test
    public void testSetNonQualifiedAssets() {
        summary.setNonQualifiedAssets(400000);
        assertEquals(400000, summary.nonQualifiedAssets(), 0.001);
    }

    @Test
    public void testSetRothAssets() {
        summary.setRothAssets(250000);
        assertEquals(250000, summary.rothAssets(), 0.001);
    }

    @Test
    public void testSetCash() {
        summary.setCash(75000);
        assertEquals(75000, summary.cash(), 0.001);
    }

    @Test
    public void testSetCashWithdrawals() {
        summary.setCashWithdrawals(10000);
        assertEquals(10000, summary.cashWithdrawals(), 0.001);
    }

    @Test
    public void testSetRothContributions() {
        summary.setRothContributions(14000);
        assertEquals(14000, summary.rothContributions(), 0.001);
    }

    @Test
    public void testSetQualifiedContributions() {
        summary.setQualifiedContributions(45000);
        assertEquals(45000, summary.qualifiedContributions(), 0.001);
    }

    @Test
    public void testSetNonQualifiedContributions() {
        summary.setNonQualifiedContributions(100000);
        assertEquals(100000, summary.nonQualifiedContributions(), 0.001);
    }

    @Test
    public void testSetDeficit() {
        summary.setDeficit(25000);
        assertEquals(25000, summary.deficit(), 0.001);
    }

    @Test
    public void testSetFederalIncomeTax() {
        summary.setFederalIncomeTax(25000);
        assertEquals(25000, summary.federalIncomeTax(), 0.001);
    }

    @Test
    public void testSetStateIncomeTax() {
        summary.setStateIncomeTax(8000);
        assertEquals(8000, summary.stateIncomeTax(), 0.001);
    }

    @Test
    public void testSetCapitalGainsTax() {
        summary.setCapitalGainsTax(5000);
        assertEquals(5000, summary.capitalGainsTax(), 0.001);
    }

    @Test
    public void testSetSocialSecurityTax() {
        summary.setSocialSecurityTax(9000);
        assertEquals(9000, summary.socialSecurityTax(), 0.001);
    }

    @Test
    public void testSetMedicareTax() {
        summary.setMedicareTax(2500);
        assertEquals(2500, summary.medicareTax(), 0.001);
    }

    @Test
    public void testTotalTaxes() {
        summary.setFederalIncomeTax(25000);
        summary.setStateIncomeTax(8000);
        summary.setCapitalGainsTax(5000);
        summary.setSocialSecurityTax(9000);
        summary.setMedicareTax(2500);
        assertEquals(49500, summary.totalTaxes(), 0.001);
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
        // qualifiedAssets + nonQualifiedAssets + rothAssets + cash + realEstate + lifeInsuranceBenefits
        double expected = 900000 + 350000 + 180000 + 50000 + 500000 + 200000;
        assertEquals(expected, summary.totalAssets(), 0.001);
    }

    @Test
    public void testNetIncome() {
        // totalIncome - totalExpenses
        assertEquals(180000 - 120000, summary.netIncome(), 0.001);
    }

    @Test
    public void testNetWorth() {
        // totalAssets() + netIncome()
        double expectedAssets = 900000 + 350000 + 180000 + 50000 + 500000 + 200000;
        double expectedNetIncome = 180000 - 120000;
        assertEquals(expectedAssets + expectedNetIncome, summary.netWorth(), 0.001);
    }

    @Test
    public void testEquals_sameValues() {
        YearlySummary summary2 = new YearlySummary(2025, 180000, 120000, 900000, 350000, 180000, 50000, 500000, 200000, 55000, individualSummaries);
        assertEquals(summary, summary2);
    }

    @Test
    public void testEquals_differentYear() {
        YearlySummary summary2 = new YearlySummary(2026, 180000, 120000, 900000, 350000, 180000, 50000, 500000, 200000, 55000, individualSummaries);
        assertNotEquals(summary, summary2);
    }

    @Test
    public void testEquals_differentIncome() {
        YearlySummary summary2 = new YearlySummary(2025, 200000, 120000, 900000, 350000, 180000, 50000, 500000, 200000, 55000, individualSummaries);
        assertNotEquals(summary, summary2);
    }

    @Test
    public void testHashCode_sameForEqualObjects() {
        YearlySummary summary2 = new YearlySummary(2025, 180000, 120000, 900000, 350000, 180000, 50000, 500000, 200000, 55000, individualSummaries);
        assertEquals(summary.hashCode(), summary2.hashCode());
    }

    @Test
    public void testToString_containsKeyInfo() {
        String result = summary.toString();
        assertTrue(result.contains("2025"));
        assertTrue(result.contains("180000"));
    }
}
