import React, { useState } from 'react';
import { usePlan } from '../context/PlanContext';
import { FinancialEntry, ITEM_TYPES, RefreshReport } from '../types';
import './Tab.css';

const isAggregatorRow = (entry: FinancialEntry) => !!entry.source && entry.source.startsWith('aggregator:');

const dollars = (v: number) => `$${Math.round(v).toLocaleString()}`;

const EntriesTab: React.FC = () => {
  const { entries, persons, isLoading, addEntry, updateEntry, deleteEntry, refreshBalances } = usePlan();
  const [editId, setEditId] = useState<number | null>(null);
  const [confirmId, setConfirmId] = useState<number | null>(null);
  const [filterType, setFilterType] = useState<string>('ALL');
  const [report, setReport] = useState<RefreshReport | null>(null);
  const currentYear = new Date().getFullYear();

  const emptyForm = (): FinancialEntry => ({
    personName: persons.length > 0 ? persons[0].name : '',
    itemType: 'INCOME',
    description: '',
    value: 0,
    startYear: currentYear,
    endYear: currentYear + 30,
    annualPayment: 0,
  });

  const [formData, setFormData] = useState<FinancialEntry>({
    personName: '',
    itemType: 'INCOME',
    description: '',
    value: 0,
    startYear: currentYear,
    endYear: currentYear + 30,
    annualPayment: 0,
  });

  const isMortgage = formData.itemType === 'MORTGAGE';
  const monthlyPayment = (formData.annualPayment || 0) / 12;

  const filteredEntries = filterType === 'ALL'
    ? entries
    : entries.filter(e => e.itemType === filterType);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    const entry = isMortgage ? formData : { ...formData, annualPayment: 0 };
    if (editId !== null) {
      updateEntry(editId, entry);
      setEditId(null);
    } else {
      addEntry(entry);
    }
    setFormData(emptyForm());
  };

  const handleEdit = (entry: FinancialEntry) => {
    setEditId(entry.id!);
    setFormData(entry);
  };

  const handleCancel = () => {
    setEditId(null);
    setFormData(emptyForm());
  };

  const handleRefresh = async () => {
    const result = await refreshBalances();
    setReport(result);
  };

  const getItemTypeLabel = (value: string) => {
    return ITEM_TYPES.find(t => t.value === value)?.label || value;
  };

  const aggregatorCount = entries.filter(isAggregatorRow).length;
  const staleRows = report ? report.imported.filter(r => r.stale) : [];

  return (
    <div className="tab-content">
      <h2>Financial Entries</h2>

      <div className="refresh-row">
        <button type="button" onClick={handleRefresh} disabled={isLoading}>
          {isLoading ? 'Refreshing...' : 'Refresh balances from aggregator'}
        </button>
        <span className="muted">
          {aggregatorCount > 0
            ? `${aggregatorCount} ${aggregatorCount === 1 ? 'entry comes' : 'entries come'} from the aggregator`
            : 'No aggregator entries yet'}
        </span>
      </div>

      {report && (
        <div className="refresh-report">
          <h3>
            Refreshed {report.imported.length} {report.imported.length === 1 ? 'entry' : 'entries'} as {report.year} balances
            <button type="button" style={{ marginLeft: 12 }} onClick={() => setReport(null)}>Dismiss</button>
          </h3>
          <p className="muted">
            Each balance is grown at the plan rate for its bucket from its capture date to 31 December {report.year},
            so the value is the projected start-of-{report.year + 1} balance.
          </p>
          {staleRows.length > 0 && (
            <div className="warn">
              <strong>{staleRows.length} stale {staleRows.length === 1 ? 'capture' : 'captures'}:</strong>
              <ul>
                {staleRows.map(r => (
                  <li key={`${r.accountId}-${r.personName}`}>{r.description}</li>
                ))}
              </ul>
            </div>
          )}
          {report.unknownPersons.length > 0 && (
            <div className="warn">
              <strong>Persons not in this plan:</strong> {report.unknownPersons.join(', ')}.
              Add them on the Persons tab or fix the persons map in the aggregator config.
            </div>
          )}
          {report.handEnteredAssetRows.length > 0 && (
            <div className="warn">
              <strong>Hand-entered asset rows still in the plan</strong> (they add to the refreshed balances, so delete
              any that the aggregator now covers):
              <ul>
                {report.handEnteredAssetRows.map(e => (
                  <li key={e.id}>{e.personName}: {getItemTypeLabel(e.itemType)} / {e.description} / ${e.value.toLocaleString()}</li>
                ))}
              </ul>
            </div>
          )}
          {report.skipped.length > 0 && (
            <div>
              <strong>Skipped {report.skipped.length}:</strong>
              <ul>
                {report.skipped.map((s, i) => (
                  <li key={i}>{s.account}: <span className="muted">{s.reason}</span></li>
                ))}
              </ul>
            </div>
          )}
        </div>
      )}

      <div className="filter-row">
        <label>Filter by Type: </label>
        <select value={filterType} onChange={e => setFilterType(e.target.value)}>
          <option value="ALL">All Types</option>
          {ITEM_TYPES.map(type => (
            <option key={type.value} value={type.value}>{type.label}</option>
          ))}
        </select>
      </div>

      <form onSubmit={handleSubmit} className="form-grid">
        <div className="form-group">
          <label>Person</label>
          <select
            value={formData.personName}
            onChange={e => setFormData({ ...formData, personName: e.target.value })}
            required
          >
            <option value="">Select Person</option>
            {persons.map(p => (
              <option key={p.name} value={p.name}>{p.name}</option>
            ))}
          </select>
        </div>

        <div className="form-group">
          <label>Type</label>
          <select
            value={formData.itemType}
            onChange={e => setFormData({ ...formData, itemType: e.target.value })}
            required
          >
            {ITEM_TYPES.map(type => (
              <option key={type.value} value={type.value}>{type.label}</option>
            ))}
          </select>
        </div>

        <div className="form-group">
          <label>Description</label>
          <input
            type="text"
            value={formData.description}
            onChange={e => setFormData({ ...formData, description: e.target.value })}
            required
          />
        </div>

        <div className="form-group">
          <label>{isMortgage ? 'Outstanding balance' : 'Value'}</label>
          <input
            type="number"
            value={formData.value}
            onChange={e => setFormData({ ...formData, value: parseInt(e.target.value) || 0 })}
            required
          />
        </div>

        {isMortgage && (
          <div className="form-group">
            <label>Annual payment</label>
            <input
              type="number"
              min={0}
              step={1}
              value={formData.annualPayment || 0}
              onChange={e => setFormData({ ...formData, annualPayment: Math.max(0, parseFloat(e.target.value) || 0) })}
              required
            />
            <span className="form-hint">
              {monthlyPayment > 0 ? `${dollars(monthlyPayment)} a month. ` : ''}
              Principal plus interest; interest uses the Mortgage rate on the Rates tab.
            </span>
          </div>
        )}

        <div className="form-group">
          <label>Start Year</label>
          <input
            type="number"
            value={formData.startYear}
            onChange={e => setFormData({ ...formData, startYear: parseInt(e.target.value) })}
            required
          />
        </div>

        <div className="form-group">
          <label>End Year</label>
          <input
            type="number"
            value={formData.endYear}
            onChange={e => setFormData({ ...formData, endYear: parseInt(e.target.value) })}
            required
          />
        </div>

        <div className="form-actions">
          <button type="submit">{editId !== null ? 'Update' : 'Add Entry'}</button>
          {editId !== null && (
            <button type="button" onClick={handleCancel}>Cancel</button>
          )}
        </div>
      </form>

      <table className="data-table">
        <thead>
          <tr>
            <th>Person</th>
            <th>Type</th>
            <th>Description</th>
            <th>Value</th>
            <th>Start</th>
            <th>End</th>
            <th>Actions</th>
          </tr>
        </thead>
        <tbody>
          {filteredEntries.map(entry => {
            const fromAggregator = isAggregatorRow(entry);
            return (
              <tr key={entry.id}>
                <td data-label="Person" className="cell-primary">{entry.personName}</td>
                <td data-label="Type">{getItemTypeLabel(entry.itemType)}</td>
                <td data-label="Description">
                  {fromAggregator && <span className="source-badge" title="Refreshed from the finance aggregator; the next refresh replaces this row">AGG</span>}
                  {entry.description}
                </td>
                <td data-label="Value">
                  ${entry.value.toLocaleString()}
                  {entry.itemType === 'MORTGAGE' && (
                    <span className="muted"> · {entry.annualPayment ? `${dollars(entry.annualPayment)}/yr` : 'no payment set'}</span>
                  )}
                </td>
                <td data-label="Start">{entry.startYear}</td>
                <td data-label="End">{entry.endYear}</td>
                <td className="row-actions-cell">
                  {confirmId === entry.id ? (
                    <div className="row-actions row-confirm" role="group" aria-label="Confirm deleting this entry">
                      <span className="confirm-text">Delete this entry?</span>
                      <button type="button" className="danger" onClick={() => { deleteEntry(entry.id!); setConfirmId(null); }}>Delete</button>
                      <button type="button" onClick={() => setConfirmId(null)} autoFocus>Keep</button>
                    </div>
                  ) : (
                    <div className="row-actions">
                      <button
                        type="button"
                        onClick={() => handleEdit(entry)}
                        disabled={fromAggregator}
                        title={fromAggregator ? 'Aggregator rows are replaced on each refresh; change the mapping in aggregator.json instead' : undefined}
                      >
                        Edit
                      </button>
                      <button type="button" className="danger-outline" onClick={() => setConfirmId(entry.id!)}>Delete</button>
                    </div>
                  )}
                </td>
              </tr>
            );
          })}
          {filteredEntries.length === 0 && (
            <tr>
              <td colSpan={7} className="empty-message">No entries found</td>
            </tr>
          )}
        </tbody>
      </table>
    </div>
  );
};

export default EntriesTab;
