import React, { useCallback, useEffect, useRef, useState } from 'react';
import { usePlan } from '../context/PlanContext';
import { planApi } from '../services/api';
import { HealthPremiumEstimate, Person, PlanTier } from '../types';

interface Props {
  /** The person as the form currently has them; the run uses these values, saved or not. */
  person: Person;
  /** Index in the plan when editing an existing person; null while adding one. */
  editIndex: number | null;
  onApply: (preMedicareMonthly: number, medicarePlanMonthly: number) => void;
}

const POLL_MS = 5000;
const SOURCES_SHOWN = 5;
const money = (v: number) => `$${Math.round(v).toLocaleString()}`;

const shortDate = (iso: string | null) => {
  if (!iso) return '';
  const d = new Date(iso);
  return isNaN(d.getTime()) ? '' : d.toLocaleDateString(undefined, { day: 'numeric', month: 'short', year: 'numeric' });
};

const tierLabel = (t: PlanTier | null) => (t === 'GOLD' ? 'Gold' : 'Silver');
const host = (url: string) => url.replace(/^https?:\/\//, '').replace(/^www\./, '').split('/')[0];

const LINK_RE = /\[([^\]]+)\]\((https?:\/\/[^)\s]+)\)/g;

/** Titles the notes gave their links, by URL, so the source list can name them. */
function linkTitles(text: string): Map<string, string> {
  const out = new Map<string, string>();
  let m: RegExpExecArray | null;
  const re = new RegExp(LINK_RE.source, 'g');
  while ((m = re.exec(text)) !== null) {
    if (!out.has(m[2])) out.set(m[2], m[1]);
  }
  return out;
}

/**
 * The model's notes, tidied: anything before its first heading is working
 * monologue and goes; its own Sources section goes too, since the list below
 * renders the same links with titles.
 */
function editorial(text: string): string[] {
  const paragraphs = text.split(/\n\s*\n/).map(p => p.trim()).filter(Boolean);
  const isHeading = (p: string) => /^#{1,6}\s+/.test(p.split('\n')[0]);
  const first = paragraphs.findIndex(isHeading);
  const kept: string[] = [];
  let skipping = false;
  paragraphs.slice(first >= 0 ? first : 0).forEach(p => {
    if (isHeading(p)) skipping = /^#{1,6}\s+(sources|references)\b/i.test(p.split('\n')[0]);
    if (!skipping) kept.push(p);
  });
  return kept;
}

const inline = (s: string): React.ReactNode[] => {
  const out: React.ReactNode[] = [];
  const re = /\[([^\]]+)\]\((https?:\/\/[^)\s]+)\)|\*\*([^*]+)\*\*/g;
  let last = 0;
  let m: RegExpExecArray | null;
  let k = 0;
  while ((m = re.exec(s)) !== null) {
    if (m.index > last) out.push(s.slice(last, m.index));
    if (m[1]) out.push(<a key={k++} href={m[2]} target="_blank" rel="noreferrer">{m[1]}</a>);
    else out.push(<strong key={k++}>{m[3]}</strong>);
    last = m.index + m[0].length;
  }
  if (last < s.length) out.push(s.slice(last));
  return out;
};

const Block: React.FC<{ text: string }> = ({ text }) => {
  const lines = text.split('\n');
  const heading = /^(#{1,6})\s+(.*)$/.exec(lines[0]);
  if (heading) {
    const rest = lines.slice(1).join('\n').trim();
    return (
      <>
        <h5>{inline(heading[2])}</h5>
        {rest && <Block text={rest} />}
      </>
    );
  }
  if (lines.every(l => /^\s*\|/.test(l))) {
    const rows = lines
      .map(l => l.trim().replace(/^\|/, '').replace(/\|$/, '').split('|').map(c => c.trim()))
      .filter(cells => !cells.every(c => /^:?-{2,}:?$/.test(c)));
    if (rows.length > 0) {
      return (
        <table className="estimate-table">
          <thead><tr>{rows[0].map((c, i) => <th key={i} scope="col">{inline(c)}</th>)}</tr></thead>
          <tbody>
            {rows.slice(1).map((r, i) => <tr key={i}>{r.map((c, j) => <td key={j}>{inline(c)}</td>)}</tr>)}
          </tbody>
        </table>
      );
    }
  }
  if (lines.every(l => /^\s*([-*]|\d+\.)\s+/.test(l))) {
    return <ul>{lines.map((l, j) => <li key={j}>{inline(l.replace(/^\s*([-*]|\d+\.)\s+/, ''))}</li>)}</ul>;
  }
  return <p>{inline(lines.join(' '))}</p>;
};

const Notes: React.FC<{ text: string }> = ({ text }) => (
  <>{editorial(text).map((p, i) => <Block key={i} text={p} />)}</>
);

const PremiumEstimatePanel: React.FC<Props> = ({ person, editIndex, onApply }) => {
  const { persons, entries, rates, statesByYear, updateRate } = usePlan();
  const [tier, setTier] = useState<PlanTier>('SILVER');
  const [estimate, setEstimate] = useState<HealthPremiumEstimate | null>(null);
  const [requestError, setRequestError] = useState<string | null>(null);
  const [previousRate, setPreviousRate] = useState<number | null>(null);
  const [allSources, setAllSources] = useState(false);
  const tierTouched = useRef(false);

  const personName = person.name.trim();
  const hasYob = Number.isFinite(person.yearOfBirth);
  const hasState = statesByYear.length > 0;
  const running = estimate?.status === 'RUNNING';

  const load = useCallback(async (name: string) => {
    if (!name) {
      setEstimate(null);
      return;
    }
    try {
      const e = await planApi.getHealthPremiumEstimate(name);
      setEstimate(e);
      if (!tierTouched.current && e.planTier) setTier(e.planTier);
    } catch {
      setEstimate(null);
    }
  }, []);

  // A different person in the form means a different record; the name is the key.
  useEffect(() => {
    setRequestError(null);
    setPreviousRate(null);
    setAllSources(false);
    tierTouched.current = false;
    const t = window.setTimeout(() => load(personName), 300);
    return () => window.clearTimeout(t);
  }, [personName, load]);

  useEffect(() => {
    if (!running) return;
    const id = window.setInterval(() => load(personName), POLL_MS);
    return () => window.clearInterval(id);
  }, [running, personName, load]);

  const start = async () => {
    setRequestError(null);
    setPreviousRate(null);
    const merged = editIndex !== null
      ? persons.map((p, i) => (i === editIndex ? person : p))
      : [...persons, person];
    try {
      const e = await planApi.requestHealthPremiumEstimate({
        personName,
        planTier: tier,
        plan: { persons: merged, entries, rates, statesByYear },
      });
      setEstimate(e);
    } catch (err: any) {
      setRequestError(err.response?.data?.error || err.message || 'Could not start the estimate');
    }
  };

  const blocker = !personName
    ? 'Enter a name first.'
    : !hasYob
      ? 'Enter the year of birth first.'
      : !hasState
        ? 'Set the state of residence on the States tab first.'
        : null;
  const canStart = blocker === null && !running;

  const done = estimate?.status === 'DONE' ? estimate : null;
  const tierDiffers = done !== null && done.planTier !== null && done.planTier !== tier;
  const currentRate = rates.HEALTH_PREMIUM ?? 0;
  const rateInUse = done !== null && Math.abs(currentRate - done.medicalInflationPct) < 0.005;
  const inFields = done !== null
    && Math.round(person.preMedicarePremiumMonthly || 0) === Math.round(done.preMedicarePremiumMonthly)
    && Math.round(person.medicarePlanPremiumMonthly || 0) === Math.round(done.medicarePlanPremiumMonthly);
  const stateName = done?.stateName ?? statesByYear[0]?.state ?? '';

  const startLabel = running
    ? 'Estimating…'
    : done
      ? (tierDiffers ? `Estimate ${tierLabel(tier)} instead` : 'Estimate again')
      : 'Estimate from the web';

  const setRate = () => {
    if (!done) return;
    setPreviousRate(currentRate);
    updateRate('HEALTH_PREMIUM', Math.round(done.medicalInflationPct * 10) / 10);
  };

  const sources = done?.sources ?? [];
  const titles = done?.notes ? linkTitles(done.notes) : new Map<string, string>();
  const shownSources = allSources ? sources : sources.slice(0, SOURCES_SHOWN);

  return (
    <div className="estimate-block">
      <div className="estimate-controls">
        <div className="estimate-field">
          <label htmlFor="person-tier">Marketplace plan before Medicare</label>
          <select
            id="person-tier"
            value={tier}
            onChange={e => { tierTouched.current = true; setTier(e.target.value as PlanTier); }}
            disabled={running}
          >
            <option value="SILVER">Silver</option>
            <option value="GOLD">Gold</option>
          </select>
        </div>
        <button
          type="button"
          className={done && !tierDiffers ? undefined : 'estimate-start'}
          onClick={start}
          disabled={!canStart}
          aria-describedby="person-estimate-hint"
        >
          {startLabel}
        </button>
        <span className="estimate-live" aria-live="polite">
          {estimate && estimate.status !== 'NONE' && (
            <span className={`estimate-status is-${estimate.status.toLowerCase()}`}>
              {running && <span className="estimate-spinner" aria-hidden="true" />}
              {running
                ? `Looking up ${tierLabel(estimate.planTier)} in ${estimate.stateName ?? estimate.state}, two to four minutes`
                : estimate.status === 'DONE'
                  ? `${tierLabel(estimate.planTier)} · ${estimate.stateName ?? estimate.state} · ${shortDate(estimate.completedAt)}`
                  : `Failed ${shortDate(estimate.completedAt)}`}
            </span>
          )}
        </span>
      </div>

      <span className="form-hint" id="person-estimate-hint">
        {blocker ?? (done
          ? `Based on a person born ${person.yearOfBirth} in ${stateName}${done.gapStart > 0 ? `, buying cover ${done.gapStart}–${done.gapEnd}` : ''}, Medicare from ${done.medicareYear}. Estimating again replaces this result and takes two to four minutes.`
          : `Looks up current premiums on the web for this person's years: born ${person.yearOfBirth}, ${stateName}. Silver is the benchmark plan; Gold costs more each month for lower deductibles. Takes two to four minutes; the answer fills the two fields below for you to check and save.`)}
      </span>

      <span aria-live="assertive">
        {requestError && <p className="estimate-error" role="alert">{requestError}</p>}
        {estimate?.status === 'ERROR' && !requestError && (
          <p className="estimate-error" role="alert">
            {estimate.error || 'The run failed.'} You can try again, or type the premiums below by hand.
          </p>
        )}
      </span>

      {done && (
        <div className="estimate-result">
          <dl className="estimate-figures">
            <div>
              <dt>Before Medicare</dt>
              <dd>
                {done.gapStart > 0 ? `${money(done.preMedicarePremiumMonthly)}/mo` : 'No gap'}
                {done.gapStart > 0 && <span className="muted"> {done.gapStart}–{done.gapEnd}</span>}
              </dd>
            </div>
            <div>
              <dt>Medicare plan</dt>
              <dd>
                {money(done.medicarePlanPremiumMonthly)}/mo
                <span className="muted"> from {done.medicareYear}</span>
              </dd>
            </div>
            <div>
              <dt>Medical inflation</dt>
              <dd>{done.medicalInflationPct.toFixed(1)}%<span className="muted"> a year</span></dd>
            </div>
          </dl>

          <div className="estimate-actions">
            {inFields ? (
              <span className="estimate-note">Premiums are in the fields below. Save the person to keep them.</span>
            ) : (
              <button
                type="button"
                className="estimate-apply"
                onClick={() => onApply(done.preMedicarePremiumMonthly, done.medicarePlanPremiumMonthly)}
              >
                Use these figures
              </button>
            )}
          </div>
          <div className="estimate-actions">
            {rateInUse ? (
              <span className="estimate-note">
                Medical Inflation is {done.medicalInflationPct.toFixed(1)}% for the whole plan.
                {previousRate !== null && previousRate !== currentRate && (
                  <>
                    {' '}
                    <button type="button" className="link-button" onClick={() => { updateRate('HEALTH_PREMIUM', previousRate); setPreviousRate(null); }}>
                      Undo (back to {previousRate.toFixed(1)}%)
                    </button>
                  </>
                )}
              </span>
            ) : (
              <>
                <button type="button" onClick={setRate}>
                  Set plan-wide Medical Inflation to {done.medicalInflationPct.toFixed(1)}%
                </button>
                <span className="estimate-note">Changes the Rates tab for every person at once, now {currentRate.toFixed(1)}%.</span>
              </>
            )}
          </div>

          {(done.notes || sources.length > 0) && (
            <details className="estimate-notes">
              <summary>How this was estimated</summary>
              {done.notes && <Notes text={done.notes} />}
              {sources.length > 0 && (
                <>
                  <h5>Sources</h5>
                  <ul className="estimate-sources">
                    {shownSources.map(s => (
                      <li key={s}>
                        <a href={s} target="_blank" rel="noreferrer">{titles.get(s) ?? host(s)}</a>
                        {titles.has(s) && <span className="muted"> {host(s)}</span>}
                      </li>
                    ))}
                  </ul>
                  {sources.length > SOURCES_SHOWN && !allSources && (
                    <button type="button" className="link-button" onClick={() => setAllSources(true)}>
                      Show all {sources.length} sources
                    </button>
                  )}
                </>
              )}
            </details>
          )}
        </div>
      )}
    </div>
  );
};

export default PremiumEstimatePanel;
