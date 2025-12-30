package rg.financialplanning.calculator;

import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;

public class RMDCalculatorTest {

    private RMDCalculator calculator;

    @Before
    public void setUp() {
        calculator = new RMDCalculator();
    }

    // ===== calculateRMD tests =====

    @Test
    public void testCalculateRMD_age73() {
        // At age 73, life expectancy factor is 26.5
        double balance = 500000;
        double expected = balance / 26.5;
        assertEquals(expected, calculator.calculateRMD(73, balance), 0.01);
    }

    @Test
    public void testCalculateRMD_age75() {
        // At age 75, life expectancy factor is 24.6
        double balance = 1000000;
        double expected = balance / 24.6;
        assertEquals(expected, calculator.calculateRMD(75, balance), 0.01);
    }

    @Test
    public void testCalculateRMD_age80() {
        // At age 80, life expectancy factor is 20.2
        double balance = 750000;
        double expected = balance / 20.2;
        assertEquals(expected, calculator.calculateRMD(80, balance), 0.01);
    }

    @Test
    public void testCalculateRMD_age90() {
        // At age 90, life expectancy factor is 12.2
        double balance = 300000;
        double expected = balance / 12.2;
        assertEquals(expected, calculator.calculateRMD(90, balance), 0.01);
    }

    @Test
    public void testCalculateRMD_age100() {
        // At age 100, life expectancy factor is 6.4
        double balance = 100000;
        double expected = balance / 6.4;
        assertEquals(expected, calculator.calculateRMD(100, balance), 0.01);
    }

    @Test
    public void testCalculateRMD_age120Plus() {
        // At age 120+, life expectancy factor is 2.0
        double balance = 50000;
        double expected = balance / 2.0;
        assertEquals(expected, calculator.calculateRMD(125, balance), 0.01);
    }

    @Test
    public void testCalculateRMD_zeroBalance() {
        assertEquals(0, calculator.calculateRMD(75, 0), 0.001);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCalculateRMD_underAge72ThrowsException() {
        calculator.calculateRMD(71, 500000);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCalculateRMD_negativeBalanceThrowsException() {
        calculator.calculateRMD(75, -100000);
    }

    // ===== getLifeExpectancyFactor tests =====

    @Test
    public void testGetLifeExpectancyFactor_age72() {
        assertEquals(27.4, calculator.getLifeExpectancyFactor(72), 0.01);
    }

    @Test
    public void testGetLifeExpectancyFactor_age73() {
        assertEquals(26.5, calculator.getLifeExpectancyFactor(73), 0.01);
    }

    @Test
    public void testGetLifeExpectancyFactor_age75() {
        assertEquals(24.6, calculator.getLifeExpectancyFactor(75), 0.01);
    }

    @Test
    public void testGetLifeExpectancyFactor_age120() {
        assertEquals(2.0, calculator.getLifeExpectancyFactor(120), 0.01);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetLifeExpectancyFactor_underAge72ThrowsException() {
        calculator.getLifeExpectancyFactor(71);
    }

    // ===== getRMDStartAge tests =====

    @Test
    public void testGetRMDStartAge_born1950OrEarlier() {
        assertEquals(72, calculator.getRMDStartAge(1950));
        assertEquals(72, calculator.getRMDStartAge(1945));
        assertEquals(72, calculator.getRMDStartAge(1940));
    }

    @Test
    public void testGetRMDStartAge_born1951To1959() {
        assertEquals(73, calculator.getRMDStartAge(1951));
        assertEquals(73, calculator.getRMDStartAge(1955));
        assertEquals(73, calculator.getRMDStartAge(1959));
    }

    @Test
    public void testGetRMDStartAge_born1960OrLater() {
        assertEquals(75, calculator.getRMDStartAge(1960));
        assertEquals(75, calculator.getRMDStartAge(1970));
        assertEquals(75, calculator.getRMDStartAge(1980));
    }

    // ===== isRMDRequired tests =====

    @Test
    public void testIsRMDRequired_born1950Age72() {
        assertTrue(calculator.isRMDRequired(72, 1950));
    }

    @Test
    public void testIsRMDRequired_born1950Age71() {
        assertFalse(calculator.isRMDRequired(71, 1950));
    }

    @Test
    public void testIsRMDRequired_born1955Age73() {
        assertTrue(calculator.isRMDRequired(73, 1955));
    }

    @Test
    public void testIsRMDRequired_born1955Age72() {
        assertFalse(calculator.isRMDRequired(72, 1955));
    }

    @Test
    public void testIsRMDRequired_born1965Age75() {
        assertTrue(calculator.isRMDRequired(75, 1965));
    }

    @Test
    public void testIsRMDRequired_born1965Age74() {
        assertFalse(calculator.isRMDRequired(74, 1965));
    }

    @Test
    public void testIsRMDRequired_born1965Age80() {
        assertTrue(calculator.isRMDRequired(80, 1965));
    }

    // ===== getRMDPercentage tests =====

    @Test
    public void testGetRMDPercentage_underAge72() {
        assertEquals(0, calculator.getRMDPercentage(70), 0.001);
    }

    @Test
    public void testGetRMDPercentage_age73() {
        // Life expectancy factor at 73 is 26.5
        // Percentage = 1/26.5 * 100 = 3.77%
        double expected = (1.0 / 26.5) * 100;
        assertEquals(expected, calculator.getRMDPercentage(73), 0.01);
    }

    @Test
    public void testGetRMDPercentage_age80() {
        // Life expectancy factor at 80 is 20.2
        double expected = (1.0 / 20.2) * 100;
        assertEquals(expected, calculator.getRMDPercentage(80), 0.01);
    }

    @Test
    public void testGetRMDPercentage_increasesWithAge() {
        double percentage75 = calculator.getRMDPercentage(75);
        double percentage80 = calculator.getRMDPercentage(80);
        double percentage90 = calculator.getRMDPercentage(90);

        assertTrue(percentage80 > percentage75);
        assertTrue(percentage90 > percentage80);
    }
}
