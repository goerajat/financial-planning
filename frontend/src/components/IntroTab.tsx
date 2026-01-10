import React from 'react';
import './IntroTab.css';

const IntroTab: React.FC = () => {
  return (
    <div className="tab-content intro-tab">
      <h2>Welcome to Financial Planner</h2>
      <p className="intro-subtitle">
        Plan your financial future with comprehensive projections, tax analysis, and scenario comparisons.
      </p>

      <div className="intro-section">
        <h3>Getting Started</h3>
        <p>Follow these steps to create your financial plan:</p>

        <div className="steps-container">
          <div className="step">
            <div className="step-number">1</div>
            <div className="step-content">
              <h4>Add Persons</h4>
              <p>Start by adding yourself and any family members. Enter names, birth years, and self-employment status.</p>
            </div>
          </div>

          <div className="step">
            <div className="step-number">2</div>
            <div className="step-content">
              <h4>Add Financial Entries</h4>
              <p>Enter your income sources, expenses, assets, and contributions. Specify the person, type, amount, and the years each entry applies.</p>
            </div>
          </div>

          <div className="step">
            <div className="step-number">3</div>
            <div className="step-content">
              <h4>Configure Rates</h4>
              <p>Adjust growth rates for different asset types and inflation rates for income/expenses. Default values are provided.</p>
            </div>
          </div>

          <div className="step">
            <div className="step-number">4</div>
            <div className="step-content">
              <h4>Set State Residency</h4>
              <p>Define which state you'll live in for each year range. This affects state tax calculations.</p>
            </div>
          </div>

          <div className="step">
            <div className="step-number">5</div>
            <div className="step-content">
              <h4>Generate Results</h4>
              <p>View your financial projections as a detailed PDF report showing year-by-year cash flow, assets, and net worth.</p>
            </div>
          </div>
        </div>
      </div>

      <div className="intro-section">
        <h3>Entry Types Explained</h3>
        <div className="types-grid">
          <div className="type-card">
            <h4>Income & Expenses</h4>
            <ul>
              <li><strong>Income</strong> - Salary, wages, business income</li>
              <li><strong>Expense</strong> - Living costs, bills, discretionary spending</li>
              <li><strong>One-off Proceeds</strong> - One-time income like property sales or inheritance</li>
            </ul>
          </div>

          <div className="type-card">
            <h4>Investment Accounts</h4>
            <ul>
              <li><strong>Qualified Assets</strong> - 401(k), Traditional IRA (pre-tax)</li>
              <li><strong>Roth IRA</strong> - After-tax retirement savings</li>
              <li><strong>Non-Qualified</strong> - Taxable brokerage accounts</li>
              <li><strong>Cash</strong> - Savings, checking, money market</li>
            </ul>
          </div>

          <div className="type-card">
            <h4>Contributions</h4>
            <ul>
              <li><strong>Qualified Contribution</strong> - Annual 401(k)/IRA contributions</li>
              <li><strong>Roth Contribution</strong> - Annual Roth IRA contributions</li>
              <li><strong>Life Insurance Premium</strong> - Insurance policy payments</li>
            </ul>
          </div>

          <div className="type-card">
            <h4>Other Assets</h4>
            <ul>
              <li><strong>Real Estate</strong> - Property values</li>
              <li><strong>Mortgage</strong> - Home loan balance</li>
              <li><strong>Social Security</strong> - Expected SS benefits</li>
              <li><strong>Life Insurance Benefit</strong> - Death benefit amounts</li>
            </ul>
          </div>
        </div>
      </div>

      <div className="intro-section">
        <h3>Advanced Analysis</h3>
        <div className="features-grid">
          <div className="feature-card">
            <h4>Sensitivity Analysis</h4>
            <p>See how changes in growth rates or inflation affect your long-term projections. Identify potential risks and opportunities.</p>
          </div>

          <div className="feature-card">
            <h4>Roth Conversion Comparison</h4>
            <p>Compare scenarios with different Roth conversion strategies to optimize your tax situation over time.</p>
          </div>

          <div className="feature-card">
            <h4>State Scenario Analysis</h4>
            <p>Compare the financial impact of living in different states, including state income tax differences.</p>
          </div>
        </div>
      </div>

      <div className="intro-section tips-section">
        <h3>Tips</h3>
        <ul className="tips-list">
          <li>Use <strong>Save Plan</strong> to download your data as a JSON file for backup</li>
          <li>Use <strong>Load Plan</strong> to restore a previously saved plan</li>
          <li>Entry values should be annual amounts (the system handles projections)</li>
          <li>Set end year to a far future date (e.g., 2100) for ongoing income/expenses</li>
        </ul>
      </div>
    </div>
  );
};

export default IntroTab;
