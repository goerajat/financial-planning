import React from 'react';
import { usePlan } from '../context/PlanContext';
import { ITEM_TYPES } from '../types';
import './Tab.css';

const RatesTab: React.FC = () => {
  const { rates, rateConfigs, updateRate } = usePlan();

  const getDescription = (itemType: string): string => {
    const config = rateConfigs.find(c => c.itemType === itemType);
    return config?.description || '';
  };

  const getDisplayName = (itemType: string): string => {
    const config = rateConfigs.find(c => c.itemType === itemType);
    if (config) return config.displayName;
    return ITEM_TYPES.find(t => t.value === itemType)?.label || itemType;
  };

  // Entry types in their usual order, then rate-only keys the server knows (medical inflation).
  const rateKeys = [
    ...ITEM_TYPES.map(t => t.value),
    ...rateConfigs.map(c => c.itemType).filter(k => !ITEM_TYPES.some(t => t.value === k)),
  ];

  return (
    <div className="tab-content">
      <h2>Growth & Interest Rates</h2>
      <p className="tab-description">
        Configure the annual percentage rates used for projections.
      </p>

      <table className="data-table rates-table">
        <thead>
          <tr>
            <th>Item Type</th>
            <th>Rate (%)</th>
            <th>Description</th>
          </tr>
        </thead>
        <tbody>
          {rateKeys.map(key => (
            <tr key={key}>
              <td data-label="Item type" className="cell-primary">{getDisplayName(key)}</td>
              <td data-label="Rate (%)">
                <input
                  type="number"
                  step="0.1"
                  inputMode="decimal"
                  aria-label={`${getDisplayName(key)} rate, percent per year`}
                  value={rates[key] ?? 0}
                  onChange={e => updateRate(key, parseFloat(e.target.value) || 0)}
                  className="rate-input"
                />
              </td>
              <td data-label="Description" className="description-cell">{getDescription(key)}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
};

export default RatesTab;
