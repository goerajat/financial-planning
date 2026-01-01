package rg.financialplanning.calculator;

import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;

public class MortgageCalculatorTest {

    private MortgageCalculator calculator;

    @Before
    public void setUp() {
        calculator = new MortgageCalculator();
    }

    // ===== calculateAnnualPayment tests =====

    @Test
    public void testCalculateAnnualPayment_standard30Year() {
        // $500,000 at 6.5% for 30 years
        double principal = 500000;
        double rate = 6.5;
        int term = 30;
        double payment = calculator.calculateAnnualPayment(principal, rate, term);
        // Verify payment is reasonable (between $35k and $45k annually)
        assertTrue(payment > 35000 && payment < 45000);
        assertEquals(38288.72, payment, 1.0);
    }

    @Test
    public void testCalculateAnnualPayment_15Year() {
        // $300,000 at 5.0% for 15 years
        double principal = 300000;
        double rate = 5.0;
        int term = 15;
        double payment = calculator.calculateAnnualPayment(principal, rate, term);
        // Verify payment is reasonable
        assertEquals(28902.69, payment, 1.0);
    }

    @Test
    public void testCalculateAnnualPayment_zeroInterest() {
        // $120,000 at 0% for 10 years
        double principal = 120000;
        double rate = 0;
        int term = 10;
        double payment = calculator.calculateAnnualPayment(principal, rate, term);
        assertEquals(12000, payment, 0.01);
    }

    @Test
    public void testCalculateAnnualPayment_zeroPrincipal() {
        double payment = calculator.calculateAnnualPayment(0, 6.5, 30);
        assertEquals(0, payment, 0.01);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCalculateAnnualPayment_negativePrincipal() {
        calculator.calculateAnnualPayment(-100000, 6.5, 30);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCalculateAnnualPayment_negativeRate() {
        calculator.calculateAnnualPayment(100000, -1, 30);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCalculateAnnualPayment_zeroTerm() {
        calculator.calculateAnnualPayment(100000, 6.5, 0);
    }

    // ===== calculateRemainingBalance tests =====

    @Test
    public void testCalculateRemainingBalance_yearZero() {
        double principal = 500000;
        double balance = calculator.calculateRemainingBalance(principal, 6.5, 30, 0);
        assertEquals(principal, balance, 0.01);
    }

    @Test
    public void testCalculateRemainingBalance_afterOneYear() {
        double principal = 500000;
        double rate = 6.5;
        int term = 30;
        double balance = calculator.calculateRemainingBalance(principal, rate, term, 1);
        // After 1 year, balance should be less than principal
        assertTrue(balance < principal);
        // Balance should be close to original (most of first year is interest)
        assertTrue(balance > principal * 0.98);
    }

    @Test
    public void testCalculateRemainingBalance_halfwayThrough() {
        double principal = 500000;
        double rate = 6.5;
        int term = 30;
        double balance = calculator.calculateRemainingBalance(principal, rate, term, 15);
        // After 15 years, balance should be reduced but more than half remains
        // (due to front-loaded interest in amortization)
        assertTrue(balance < principal);
        assertTrue(balance > 0);
    }

    @Test
    public void testCalculateRemainingBalance_atEnd() {
        double principal = 500000;
        double balance = calculator.calculateRemainingBalance(principal, 6.5, 30, 30);
        assertEquals(0, balance, 0.01);
    }

    @Test
    public void testCalculateRemainingBalance_beyondTerm() {
        double principal = 500000;
        double balance = calculator.calculateRemainingBalance(principal, 6.5, 30, 35);
        assertEquals(0, balance, 0.01);
    }

    @Test
    public void testCalculateRemainingBalance_zeroInterest() {
        double principal = 120000;
        double balance = calculator.calculateRemainingBalance(principal, 0, 10, 5);
        assertEquals(60000, balance, 0.01);
    }

    // ===== calculateInterestPortion tests =====

    @Test
    public void testCalculateInterestPortion_firstYear() {
        double balance = 500000;
        double rate = 6.5;
        double interest = calculator.calculateInterestPortion(balance, rate);
        assertEquals(32500, interest, 0.01);
    }

    @Test
    public void testCalculateInterestPortion_zeroBalance() {
        double interest = calculator.calculateInterestPortion(0, 6.5);
        assertEquals(0, interest, 0.01);
    }

    @Test
    public void testCalculateInterestPortion_zeroRate() {
        double interest = calculator.calculateInterestPortion(500000, 0);
        assertEquals(0, interest, 0.01);
    }

    // ===== calculatePrincipalPortion tests =====

    @Test
    public void testCalculatePrincipalPortion_firstYear() {
        double principal = 500000;
        double rate = 6.5;
        int term = 30;
        double payment = calculator.calculateAnnualPayment(principal, rate, term);
        double principalPortion = calculator.calculatePrincipalPortion(payment, principal, rate);
        // First year: payment - interest
        double expectedInterest = 32500; // 500000 * 0.065
        double expectedPrincipal = payment - expectedInterest;
        assertEquals(expectedPrincipal, principalPortion, 1.0);
    }

    // ===== calculateBalanceAfterPayment tests =====

    @Test
    public void testCalculateBalanceAfterPayment() {
        double principal = 500000;
        double rate = 6.5;
        int term = 30;
        double payment = calculator.calculateAnnualPayment(principal, rate, term);
        double newBalance = calculator.calculateBalanceAfterPayment(principal, payment, rate);
        // Should match remaining balance after 1 year
        double expectedBalance = calculator.calculateRemainingBalance(principal, rate, term, 1);
        assertEquals(expectedBalance, newBalance, 1.0);
    }

    @Test
    public void testCalculateBalanceAfterPayment_doesNotGoNegative() {
        double balance = 100;
        double payment = 200;
        double rate = 5.0;
        double newBalance = calculator.calculateBalanceAfterPayment(balance, payment, rate);
        assertTrue(newBalance >= 0);
    }

    // ===== Integration test =====

    @Test
    public void testFullAmortization() {
        double principal = 100000;
        double rate = 6.0;
        int term = 10;
        double payment = calculator.calculateAnnualPayment(principal, rate, term);

        double balance = principal;
        for (int year = 0; year < term; year++) {
            balance = calculator.calculateBalanceAfterPayment(balance, payment, rate);
        }

        // After all payments, balance should be zero (or very close due to floating point)
        assertEquals(0, balance, 0.01);
    }
}
