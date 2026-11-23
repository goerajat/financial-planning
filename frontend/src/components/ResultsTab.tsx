import React, { useState, useEffect } from 'react';
import { usePlan } from '../context/PlanContext';
import { YearSummary } from '../types';
import './Tab.css';

const thousands = (v: number) => {
  const k = Math.round(v / 1000);
  return k === 0 ? '0' : k.toLocaleString();
};

const money = (v: number) => {
  if (Math.abs(v) >= 1_000_000) return `$${(v / 1_000_000).toFixed(2)}M`;
  return `$${Math.round(v / 1000).toLocaleString()}k`;
};

interface Column {
  key: string;
  label: string;
  value: (r: YearSummary) => number;
  /** A part of the column before it, shown muted and left out of the group total. */
  ofWhich?: boolean;
}

interface ColumnGroup {
  id: 'assets' | 'liabilities' | 'inflows' | 'outflows';
  label: string;
  period: string;
  totalLabel: string;
  columns: Column[];
}

const FOLDABLE_IDS = ['assets', 'inflows', 'outflows'];
const PHONE_QUERY = '(max-width: 640px)';

const isPhone = () => typeof window !== 'undefined' && window.matchMedia(PHONE_QUERY).matches;

/**
 * Columns grouped the way the engine computes them: balances at year end, then the
 * year's inflows and outflows. Income entries are wages; Social Security, withdrawals,
 * health premiums, mortgage payments and taxes are all separate figures, so none of
 * them hides inside another column.
 */
function buildGroups(rows: YearSummary[]): ColumnGroup[] {
  const hasMortgage = rows.some(r => r.mortgageBalance > 0 || r.mortgagePayment > 0);
  const hasOther = rows.some(r => r.realEstate + r.lifeInsuranceCash + r.lifeInsuranceBenefits > 0);
  const hasHealth = rows.some(r => r.healthPremiums > 0);
  const hasIrmaa = rows.some(r => r.irmaaSurcharge > 0);
  const hasSocialSecurity = rows.some(r => r.totalSocialSecurity > 0);

  const assets: Column[] = [
    { key: 'qualified', label: 'Qualified', value: r => r.qualifiedAssets },
    { key: 'nonQualified', label: 'Non-qualified', value: r => r.nonQualifiedAssets },
    { key: 'roth', label: 'Roth', value: r => r.rothAssets },
    { key: 'cash', label: 'Cash', value: r => r.cash },
  ];
  if (hasOther) assets.push({ key: 'other', label: 'Other', value: r => r.realEstate + r.lifeInsuranceCash + r.lifeInsuranceBenefits });

  const inflows: Column[] = [{ key: 'wages', label: 'Wages', value: r => r.totalIncome }];
  if (hasSocialSecurity) inflows.push({ key: 'socialSecurity', label: 'Social Security', value: r => r.totalSocialSecurity });
  inflows.push({ key: 'withdrawals', label: 'Withdrawals', value: r => r.totalWithdrawals });
  inflows.push({ key: 'rmd', label: 'of which RMD', value: r => r.rmdWithdrawals, ofWhich: true });

  const outflows: Column[] = [
    { key: 'living', label: 'Living', value: r => r.totalExpenses },
    { key: 'taxes', label: 'Taxes', value: r => r.totalTaxes },
  ];
  if (hasHealth) outflows.push({ key: 'health', label: 'Health', value: r => r.healthPremiums });
  if (hasIrmaa) outflows.push({ key: 'irmaa', label: 'of which IRMAA', value: r => r.irmaaSurcharge, ofWhich: true });
  if (hasMortgage) {
    outflows.push({ key: 'mortgageInterest', label: 'Mtg interest', value: r => r.mortgageInterest });
    outflows.push({ key: 'mortgagePrincipal', label: 'Mtg principal', value: r => r.mortgagePrincipal + r.mortgageRepayment });
  }

  const groups: ColumnGroup[] = [
    { id: 'assets', label: 'Assets', period: 'end of year', totalLabel: 'Total assets', columns: assets },
  ];
  if (hasMortgage) {
    groups.push({ id: 'liabilities', label: 'Liabilities', period: 'end of year', totalLabel: 'Mortgage',
      columns: [{ key: 'mortgageBalance', label: 'Mortgage', value: r => r.mortgageBalance }] });
  }
  groups.push({ id: 'inflows', label: 'Inflows', period: 'during the year', totalLabel: 'Total inflows', columns: inflows });
  groups.push({ id: 'outflows', label: 'Outflows', period: 'during the year', totalLabel: 'Total outflows', columns: outflows });
  return groups;
}

const groupTotal = (r: YearSummary, g: ColumnGroup) =>
  g.columns.filter(c => !c.ofWhich).reduce((sum, c) => sum + c.value(r), 0);

const Chevron: React.FC<{ open: boolean }> = ({ open }) => (
  <svg className="group-chevron" width="10" height="10" viewBox="0 0 10 10" aria-hidden="true" focusable="false">
    <path d={open ? 'M1.5 3.5 5 7l3.5-3.5' : 'M3.5 1.5 7 5l-3.5 3.5'}
          fill="none" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round" />
  </svg>
);

const ResultsTab: React.FC = () => {
  const { pdfBlob, summary, isLoading, error, generatePdf } = usePlan();
  const [pdfUrl, setPdfUrl] = useState<string | null>(null);
  const [zoom, setZoom] = useState(100);
  // A phone starts with the groups folded to their totals; a wide screen shows every column.
  const [collapsed, setCollapsed] = useState<Set<string>>(() => new Set(isPhone() ? FOLDABLE_IDS : []));

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

  const toggleGroup = (id: string) => {
    setCollapsed(prev => {
      const next = new Set(prev);
      if (next.has(id)) next.delete(id); else next.add(id);
      return next;
    });
  };

  const rows: YearSummary[] = summary || [];
  const first = rows[0];
  const last = rows[rows.length - 1];
  const firstShortfall = rows.find(r => r.deficit > 0);
  const hasShortfall = firstShortfall !== undefined;
  const groups = buildGroups(rows);
  const foldable = (g: ColumnGroup) => g.columns.length > 1;
  const isOpen = (g: ColumnGroup) => !foldable(g) || !collapsed.has(g.id);
  const hasOther = groups[0]?.columns.some(c => c.key === 'other');
  const hasLiabilities = groups.some(g => g.id === 'liabilities');

  return (
    <div className="tab-content results-tab">
      <div className="results-header">
        <h2>Financial Plan Results</h2>
        <div className="results-actions">
          <button onClick={generatePdf} disabled={isLoading}>
            {isLoading ? 'Generating...' : (pdfUrl ? 'Regenerate' : 'Generate Plan')}
          </button>
          {pdfUrl && (
            <>
              <a className="button-link" href={pdfUrl} target="_blank" rel="noopener noreferrer">Open PDF</a>
              <button type="button" onClick={handleDownload}>Download PDF</button>
              <div className="zoom-controls desktop-only" aria-label="PDF zoom">
                <button type="button" onClick={handleZoomOut} aria-label="Zoom out">-</button>
                <span>{zoom}%</span>
                <button type="button" onClick={handleZoomIn} aria-label="Zoom in">+</button>
              </div>
            </>
          )}
        </div>
      </div>

      {error && <div className="error-message">{error}</div>}

      {isLoading && <div className="loading">Running the projection...</div>}

      {!isLoading && rows.length === 0 && !pdfUrl && (
        <div className="empty-message">
          Press Generate Plan to project the plan year by year. The table appears here and the full
          report opens as a PDF.
        </div>
      )}

      {rows.length > 0 && first && last && (
        <section className="summary-section" aria-label="Yearly projection">
          <p className="summary-lede">
            Net worth {money(first.netWorth)} in {first.year}, {money(last.netWorth)} in {last.year}.{' '}
            {firstShortfall
              ? `First shortfall in ${firstShortfall.year}: expenses exceed income and withdrawals by ${money(firstShortfall.deficit)}.`
              : 'No year runs short.'}
          </p>
          <div className="table-container">
            <table className="summary-table grouped">
              <caption>Figures in thousands of dollars</caption>
              <thead>
                <tr className="group-row">
                  <th scope="col" rowSpan={2} className="standalone">Year</th>
                  <th scope="col" rowSpan={2} className="standalone">Net worth</th>
                  {groups.map(g => (
                    <th key={g.id} scope="colgroup" colSpan={isOpen(g) ? g.columns.length : 1}
                        className={`group group-${g.id} group-start`}>
                      <span className="group-label">
                        {foldable(g) ? (
                          <button type="button" onClick={() => toggleGroup(g.id)} aria-expanded={isOpen(g)}>
                            <Chevron open={isOpen(g)} />
                            {g.label}
                          </button>
                        ) : g.label}
                        {isOpen(g) && <span className="group-period">{g.period}</span>}
                      </span>
                    </th>
                  ))}
                  {hasShortfall && <th scope="col" rowSpan={2} className="standalone group-start">Shortfall</th>}
                </tr>
                <tr className="column-row">
                  {groups.map(g => isOpen(g)
                    ? g.columns.map((c, i) => (
                        <th key={c.key} scope="col"
                            className={`group-${g.id}${i === 0 ? ' group-start' : ''}${c.ofWhich ? ' of-which' : ''}`}>
                          {c.label}
                        </th>
                      ))
                    : <th key={g.id} scope="col" className={`group-${g.id} group-start`}>{g.totalLabel}</th>)}
                </tr>
              </thead>
              <tbody>
                {rows.map(r => (
                  <tr key={r.year} className={r.deficit > 0 ? 'shortfall-row' : undefined}>
                    <th scope="row">{r.year}</th>
                    <td className="num strong">{thousands(r.netWorth)}</td>
                    {groups.map(g => isOpen(g)
                      ? g.columns.map((c, i) => (
                          <td key={c.key} className={`num${i === 0 ? ' group-start' : ''}${c.ofWhich ? ' of-which' : ''}`}>
                            {thousands(c.value(r))}
                          </td>
                        ))
                      : <td key={g.id} className="num group-start strong">{thousands(groupTotal(r, g))}</td>)}
                    {hasShortfall && (
                      <td className="num group-start strong shortfall">{r.deficit > 0 ? thousands(r.deficit) : ''}</td>
                    )}
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <p className="summary-footnote">
            Tap a group name to fold it to its total or open it again.{' '}
            <b>Net worth</b> is assets less the mortgage balance.
            {hasOther && <> <b>Other</b> is real estate and life insurance cash value.</>}
            {' '}<b>Withdrawals</b> are what the plan draws from the accounts to cover the year.
            {hasLiabilities && <> <b>Mtg principal</b> includes extra repayments.</>}
          </p>
        </section>
      )}

      {pdfUrl && (
        <div className="pdf-container desktop-only">
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
        </div>
      )}
    </div>
  );
};

export default ResultsTab;
