package rg.financialplanning.calculator;

import rg.financialplanning.model.FilingStatus;

/**
 * Medicare Part B and Part D premiums with the income-related monthly
 * adjustment (IRMAA). Figures are the CMS 2026 schedule; premiums grow at a
 * medical inflation rate the caller supplies, and the IRMAA thresholds are
 * indexed at {@link #THRESHOLD_INDEX_RATE} as the law ties them to CPI-U.
 * IRMAA for a year is set from MAGI two years earlier; the caller supplies
 * that MAGI.
 */
public class MedicarePremiumCalculator {

    public static final int BASE_YEAR = 2026;
    public static final int ELIGIBILITY_AGE = 65;
    /** Age from which this year's MAGI sets a premium after Medicare starts. */
    public static final int LOOKBACK_YEARS = 2;
    /** CPI-U assumption for the threshold index, a statutory rule not a plan rate. */
    public static final double THRESHOLD_INDEX_RATE = 2.5;

    /** Standard Part B premium per month, 2026. */
    public static final double PART_B_STANDARD_MONTHLY = 202.90;

    /** MAGI tier floors, married filing jointly, 2026. A MAGI at or below a floor is the tier below. */
    private static final double[] MFJ_THRESHOLDS = {218_000, 274_000, 342_000, 410_000, 750_000};
    /** MAGI tier floors, single, 2026. */
    private static final double[] SINGLE_THRESHOLDS = {109_000, 137_000, 171_000, 205_000, 500_000};
    /** Part B monthly surcharge per tier above the standard premium, 2026. */
    private static final double[] PART_B_SURCHARGE = {82.60, 206.40, 330.30, 454.10, 495.40};
    /** Part D monthly surcharge per tier, 2026. */
    private static final double[] PART_D_SURCHARGE = {14.50, 37.50, 60.40, 83.30, 91.00};

    /** Thresholds for a year, indexed from the base year. */
    public static double[] thresholdsFor(int year, FilingStatus filingStatus) {
        double[] base = isJoint(filingStatus) ? MFJ_THRESHOLDS : SINGLE_THRESHOLDS;
        double index = Math.pow(1 + THRESHOLD_INDEX_RATE / 100.0, year - BASE_YEAR);
        double[] out = new double[base.length];
        for (int i = 0; i < base.length; i++) out[i] = base[i] * index;
        return out;
    }

    /** IRMAA tier for a MAGI: 0 is no surcharge, 5 the top tier. */
    public static int tier(double magi, int year, FilingStatus filingStatus) {
        double[] thresholds = thresholdsFor(year, filingStatus);
        int tier = 0;
        for (double t : thresholds) {
            if (magi > t) tier++;
        }
        return tier;
    }

    /** The lowest MAGI that would move a year's premium into the next tier, or +infinity at the top. */
    public static double nextThresholdAbove(double magi, int year, FilingStatus filingStatus) {
        for (double t : thresholdsFor(year, filingStatus)) {
            if (magi <= t) return t;
        }
        return Double.POSITIVE_INFINITY;
    }

    /** Yearly Part B and Part D IRMAA surcharge for one person, grown at medical inflation. */
    public static double annualSurcharge(double lookbackMagi, int year, FilingStatus filingStatus, double medicalInflationRate) {
        int tier = tier(lookbackMagi, year, filingStatus);
        if (tier == 0) return 0;
        double monthly = PART_B_SURCHARGE[tier - 1] + PART_D_SURCHARGE[tier - 1];
        return monthly * 12 * growth(year, medicalInflationRate);
    }

    /** Yearly standard Part B premium for one person, grown at medical inflation. */
    public static double annualPartB(int year, double medicalInflationRate) {
        return PART_B_STANDARD_MONTHLY * 12 * growth(year, medicalInflationRate);
    }

    public static double growth(int year, double medicalInflationRate) {
        return Math.pow(1 + medicalInflationRate / 100.0, year - BASE_YEAR);
    }

    private static boolean isJoint(FilingStatus filingStatus) {
        return filingStatus == FilingStatus.MARRIED_FILING_JOINTLY || filingStatus == FilingStatus.HEAD_OF_HOUSEHOLD;
    }
}
