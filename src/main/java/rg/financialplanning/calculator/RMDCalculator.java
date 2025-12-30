package rg.financialplanning.calculator;

/**
 * Calculates Required Minimum Distributions (RMD) from IRA accounts.
 * Uses the IRS Uniform Lifetime Table (updated for 2024+).
 * RMD age is 73 for those born 1951-1959, and 75 for those born 1960 or later.
 */
public class RMDCalculator {

    // IRS Uniform Lifetime Table - Life expectancy factors by age
    // Index 0 = age 72, Index 1 = age 73, etc.
    private static final double[] UNIFORM_LIFETIME_TABLE = {
        27.4,  // 72
        26.5,  // 73
        25.5,  // 74
        24.6,  // 75
        23.7,  // 76
        22.9,  // 77
        22.0,  // 78
        21.1,  // 79
        20.2,  // 80
        19.4,  // 81
        18.5,  // 82
        17.7,  // 83
        16.8,  // 84
        16.0,  // 85
        15.2,  // 86
        14.4,  // 87
        13.7,  // 88
        12.9,  // 89
        12.2,  // 90
        11.5,  // 91
        10.8,  // 92
        10.1,  // 93
        9.5,   // 94
        8.9,   // 95
        8.4,   // 96
        7.8,   // 97
        7.3,   // 98
        6.8,   // 99
        6.4,   // 100
        6.0,   // 101
        5.6,   // 102
        5.2,   // 103
        4.9,   // 104
        4.6,   // 105
        4.3,   // 106
        4.1,   // 107
        3.9,   // 108
        3.7,   // 109
        3.5,   // 110
        3.4,   // 111
        3.3,   // 112
        3.1,   // 113
        3.0,   // 114
        2.9,   // 115
        2.8,   // 116
        2.7,   // 117
        2.5,   // 118
        2.3,   // 119
        2.0    // 120+
    };

    private static final int MIN_TABLE_AGE = 72;
    private static final int MAX_TABLE_AGE = 120;

    /**
     * Calculates the Required Minimum Distribution for an IRA account.
     *
     * @param age the age of the account holder at the end of the distribution year
     * @param previousYearEndBalance the account balance at the end of the previous year
     * @return the RMD amount
     */
    public double calculateRMD(int age, double previousYearEndBalance) {
        if (age < MIN_TABLE_AGE) {
            throw new IllegalArgumentException("RMD is not required for individuals under age " + MIN_TABLE_AGE);
        }
        if (previousYearEndBalance < 0) {
            throw new IllegalArgumentException("Account balance cannot be negative");
        }
        if (previousYearEndBalance == 0) {
            return 0;
        }

        double lifeExpectancyFactor = getLifeExpectancyFactor(age);
        return previousYearEndBalance / lifeExpectancyFactor;
    }

    /**
     * Gets the life expectancy factor from the Uniform Lifetime Table.
     *
     * @param age the age of the account holder
     * @return the life expectancy factor
     */
    public double getLifeExpectancyFactor(int age) {
        if (age < MIN_TABLE_AGE) {
            throw new IllegalArgumentException("Age must be at least " + MIN_TABLE_AGE);
        }

        int index = Math.min(age - MIN_TABLE_AGE, UNIFORM_LIFETIME_TABLE.length - 1);
        return UNIFORM_LIFETIME_TABLE[index];
    }

    /**
     * Determines the RMD starting age based on birth year.
     * - Born 1950 or earlier: age 72
     * - Born 1951-1959: age 73
     * - Born 1960 or later: age 75
     *
     * @param birthYear the birth year of the account holder
     * @return the age at which RMDs must begin
     */
    public int getRMDStartAge(int birthYear) {
        if (birthYear <= 1950) {
            return 72;
        } else if (birthYear <= 1959) {
            return 73;
        } else {
            return 75;
        }
    }

    /**
     * Checks if RMD is required for the given age and birth year.
     *
     * @param age the current age of the account holder
     * @param birthYear the birth year of the account holder
     * @return true if RMD is required, false otherwise
     */
    public boolean isRMDRequired(int age, int birthYear) {
        return age >= getRMDStartAge(birthYear);
    }

    /**
     * Calculates the RMD as a percentage of the account balance.
     *
     * @param age the age of the account holder
     * @return the RMD percentage (e.g., 3.65 for 3.65%)
     */
    public double getRMDPercentage(int age) {
        if (age < MIN_TABLE_AGE) {
            return 0;
        }
        double factor = getLifeExpectancyFactor(age);
        return (1.0 / factor) * 100;
    }
}
