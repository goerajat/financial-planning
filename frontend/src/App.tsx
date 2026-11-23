import React, { useState, useRef, useEffect } from 'react';
import { PlanProvider, usePlan } from './context/PlanContext';
import IntroTab from './components/IntroTab';
import PersonsTab from './components/PersonsTab';
import EntriesTab from './components/EntriesTab';
import RatesTab from './components/RatesTab';
import StatesTab from './components/StatesTab';
import ResultsTab from './components/ResultsTab';
import SensitivityTab from './components/SensitivityTab';
import RothComparisonTab from './components/RothComparisonTab';
import StateScenarioTab from './components/StateScenarioTab';
import './App.css';

type TabName = 'intro' | 'persons' | 'entries' | 'rates' | 'states' | 'results' | 'sensitivity' | 'roth-comparison' | 'state-scenario';

const AppContent: React.FC = () => {
  const [activeTab, setActiveTab] = useState<TabName>('intro');
  const { exportPlan, importPlan, isLoading, saveStatus, saveError, statesByYear, addStateByYear, updateStateByYear, deleteStateByYear } = usePlan();
  const fileInputRef = useRef<HTMLInputElement>(null);
  const navRef = useRef<HTMLElement>(null);

  // On a phone the tab strip scrolls; keep the active tab in view so the strip
  // never shows a selection the visitor cannot see.
  useEffect(() => {
    const active = navRef.current?.querySelector<HTMLButtonElement>('.tab-button.active');
    active?.scrollIntoView({ block: 'nearest', inline: 'center', behavior: 'smooth' });
  }, [activeTab]);

  const handleLoad = () => {
    fileInputRef.current?.click();
  };

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) {
      importPlan(file);
      e.target.value = '';
    }
  };

  const tabs: { id: TabName; label: string }[] = [
    { id: 'intro', label: 'Getting Started' },
    { id: 'persons', label: 'Persons' },
    { id: 'entries', label: 'Entries' },
    { id: 'rates', label: 'Rates' },
    { id: 'states', label: 'States' },
    { id: 'results', label: 'Results' },
    { id: 'sensitivity', label: 'Sensitivity' },
    { id: 'roth-comparison', label: 'Roth Comparison' },
    { id: 'state-scenario', label: 'State Scenarios' },
  ];

  return (
    <div className="app">
      <header className="app-header">
        <h1>Financial Planner</h1>
        <div className="header-actions">
          <span className={`save-status save-status-${saveStatus}`} role="status" title={saveError || undefined}>
            {saveStatus === 'loading' && 'Loading'}
            {saveStatus === 'saving' && 'Saving'}
            {saveStatus === 'saved' && 'Saved'}
            {saveStatus === 'error' && (saveError || 'Not saved')}
          </span>
          <button onClick={exportPlan} title="Download the plan as a JSON file">Export</button>
          <button onClick={handleLoad} disabled={isLoading} title="Load a JSON file into the stored plan">Import</button>
          <input
            ref={fileInputRef}
            type="file"
            accept=".json"
            aria-label="Import a plan file"
            onChange={handleFileChange}
            style={{ display: 'none' }}
          />
        </div>
      </header>

      <nav className="tab-nav" ref={navRef} role="tablist" aria-label="Plan sections">
        {tabs.map(tab => (
          <button
            key={tab.id}
            role="tab"
            aria-selected={activeTab === tab.id}
            className={`tab-button ${activeTab === tab.id ? 'active' : ''}`}
            onClick={() => setActiveTab(tab.id)}
          >
            {tab.label}
          </button>
        ))}
      </nav>

      <main className="main-content">
        {activeTab === 'intro' && <IntroTab />}
        {activeTab === 'persons' && <PersonsTab />}
        {activeTab === 'entries' && <EntriesTab />}
        {activeTab === 'rates' && <RatesTab />}
        {activeTab === 'states' && (
          <StatesTab
            statesByYear={statesByYear}
            onAdd={addStateByYear}
            onUpdate={updateStateByYear}
            onDelete={deleteStateByYear}
          />
        )}
        {activeTab === 'results' && <ResultsTab />}
        {activeTab === 'sensitivity' && <SensitivityTab />}
        {activeTab === 'roth-comparison' && <RothComparisonTab />}
        {activeTab === 'state-scenario' && <StateScenarioTab />}
      </main>
    </div>
  );
};

function App() {
  return (
    <PlanProvider>
      <AppContent />
    </PlanProvider>
  );
}

export default App;
