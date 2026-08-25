CREATE TABLE transactions (
    transaction_id VARCHAR(50) PRIMARY KEY,
    vendor VARCHAR(255) NOT NULL,
    employee VARCHAR(100) NOT NULL,
    amount NUMERIC(15,2) NOT NULL,
    transaction_time TIMESTAMP NOT NULL,
    category VARCHAR(100) NOT NULL
);

CREATE TABLE risk_analysis_runs (
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

CREATE INDEX idx_analysis_runs_transaction
    ON risk_analysis_runs(transaction_id);

CREATE INDEX idx_analysis_runs_analyzed_at
    ON risk_analysis_runs(analyzed_at);


CREATE TABLE risk_findings (
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

CREATE INDEX idx_risk_findings_analysis_run
    ON risk_findings(analysis_run_id);

CREATE INDEX idx_risk_findings_transaction
    ON risk_findings(transaction_id);

CREATE INDEX idx_risk_findings_type
    ON risk_findings(risk_type);

CREATE INDEX idx_risk_findings_severity
    ON risk_findings(severity);