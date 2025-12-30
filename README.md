# Financial Planning Application

A Java-based financial planning tool designed to help individuals and couples plan their retirement finances with comprehensive tax optimization strategies.

## Overview

This application models long-term financial projections including:
- Income and expense tracking over multiple years
- Retirement account management (401k, IRA, Roth IRA)
- Required Minimum Distribution (RMD) calculations
- Tax-optimized withdrawal strategies
- Roth conversion optimization
- Multi-year financial summaries with CSV export

## Features

### Tax Optimization Strategies

The application implements a composite strategy pattern with four optimization strategies applied in sequence:

1. **RMD Optimization** - Calculates Required Minimum Distributions based on IRS rules:
   - Born 1950 or earlier: RMDs begin at age 72
   - Born 1951-1959: RMDs begin at age 73
   - Born 1960 or later: RMDs begin at age 75

2. **Expense Management** - Handles surplus/deficit after taxes:
   - Surplus: Distributed to non-qualified investment accounts
   - Deficit: Withdrawals in priority order (non-qualified → qualified → Roth → cash)

3. **Roth Conversion Optimization** - Strategic conversions to fill lower tax brackets

4. **Tax Calculation** - Computes and stores all taxes:
   - Federal income tax (2026 projected brackets)
   - NJ state income tax
   - Social Security tax (6.2% up to wage base)
   - Medicare tax (1.45% + additional 0.9% for high earners)
   - Long-term capital gains tax

### Tax Calculators

- **Federal Tax Calculator** - Progressive tax brackets for all filing statuses
- **NJ State Tax Calculator** - New Jersey state income tax brackets
- **Social Security Tax Calculator** - FICA taxes with wage base limits
- **Long-term Capital Gains Calculator** - Capital gains with configurable cost basis
- **RMD Calculator** - IRS Uniform Lifetime Table calculations

### Supported Filing Statuses

- Single
- Married Filing Jointly
- Married Filing Separately
- Head of Household

## Project Structure

```
financial-planning/
├── src/
│   ├── main/java/rg/financialplanning/
│   │   ├── FinancialPlanner.java          # Main entry point
│   │   ├── calculator/                     # Tax calculators
│   │   │   ├── FederalTaxCalculator.java
│   │   │   ├── NJStateTaxCalculator.java
│   │   │   ├── SocialSecurityTaxCalculator.java
│   │   │   ├── LongTermCapitalGainsCalculator.java
│   │   │   ├── RMDCalculator.java
│   │   │   └── IncrementalIncomeCalculator.java
│   │   ├── model/                          # Data models
│   │   │   ├── Person.java
│   │   │   ├── FinancialEntry.java
│   │   │   ├── YearlySummary.java
│   │   │   ├── IndividualYearlySummary.java
│   │   │   ├── ItemType.java
│   │   │   ├── ItemTypePercentage.java
│   │   │   └── FilingStatus.java
│   │   ├── parser/                         # CSV parsers
│   │   │   ├── FinancialDataProcessor.java
│   │   │   ├── PersonParser.java
│   │   │   └── ItemTypePercentageParser.java
│   │   └── strategy/                       # Tax optimization strategies
│   │       ├── TaxOptimizationStrategy.java
│   │       ├── CompositeTaxOptimizationStrategy.java
│   │       ├── RMDOptimizationStrategy.java
│   │       ├── ExpenseManagementStrategy.java
│   │       ├── RothConversionOptimizationStrategy.java
│   │       └── TaxCalculationStrategy.java
│   └── test/java/rg/financialplanning/    # Unit tests (466 tests)
├── sample_data.csv                         # Sample financial entries
├── persons.csv                             # Person definitions
├── item_percentages.csv                    # Annual growth rates
└── pom.xml                                 # Maven configuration
```

## Prerequisites

- Java 17 or higher
- Maven 3.6 or higher

## Installation

1. Clone the repository:
   ```bash
   git clone https://github.com/goerajat/financial-planning.git
   cd financial-planning
   ```

2. Build the project:
   ```bash
   mvn clean compile
   ```

3. Run tests:
   ```bash
   mvn test
   ```

## Usage

### Running the Application

```bash
mvn exec:java -Dexec.mainClass="rg.financialplanning.FinancialPlanner" \
  -Dexec.args="sample_data.csv persons.csv item_percentages.csv"
```

Or compile and run directly:

```bash
mvn package
java -cp target/classes rg.financialplanning.FinancialPlanner sample_data.csv persons.csv item_percentages.csv
```

### Output

The application generates:
1. Console output with yearly summaries
2. `FinancialPlanner-Out.csv` - CSV export of all yearly data

## Input File Formats

### Financial Data CSV (`sample_data.csv`)

| Column | Description |
|--------|-------------|
| name | Person's name |
| item | Entry type (see Item Types below) |
| description | Description of the entry |
| value | Dollar amount |
| startYear | First year this entry is active |
| endYear | Last year this entry is active |

Example:
```csv
name,item,description,value,startYear,endYear
John,INCOME,Salary,100000,2024,2030
John,QUALIFIED,401k,500000,2024,2024
Jane,INCOME,Salary,80000,2024,2028
```

### Item Types

| Type | Description |
|------|-------------|
| INCOME | Earned income (wages, salary) |
| EXPENSE | Living expenses |
| QUALIFIED | Pre-tax retirement accounts (401k, Traditional IRA) |
| NON_QUALIFIED | Taxable investment accounts |
| ROTH | Roth IRA/401k accounts |
| CASH | Liquid cash holdings |
| SOCIAL_SECURITY_BENEFITS | Social Security income |
| REAL_ESTATE | Real estate value |
| LIFE_INSURANCE_BENEFIT | Life insurance benefits |

### Persons CSV (`persons.csv`)

| Column | Description |
|--------|-------------|
| name | Person's name (must match financial data) |
| yearOfBirth | Birth year (used for age calculations) |

Example:
```csv
name,yearOfBirth
John,1960
Jane,1965
```

### Percentages CSV (`item_percentages.csv`)

| Column | Description |
|--------|-------------|
| itemType | The item type |
| percentage | Annual growth/increase rate |

Example:
```csv
itemType,percentage
INCOME,3.0
EXPENSE,2.5
QUALIFIED,7.0
NON_QUALIFIED,6.0
ROTH,7.0
```

## CSV Export Format

The exported `FinancialPlanner-Out.csv` contains:

| Row | Description |
|-----|-------------|
| Total Income | Combined income for all individuals |
| {Name} Income | Income for each person |
| Total RMD Withdrawals | Combined RMD withdrawals |
| {Name} RMD Withdrawals | RMD for each person |
| Total Qualified Withdrawals | Combined qualified account withdrawals |
| {Name} Qualified Withdrawals | Qualified withdrawals per person |
| Total Non-Qualified Withdrawals | Combined non-qualified withdrawals |
| {Name} Non-Qualified Withdrawals | Non-qualified withdrawals per person |
| Total Federal Income Tax | Federal tax liability |
| Total State Income Tax | NJ state tax liability |
| Total Capital Gains Tax | Capital gains tax on non-qualified withdrawals |
| Total Social Security Tax | Social Security (FICA) tax |
| Total Medicare Tax | Medicare tax |
| Total Expenses | Combined expenses |

## Tax Calculation Details

### Taxable Income Components
- Earned income (wages, salary)
- RMD withdrawals (fully taxable)
- Qualified account withdrawals (fully taxable)
- 85% of Social Security benefits (for higher earners)
- Roth conversions (taxable as ordinary income)

### Withdrawal Priority (for deficits)
1. **Non-qualified assets** - Subject to capital gains tax
2. **Qualified assets** - Subject to ordinary income tax (age 59+ only)
3. **Roth assets** - Tax-free withdrawals
4. **Cash** - No tax implications

## Testing

Run the full test suite:
```bash
mvn test
```

The project includes 466 unit tests covering:
- All tax calculators
- Model classes
- CSV parsers
- Tax optimization strategies

## License

This project is for personal financial planning purposes.

## Contributing

Contributions are welcome! Please feel free to submit issues and pull requests.
