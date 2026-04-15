-- Rename the old table to the new pluralized name.
ALTER TABLE outbox_event
    RENAME TO outbox_events;

-- Remove indexes tied to the old shape.
DROP INDEX IF EXISTS ux_outbox_commit_succeeded_once;
DROP INDEX IF EXISTS idx_outbox_unpublished;

-- Add the new columns required by the new outbox model.
ALTER TABLE outbox_events
    ADD COLUMN idempotency_key TEXT,
    ADD COLUMN next_attempt_at TIMESTAMPTZ;

-- Backfill new columns for historical rows.
-- Use the existing event UUID as a stable unique idempotency key.
-- For unpublished rows, make them immediately due.
UPDATE outbox_events
SET idempotency_key = id::text,
    next_attempt_at = CASE
        WHEN published_at IS NULL THEN occurred_at
        ELSE NULL
    END;

-- Add the new constraints after backfill.
ALTER TABLE outbox_events
    ALTER COLUMN idempotency_key SET NOT NULL,
    ADD CONSTRAINT uq_outbox_idempotency_key UNIQUE (idempotency_key),
    ADD CONSTRAINT ck_outbox_attempt_count_nonnegative CHECK (attempt_count >= 0);

-- Drop the old column that no longer exists in the new design.
ALTER TABLE outbox_events
    DROP COLUMN aggregate_id;

-- Recreate indexes for the new table shape.
CREATE INDEX idx_outbox_due_unpublished
ON outbox_events (next_attempt_at ASC, occurred_at ASC)
WHERE published_at IS NULL;

CREATE INDEX idx_outbox_event_type
ON outbox_events (event_type);