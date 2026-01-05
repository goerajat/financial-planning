import React, { useState } from 'react';
import { usePlan } from '../context/PlanContext';
import { planApi } from '../services/api';
import { RothComparisonResponse, RothComparisonCell, FilingStatus, FILING_STATUSES } from '../types';
import './RothComparisonTab.css';

const RothComparisonTab: React.FC = () => {
  const { persons, entries, rates, statesByYear } = usePlan();
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [results, setResults] = useState<RothComparisonResponse | null>(null);
  const [filingStatus, setFilingStatus] = useState<FilingStatus>('MARRIED_FILING_JOINTLY');
  const [yearIncrement, setYearIncrement] = useState(5);

  const formatCurrency = (value: number): string => {
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: 'USD',
      minimumFractionDigits: 0,
      maximumFractionDigits: 0,
    }).format(value);
  };

  const formatDelta = (value: number): string => {
    const prefix = value >= 0 ? '+' : '';
    return prefix + formatCurrency(value);
  };

  const runComparison = async () => {
    if (entries.length === 0) {
      setError('Please add financial entries before running comparison.');
      return;
    }

    setLoading(true);
    setError(null);

    try {
      const planData = {
        persons,
        entries,
        rates,
        statesByYear,
      };

      const response = await planApi.runRothComparison({
        planData,
        yearIncrement,
        filingStatus,
      });

      setResults(response);
    } catch (err) {
      setError('Failed to run Roth conversion comparison. Please try again.');
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  const renderCell = (cell: RothComparisonCell, isBaseline: boolean): React.ReactNode => {
    if (cell.hasShortfall) {
      return (
        <div className="roth-cell shortfall-cell">
          <div className="shortfall-label">Shortfall</div>
          <div className="shortfall-year">Year {cell.shortfallYear}</div>
        </div>
      );
    }

    const deltaClass = cell.netWorthDelta > 0 ? 'positive' : cell.netWorthDelta < 0 ? 'negative' : 'neutral';

    return (
      <div className="roth-cell">
        <div className="net-worth">{formatCurrency(cell.netWorth)}</div>
        {!isBaseline && (
          <div className={`delta ${deltaClass}`}>
            {formatDelta(cell.netWorthDelta)}
          </div>
        )}
        <div className="taxes">Tax: {formatCurrency(cell.cumulativeTaxes)}</div>
        <div className="roth-balance">Roth: {formatCurrency(cell.rothBalance)}</div>
      </div>
    );
  };

  return (
    <div className="roth-comparison-tab">
      <p className="tab-description">
        Compare 5-year net worth projections with different Roth conversion strategies.
        See how filling different tax brackets affects your long-term financial outcomes.
      </p>

      <div className="comparison-inputs">
        <div className="input-group filing-status-group">
          <label>Filing Status</label>
          <select
            value={filingStatus}
            onChange={(e) => setFilingStatus(e.target.value as FilingStatus)}
          >
            {FILING_STATUSES.map((status) => (
              <option key={status.value} value={status.value}>
                {status.label}
              </option>
            ))}
          </select>
        </div>

        <div className="input-group">
          <label>Year Increment</label>
          <input
            type="number"
            min={1}
            max={10}
            value={yearIncrement}
            onChange={(e) => setYearIncrement(parseInt(e.target.value) || 5)}
          />
        </div>

        <button
          className="run-comparison-btn"
          onClick={runComparison}
          disabled={loading || entries.length === 0}
        >
          {loading ? 'Running...' : 'Run Comparison'}
        </button>
      </div>

      {error && <div className="error-message">{error}</div>}

      {results && (
        <div className="comparison-results">
          <h3>Roth Conversion Strategy Comparison</h3>
          <div className="table-container">
            <table className="comparison-table">
              <thead>
                <tr>
                  <th className="scenario-header">Scenario</th>
                  {results.milestoneYears.map((year, index) => (
                    <th key={year} className="year-header">
                      <div className="year-label">{year}</div>
                      <div className="year-offset">
                        (+{year - results.firstDataYear} yrs)
                      </div>
                    </th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {results.rows.map((row, rowIndex) => (
                  <tr key={row.scenarioName} className={rowIndex === 0 ? 'baseline-row' : ''}>
                    <td className="scenario-cell">
                      <div className="scenario-name">{row.scenarioName}</div>
                      {row.bracketThreshold && (
                        <div className="bracket-threshold">
                          Up to {formatCurrency(row.bracketThreshold)}
                        </div>
                      )}
                    </td>
                    {row.cells.map((cell) => (
                      <td
                        key={cell.year}
                        className={`data-cell ${cell.hasShortfall ? 'cell-shortfall' : cell.netWorthDelta > 0 ? 'cell-positive' : cell.netWorthDelta < 0 ? 'cell-negative' : ''}`}
                      >
                        {renderCell(cell, rowIndex === 0)}
                      </td>
                    ))}
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          <div className="legend">
            <div className="legend-item">
              <div className="legend-color baseline"></div>
              <span>Baseline (No Conversion)</span>
            </div>
            <div className="legend-item">
              <div className="legend-color positive"></div>
              <span>Higher Net Worth vs Baseline</span>
            </div>
            <div className="legend-item">
              <div className="legend-color negative"></div>
              <span>Lower Net Worth vs Baseline</span>
            </div>
            <div className="legend-item">
              <div className="legend-color shortfall"></div>
              <span>Cash Shortfall</span>
            </div>
          </div>

          <div className="comparison-notes">
            <h4>Understanding the Results</h4>
            <ul>
              <li><strong>No Conversion:</strong> Baseline scenario with no Roth conversions performed.</li>
              <li><strong>Fill 12% Bracket:</strong> Convert enough to fill the 12% federal tax bracket.</li>
              <li><strong>Fill 22% Bracket:</strong> Convert enough to fill the 22% federal tax bracket.</li>
              <li><strong>Fill 24% Bracket:</strong> Convert enough to fill the 24% federal tax bracket.</li>
              <li><strong>Net Worth:</strong> Total assets minus liabilities at each milestone.</li>
              <li><strong>Delta:</strong> Difference in net worth compared to no-conversion baseline.</li>
              <li><strong>Tax:</strong> Cumulative federal + state income taxes paid from start to milestone.</li>
              <li><strong>Roth:</strong> Roth account balance at each milestone.</li>
            </ul>
          </div>
        </div>
      )}

      {!results && !loading && (
        <div className="empty-message">
          Click "Run Comparison" to analyze Roth conversion strategies.
        </div>
      )}
    </div>
  );
};

export default RothComparisonTab;
