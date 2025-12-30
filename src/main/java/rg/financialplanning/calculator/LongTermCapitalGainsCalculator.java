package rg.financialplanning.calculator;

/**
 * Calculates long-term capital gains tax.
 * Assumes a fixed cost basis factor and capital gains tax rate.
 *
 * Default assumptions:
 * - Cost basis factor of 0.25 (25%): This means the original purchase price (cost basis)
 *   is assumed to be 25% of the current sale proceeds. For example, if you sell assets
 *   for $100,000, the cost basis is assumed to be $25,000, resulting in a capital gain
 *   of $75,000. This approximates significant long-term appreciation typical of
 *   investments held for many years.
 *
 * - Capital gains tax rate of 0.20 (20%): This is the federal long-term capital gains
 *   rate for high-income taxpayers (those in the highest tax brackets). Long-term
 *   capital gains rates are 0%, 15%, or 20% depending on taxable income. The 20% rate
 *   applies to single filers with taxable income over ~$518,900 or married filing
 *   jointly over ~$583,750 (2024 thresholds).
 */
public class LongTermCapitalGainsCalculator {

    /**
     * Default cost basis factor of 0.25 (25%).
     * Assumes the original investment cost was 25% of current sale value,
     * representing substantial appreciation over time.
     */
    public static final double DEFAULT_COST_BASIS_FACTOR = 0.25;

    /**
     * Default long-term capital gains tax rate of 0.20 (20%).
     * This is the highest federal rate for long-term capital gains,
     * applicable to high-income taxpayers.
     */
    private static final double DEFAULT_CAPITAL_GAINS_RATE = 0.20;

    private final double costBasisFactor;
    private final double capitalGainsRate;

    public LongTermCapitalGainsCalculator() {
        this(DEFAULT_COST_BASIS_FACTOR, DEFAULT_CAPITAL_GAINS_RATE);
    }

    public LongTermCapitalGainsCalculator(double costBasisFactor, double capitalGainsRate) {
        if (costBasisFactor < 0 || costBasisFactor > 1) {
            throw new IllegalArgumentException("Cost basis factor must be between 0 and 1");
        }
        if (capitalGainsRate < 0 || capitalGainsRate > 1) {
            throw new IllegalArgumentException("Capital gains rate must be between 0 and 1");
        }
        this.costBasisFactor = costBasisFactor;
        this.capitalGainsRate = capitalGainsRate;
    }

    /**
     * Calculates the cost basis from total sales proceeds.
     *
     * @param totalSalesProceeds the total sales proceeds
     * @return the cost basis
     */
    public double calculateCostBasis(double totalSalesProceeds) {
        if (totalSalesProceeds < 0) {
            throw new IllegalArgumentException("Total sales proceeds cannot be negative");
        }
        return totalSalesProceeds * costBasisFactor;
    }

    /**
     * Calculates the capital gain from total sales proceeds.
     *
     * @param totalSalesProceeds the total sales proceeds
     * @return the capital gain (proceeds minus cost basis)
     */
    public double calculateCapitalGain(double totalSalesProceeds) {
        if (totalSalesProceeds < 0) {
            throw new IllegalArgumentException("Total sales proceeds cannot be negative");
        }
        return totalSalesProceeds - calculateCostBasis(totalSalesProceeds);
    }

    /**
     * Calculates the long-term capital gains tax.
     *
     * @param totalSalesProceeds the total sales proceeds
     * @return the capital gains tax amount
     */
    public double calculateTax(double totalSalesProceeds) {
        if (totalSalesProceeds < 0) {
            throw new IllegalArgumentException("Total sales proceeds cannot be negative");
        }
        double capitalGain = calculateCapitalGain(totalSalesProceeds);
        return capitalGain * capitalGainsRate;
    }

    /**
     * Calculates the net proceeds after capital gains tax.
     *
     * @param totalSalesProceeds the total sales proceeds
     * @return the net proceeds after tax
     */
    public double calculateNetProceeds(double totalSalesProceeds) {
        if (totalSalesProceeds < 0) {
            throw new IllegalArgumentException("Total sales proceeds cannot be negative");
        }
        return totalSalesProceeds - calculateTax(totalSalesProceeds);
    }

    /**
     * Calculates the total sales proceeds required to achieve a desired net proceeds amount.
     * This is the inverse of calculateNetProceeds.
     *
     * Formula derivation:
     * - netProceeds = totalSalesProceeds - tax
     * - netProceeds = totalSalesProceeds - (capitalGain * capitalGainsRate)
     * - netProceeds = totalSalesProceeds - (totalSalesProceeds * (1 - costBasisFactor) * capitalGainsRate)
     * - netProceeds = totalSalesProceeds * (1 - (1 - costBasisFactor) * capitalGainsRate)
     * - totalSalesProceeds = netProceeds / (1 - (1 - costBasisFactor) * capitalGainsRate)
     *
     * @param netProceeds the desired net proceeds after tax
     * @return the total sales proceeds required
     */
    public double calculateTotalSalesProceeds(double netProceeds) {
        if (netProceeds < 0) {
            throw new IllegalArgumentException("Net proceeds cannot be negative");
        }
        if (netProceeds == 0) {
            return 0;
        }
        double taxFactor = (1 - costBasisFactor) * capitalGainsRate;
        return netProceeds / (1 - taxFactor);
    }

    public double getCostBasisFactor() {
        return costBasisFactor;
    }

    public double getCapitalGainsRate() {
        return capitalGainsRate;
    }
}
