import React, { useState, useRef } from 'react';
import { PlanProvider, usePlan } from './context/PlanContext';
import PersonsTab from './components/PersonsTab';
import EntriesTab from './components/EntriesTab';
import RatesTab from './components/RatesTab';
import ResultsTab from './components/ResultsTab';
import './App.css';

type TabName = 'persons' | 'entries' | 'rates' | 'results';

const AppContent: React.FC = () => {
  const [activeTab, setActiveTab] = useState<TabName>('persons');
  const { savePlan, loadPlan, isLoading } = usePlan();
  const fileInputRef = useRef<HTMLInputElement>(null);

  const handleLoad = () => {
    fileInputRef.current?.click();
  };

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) {
      loadPlan(file);
      e.target.value = '';
    }
  };

  const tabs: { id: TabName; label: string }[] = [
    { id: 'persons', label: 'Persons' },
    { id: 'entries', label: 'Entries' },
    { id: 'rates', label: 'Rates' },
    { id: 'results', label: 'Results' },
  ];

  return (
    <div className="app">
      <header className="app-header">
        <h1>Financial Planner</h1>
        <div className="header-actions">
          <button onClick={savePlan}>Save Plan</button>
          <button onClick={handleLoad} disabled={isLoading}>Load Plan</button>
          <input
            ref={fileInputRef}
            type="file"
            accept=".json"
            onChange={handleFileChange}
            style={{ display: 'none' }}
          />
        </div>
      </header>

      <nav className="tab-nav">
        {tabs.map(tab => (
          <button
            key={tab.id}
            className={`tab-button ${activeTab === tab.id ? 'active' : ''}`}
            onClick={() => setActiveTab(tab.id)}
          >
            {tab.label}
          </button>
        ))}
      </nav>

      <main className="main-content">
        {activeTab === 'persons' && <PersonsTab />}
        {activeTab === 'entries' && <EntriesTab />}
        {activeTab === 'rates' && <RatesTab />}
        {activeTab === 'results' && <ResultsTab />}
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
