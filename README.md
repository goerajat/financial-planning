# Financial Planning Application

A Java-based financial planning tool designed to help individuals and couples plan their retirement finances with comprehensive tax optimization strategies.

## Overview

This application models long-term financial projections including:
- Income and expense tracking over multiple years
- Retirement account management (401k, IRA, Roth IRA)
- Required Minimum Distribution (RMD) calculations
- Tax-optimized withdrawal strategies
- Roth conversion optimization
- State-by-year tax configuration (NJ, NY, FL)
- Sensitivity analysis for growth rate scenarios
- Roth conversion strategy comparison
- Multi-year financial summaries with PDF and CSV export

## User Interfaces

The application is a web interface (Spring Boot + React) plus a CLI for batch
runs from CSV files. The earlier JavaFX desktop interface was removed in
November 2026; the web interface is the only GUI.

### Web Interface (Spring Boot + React)
A web-based interface accessible through any browser, including a phone. Features include:
- Tabbed interface for managing Persons, Financial Entries, Rates, and States
- State-by-year tax configuration (NJ, NY, FL)
- Interactive results display with PDF generation
- Sensitivity analysis tool
- Roth conversion comparison tool
- Plan stored in SQLite with autosave; Export/Import as JSON files

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
- **NY State Tax Calculator** - New York state income tax brackets
- **FL State Tax Calculator** - Florida (0% state income tax)
- **State Tax Calculator Factory** - Selects appropriate state calculator by year
- **Social Security Tax Calculator** - FICA taxes with wage base limits
- **Long-term Capital Gains Calculator** - Capital gains with configurable cost basis
- **RMD Calculator** - IRS Uniform Lifetime Table calculations

### Social Security and Health Premiums

Each person carries their own Social Security record and health cover, entered on
the Persons tab in today's dollars per month:

- **Benefit at full retirement age** (the SSA statement figure) and a **claiming
  age** from 62 to 70. The engine applies the statutory reduction for claiming
  early (5/9 of 1% a month for 36 months, 5/12 of 1% beyond) and the delayed
  credit (8% a year to 70), starts the benefit in the year the person reaches the
  claiming age, and grows it at the Social Security COLA rate from the plan's
  first year. A spouse receives the larger of their own benefit and half of the
  other's full-retirement-age benefit (reduced if claimed early) once both have
  claimed. Hand-entered Social Security rows still work for anyone who prefers to
  type the amount.
- **Premium before Medicare** (COBRA or an ACA plan), charged in years when nobody
  in the plan has wage income, until the person turns 65.
- **Medicare supplement and Part D plan premium.** From 65 the engine adds the
  standard Part B premium and the income-related surcharge (IRMAA) for Part B
  and Part D, set from the household's modified adjusted gross income two years
  earlier. The premium tables are the CMS 2026 schedule; thresholds are indexed at
  2.5% a year and premiums grow at the **Medical Inflation** rate on the Rates tab
  (default 5.5%).

The Persons form can research these two premiums for you: pick Silver or Gold
and press **Estimate from the web**. The server runs the local `claude` CLI
(`claude -p` with web search, billed to the subscription, `claude.executable`
in `application.properties` if it is not on PATH) with a prompt built from the
plan: year of birth, state of residence by year from the States tab, the last
wage year of the person and of the household, the resulting gap before
Medicare and the Medicare year. The answer, its sources and a suggested
Medical Inflation rate come back into the form for you to apply; every run is
kept in the plan store with its prompt. A run takes two to four minutes and
nothing in it names a person.

Health premiums are a cash outflow like any expense and appear as their own line
in the results, the PDF and the refresh history. From the year anyone in the plan
turns 63, the Roth conversion strategy stops a conversion short of the next IRMAA
threshold, since that year's income sets a premium two years on.

### Supported Filing Statuses

- Single
- Married Filing Jointly
- Married Filing Separately
- Head of Household

### Sensitivity Analysis

Analyze how different growth rates affect your net worth over time:
- **Rate Types**: Expense, Income, Qualified, Non-Qualified, Roth, and Cash growth rates
- **Configurable Range**: Set minimum and maximum rates with custom increments
- **Milestone Years**: View projected net worth at 5-year intervals
- **Shortfall Detection**: Identifies when deficits occur before milestones

### Roth Conversion Comparison

Compare different Roth conversion strategies side-by-side:
- **No Conversion**: Baseline scenario with no Roth conversions
- **Fill 12% Bracket**: Convert enough to fill the 12% federal tax bracket
- **Fill 22% Bracket**: Convert enough to fill the 22% federal tax bracket
- **Fill 24% Bracket**: Convert enough to fill the 24% federal tax bracket

For each strategy, view:
- Net worth at milestone years
- Delta compared to baseline (no conversion)
- Cumulative taxes paid
- Roth account balance

### State-by-Year Tax Configuration

Configure which state's tax rules apply for each year range in your financial plan:

| State | Description |
|-------|-------------|
| NJ | New Jersey - Progressive state income tax (default) |
| NY | New York - Progressive state income tax |
| FL | Florida - No state income tax (0%) |

Use cases:
- **Retirement relocation**: Plan to move from NJ to FL in 2030? Configure NJ for 2024-2029 and FL for 2030+
- **Tax optimization**: Compare total tax burden across different relocation scenarios
- **Multi-state planning**: Model complex scenarios with multiple state transitions

The state configuration affects:
- State income tax calculations for each year
- Sensitivity analysis projections
- Roth conversion comparison results

## Project Structure

```
financial-planning/
├── src/
│   ├── main/java/rg/financialplanning/
│   │   ├── FinancialPlanner.java          # CLI entry point
│   │   ├── calculator/                     # Tax calculators
│   │   │   ├── FederalTaxCalculator.java
│   │   │   ├── NJStateTaxCalculator.java
│   │   │   ├── NYStateTaxCalculator.java
│   │   │   ├── FloridaTaxCalculator.java
│   │   │   ├── StateTaxCalculatorFactory.java
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
│   │   │   ├── FilingStatus.java
│   │   │   ├── State.java
│   │   │   └── StateByYear.java
│   │   ├── parser/                         # CSV parsers
│   │   │   ├── FinancialDataProcessor.java
│   │   │   ├── PersonParser.java
│   │   │   └── ItemTypePercentageParser.java
│   │   ├── strategy/                       # Tax optimization strategies
│   │   │   ├── TaxOptimizationStrategy.java
│   │   │   ├── CompositeTaxOptimizationStrategy.java
│   │   │   ├── RMDOptimizationStrategy.java
│   │   │   ├── ExpenseManagementStrategy.java
│   │   │   ├── RothConversionOptimizationStrategy.java
│   │   │   └── TaxCalculationStrategy.java
│   │   └── web/                            # Spring Boot web backend
│   │       ├── FinancialPlannerWebApp.java
│   │       ├── controller/
│   │       │   └── PlanController.java
│   │       ├── service/
│   │       │   └── FinancialPlanService.java
│   │       ├── dto/                        # Data transfer objects
│   │       └── config/
│   └── test/java/rg/financialplanning/    # Unit tests
├── frontend/                               # React web frontend
│   ├── src/
│   │   ├── App.tsx
│   │   ├── components/
│   │   │   ├── PersonsTab.tsx
│   │   │   ├── EntriesTab.tsx
│   │   │   ├── RatesTab.tsx
│   │   │   ├── StatesTab.tsx
│   │   │   ├── ResultsTab.tsx
│   │   │   ├── SensitivityTab.tsx
│   │   │   └── RothComparisonTab.tsx
│   │   ├── context/
│   │   └── services/
│   └── package.json
├── sample_data.csv                         # Sample financial entries
├── persons.csv                             # Person definitions
├── item_percentages.csv                    # Annual growth rates
└── pom.xml                                 # Maven configuration
```

## Prerequisites

- Java 17 or higher
- Maven 3.6 or higher
- Node.js 20+ (for web interface development, automatically installed by Maven during build)

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

### Running the Web Interface

Build and run the Spring Boot application with embedded React frontend:

```bash
mvn clean package
java -jar target/financial-planning-1.0-SNAPSHOT-web.jar
```

Then open http://localhost:8080 in your browser.

For development (backend + frontend separately):

```bash
# Terminal 1: Start the Spring Boot backend
mvn spring-boot:run

# Terminal 2: Start the React frontend (from frontend directory)
cd frontend
npm start
```

### Running the CLI

```bash
mvn exec:java -Dexec.mainClass="rg.financialplanning.FinancialPlanner" \
  -Dexec.args="sample_data.csv persons.csv item_percentages.csv"
```

Or compile and run directly:

```bash
mvn package -Dskip.frontend=true
java -cp target/classes rg.financialplanning.FinancialPlanner sample_data.csv persons.csv item_percentages.csv
```

### Output

The application generates:
1. PDF reports with charts and yearly summaries
2. Console output with yearly summaries (CLI mode)
3. `FinancialPlanner-Out.csv` - CSV export of all yearly data

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
| LIFE_INSURANCE_CASH | Cash value of permanent life insurance (default growth 4%) |

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

## Where the Web Plan Is Stored

The web interface keeps the plan in a SQLite database at
`~/.financial-planning/planner.db` (override with `planner.db=` in
`application.properties`). The schema is created on first start from
`planner-schema.sql`. Every edit is written back about half a second later; the
header shows Saving, Saved or the error. A tab that comes back to the foreground
reloads the stored plan, so a phone and a desktop see the same plan.

- `GET /api/plan` returns the stored plan, `PUT /api/plan` replaces it.
- **Export** downloads the plan as JSON; **Import** loads a JSON file into the
  database. The JSON file is the planner's own export format.
- Every aggregator refresh is recorded: `refresh_runs`, the balances it brought
  in (`refresh_balances`, with the captured balance and the projected year-end
  figure per account and person) and the yearly projection the plan produced
  right after (`projection_history`). `GET /api/plan/history` lists the runs and
  `GET /api/plan/history/{id}` returns one with its rows, for a plan-vs-actual
  comparison later.
- The database holds your figures, so it lives outside the repository.

## Refreshing Balances from the Finance Aggregator

The web interface can pull current account balances from a locally running
[finance aggregator](https://github.com/goerajat/finance-aggregator) and write them
into the plan as asset entries (QUALIFIED, NON_QUALIFIED, ROTH, CASH,
LIFE_INSURANCE_CASH). Press **Refresh balances from aggregator** on the Entries tab.

- Each balance is compounded at the plan rate for its bucket from its capture date to
  31 December of the current year and written as a current-year entry, which the
  planner treats as the end-of-year balance (growth applies from the following year).
- Refreshed rows carry a `source` of `aggregator:<accountId>`, show an `AGG` badge, and
  are replaced wholesale on the next refresh. Hand-entered rows are never touched; the
  refresh report lists hand-entered asset rows that would double count.
- The mapping lives outside the repository in `~/.financial-planning/aggregator.json`
  (override the path with the `aggregator.config` property), because it names people
  and accounts:

```json
{
  "baseUrl": "http://127.0.0.1:8765",
  "staleDays": 3,
  "persons": { "JOHN DOE": "John", "Jane Roe": "Jane" },
  "typeMap": { "HSA": "ROTH" },
  "accounts": {
    "66": { "itemType": "LIFE_INSURANCE_CASH", "person": "John" },
    "99": { "skip": true }
  }
}
```

`persons` maps aggregator holder names (matched case-insensitively) to plan person
names; joint accounts are split evenly. `typeMap` overrides the default account-type
mapping (IRA/401K/403B to QUALIFIED, ROTH_IRA to ROTH, BROKERAGE to NON_QUALIFIED,
CHECKING/SAVINGS/CASH to CASH; anything else is skipped with a reason). `accounts`
overrides one account by its aggregator id.

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
| Total State Income Tax | State tax liability (NJ, NY, or FL based on configuration) |
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

Skip frontend build during testing:
```bash
mvn test -Dskip.frontend=true
```

The project includes comprehensive unit tests covering:
- All tax calculators
- Model classes
- CSV parsers
- Tax optimization strategies
- Web service layer

## License

This project is for personal financial planning purposes.

## Contributing

Contributions are welcome! Please feel free to submit issues and pull requests.
