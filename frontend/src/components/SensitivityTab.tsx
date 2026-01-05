import React, { useState, useCallback } from 'react';
import { usePlan } from '../context/PlanContext';
import { planApi } from '../services/api';
import {
  SensitivityAnalysisResponse,
  SensitivityRow,
  SensitivityCell,
  SensitivityRateType,
  SENSITIVITY_RATE_TYPES
} from '../types';
import './Tab.css';
import './SensitivityTab.css';

const SensitivityTab: React.FC = () => {
  const { persons, entries, rates, statesByYear } = usePlan();

  const [rateType, setRateType] = useState<SensitivityRateType>('EXPENSE');
  const [minRate, setMinRate] = useState<number>(2.0);
  const [maxRate, setMaxRate] = useState<number>(5.0);
  const [rateIncrement, setRateIncrement] = useState<number>(0.25);
  const [yearIncrement, setYearIncrement] = useState<number>(5);

  const [analysisResult, setAnalysisResult] = useState<SensitivityAnalysisResponse | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleRateTypeChange = (newRateType: SensitivityRateType) => {
    setRateType(newRateType);
    const config = SENSITIVITY_RATE_TYPES.find(rt => rt.value === newRateType);
    if (config) {
      setMinRate(config.defaultMin);
      setMaxRate(config.defaultMax);
    }
    setAnalysisResult(null);
  };

  const runAnalysis = useCallback(async () => {
    if (entries.length === 0) {
      setError('Please add some financial entries before running the analysis.');
      return;
    }

    if (minRate >= maxRate) {
      setError('Minimum rate must be less than maximum rate.');
      return;
    }

    setIsLoading(true);
    setError(null);

    try {
      const request = {
        planData: { persons, entries, rates, statesByYear },
        rateType,
        minRate,
        maxRate,
        rateIncrement,
        yearIncrement,
      };

      const result = await planApi.runSensitivityAnalysis(request);
      setAnalysisResult(result);
    } catch (err: any) {
      setError(err.message || 'Failed to run sensitivity analysis');
    } finally {
      setIsLoading(false);
    }
  }, [persons, entries, rates, rateType, minRate, maxRate, rateIncrement, yearIncrement]);

  const formatCurrency = (value: number): string => {
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: 'USD',
      minimumFractionDigits: 0,
      maximumFractionDigits: 0,
    }).format(value);
  };

  const renderCell = (cell: SensitivityCell): React.ReactNode => {
    if (cell.hasShortfall) {
      return (
        <span className="shortfall-cell" title={`Shortfall in year ${cell.shortfallYear}`}>
          Shortfall ({cell.shortfallYear})
        </span>
      );
    }
    return formatCurrency(cell.netWorth || 0);
  };

  const getCellClass = (cell: SensitivityCell): string => {
    if (cell.hasShortfall) return 'cell-shortfall';
    if (cell.netWorth && cell.netWorth > 0) return 'cell-positive';
    return 'cell-negative';
  };

  const selectedRateTypeConfig = SENSITIVITY_RATE_TYPES.find(rt => rt.value === rateType);

  return (
    <div className="tab-content sensitivity-tab">
      <h2>Rate Sensitivity Analysis</h2>
      <p className="tab-description">
        Analyze how different growth rates affect your net worth over time.
        Select a rate type, set the range, and see the projected net worth at milestone years.
        "Shortfall" indicates a deficit occurred before that milestone.
      </p>

      <div className="sensitivity-inputs">
        <div className="input-group rate-type-group">
          <label htmlFor="rateType">Rate Type</label>
          <select
            id="rateType"
            value={rateType}
            onChange={e => handleRateTypeChange(e.target.value as SensitivityRateType)}
          >
            {SENSITIVITY_RATE_TYPES.map(rt => (
              <option key={rt.value} value={rt.value}>{rt.label}</option>
            ))}
          </select>
        </div>

        <div className="input-group">
          <label htmlFor="minRate">Min Rate (%)</label>
          <input
            id="minRate"
            type="number"
            step="0.25"
            min="0"
            max="20"
            value={minRate}
            onChange={e => setMinRate(parseFloat(e.target.value) || 0)}
          />
        </div>

        <div className="input-group">
          <label htmlFor="maxRate">Max Rate (%)</label>
          <input
            id="maxRate"
            type="number"
            step="0.25"
            min="0"
            max="20"
            value={maxRate}
            onChange={e => setMaxRate(parseFloat(e.target.value) || 0)}
          />
        </div>

        <div className="input-group">
          <label htmlFor="rateIncrement">Rate Increment (%)</label>
          <input
            id="rateIncrement"
            type="number"
            step="0.05"
            min="0.05"
            max="1"
            value={rateIncrement}
            onChange={e => setRateIncrement(parseFloat(e.target.value) || 0.25)}
          />
        </div>

        <div className="input-group">
          <label htmlFor="yearIncrement">Year Increment</label>
          <input
            id="yearIncrement"
            type="number"
            step="1"
            min="1"
            max="10"
            value={yearIncrement}
            onChange={e => setYearIncrement(parseInt(e.target.value) || 5)}
          />
        </div>

        <button
          onClick={runAnalysis}
          disabled={isLoading}
          className="run-analysis-btn"
        >
          {isLoading ? 'Analyzing...' : 'Run Analysis'}
        </button>
      </div>

      {error && <div className="error-message">{error}</div>}

      {analysisResult && (
        <div className="sensitivity-results">
          <h3>{analysisResult.rateTypeLabel} Analysis (First Data Year: {analysisResult.firstDataYear})</h3>
          <div className="table-container">
            <table className="sensitivity-table">
              <thead>
                <tr>
                  <th className="rate-header">{selectedRateTypeConfig?.label || 'Rate'}</th>
                  {analysisResult.milestoneYears.map(year => (
                    <th key={year} className="year-header">
                      <div className="year-label">{year}</div>
                      <div className="year-offset">
                        (+{year - analysisResult.firstDataYear} yrs)
                      </div>
                    </th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {analysisResult.rows.map((row: SensitivityRow) => (
                  <tr key={row.rate}>
                    <td className="rate-cell">{row.rate.toFixed(2)}%</td>
                    {row.cells.map((cell: SensitivityCell) => (
                      <td key={cell.year} className={getCellClass(cell)}>
                        {renderCell(cell)}
                      </td>
                    ))}
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          <div className="legend">
            <div className="legend-item">
              <span className="legend-color positive"></span>
              <span>Positive Net Worth</span>
            </div>
            <div className="legend-item">
              <span className="legend-color shortfall"></span>
              <span>Shortfall (Deficit occurred)</span>
            </div>
          </div>
        </div>
      )}

      {!analysisResult && !isLoading && (
        <div className="empty-message">
          Select a rate type, configure the range, and click "Run Analysis" to see results.
        </div>
      )}
    </div>
  );
};

export default SensitivityTab;
