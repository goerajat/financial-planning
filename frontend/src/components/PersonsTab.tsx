import React, { useRef, useState } from 'react';
import { usePlan } from '../context/PlanContext';
import { Person } from '../types';
import {
  CLAIM_AGES,
  MEDICARE_AGE,
  claimAgeEffect,
  defaultClaimAge,
  fullRetirementAgeLabel,
} from '../lib/socialSecurity';
import PremiumEstimatePanel from './PremiumEstimatePanel';
import './Tab.css';

const money = (v: number) => `$${Math.round(v).toLocaleString()}`;

// Year of birth starts empty (NaN) so a default never passes for an entered value.
const blankPerson = (): Person => ({
  name: '',
  yearOfBirth: NaN,
  selfEmployed: false,
  socialSecurityFraMonthly: 0,
  socialSecurityClaimAge: 0,
  preMedicarePremiumMonthly: 0,
  medicarePlanPremiumMonthly: 0,
});

const num = (v: string) => Math.max(0, parseFloat(v) || 0);

const PersonsTab: React.FC = () => {
  const { persons, addPerson, updatePerson, deletePerson } = usePlan();
  const [editIndex, setEditIndex] = useState<number | null>(null);
  const [confirmIndex, setConfirmIndex] = useState<number | null>(null);
  const [formData, setFormData] = useState<Person>(blankPerson());
  const formRef = useRef<HTMLFormElement>(null);
  const nameRef = useRef<HTMLInputElement>(null);
  const thisYear = new Date().getFullYear();

  const hasYob = Number.isFinite(formData.yearOfBirth);
  const yob = hasYob ? formData.yearOfBirth : thisYear - 45;
  const fraAge = defaultClaimAge(yob);
  const claimAge = formData.socialSecurityClaimAge || fraAge;
  const medicareYear = yob + MEDICARE_AGE;
  const editing = editIndex !== null;

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!hasYob) return;
    if (editIndex !== null) {
      updatePerson(editIndex, formData);
      setEditIndex(null);
    } else {
      addPerson(formData);
    }
    setFormData(blankPerson());
    nameRef.current?.focus();
  };

  const handleEdit = (index: number) => {
    setEditIndex(index);
    setConfirmIndex(null);
    setFormData({ ...blankPerson(), ...persons[index] });
    formRef.current?.scrollIntoView({ block: 'start', behavior: 'smooth' });
    window.setTimeout(() => nameRef.current?.focus({ preventScroll: true }), 350);
  };

  const handleCancel = () => {
    setEditIndex(null);
    setFormData(blankPerson());
  };

  const handleDelete = (index: number) => {
    deletePerson(index);
    setConfirmIndex(null);
    if (editIndex === index) handleCancel();
  };

  const socialSecuritySummary = (p: Person) => {
    const fra = p.socialSecurityFraMonthly || 0;
    if (fra <= 0) return null;
    const age = p.socialSecurityClaimAge || defaultClaimAge(p.yearOfBirth);
    return `${money(fra)}/mo at full retirement age · claims at ${age} (${claimAgeEffect(p.yearOfBirth, age)})`;
  };

  const healthSummary = (p: Person) => {
    const parts: string[] = [];
    if ((p.preMedicarePremiumMonthly || 0) > 0) parts.push(`${money(p.preMedicarePremiumMonthly!)}/mo before 65`);
    if ((p.medicarePlanPremiumMonthly || 0) > 0) parts.push(`${money(p.medicarePlanPremiumMonthly!)}/mo Medicare plan`);
    return parts.length > 0 ? parts.join(' · ') : null;
  };

  const moneyField = (
    id: string,
    label: string,
    value: number | undefined,
    onChange: (v: number) => void,
    hint: React.ReactNode,
  ) => (
    <div className="form-group">
      <label htmlFor={id}>{label}</label>
      <div className="input-affix">
        <span className="affix" aria-hidden="true">$</span>
        <input
          id={id}
          type="number"
          inputMode="decimal"
          min={0}
          step={1}
          placeholder="0"
          value={value || ''}
          onChange={e => onChange(num(e.target.value))}
          aria-describedby={`${id}-hint`}
        />
        <span className="affix" aria-hidden="true">/ month</span>
      </div>
      <span className="form-hint" id={`${id}-hint`}>{hint}</span>
    </div>
  );

  return (
    <div className="tab-content">
      <h2>Persons</h2>
      <p className="tab-description">
        Each person carries their own Social Security record and health cover. Amounts are per month in
        today's dollars; the projection grows them at the COLA and Medical inflation rates on the Rates tab.
      </p>

      <form
        ref={formRef}
        onSubmit={handleSubmit}
        className={`form-grid persons-form${editing ? ' editing' : ''}`}
        aria-labelledby="persons-form-title"
      >
        <h3 className="form-title" id="persons-form-title">
          {editing ? `Editing ${persons[editIndex!]?.name || 'person'}` : 'Add a person'}
        </h3>

        <div className="form-group">
          <label htmlFor="person-name">Name</label>
          <input
            id="person-name"
            ref={nameRef}
            type="text"
            autoComplete="off"
            value={formData.name}
            onChange={e => setFormData({ ...formData, name: e.target.value })}
            required
          />
        </div>

        <div className="form-group">
          <label htmlFor="person-yob">Year of birth</label>
          <input
            id="person-yob"
            type="number"
            inputMode="numeric"
            min={1900}
            max={thisYear}
            placeholder="e.g. 1970"
            value={hasYob ? formData.yearOfBirth : ''}
            onChange={e => setFormData({ ...formData, yearOfBirth: parseInt(e.target.value) })}
            required
          />
        </div>

        <div className="form-group form-group-check">
          <label className="checkbox-label" htmlFor="person-self-employed">
            <input
              id="person-self-employed"
              type="checkbox"
              checked={formData.selfEmployed || false}
              onChange={e => setFormData({ ...formData, selfEmployed: e.target.checked })}
            />
            Self-employed
          </label>
          <span className="form-hint">Pays both halves of Social Security and Medicare tax on wages.</span>
        </div>

        <h4 className="form-section">Social Security</h4>

        {moneyField(
          'person-ss-fra',
          'Benefit at full retirement age',
          formData.socialSecurityFraMonthly,
          v => setFormData({ ...formData, socialSecurityFraMonthly: v }),
          <>
            The monthly figure on the SSA statement (ssa.gov/myaccount).
            {hasYob ? ` Full retirement age for ${yob} is ${fullRetirementAgeLabel(yob)}.` : ''}
            {' '}Leave empty if this person has no work record; a spouse still gets half of the other's benefit.
          </>,
        )}

        <div className="form-group">
          <label htmlFor="person-ss-age">Claim at age</label>
          <select
            id="person-ss-age"
            value={claimAge}
            onChange={e => setFormData({ ...formData, socialSecurityClaimAge: parseInt(e.target.value) })}
            aria-describedby="person-ss-age-hint"
          >
            {CLAIM_AGES.map(age => (
              <option key={age} value={age}>
                {age} · {age === fraAge ? 'full retirement age' : claimAgeEffect(yob, age)}
              </option>
            ))}
          </select>
          <span className="form-hint" id="person-ss-age-hint">
            {hasYob ? `Benefits start in ${yob + claimAge}. ` : ''}
            Claiming early reduces the benefit for life; each year of delay to 70 adds 8%.
          </span>
        </div>

        <h4 className="form-section">Health cover</h4>

        <PremiumEstimatePanel
          person={formData}
          editIndex={editIndex}
          onApply={(pre, plan) => setFormData({
            ...formData,
            preMedicarePremiumMonthly: Math.round(pre),
            medicarePlanPremiumMonthly: Math.round(plan),
          })}
        />

        {moneyField(
          'person-pre65',
          'Premium before Medicare',
          formData.preMedicarePremiumMonthly,
          v => setFormData({ ...formData, preMedicarePremiumMonthly: v }),
          <>COBRA or an ACA plan. Charged in years when nobody in the plan has wages{hasYob ? `, until ${medicareYear}` : ''}.</>,
        )}

        {moneyField(
          'person-medicare',
          'Medicare plan premiums',
          formData.medicarePlanPremiumMonthly,
          v => setFormData({ ...formData, medicarePlanPremiumMonthly: v }),
          <>
            Medigap or Advantage plan plus the Part D drug plan, together. Part B and any income surcharge
            (IRMAA) are added automatically{hasYob ? ` from ${medicareYear}` : ''}.
          </>,
        )}

        <div className="form-actions">
          <button type="submit">{editing ? 'Save changes' : 'Add person'}</button>
          {editing && (
            <button type="button" onClick={handleCancel}>Cancel</button>
          )}
        </div>
      </form>

      <table className="data-table persons-table">
        <thead>
          <tr>
            <th>Name</th>
            <th>Born</th>
            <th>Social Security</th>
            <th>Health cover</th>
            <th>Actions</th>
          </tr>
        </thead>
        <tbody>
          {persons.map((person, index) => {
            const ss = socialSecuritySummary(person);
            const health = healthSummary(person);
            const confirming = confirmIndex === index;
            return (
              <tr key={index} className={editIndex === index ? 'row-editing' : undefined}>
                <td data-label="Name" className="cell-primary">
                  {person.name}
                  {person.selfEmployed && <span className="tag" title="Self-employed: pays both halves of payroll tax">Self-employed</span>}
                </td>
                <td data-label="Born">
                  <span>{person.yearOfBirth} <span className="muted">(age {thisYear - person.yearOfBirth})</span></span>
                </td>
                <td data-label="Social Security" className="cell-detail">
                  {ss ?? <span className="muted">No record of their own; spousal benefit if the other has one</span>}
                </td>
                <td data-label="Health cover" className="cell-detail">
                  {health ?? <span className="muted">Part B only, from {person.yearOfBirth + MEDICARE_AGE}</span>}
                </td>
                <td className="row-actions-cell">
                  {confirming ? (
                    <div className="row-actions row-confirm" role="group" aria-label={`Confirm deleting ${person.name}`}>
                      <span className="confirm-text">Delete {person.name} and their entries?</span>
                      <button type="button" className="danger" onClick={() => handleDelete(index)}>Delete</button>
                      <button type="button" onClick={() => setConfirmIndex(null)} autoFocus>Keep</button>
                    </div>
                  ) : (
                    <div className="row-actions">
                      <button type="button" onClick={() => handleEdit(index)}>Edit</button>
                      <button type="button" className="danger-outline" onClick={() => setConfirmIndex(index)}>Delete</button>
                    </div>
                  )}
                </td>
              </tr>
            );
          })}
          {persons.length === 0 && (
            <tr>
              <td colSpan={5} className="empty-message">No persons yet. Add yourself first, then a spouse.</td>
            </tr>
          )}
        </tbody>
      </table>
    </div>
  );
};

export default PersonsTab;
