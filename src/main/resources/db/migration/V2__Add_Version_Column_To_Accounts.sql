-- Add version column for optimistic locking to prevent race conditions

ALTER TABLE accounts ADD COLUMN version BIGINT DEFAULT 0;

-- Initialize existing records with version 0
UPDATE accounts SET version = 0 WHERE version IS NULL;

-- Make the version column not null after initialization
ALTER TABLE accounts MODIFY COLUMN version BIGINT NOT NULL DEFAULT 0;