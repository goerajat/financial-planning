package rg.financialplanning.calculator;

import org.junit.Test;
import rg.financialplanning.model.FilingStatus;

import static org.junit.Assert.*;

public class MedicarePremiumCalculatorTest {

    private static final FilingStatus MFJ = FilingStatus.MARRIED_FILING_JOINTLY;
    private static final double TOL = 0.01;

    @Test
    public void tiersInTheBaseYear() {
        assertEquals(0, MedicarePremiumCalculator.tier(218_000, 2026, MFJ));
        assertEquals(1, MedicarePremiumCalculator.tier(218_001, 2026, MFJ));
        assertEquals(2, MedicarePremiumCalculator.tier(300_000, 2026, MFJ));
        assertEquals(4, MedicarePremiumCalculator.tier(500_000, 2026, MFJ));
        assertEquals(5, MedicarePremiumCalculator.tier(800_000, 2026, MFJ));
        assertEquals(1, MedicarePremiumCalculator.tier(120_000, 2026, FilingStatus.SINGLE));
        assertEquals(0, MedicarePremiumCalculator.tier(120_000, 2026, MFJ));
    }

    @Test
    public void thresholdsAreIndexedAtCpi() {
        double[] t2026 = MedicarePremiumCalculator.thresholdsFor(2026, MFJ);
        double[] t2036 = MedicarePremiumCalculator.thresholdsFor(2036, MFJ);
        assertEquals(218_000, t2026[0], TOL);
        assertEquals(218_000 * Math.pow(1.025, 10), t2036[0], TOL);
        // Just over the 2026 floor is under the 2036 floor.
        assertEquals(0, MedicarePremiumCalculator.tier(220_000, 2036, MFJ));
    }

    @Test
    public void surchargeIsPartBPlusPartDGrownAtMedicalInflation() {
        assertEquals(0, MedicarePremiumCalculator.annualSurcharge(200_000, 2026, MFJ, 5.5), TOL);
        assertEquals((82.60 + 14.50) * 12, MedicarePremiumCalculator.annualSurcharge(250_000, 2026, MFJ, 5.5), TOL);
        assertEquals((495.40 + 91.00) * 12, MedicarePremiumCalculator.annualSurcharge(900_000, 2026, MFJ, 5.5), TOL);
        assertEquals((82.60 + 14.50) * 12 * 1.055, MedicarePremiumCalculator.annualSurcharge(230_000, 2027, MFJ, 5.5), TOL);
    }

    @Test
    public void partBStandardGrows() {
        assertEquals(202.90 * 12, MedicarePremiumCalculator.annualPartB(2026, 5.5), TOL);
        assertEquals(202.90 * 12 * 1.055 * 1.055, MedicarePremiumCalculator.annualPartB(2028, 5.5), TOL);
    }

    @Test
    public void nextThresholdAboveGivesTheRoomBeforeTheNextTier() {
        assertEquals(218_000, MedicarePremiumCalculator.nextThresholdAbove(150_000, 2026, MFJ), TOL);
        assertEquals(274_000, MedicarePremiumCalculator.nextThresholdAbove(218_001, 2026, MFJ), TOL);
        assertTrue(Double.isInfinite(MedicarePremiumCalculator.nextThresholdAbove(800_000, 2026, MFJ)));
    }
}
