-- Add version column to accounts table for optimistic locking

ALTER TABLE accounts ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

-- Initialize version for existing accounts
UPDATE accounts SET version = 0 WHERE version IS NULL;