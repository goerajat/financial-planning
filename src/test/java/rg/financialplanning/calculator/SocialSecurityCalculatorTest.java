package rg.financialplanning.calculator;

import org.junit.Test;

import static org.junit.Assert.*;

public class SocialSecurityCalculatorTest {

    private static final double TOL = 1e-9;

    @Test
    public void fullRetirementAgeFollowsTheSsaTable() {
        assertEquals(65 * 12, SocialSecurityCalculator.fullRetirementAgeMonths(1937));
        assertEquals(65 * 12 + 6, SocialSecurityCalculator.fullRetirementAgeMonths(1940));
        assertEquals(66 * 12, SocialSecurityCalculator.fullRetirementAgeMonths(1950));
        assertEquals(66 * 12 + 8, SocialSecurityCalculator.fullRetirementAgeMonths(1958));
        assertEquals(67 * 12, SocialSecurityCalculator.fullRetirementAgeMonths(1960));
        assertEquals(67 * 12, SocialSecurityCalculator.fullRetirementAgeMonths(1973));
    }

    @Test
    public void ownBenefitFactorsForFra67() {
        assertEquals(0.70, SocialSecurityCalculator.ownBenefitFactor(1973, 62), TOL);
        assertEquals(0.75, SocialSecurityCalculator.ownBenefitFactor(1973, 63), TOL);
        assertEquals(0.80, SocialSecurityCalculator.ownBenefitFactor(1973, 64), TOL);
        assertEquals(0.8666666667, SocialSecurityCalculator.ownBenefitFactor(1973, 65), 1e-9);
        assertEquals(1.00, SocialSecurityCalculator.ownBenefitFactor(1973, 67), TOL);
        assertEquals(1.08, SocialSecurityCalculator.ownBenefitFactor(1973, 68), TOL);
        assertEquals(1.24, SocialSecurityCalculator.ownBenefitFactor(1973, 70), TOL);
    }

    @Test
    public void ownBenefitFactorsForFra66() {
        assertEquals(0.75, SocialSecurityCalculator.ownBenefitFactor(1950, 62), TOL);
        assertEquals(1.00, SocialSecurityCalculator.ownBenefitFactor(1950, 66), TOL);
        assertEquals(1.32, SocialSecurityCalculator.ownBenefitFactor(1950, 70), TOL);
    }

    @Test
    public void spousalFactorIsHalfReducedForEarlyClaimAndNeverCredited() {
        assertEquals(0.50, SocialSecurityCalculator.spousalBenefitFactor(1973, 67), TOL);
        assertEquals(0.50, SocialSecurityCalculator.spousalBenefitFactor(1973, 70), TOL);
        assertEquals(0.325, SocialSecurityCalculator.spousalBenefitFactor(1973, 62), TOL);
        assertEquals(0.375, SocialSecurityCalculator.spousalBenefitFactor(1973, 64), TOL);
    }

    @Test
    public void annualAmountsAreTwelveMonthsTimesFactor() {
        assertEquals(3000 * 12 * 1.24, SocialSecurityCalculator.annualOwnBenefit(3000, 1973, 70), TOL);
        assertEquals(3000 * 12 * 0.5, SocialSecurityCalculator.annualSpousalBenefit(3000, 1974, 67), TOL);
    }

    @Test(expected = IllegalArgumentException.class)
    public void claimingBefore62IsRejected() {
        SocialSecurityCalculator.ownBenefitFactor(1973, 61);
    }

    @Test(expected = IllegalArgumentException.class)
    public void claimingAfter70IsRejected() {
        SocialSecurityCalculator.ownBenefitFactor(1973, 71);
    }
}
