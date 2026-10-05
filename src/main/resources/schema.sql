-- AUREX Financial Audit Risk Platform - Database Schema
-- Idempotent Schema Initialization Script

CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_user_role CHECK (role IN ('ADMIN', 'AUDITOR', 'VIEWER'))
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_users_username
    ON users(username);

CREATE INDEX IF NOT EXISTS idx_users_role
    ON users(role);


CREATE TABLE IF NOT EXISTS transactions (
    transaction_id VARCHAR(50) PRIMARY KEY,
    vendor VARCHAR(255) NOT NULL,
    employee VARCHAR(100) NOT NULL,
    amount NUMERIC(15,2) NOT NULL,
    transaction_time TIMESTAMP NOT NULL,
    category VARCHAR(100) NOT NULL
);

-- Transaction Indexes for High-Performance Queries & Relationship Intelligence
CREATE INDEX IF NOT EXISTS idx_transactions_vendor
    ON transactions(vendor);

CREATE INDEX IF NOT EXISTS idx_transactions_employee
    ON transactions(employee);

CREATE INDEX IF NOT EXISTS idx_transactions_category
    ON transactions(category);

CREATE INDEX IF NOT EXISTS idx_transactions_time
    ON transactions(transaction_time);


CREATE TABLE IF NOT EXISTS risk_analysis_runs (
    id BIGSERIAL PRIMARY KEY,
    transaction_id VARCHAR(50) NOT NULL,
    risk_score INTEGER NOT NULL,
    risk_level VARCHAR(20) NOT NULL,
    analyzed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_analysis_transaction
        FOREIGN KEY (transaction_id)
        REFERENCES transactions(transaction_id)
        ON DELETE CASCADE
);

-- Risk Analysis Run Indexes
CREATE INDEX IF NOT EXISTS idx_analysis_runs_transaction
    ON risk_analysis_runs(transaction_id);

CREATE INDEX IF NOT EXISTS idx_analysis_runs_analyzed_at
    ON risk_analysis_runs(analyzed_at);

-- Composite Index for Efficient DISTINCT ON Latest Run Queries
CREATE INDEX IF NOT EXISTS idx_analysis_runs_txn_analyzed
    ON risk_analysis_runs(transaction_id, analyzed_at DESC, id DESC);


CREATE TABLE IF NOT EXISTS risk_findings (
    id BIGSERIAL PRIMARY KEY,
    analysis_run_id BIGINT NOT NULL,
    transaction_id VARCHAR(50) NOT NULL,
    risk_type VARCHAR(100) NOT NULL,
    score INTEGER NOT NULL,
    severity VARCHAR(20) NOT NULL,
    explanation TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_finding_analysis_run
        FOREIGN KEY (analysis_run_id)
        REFERENCES risk_analysis_runs(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_risk_transaction
        FOREIGN KEY (transaction_id)
        REFERENCES transactions(transaction_id)
        ON DELETE CASCADE
);

-- Risk Findings Indexes
CREATE INDEX IF NOT EXISTS idx_risk_findings_analysis_run
    ON risk_findings(analysis_run_id);

CREATE INDEX IF NOT EXISTS idx_risk_findings_transaction
    ON risk_findings(transaction_id);

CREATE INDEX IF NOT EXISTS idx_risk_findings_type
    ON risk_findings(risk_type);

CREATE INDEX IF NOT EXISTS idx_risk_findings_severity
    ON risk_findings(severity);


CREATE TABLE IF NOT EXISTS audit_decisions (
    id BIGSERIAL PRIMARY KEY,
    transaction_id VARCHAR(50) NOT NULL,
    analysis_run_id BIGINT NOT NULL,
    decision VARCHAR(40) NOT NULL,
    comment TEXT,
    decided_by VARCHAR(100),
    decided_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_audit_decisions_transaction
        FOREIGN KEY (transaction_id)
        REFERENCES transactions(transaction_id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_audit_decisions_analysis_run
        FOREIGN KEY (analysis_run_id)
        REFERENCES risk_analysis_runs(id)
        ON DELETE RESTRICT
);

CREATE INDEX IF NOT EXISTS idx_audit_decisions_transaction
    ON audit_decisions(transaction_id);

CREATE INDEX IF NOT EXISTS idx_audit_decisions_analysis_run
    ON audit_decisions(analysis_run_id);

CREATE INDEX IF NOT EXISTS idx_audit_decisions_decided_at
    ON audit_decisions(decided_at DESC, id DESC);


CREATE TABLE IF NOT EXISTS audit_events (
    id BIGSERIAL PRIMARY KEY,
    transaction_id VARCHAR(50) NOT NULL,
    decision_id BIGINT,
    event_type VARCHAR(50) NOT NULL,
    event_details TEXT,
    actor VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_audit_events_transaction
        FOREIGN KEY (transaction_id)
        REFERENCES transactions(transaction_id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_audit_events_decision
        FOREIGN KEY (decision_id)
        REFERENCES audit_decisions(id)
        ON DELETE RESTRICT
);

CREATE INDEX IF NOT EXISTS idx_audit_events_transaction
    ON audit_events(transaction_id);

CREATE INDEX IF NOT EXISTS idx_audit_events_decision
    ON audit_events(decision_id);

CREATE INDEX IF NOT EXISTS idx_audit_events_created_at
    ON audit_events(created_at DESC, id DESC);

