-- Add version column to accounts table for optimistic locking
ALTER TABLE accounts ADD COLUMN version BIGINT DEFAULT 0;

-- Update existing records to have version 0
UPDATE accounts SET version = 0 WHERE version IS NULL;

-- Make the column NOT NULL after updating existing records
ALTER TABLE accounts ALTER COLUMN version SET NOT NULL;