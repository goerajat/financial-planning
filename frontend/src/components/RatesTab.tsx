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
          {ITEM_TYPES.map(type => (
            <tr key={type.value}>
              <td>{getDisplayName(type.value)}</td>
              <td>
                <input
                  type="number"
                  step="0.1"
                  value={rates[type.value] ?? 0}
                  onChange={e => updateRate(type.value, parseFloat(e.target.value) || 0)}
                  className="rate-input"
                />
              </td>
              <td className="description-cell">{getDescription(type.value)}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
};

export default RatesTab;
