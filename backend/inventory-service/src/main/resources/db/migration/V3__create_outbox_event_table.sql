CREATE TABLE outbox_event(
    id              UUID            PRIMARY KEY,
    event_type      VARCHAR(100)    NOT NULL,
    aggregate_id    UUID            NOT NULL,
    payload         JSONB           NOT NULL,
    occurred_at     TIMESTAMPTZ     NOT NULL,

    published_at    TIMESTAMPTZ,
    attempt_count   INT             NOT NULL DEFAULT 0,
    last_error      TEXT
);

CREATE TABLE processed_event(
    event_id        UUID            PRIMARY KEY,
    event_type      VARCHAR(100)    NOT NULL,
    processed_at    TIMESTAMPTZ     NOT NULL
);

CREATE UNIQUE INDEX ux_outbox_commit_succeeded_once
ON outbox_event (aggregate_id)
WHERE event_type = 'INVENTORY_RESERVATIONS_COMMIT_SUCCEEDED';

CREATE INDEX idx_outbox_unpublished
ON outbox_event (occurred_at)
WHERE published_at IS NULL;