// Mirrors SocialSecurityCalculator on the server, for labels only; the engine does the arithmetic.

export const EARLIEST_CLAIM_AGE = 62;
export const LATEST_CLAIM_AGE = 70;
export const MEDICARE_AGE = 65;

export const CLAIM_AGES: number[] = [];
for (let a = EARLIEST_CLAIM_AGE; a <= LATEST_CLAIM_AGE; a++) CLAIM_AGES.push(a);

export function fullRetirementAgeMonths(yearOfBirth: number): number {
  if (yearOfBirth <= 1937) return 65 * 12;
  if (yearOfBirth <= 1942) return 65 * 12 + 2 * (yearOfBirth - 1937);
  if (yearOfBirth <= 1954) return 66 * 12;
  if (yearOfBirth <= 1959) return 66 * 12 + 2 * (yearOfBirth - 1954);
  return 67 * 12;
}

export function fullRetirementAgeLabel(yearOfBirth: number): string {
  const months = fullRetirementAgeMonths(yearOfBirth);
  const years = Math.floor(months / 12);
  const rest = months % 12;
  return rest === 0 ? `${years}` : `${years} and ${rest} months`;
}

/** The whole-year claiming age the server uses when none is chosen. */
export function defaultClaimAge(yearOfBirth: number): number {
  return Math.round(fullRetirementAgeMonths(yearOfBirth) / 12);
}

/** Factor on the full-retirement-age benefit for claiming at a whole-year age. */
export function ownBenefitFactor(yearOfBirth: number, claimAge: number): number {
  const months = claimAge * 12 - fullRetirementAgeMonths(yearOfBirth);
  if (months < 0) {
    const early = -months;
    const first = Math.min(early, 36);
    const rest = early - first;
    return 1 - (first * (5 / 9)) / 100 - (rest * (5 / 12)) / 100;
  }
  return 1 + (months * (2 / 3)) / 100;
}

/** "+24%" / "−30%" / "full benefit" for a claiming age. */
export function claimAgeEffect(yearOfBirth: number, claimAge: number): string {
  const pct = Math.round((ownBenefitFactor(yearOfBirth, claimAge) - 1) * 1000) / 10;
  if (Math.abs(pct) < 0.05) return 'full benefit';
  return pct > 0 ? `+${pct}%` : `−${Math.abs(pct)}%`;
}
