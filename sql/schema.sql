-- PostgreSQL schema for Settlement Batch System

CREATE TABLE IF NOT EXISTS settlement_batch (
    batch_id    VARCHAR(50) PRIMARY KEY,
    batch_date  DATE NOT NULL
);

CREATE TABLE IF NOT EXISTS transactions (
    txn_id         VARCHAR(50) PRIMARY KEY,
    sender_bank    VARCHAR(20) NOT NULL,
    receiver_bank  VARCHAR(20) NOT NULL,
    channel        VARCHAR(20) NOT NULL,
    amount         NUMERIC(18,2) NOT NULL CHECK (amount > 0),
    txn_time       TIMESTAMP NOT NULL,
    dr_cr          VARCHAR(5) NOT NULL,
    status         VARCHAR(20) NOT NULL,
    batch_id       VARCHAR(50) NOT NULL,
    CONSTRAINT fk_transactions_batch
        FOREIGN KEY (batch_id)
        REFERENCES settlement_batch(batch_id)
        ON UPDATE CASCADE
        ON DELETE RESTRICT,
    CONSTRAINT chk_transactions_dr_cr
        CHECK (dr_cr IN ('DR', 'CR')),
    CONSTRAINT chk_transactions_status
        CHECK (status IN ('SUCCESS', 'FAILED', 'PENDING')),
    CONSTRAINT chk_transactions_sender_receiver_diff
        CHECK (sender_bank <> receiver_bank)
);

CREATE INDEX IF NOT EXISTS idx_transactions_batch_id ON transactions(batch_id);
CREATE INDEX IF NOT EXISTS idx_transactions_status ON transactions(status);
CREATE INDEX IF NOT EXISTS idx_transactions_txn_time ON transactions(txn_time);
