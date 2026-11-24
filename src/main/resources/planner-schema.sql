-- Financial planner store, SQLite. One plan per row of plans; every other
-- table hangs off it. Refresh history keeps what the aggregator reported and
-- what the plan projected at that moment, for a plan-vs-actual view later.

CREATE TABLE IF NOT EXISTS schema_meta (
    version     INTEGER PRIMARY KEY,
    applied_at  TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS plans (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    name        TEXT NOT NULL UNIQUE,
    created_at  TEXT NOT NULL,
    updated_at  TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS persons (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    plan_id       INTEGER NOT NULL REFERENCES plans(id) ON DELETE CASCADE,
    position      INTEGER NOT NULL,
    name          TEXT NOT NULL,
    year_of_birth INTEGER NOT NULL,
    self_employed INTEGER NOT NULL DEFAULT 0,
    ss_fra_monthly               REAL NOT NULL DEFAULT 0,
    ss_claim_age                 INTEGER NOT NULL DEFAULT 0,
    pre_medicare_premium_monthly REAL NOT NULL DEFAULT 0,
    medicare_plan_premium_monthly REAL NOT NULL DEFAULT 0
);

-- id is the entry id the UI works with; it is assigned by the client (or by
-- a refresh) and kept as given so an open edit form never loses its row.
CREATE TABLE IF NOT EXISTS entries (
    plan_id      INTEGER NOT NULL REFERENCES plans(id) ON DELETE CASCADE,
    id           INTEGER NOT NULL,
    position     INTEGER NOT NULL,
    person_name  TEXT NOT NULL,
    item_type    TEXT NOT NULL,
    description  TEXT NOT NULL,
    value        INTEGER NOT NULL,
    start_year   INTEGER NOT NULL,
    end_year     INTEGER NOT NULL,
    source       TEXT,
    annual_payment REAL NOT NULL DEFAULT 0,
    PRIMARY KEY (plan_id, id)
);

CREATE TABLE IF NOT EXISTS rates (
    plan_id    INTEGER NOT NULL REFERENCES plans(id) ON DELETE CASCADE,
    item_type  TEXT NOT NULL,
    rate       REAL NOT NULL,
    PRIMARY KEY (plan_id, item_type)
);

CREATE TABLE IF NOT EXISTS states_by_year (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    plan_id     INTEGER NOT NULL REFERENCES plans(id) ON DELETE CASCADE,
    position    INTEGER NOT NULL,
    state       TEXT NOT NULL,
    start_year  INTEGER NOT NULL,
    end_year    INTEGER NOT NULL
);

-- One row per pull from the aggregator.
CREATE TABLE IF NOT EXISTS refresh_runs (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    plan_id         INTEGER NOT NULL REFERENCES plans(id) ON DELETE CASCADE,
    refreshed_at    TEXT NOT NULL,
    plan_year       INTEGER NOT NULL,
    imported_count  INTEGER NOT NULL,
    skipped_count   INTEGER NOT NULL
);

-- What the aggregator reported, per account and person share: the balance as
-- captured (actual) and what the plan grew it to for 31 December (projected).
CREATE TABLE IF NOT EXISTS refresh_balances (
    id           INTEGER PRIMARY KEY AUTOINCREMENT,
    run_id       INTEGER NOT NULL REFERENCES refresh_runs(id) ON DELETE CASCADE,
    account_id   INTEGER NOT NULL,
    person_name  TEXT NOT NULL,
    item_type    TEXT NOT NULL,
    description  TEXT NOT NULL,
    actual       REAL NOT NULL,
    as_of        TEXT NOT NULL,
    stale        INTEGER NOT NULL,
    projected    REAL NOT NULL
);

-- The yearly projection the plan produced right after the refresh.
CREATE TABLE IF NOT EXISTS projection_history (
    run_id                  INTEGER NOT NULL REFERENCES refresh_runs(id) ON DELETE CASCADE,
    year                    INTEGER NOT NULL,
    net_worth               REAL NOT NULL,
    total_assets            REAL NOT NULL,
    qualified               REAL NOT NULL,
    non_qualified           REAL NOT NULL,
    roth                    REAL NOT NULL,
    cash                    REAL NOT NULL,
    real_estate             REAL NOT NULL,
    life_insurance_cash     REAL NOT NULL,
    life_insurance_benefits REAL NOT NULL,
    mortgage                REAL NOT NULL,
    mortgage_interest       REAL NOT NULL DEFAULT 0,
    mortgage_principal      REAL NOT NULL DEFAULT 0,
    mortgage_extra          REAL NOT NULL DEFAULT 0,
    health_premiums         REAL NOT NULL DEFAULT 0,
    irmaa                   REAL NOT NULL DEFAULT 0,
    income                  REAL NOT NULL,
    social_security         REAL NOT NULL,
    expenses                REAL NOT NULL,
    taxes                   REAL NOT NULL,
    withdrawals             REAL NOT NULL,
    rmd                     REAL NOT NULL,
    deficit                 REAL NOT NULL,
    PRIMARY KEY (run_id, year)
);

CREATE INDEX IF NOT EXISTS idx_refresh_balances_account ON refresh_balances(account_id, run_id);

-- Premium research runs, newest per person is what the UI shows. The prompt is
-- kept so a figure in the plan can be traced to what was asked and found.
CREATE TABLE IF NOT EXISTS premium_estimates (
    id                    INTEGER PRIMARY KEY AUTOINCREMENT,
    plan_id               INTEGER NOT NULL REFERENCES plans(id) ON DELETE CASCADE,
    person_key            TEXT NOT NULL,
    person_name           TEXT NOT NULL,
    plan_tier             TEXT NOT NULL,
    requested_at          TEXT NOT NULL,
    completed_at          TEXT,
    status                TEXT NOT NULL,
    state                 TEXT,
    gap_start             INTEGER NOT NULL DEFAULT 0,
    gap_end               INTEGER NOT NULL DEFAULT 0,
    medicare_year         INTEGER NOT NULL DEFAULT 0,
    pre_medicare_monthly  REAL NOT NULL DEFAULT 0,
    medicare_plan_monthly REAL NOT NULL DEFAULT 0,
    medical_inflation_pct REAL NOT NULL DEFAULT 0,
    notes                 TEXT,
    sources               TEXT,
    prompt                TEXT,
    error                 TEXT,
    duration_ms           INTEGER NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_premium_estimates_person ON premium_estimates(plan_id, person_key, id);

-- Questions asked about a projection and the answers given, kept per plan
-- version (plan_hash) so a changed plan starts a fresh thread.
CREATE TABLE IF NOT EXISTS explanations (
    id           INTEGER PRIMARY KEY AUTOINCREMENT,
    plan_id      INTEGER NOT NULL REFERENCES plans(id) ON DELETE CASCADE,
    plan_hash    TEXT NOT NULL,
    question_key TEXT NOT NULL,
    question     TEXT NOT NULL,
    answer       TEXT,
    prompt       TEXT,
    status       TEXT NOT NULL,
    error        TEXT,
    asked_at     TEXT NOT NULL,
    answered_at  TEXT,
    duration_ms  INTEGER NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_explanations_plan ON explanations(plan_id, plan_hash, id);
