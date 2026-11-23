package rg.financialplanning.model;

import rg.financialplanning.calculator.SocialSecurityCalculator;

import java.time.Year;
import java.util.Objects;

/**
 * Class representing a person with their name and year of birth.
 */
public class Person {
    private final String name;
    private final int yearOfBirth;
    private final boolean selfEmployed;
    /** Social Security benefit at full retirement age, per month, in the plan's first-year dollars. 0 = none. */
    private final double socialSecurityFraMonthly;
    /** Whole-year claiming age 62..70; 0 means full retirement age. */
    private final int socialSecurityClaimAge;
    /** Health cover before Medicare, per month in first-year dollars, charged in years without wages. */
    private final double preMedicarePremiumMonthly;
    /** Medicare supplement (or Advantage) plus Part D plan premium, per month in first-year dollars. */
    private final double medicarePlanPremiumMonthly;

    public Person(String name, int yearOfBirth) {
        this(name, yearOfBirth, false);
    }

    public Person(String name, int yearOfBirth, boolean selfEmployed) {
        this(name, yearOfBirth, selfEmployed, 0, 0, 0, 0);
    }

    public Person(String name, int yearOfBirth, boolean selfEmployed, double socialSecurityFraMonthly,
                  int socialSecurityClaimAge, double preMedicarePremiumMonthly, double medicarePlanPremiumMonthly) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Name cannot be null or empty");
        }
        if (yearOfBirth < 1900 || yearOfBirth > 2100) {
            throw new IllegalArgumentException("Year of birth must be between 1900 and 2100");
        }
        if (socialSecurityClaimAge != 0 && !SocialSecurityCalculator.isValidClaimAge(socialSecurityClaimAge)) {
            throw new IllegalArgumentException("Social Security claiming age must be between 62 and 70");
        }
        if (socialSecurityFraMonthly < 0 || preMedicarePremiumMonthly < 0 || medicarePlanPremiumMonthly < 0) {
            throw new IllegalArgumentException("Benefit and premium amounts cannot be negative");
        }
        this.name = name;
        this.yearOfBirth = yearOfBirth;
        this.selfEmployed = selfEmployed;
        this.socialSecurityFraMonthly = socialSecurityFraMonthly;
        this.socialSecurityClaimAge = socialSecurityClaimAge;
        this.preMedicarePremiumMonthly = preMedicarePremiumMonthly;
        this.medicarePlanPremiumMonthly = medicarePlanPremiumMonthly;
    }

    public double socialSecurityFraMonthly() {
        return socialSecurityFraMonthly;
    }

    public int socialSecurityClaimAge() {
        return socialSecurityClaimAge;
    }

    /** The claiming age to use: the chosen one, or full retirement age rounded to whole years. */
    public int socialSecurityClaimAgeOrFra() {
        if (socialSecurityClaimAge != 0) return socialSecurityClaimAge;
        return (int) Math.round(SocialSecurityCalculator.fullRetirementAgeMonths(yearOfBirth) / 12.0);
    }

    public double preMedicarePremiumMonthly() {
        return preMedicarePremiumMonthly;
    }

    public double medicarePlanPremiumMonthly() {
        return medicarePlanPremiumMonthly;
    }

    public String name() {
        return name;
    }

    public int yearOfBirth() {
        return yearOfBirth;
    }

    public boolean isSelfEmployed() {
        return selfEmployed;
    }

    public int getAgeInYear(int year) {
        return year - yearOfBirth;
    }

    public int getCurrentAge() {
        return Year.now().getValue() - yearOfBirth;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Person person = (Person) o;
        return yearOfBirth == person.yearOfBirth && selfEmployed == person.selfEmployed && Objects.equals(name, person.name)
                && socialSecurityFraMonthly == person.socialSecurityFraMonthly
                && socialSecurityClaimAge == person.socialSecurityClaimAge
                && preMedicarePremiumMonthly == person.preMedicarePremiumMonthly
                && medicarePlanPremiumMonthly == person.medicarePlanPremiumMonthly;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, yearOfBirth, selfEmployed, socialSecurityFraMonthly, socialSecurityClaimAge,
                preMedicarePremiumMonthly, medicarePlanPremiumMonthly);
    }

    @Override
    public String toString() {
        return String.format("%s (born %d%s)", name, yearOfBirth, selfEmployed ? ", self-employed" : "");
    }
}
