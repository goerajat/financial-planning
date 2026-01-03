import React, { useState } from 'react';
import { usePlan } from '../context/PlanContext';
import { FinancialEntry, ITEM_TYPES } from '../types';
import './Tab.css';

const EntriesTab: React.FC = () => {
  const { entries, persons, addEntry, updateEntry, deleteEntry } = usePlan();
  const [editId, setEditId] = useState<number | null>(null);
  const [filterType, setFilterType] = useState<string>('ALL');
  const currentYear = new Date().getFullYear();

  const [formData, setFormData] = useState<FinancialEntry>({
    personName: '',
    itemType: 'INCOME',
    description: '',
    value: 0,
    startYear: currentYear,
    endYear: currentYear + 30,
  });

  const filteredEntries = filterType === 'ALL'
    ? entries
    : entries.filter(e => e.itemType === filterType);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (editId !== null) {
      updateEntry(editId, formData);
      setEditId(null);
    } else {
      addEntry(formData);
    }
    setFormData({
      personName: persons.length > 0 ? persons[0].name : '',
      itemType: 'INCOME',
      description: '',
      value: 0,
      startYear: currentYear,
      endYear: currentYear + 30,
    });
  };

  const handleEdit = (entry: FinancialEntry) => {
    setEditId(entry.id!);
    setFormData(entry);
  };

  const handleCancel = () => {
    setEditId(null);
    setFormData({
      personName: persons.length > 0 ? persons[0].name : '',
      itemType: 'INCOME',
      description: '',
      value: 0,
      startYear: currentYear,
      endYear: currentYear + 30,
    });
  };

  const getItemTypeLabel = (value: string) => {
    return ITEM_TYPES.find(t => t.value === value)?.label || value;
  };

  return (
    <div className="tab-content">
      <h2>Financial Entries</h2>

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
          <label>Value</label>
          <input
            type="number"
            value={formData.value}
            onChange={e => setFormData({ ...formData, value: parseInt(e.target.value) || 0 })}
            required
          />
        </div>

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
          {filteredEntries.map(entry => (
            <tr key={entry.id}>
              <td>{entry.personName}</td>
              <td>{getItemTypeLabel(entry.itemType)}</td>
              <td>{entry.description}</td>
              <td>${entry.value.toLocaleString()}</td>
              <td>{entry.startYear}</td>
              <td>{entry.endYear}</td>
              <td>
                <button onClick={() => handleEdit(entry)}>Edit</button>
                <button onClick={() => deleteEntry(entry.id!)}>Delete</button>
              </td>
            </tr>
          ))}
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
