package rg.financialplanning.parser;

import org.junit.Test;
import rg.financialplanning.model.FinancialEntry;
import rg.financialplanning.model.ItemType;
import rg.financialplanning.model.Person;
import rg.financialplanning.model.YearlySummary;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class MortgageAmortizationTest {

    private static final double TOL = 0.01;

    private static YearlySummary[] project(List<FinancialEntry> entries, double mortgageRate) {
        FinancialDataProcessor processor = new FinancialDataProcessor();
        processor.setEntries(entries);
        Map<ItemType, Double> rates = new HashMap<>();
        rates.put(ItemType.MORTGAGE, mortgageRate);
        rates.put(ItemType.REAL_ESTATE, 2.0);
        Map<String, Person> persons = new HashMap<>();
        persons.put("A", new Person("A", 1970));
        return processor.generateYearlySummaries(rates, persons);
    }

    @Test
    public void paymentComesFromTheEntryNotTheDescription() {
        FinancialEntry mortgage = new FinancialEntry("A", ItemType.MORTGAGE, "82 Elm St", 750000, 2026, 2056, 87600);
        FinancialEntry income = new FinancialEntry("A", ItemType.INCOME, "Salary", 300000, 2026, 2056);
        YearlySummary[] s = project(List.of(mortgage, income), 6.25);

        YearlySummary y0 = s[0];
        assertEquals(2026, y0.year());
        assertEquals(750000 * 0.0625, y0.mortgageInterest(), TOL);
        assertEquals(87600 - 750000 * 0.0625, y0.mortgagePrincipal(), TOL);
        assertEquals(87600, y0.mortgagePayment(), TOL);
        assertEquals(750000 - y0.mortgagePrincipal(), y0.mortgageBalance(), TOL);

        // The payment is a cash outflow: the surplus invested that year is smaller by exactly the payment.
        YearlySummary[] without = project(List.of(income), 6.25);
        assertEquals(87600, without[0].nonQualifiedAssets() - y0.nonQualifiedAssets(), TOL);
    }

    @Test
    public void balanceReachesZeroAndPaymentsStop() {
        FinancialEntry mortgage = new FinancialEntry("A", ItemType.MORTGAGE, "House", 750000, 2026, 2056, 87600);
        FinancialEntry income = new FinancialEntry("A", ItemType.INCOME, "Salary", 300000, 2026, 2056);
        YearlySummary[] s = project(List.of(mortgage, income), 6.25);

        int payoffYear = -1;
        double previousBalance = 750000;
        for (YearlySummary y : s) {
            assertTrue("balance never rises", y.mortgageBalance() <= previousBalance + TOL);
            assertEquals("payment is interest plus principal",
                    y.mortgagePayment(), y.mortgageInterest() + y.mortgagePrincipal(), TOL);
            if (payoffYear < 0 && y.mortgageBalance() == 0) payoffYear = y.year();
            previousBalance = y.mortgageBalance();
        }
        // 750k at 6.25% with 87.6k a year amortises in about 13 years.
        assertEquals(2038, payoffYear);

        YearlySummary payoff = s[payoffYear - 2026];
        assertTrue("final payment is smaller than the scheduled one", payoff.mortgagePayment() < 87600);
        YearlySummary after = s[payoffYear - 2026 + 1];
        assertEquals(0, after.mortgagePayment(), 0);
        assertEquals(0, after.mortgageInterest(), 0);
        assertEquals(0, after.mortgageBalance(), 0);
    }

    @Test
    public void extraPrincipalIsCappedAtWhatIsOwed() {
        FinancialEntry mortgage = new FinancialEntry("A", ItemType.MORTGAGE, "House", 100000, 2026, 2030, 30000);
        FinancialEntry extra = new FinancialEntry("A", ItemType.MORTGAGE_REPAYMENT, "Extra", 50000, 2026, 2030);
        FinancialEntry income = new FinancialEntry("A", ItemType.INCOME, "Salary", 300000, 2026, 2030);
        YearlySummary[] s = project(List.of(mortgage, extra, income), 5.0);

        YearlySummary y0 = s[0];
        assertEquals(5000, y0.mortgageInterest(), TOL);
        assertEquals(50000, y0.mortgageRepayment(), TOL);
        assertEquals(25000, y0.mortgagePrincipal(), TOL);
        assertEquals(25000, y0.mortgageBalance(), TOL);

        YearlySummary y1 = s[1];
        assertEquals(1250, y1.mortgageInterest(), TOL);
        assertEquals("only the remaining balance is prepaid", 25000, y1.mortgageRepayment(), TOL);
        assertEquals(0, y1.mortgagePrincipal(), TOL);
        assertEquals(1250, y1.mortgagePayment(), TOL);
        assertEquals(0, y1.mortgageBalance(), 0);

        YearlySummary y2 = s[2];
        assertEquals(0, y2.mortgageRepayment(), 0);
        assertEquals(0, y2.mortgagePayment(), 0);
    }

    @Test
    public void noPaymentMeansNegativeAmortisation() {
        FinancialEntry mortgage = new FinancialEntry("A", ItemType.MORTGAGE, "House", 100000, 2026, 2028);
        YearlySummary[] s = project(List.of(mortgage), 5.0);
        assertEquals(0, s[0].mortgagePayment(), 0);
        assertEquals(105000, s[0].mortgageBalance(), TOL);
    }
}
