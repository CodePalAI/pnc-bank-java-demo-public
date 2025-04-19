-- Initial Schema Creation

-- Create Accounts table
CREATE TABLE accounts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    account_number VARCHAR(10) NOT NULL UNIQUE,
    account_holder VARCHAR(100) NOT NULL,
    account_type VARCHAR(20) NOT NULL,
    balance DECIMAL(19, 2) NOT NULL DEFAULT 0.00,
    created_date TIMESTAMP NOT NULL,
    last_modified_date TIMESTAMP,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
);

-- Create Transactions table
CREATE TABLE transactions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    account_id BIGINT NOT NULL,
    transaction_type VARCHAR(20) NOT NULL,
    amount DECIMAL(19, 2) NOT NULL,
    reference_id VARCHAR(36),
    description VARCHAR(255),
    transaction_date TIMESTAMP NOT NULL,
    balance_after_transaction DECIMAL(19, 2),
    FOREIGN KEY (account_id) REFERENCES accounts(id)
);

-- Create indices for better performance
CREATE INDEX idx_accounts_account_number ON accounts(account_number);
CREATE INDEX idx_accounts_status ON accounts(status);
CREATE INDEX idx_transactions_account_id ON transactions(account_id);
CREATE INDEX idx_transactions_transaction_date ON transactions(transaction_date);
CREATE INDEX idx_transactions_reference_id ON transactions(reference_id); 