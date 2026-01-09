import React, { useState } from 'react';
import { usePlan } from '../context/PlanContext';
import { planApi } from '../services/api';
import {
  StateScenario,
  StateScenarioResponse,
  StateScenarioCell,
  StateScenarioRow,
  StateByYear,
  State,
  FilingStatus,
  FILING_STATUSES,
  STATES,
} from '../types';
import './StateScenarioTab.css';

const StateScenarioTab: React.FC = () => {
  const { persons, entries, rates } = usePlan();
  const [loading, setLoading] = useState(false);
  const [pdfLoading, setPdfLoading] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [results, setResults] = useState<StateScenarioResponse | null>(null);
  const [filingStatus, setFilingStatus] = useState<FilingStatus>('MARRIED_FILING_JOINTLY');
  const [yearIncrement, setYearIncrement] = useState(5);

  // Scenario management
  const [scenarios, setScenarios] = useState<StateScenario[]>([]);
  const [editingIndex, setEditingIndex] = useState<number | null>(null);
  const [editorName, setEditorName] = useState('');
  const [editorStateRanges, setEditorStateRanges] = useState<StateByYear[]>([]);

  const currentYear = new Date().getFullYear();

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

  const getStateClass = (stateName: string): string => {
    if (stateName.includes('New Jersey') || stateName === 'NJ') return 'state-nj';
    if (stateName.includes('New York') || stateName === 'NY') return 'state-ny';
    if (stateName.includes('Florida') || stateName === 'FL') return 'state-fl';
    return 'state-nj';
  };

  const generateScenarioDescription = (statesByYear: StateByYear[]): string => {
    if (!statesByYear || statesByYear.length === 0) return 'NJ (default)';
    return statesByYear
      .map((s) => `${s.state} ${s.startYear}-${s.endYear}`)
      .join(', ');
  };

  const openNewScenarioEditor = () => {
    setEditingIndex(-1); // -1 indicates new scenario
    setEditorName('');
    setEditorStateRanges([{ state: 'NJ', startYear: currentYear, endYear: currentYear + 25 }]);
  };

  const openEditScenarioEditor = (index: number) => {
    const scenario = scenarios[index];
    setEditingIndex(index);
    setEditorName(scenario.name);
    setEditorStateRanges([...scenario.statesByYear]);
  };

  const closeEditor = () => {
    setEditingIndex(null);
    setEditorName('');
    setEditorStateRanges([]);
  };

  const saveScenario = () => {
    if (!editorName.trim()) {
      setError('Please enter a scenario name');
      return;
    }
    if (editorStateRanges.length === 0) {
      setError('Please add at least one state range');
      return;
    }

    const newScenario: StateScenario = {
      name: editorName.trim(),
      statesByYear: editorStateRanges,
    };

    if (editingIndex === -1) {
      // Adding new scenario
      setScenarios([...scenarios, newScenario]);
    } else if (editingIndex !== null) {
      // Editing existing scenario
      const updated = [...scenarios];
      updated[editingIndex] = newScenario;
      setScenarios(updated);
    }

    closeEditor();
    setError(null);
  };

  const deleteScenario = (index: number) => {
    setScenarios(scenarios.filter((_, i) => i !== index));
  };

  const addStateRange = () => {
    const lastRange = editorStateRanges[editorStateRanges.length - 1];
    const nextStart = lastRange ? lastRange.endYear + 1 : currentYear;
    setEditorStateRanges([
      ...editorStateRanges,
      { state: 'FL', startYear: nextStart, endYear: nextStart + 10 },
    ]);
  };

  const removeStateRange = (index: number) => {
    setEditorStateRanges(editorStateRanges.filter((_, i) => i !== index));
  };

  const updateStateRange = (index: number, field: keyof StateByYear, value: string | number) => {
    const updated = [...editorStateRanges];
    if (field === 'state') {
      updated[index] = { ...updated[index], state: value as State };
    } else {
      updated[index] = { ...updated[index], [field]: Number(value) };
    }
    setEditorStateRanges(updated);
  };

  const runComparison = async () => {
    if (entries.length === 0) {
      setError('Please add financial entries before running comparison.');
      return;
    }

    if (scenarios.length < 2) {
      setError('Please add at least 2 scenarios to compare.');
      return;
    }

    setLoading(true);
    setError(null);

    try {
      const planData = {
        persons,
        entries,
        rates,
      };

      const response = await planApi.runStateScenarioAnalysis({
        planData,
        scenarios,
        yearIncrement,
        filingStatus,
      });

      setResults(response);
    } catch (err) {
      setError('Failed to run state scenario comparison. Please try again.');
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  const handleCellClick = async (row: StateScenarioRow, scenarioIndex: number) => {
    if (pdfLoading) return;

    const scenarioName = row.scenarioName;
    setPdfLoading(scenarioName);
    setError(null);

    try {
      const planData = {
        persons,
        entries,
        rates,
      };

      // Get the statesByYear for this scenario
      const scenarioStatesByYear = scenarios[scenarioIndex]?.statesByYear;

      const pdfBlob = await planApi.generateScenarioPdf({
        planData,
        filingStatus,
        scenarioStatesByYear,
        scenarioName,
      });

      // Download the PDF
      const url = window.URL.createObjectURL(pdfBlob);
      const link = document.createElement('a');
      link.href = url;
      link.download = `financial_plan_${scenarioName.replace(/[^a-zA-Z0-9]/g, '_')}.pdf`;
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      window.URL.revokeObjectURL(url);
    } catch (err) {
      setError('Failed to generate PDF for this scenario. Please try again.');
      console.error(err);
    } finally {
      setPdfLoading(null);
    }
  };

  const renderCell = (cell: StateScenarioCell, isBaseline: boolean): React.ReactNode => {
    if (cell.hasShortfall) {
      return (
        <div className="state-cell shortfall-cell">
          <div className="shortfall-label">Shortfall</div>
          <div className="shortfall-year">Year {cell.shortfallYear}</div>
        </div>
      );
    }

    const deltaClass = cell.netWorthDelta > 0 ? 'positive' : cell.netWorthDelta < 0 ? 'negative' : 'neutral';
    const savingsClass = cell.stateTaxSavings > 0 ? 'positive' : cell.stateTaxSavings < 0 ? 'negative' : 'neutral';

    return (
      <div className="state-cell">
        <div className="net-worth">{formatCurrency(cell.netWorth)}</div>
        {!isBaseline && (
          <div className={`delta ${deltaClass}`}>
            {formatDelta(cell.netWorthDelta)}
          </div>
        )}
        {!isBaseline && cell.stateTaxSavings !== 0 && (
          <div className={`tax-savings ${savingsClass}`}>
            Tax savings: {formatDelta(cell.stateTaxSavings)}
          </div>
        )}
        <span className={`active-state ${getStateClass(cell.activeState)}`}>
          {cell.activeState}
        </span>
      </div>
    );
  };

  return (
    <div className="state-scenario-tab">
      <p className="tab-description">
        Compare different state residence scenarios to see how relocating affects your taxes and net worth.
        Define multiple scenarios with different state sequences and compare their long-term outcomes.
        <strong> Click any row to generate a PDF for that scenario.</strong>
      </p>

      {/* Scenario Builder */}
      <div className="scenario-builder">
        <h4>Scenarios ({scenarios.length}/5)</h4>

        {scenarios.length > 0 && (
          <div className="scenarios-list">
            {scenarios.map((scenario, index) => (
              <div
                key={index}
                className={`scenario-item ${editingIndex === index ? 'editing' : ''}`}
              >
                <div className="scenario-info">
                  <div className="scenario-name">
                    {index + 1}. {scenario.name}
                    {index === 0 && <span style={{ color: '#666', fontSize: '12px' }}> (baseline)</span>}
                  </div>
                  <div className="scenario-description">
                    {generateScenarioDescription(scenario.statesByYear)}
                  </div>
                </div>
                <div className="scenario-actions">
                  <button
                    className="edit-btn"
                    onClick={() => openEditScenarioEditor(index)}
                    disabled={editingIndex !== null}
                  >
                    Edit
                  </button>
                  <button
                    className="delete-btn"
                    onClick={() => deleteScenario(index)}
                    disabled={editingIndex !== null}
                  >
                    Delete
                  </button>
                </div>
              </div>
            ))}
          </div>
        )}

        {scenarios.length < 5 && editingIndex === null && (
          <button className="add-scenario-btn" onClick={openNewScenarioEditor}>
            + Add Scenario
          </button>
        )}

        {scenarios.length < 2 && editingIndex === null && (
          <p className="min-scenarios-message">
            Add at least 2 scenarios to run a comparison.
          </p>
        )}

        {/* Scenario Editor */}
        {editingIndex !== null && (
          <div className="scenario-editor">
            <h5>{editingIndex === -1 ? 'New Scenario' : 'Edit Scenario'}</h5>

            <div className="editor-row">
              <div className="input-group name-group">
                <label>Scenario Name</label>
                <input
                  type="text"
                  value={editorName}
                  onChange={(e) => setEditorName(e.target.value)}
                  placeholder="e.g., Move to FL in 2030"
                />
              </div>
            </div>

            <div className="state-ranges-section">
              <h6>State Residence Periods</h6>
              <div className="state-range-list">
                {editorStateRanges.map((range, index) => (
                  <div key={index} className="state-range-item">
                    <select
                      value={range.state}
                      onChange={(e) => updateStateRange(index, 'state', e.target.value)}
                    >
                      {STATES.map((state) => (
                        <option key={state.value} value={state.value}>
                          {state.label}
                        </option>
                      ))}
                    </select>
                    <span>from</span>
                    <input
                      type="number"
                      value={range.startYear}
                      onChange={(e) => updateStateRange(index, 'startYear', e.target.value)}
                      min={1900}
                      max={2100}
                    />
                    <span>to</span>
                    <input
                      type="number"
                      value={range.endYear}
                      onChange={(e) => updateStateRange(index, 'endYear', e.target.value)}
                      min={1900}
                      max={2100}
                    />
                    {editorStateRanges.length > 1 && (
                      <button
                        className="remove-range-btn"
                        onClick={() => removeStateRange(index)}
                      >
                        Remove
                      </button>
                    )}
                  </div>
                ))}
              </div>
              <button className="add-range-btn" onClick={addStateRange}>
                + Add State Period
              </button>
            </div>

            <div className="editor-buttons">
              <button className="save-btn" onClick={saveScenario}>
                {editingIndex === -1 ? 'Add Scenario' : 'Save Changes'}
              </button>
              <button className="cancel-btn" onClick={closeEditor}>
                Cancel
              </button>
            </div>
          </div>
        )}
      </div>

      {/* Comparison Controls */}
      <div className="comparison-controls">
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
          disabled={loading || entries.length === 0 || scenarios.length < 2}
        >
          {loading ? 'Running...' : 'Run Comparison'}
        </button>
      </div>

      {error && <div className="error-message">{error}</div>}

      {pdfLoading && (
        <div className="pdf-loading-message">
          Generating PDF for "{pdfLoading}"...
        </div>
      )}

      {/* Results */}
      {results && (
        <div className="comparison-results">
          <h3>State Residence Scenario Comparison</h3>
          <div className="table-container">
            <table className="comparison-table">
              <thead>
                <tr>
                  <th className="scenario-header">Scenario</th>
                  {results.milestoneYears.map((year) => (
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
                    <td
                      className="scenario-cell clickable-cell"
                      onClick={() => handleCellClick(row, rowIndex)}
                      title="Click to generate PDF for this scenario"
                    >
                      <div className="scenario-name-cell">{row.scenarioName}</div>
                      <div className="scenario-desc-cell">{row.scenarioDescription}</div>
                    </td>
                    {row.cells.map((cell) => (
                      <td
                        key={cell.year}
                        className={`data-cell clickable-cell ${
                          cell.hasShortfall
                            ? 'cell-shortfall'
                            : cell.netWorthDelta > 0
                            ? 'cell-positive'
                            : cell.netWorthDelta < 0
                            ? 'cell-negative'
                            : ''
                        }`}
                        onClick={() => handleCellClick(row, rowIndex)}
                        title="Click to generate PDF for this scenario"
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
              <span>Baseline (First Scenario)</span>
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
              <li><strong>Net Worth:</strong> Total assets minus liabilities at each milestone year.</li>
              <li><strong>Delta:</strong> Difference in net worth compared to the first scenario (baseline).</li>
              <li><strong>Tax Savings:</strong> Cumulative state tax savings compared to baseline.</li>
              <li><strong>State Badge:</strong> Shows which state you'd be residing in at each milestone.</li>
              <li><strong>Florida (FL):</strong> Has no state income tax, which can lead to significant savings.</li>
              <li><strong>New Jersey (NJ) & New York (NY):</strong> Have progressive state income taxes.</li>
            </ul>
          </div>
        </div>
      )}

      {!results && !loading && scenarios.length >= 2 && (
        <div className="empty-message">
          Click "Run Comparison" to analyze state residence scenarios.
        </div>
      )}
    </div>
  );
};

export default StateScenarioTab;
