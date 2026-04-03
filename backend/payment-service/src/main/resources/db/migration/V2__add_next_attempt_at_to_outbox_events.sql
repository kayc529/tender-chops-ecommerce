ALTER TABLE outbox_events
ADD COLUMN next_attempt_at TIMESTAMPTZ NOT NULL DEFAULT NOW();

DROP INDEX IF EXISTS idx_outbox_unpublished;

CREATE INDEX idx_outbox_due_unpublished
ON outbox_events (next_attempt_at ASC, occurred_at ASC)
WHERE published_at IS NULL;