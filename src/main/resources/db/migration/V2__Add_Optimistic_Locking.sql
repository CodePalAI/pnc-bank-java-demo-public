-- Add version column for optimistic locking to accounts table
ALTER TABLE accounts ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

-- Create index on version column for better performance during optimistic locking
CREATE INDEX idx_accounts_version ON accounts(version);