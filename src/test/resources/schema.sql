CREATE TABLE transactions (
    transaction_id VARCHAR(50) PRIMARY KEY,
    vendor VARCHAR(255) NOT NULL,
    employee VARCHAR(100) NOT NULL,
    amount NUMERIC(15,2) NOT NULL,
    transaction_time TIMESTAMP NOT NULL,
    category VARCHAR(100) NOT NULL
);