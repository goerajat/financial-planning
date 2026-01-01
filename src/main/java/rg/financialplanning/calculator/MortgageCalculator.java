package rg.financialplanning.calculator;

/**
 * Calculates mortgage payments and balances for standard amortizing loans.
 * Uses the standard amortization formula: M = P * [r(1+r)^n] / [(1+r)^n - 1]
 * where M = payment, P = principal, r = periodic interest rate, n = number of periods.
 */
public class MortgageCalculator {

    /**
     * Calculates the fixed annual payment for an amortizing mortgage.
     *
     * @param principal the original loan amount
     * @param annualInterestRate the annual interest rate as a percentage (e.g., 6.5 for 6.5%)
     * @param termYears the loan term in years
     * @return the fixed annual payment amount
     */
    public double calculateAnnualPayment(double principal, double annualInterestRate, int termYears) {
        if (principal < 0) {
            throw new IllegalArgumentException("Principal cannot be negative");
        }
        if (annualInterestRate < 0) {
            throw new IllegalArgumentException("Interest rate cannot be negative");
        }
        if (termYears <= 0) {
            throw new IllegalArgumentException("Term must be at least 1 year");
        }
        if (principal == 0) {
            return 0;
        }

        // Handle 0% interest rate case
        if (annualInterestRate == 0) {
            return principal / termYears;
        }

        double r = annualInterestRate / 100.0; // Convert percentage to decimal
        int n = termYears;

        // Amortization formula: M = P * [r(1+r)^n] / [(1+r)^n - 1]
        double factor = Math.pow(1 + r, n);
        return principal * (r * factor) / (factor - 1);
    }

    /**
     * Calculates the remaining balance after a specified number of years.
     *
     * @param principal the original loan amount
     * @param annualInterestRate the annual interest rate as a percentage
     * @param termYears the total loan term in years
     * @param yearsElapsed the number of years of payments made
     * @return the remaining balance
     */
    public double calculateRemainingBalance(double principal, double annualInterestRate, int termYears, int yearsElapsed) {
        if (yearsElapsed < 0) {
            throw new IllegalArgumentException("Years elapsed cannot be negative");
        }
        if (yearsElapsed >= termYears) {
            return 0; // Loan is fully paid off
        }
        if (principal == 0) {
            return 0;
        }

        // Handle 0% interest rate case
        if (annualInterestRate == 0) {
            double annualPayment = principal / termYears;
            return principal - (annualPayment * yearsElapsed);
        }

        double r = annualInterestRate / 100.0;
        int n = termYears;
        int p = yearsElapsed; // payments made

        // Remaining balance formula: B = P * [(1+r)^n - (1+r)^p] / [(1+r)^n - 1]
        double factorN = Math.pow(1 + r, n);
        double factorP = Math.pow(1 + r, p);
        return principal * (factorN - factorP) / (factorN - 1);
    }

    /**
     * Calculates the interest portion of a payment given the current balance.
     *
     * @param remainingBalance the current outstanding balance
     * @param annualInterestRate the annual interest rate as a percentage
     * @return the interest portion of the annual payment
     */
    public double calculateInterestPortion(double remainingBalance, double annualInterestRate) {
        if (remainingBalance < 0) {
            throw new IllegalArgumentException("Remaining balance cannot be negative");
        }
        if (annualInterestRate < 0) {
            throw new IllegalArgumentException("Interest rate cannot be negative");
        }
        return remainingBalance * (annualInterestRate / 100.0);
    }

    /**
     * Calculates the principal portion of a payment.
     *
     * @param annualPayment the fixed annual payment
     * @param remainingBalance the current outstanding balance
     * @param annualInterestRate the annual interest rate as a percentage
     * @return the principal portion of the payment
     */
    public double calculatePrincipalPortion(double annualPayment, double remainingBalance, double annualInterestRate) {
        double interestPortion = calculateInterestPortion(remainingBalance, annualInterestRate);
        return annualPayment - interestPortion;
    }

    /**
     * Calculates the new balance after making an annual payment.
     *
     * @param currentBalance the current outstanding balance
     * @param annualPayment the annual payment amount
     * @param annualInterestRate the annual interest rate as a percentage
     * @return the new balance after the payment
     */
    public double calculateBalanceAfterPayment(double currentBalance, double annualPayment, double annualInterestRate) {
        double principalPortion = calculatePrincipalPortion(annualPayment, currentBalance, annualInterestRate);
        double newBalance = currentBalance - principalPortion;
        // Avoid negative balance due to floating point errors
        return Math.max(0, newBalance);
    }
}
