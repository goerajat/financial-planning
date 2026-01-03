import React, { useState, useEffect } from 'react';
import { usePlan } from '../context/PlanContext';
import './Tab.css';

const ResultsTab: React.FC = () => {
  const { pdfBlob, isLoading, error, generatePdf } = usePlan();
  const [pdfUrl, setPdfUrl] = useState<string | null>(null);
  const [zoom, setZoom] = useState(100);

  useEffect(() => {
    if (pdfBlob) {
      const url = URL.createObjectURL(pdfBlob);
      setPdfUrl(url);
      return () => URL.revokeObjectURL(url);
    }
  }, [pdfBlob]);

  const handleDownload = () => {
    if (pdfUrl) {
      const a = document.createElement('a');
      a.href = pdfUrl;
      a.download = 'financial_plan.pdf';
      a.click();
    }
  };

  const handleZoomIn = () => setZoom(prev => Math.min(prev + 25, 200));
  const handleZoomOut = () => setZoom(prev => Math.max(prev - 25, 50));

  return (
    <div className="tab-content results-tab">
      <div className="results-header">
        <h2>Financial Plan Results</h2>
        <div className="results-actions">
          <button onClick={generatePdf} disabled={isLoading}>
            {isLoading ? 'Generating...' : 'Generate Plan'}
          </button>
          {pdfUrl && (
            <>
              <button onClick={handleDownload}>Download PDF</button>
              <div className="zoom-controls">
                <button onClick={handleZoomOut}>-</button>
                <span>{zoom}%</span>
                <button onClick={handleZoomIn}>+</button>
              </div>
            </>
          )}
        </div>
      </div>

      {error && <div className="error-message">{error}</div>}

      <div className="pdf-container">
        {isLoading && <div className="loading">Generating PDF...</div>}
        {!isLoading && !pdfUrl && (
          <div className="empty-message">
            Click "Generate Plan" to create your financial plan PDF.
          </div>
        )}
        {pdfUrl && (
          <iframe
            src={pdfUrl}
            title="Financial Plan PDF"
            style={{
              width: '100%',
              height: '800px',
              transform: `scale(${zoom / 100})`,
              transformOrigin: 'top left',
            }}
          />
        )}
      </div>
    </div>
  );
};

export default ResultsTab;
