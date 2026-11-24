import React from 'react';
import { useExplain } from '../context/ExplainContext';

/** Opens the question panel; the three results tabs carry one. */
const AskPlanButton: React.FC = () => {
  const { open } = useExplain();
  return (
    <button type="button" className="ask-plan" onClick={open}>
      <svg width="16" height="16" viewBox="0 0 16 16" aria-hidden="true" focusable="false">
        <path d="M2.5 3.5h11v7h-6l-3 2.5v-2.5h-2z" fill="none" stroke="currentColor" strokeWidth="1.4" strokeLinejoin="round" />
        <path d="M5.5 7h5" stroke="currentColor" strokeWidth="1.4" strokeLinecap="round" />
      </svg>
      Ask about this plan
    </button>
  );
};

export default AskPlanButton;
