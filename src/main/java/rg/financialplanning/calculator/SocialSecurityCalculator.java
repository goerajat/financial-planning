package rg.financialplanning.calculator;

/**
 * Social Security retirement benefit from the primary insurance amount (PIA),
 * the benefit payable at full retirement age (FRA). Claiming ages are whole
 * years, 62 to 70. All figures are in the dollars the PIA was quoted in; COLA
 * is applied by the caller.
 */
public class SocialSecurityCalculator {

    public static final int EARLIEST_CLAIM_AGE = 62;
    public static final int LATEST_CLAIM_AGE = 70;

    /** Full retirement age in months for a birth year (SSA table). */
    public static int fullRetirementAgeMonths(int yearOfBirth) {
        if (yearOfBirth <= 1937) return 65 * 12;
        if (yearOfBirth <= 1942) return 65 * 12 + 2 * (yearOfBirth - 1937);
        if (yearOfBirth <= 1954) return 66 * 12;
        if (yearOfBirth <= 1959) return 66 * 12 + 2 * (yearOfBirth - 1954);
        return 67 * 12;
    }

    public static boolean isValidClaimAge(int claimAge) {
        return claimAge >= EARLIEST_CLAIM_AGE && claimAge <= LATEST_CLAIM_AGE;
    }

    /**
     * Factor applied to the PIA for a worker's own benefit: 5/9 of 1% per month
     * for the first 36 months before FRA, 5/12 of 1% per month beyond, and 2/3 of
     * 1% per month of delay after FRA up to 70.
     */
    public static double ownBenefitFactor(int yearOfBirth, int claimAge) {
        requireClaimAge(claimAge);
        int months = claimAge * 12 - fullRetirementAgeMonths(yearOfBirth);
        if (months < 0) {
            int early = -months;
            int first = Math.min(early, 36);
            int rest = early - first;
            return 1.0 - first * (5.0 / 9.0) / 100.0 - rest * (5.0 / 12.0) / 100.0;
        }
        return 1.0 + months * (2.0 / 3.0) / 100.0;
    }

    /**
     * Factor applied to the worker's PIA for a spouse's benefit: half of it,
     * reduced by 25/36 of 1% per month for the first 36 months before the
     * spouse's own FRA and 5/12 of 1% beyond. No credit for delay.
     */
    public static double spousalBenefitFactor(int spouseYearOfBirth, int spouseClaimAge) {
        requireClaimAge(spouseClaimAge);
        int months = spouseClaimAge * 12 - fullRetirementAgeMonths(spouseYearOfBirth);
        double factor = 0.5;
        if (months < 0) {
            int early = -months;
            int first = Math.min(early, 36);
            int rest = early - first;
            factor = 0.5 * (1.0 - first * (25.0 / 36.0) / 100.0 - rest * (5.0 / 12.0) / 100.0);
        }
        return factor;
    }

    /** Yearly own benefit for a PIA quoted per month. */
    public static double annualOwnBenefit(double piaMonthly, int yearOfBirth, int claimAge) {
        return piaMonthly * 12 * ownBenefitFactor(yearOfBirth, claimAge);
    }

    /** Yearly spousal benefit on the other spouse's PIA quoted per month. */
    public static double annualSpousalBenefit(double workerPiaMonthly, int spouseYearOfBirth, int spouseClaimAge) {
        return workerPiaMonthly * 12 * spousalBenefitFactor(spouseYearOfBirth, spouseClaimAge);
    }

    private static void requireClaimAge(int claimAge) {
        if (!isValidClaimAge(claimAge)) {
            throw new IllegalArgumentException("Claiming age must be between 62 and 70, was " + claimAge);
        }
    }
}
