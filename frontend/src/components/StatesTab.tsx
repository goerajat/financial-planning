import React, { useState } from 'react';
import { StateByYear, State, STATES } from '../types';
import './Tab.css';

interface StatesTabProps {
  statesByYear: StateByYear[];
  onAdd: (state: StateByYear) => void;
  onUpdate: (index: number, state: StateByYear) => void;
  onDelete: (index: number) => void;
}

const StatesTab: React.FC<StatesTabProps> = ({ statesByYear, onAdd, onUpdate, onDelete }) => {
  const [editingIndex, setEditingIndex] = useState<number | null>(null);
  const [formData, setFormData] = useState<StateByYear>({
    state: 'NJ',
    startYear: new Date().getFullYear(),
    endYear: new Date().getFullYear() + 10,
  });

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();

    if (formData.startYear > formData.endYear) {
      alert('Start year must be before or equal to end year');
      return;
    }

    if (editingIndex !== null) {
      onUpdate(editingIndex, formData);
      setEditingIndex(null);
    } else {
      onAdd(formData);
    }

    // Reset form
    setFormData({
      state: 'NJ',
      startYear: new Date().getFullYear(),
      endYear: new Date().getFullYear() + 10,
    });
  };

  const handleEdit = (index: number) => {
    setFormData(statesByYear[index]);
    setEditingIndex(index);
  };

  const handleCancelEdit = () => {
    setEditingIndex(null);
    setFormData({
      state: 'NJ',
      startYear: new Date().getFullYear(),
      endYear: new Date().getFullYear() + 10,
    });
  };

  const handleDelete = (index: number) => {
    if (window.confirm('Are you sure you want to delete this state configuration?')) {
      onDelete(index);
      if (editingIndex === index) {
        handleCancelEdit();
      }
    }
  };

  return (
    <div className="tab-content">
      <h2>State Tax Configuration</h2>
      <p className="tab-description">
        Configure which state's tax laws apply for different years. This is useful if you plan to move
        to a different state during retirement. If no configuration is provided, New Jersey tax rates
        will be used by default.
      </p>

      <form onSubmit={handleSubmit} className="input-form">
        <div className="form-row">
          <div className="input-group">
            <label htmlFor="state">State</label>
            <select
              id="state"
              value={formData.state}
              onChange={e => setFormData({ ...formData, state: e.target.value as State })}
              required
            >
              {STATES.map(state => (
                <option key={state.value} value={state.value}>
                  {state.label}
                </option>
              ))}
            </select>
          </div>

          <div className="input-group">
            <label htmlFor="startYear">Start Year</label>
            <input
              id="startYear"
              type="number"
              value={formData.startYear}
              onChange={e => setFormData({ ...formData, startYear: parseInt(e.target.value) || 0 })}
              required
              min="1900"
              max="2100"
            />
          </div>

          <div className="input-group">
            <label htmlFor="endYear">End Year</label>
            <input
              id="endYear"
              type="number"
              value={formData.endYear}
              onChange={e => setFormData({ ...formData, endYear: parseInt(e.target.value) || 0 })}
              required
              min="1900"
              max="2100"
            />
          </div>

          <div className="button-group">
            {editingIndex !== null ? (
              <>
                <button type="submit" className="btn-primary">Update</button>
                <button type="button" onClick={handleCancelEdit} className="btn-secondary">
                  Cancel
                </button>
              </>
            ) : (
              <button type="submit" className="btn-primary">Add State</button>
            )}
          </div>
        </div>
      </form>

      <div className="table-container">
        <table className="data-table">
          <thead>
            <tr>
              <th>State</th>
              <th>Start Year</th>
              <th>End Year</th>
              <th>Years</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {statesByYear.length === 0 ? (
              <tr>
                <td colSpan={5} className="empty-message">
                  No state configurations. New Jersey will be used as default.
                </td>
              </tr>
            ) : (
              statesByYear.map((state, index) => {
                const stateInfo = STATES.find(s => s.value === state.state);
                const years = state.endYear - state.startYear + 1;
                return (
                  <tr key={index} className={editingIndex === index ? 'editing' : ''}>
                    <td>{stateInfo?.label || state.state}</td>
                    <td>{state.startYear}</td>
                    <td>{state.endYear}</td>
                    <td>{years} year{years !== 1 ? 's' : ''}</td>
                    <td>
                      <button
                        onClick={() => handleEdit(index)}
                        className="btn-edit"
                        disabled={editingIndex !== null && editingIndex !== index}
                      >
                        Edit
                      </button>
                      <button
                        onClick={() => handleDelete(index)}
                        className="btn-delete"
                        disabled={editingIndex !== null && editingIndex !== index}
                      >
                        Delete
                      </button>
                    </td>
                  </tr>
                );
              })
            )}
          </tbody>
        </table>
      </div>

      {statesByYear.length > 0 && (
        <div className="info-box">
          <h4>Tax Rate Information</h4>
          <ul>
            <li><strong>New Jersey:</strong> Progressive state income tax (1.4% - 10.75%). Social Security benefits are not taxable.</li>
            <li><strong>New York:</strong> Progressive state income tax (4% - 10.9%). Social Security benefits are not taxable.</li>
            <li><strong>Florida:</strong> No state income tax (0%).</li>
          </ul>
          <p className="note">
            <strong>Note:</strong> If year ranges overlap, the first matching configuration will be used.
            Ensure your configurations don't overlap for predictable results.
          </p>
        </div>
      )}
    </div>
  );
};

export default StatesTab;
