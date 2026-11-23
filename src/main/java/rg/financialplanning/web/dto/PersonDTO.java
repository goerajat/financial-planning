package rg.financialplanning.web.dto;

import rg.financialplanning.model.Person;
import java.time.Year;

public class PersonDTO {
    private String name;
    private int yearOfBirth;
    private Integer currentAge;
    private boolean selfEmployed;
    private double socialSecurityFraMonthly;
    private int socialSecurityClaimAge;
    private double preMedicarePremiumMonthly;
    private double medicarePlanPremiumMonthly;

    public PersonDTO() {
    }

    public PersonDTO(String name, int yearOfBirth) {
        this(name, yearOfBirth, false);
    }

    public PersonDTO(String name, int yearOfBirth, boolean selfEmployed) {
        this.name = name;
        this.yearOfBirth = yearOfBirth;
        this.currentAge = Year.now().getValue() - yearOfBirth;
        this.selfEmployed = selfEmployed;
    }

    public static PersonDTO fromPerson(Person person) {
        PersonDTO dto = new PersonDTO(person.name(), person.yearOfBirth(), person.isSelfEmployed());
        dto.socialSecurityFraMonthly = person.socialSecurityFraMonthly();
        dto.socialSecurityClaimAge = person.socialSecurityClaimAge();
        dto.preMedicarePremiumMonthly = person.preMedicarePremiumMonthly();
        dto.medicarePlanPremiumMonthly = person.medicarePlanPremiumMonthly();
        return dto;
    }

    public Person toPerson() {
        return new Person(name, yearOfBirth, selfEmployed, socialSecurityFraMonthly, socialSecurityClaimAge,
                preMedicarePremiumMonthly, medicarePlanPremiumMonthly);
    }

    public double getSocialSecurityFraMonthly() {
        return socialSecurityFraMonthly;
    }

    public void setSocialSecurityFraMonthly(double socialSecurityFraMonthly) {
        this.socialSecurityFraMonthly = socialSecurityFraMonthly;
    }

    public int getSocialSecurityClaimAge() {
        return socialSecurityClaimAge;
    }

    public void setSocialSecurityClaimAge(int socialSecurityClaimAge) {
        this.socialSecurityClaimAge = socialSecurityClaimAge;
    }

    public double getPreMedicarePremiumMonthly() {
        return preMedicarePremiumMonthly;
    }

    public void setPreMedicarePremiumMonthly(double preMedicarePremiumMonthly) {
        this.preMedicarePremiumMonthly = preMedicarePremiumMonthly;
    }

    public double getMedicarePlanPremiumMonthly() {
        return medicarePlanPremiumMonthly;
    }

    public void setMedicarePlanPremiumMonthly(double medicarePlanPremiumMonthly) {
        this.medicarePlanPremiumMonthly = medicarePlanPremiumMonthly;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getYearOfBirth() {
        return yearOfBirth;
    }

    public void setYearOfBirth(int yearOfBirth) {
        this.yearOfBirth = yearOfBirth;
        this.currentAge = Year.now().getValue() - yearOfBirth;
    }

    public Integer getCurrentAge() {
        if (currentAge == null && yearOfBirth > 0) {
            currentAge = Year.now().getValue() - yearOfBirth;
        }
        return currentAge;
    }

    public void setCurrentAge(Integer currentAge) {
        this.currentAge = currentAge;
    }

    public boolean isSelfEmployed() {
        return selfEmployed;
    }

    public void setSelfEmployed(boolean selfEmployed) {
        this.selfEmployed = selfEmployed;
    }
}
