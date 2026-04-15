CREATE TABLE inbox_events (
    id                  UUID            PRIMARY KEY,
    -- where this message came from (e.g. "payment-service", "sqs", "sns")
    source              VARCHAR(100)    NOT NULL,
    -- the provider/transport message id (HTTP: can be outbox idempotency_key; SQS: MessageId; SNS->SQS: the envelope MessageId)
    message_id          TEXT            NOT NULL,
    event_type          VARCHAR(100)    NOT NULL,
    payload             JSONB           NOT NULL,
    received_at         TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    processed_at        TIMESTAMPTZ,
    attempt_count       INT             NOT NULL DEFAULT 0,
    next_attempt_at     TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    last_error          TEXT,
    dead                BOOLEAN         NOT NULL DEFAULT FALSE,

    CONSTRAINT uq_inbox_source_message_id
    UNIQUE (source, message_id),

    CONSTRAINT ck_inbox_attempt_count_nonnegative
    CHECK (attempt_count >= 0)
);

CREATE TABLE outbox_events (
    id                  UUID            PRIMARY KEY,
    idempotency_key     TEXT            NOT NULL,
    event_type          VARCHAR(100)    NOT NULL,
    payload             JSONB           NOT NULL,
    occurred_at         TIMESTAMPTZ     NOT NULL,
    published_at        TIMESTAMPTZ,
    attempt_count       INT             NOT NULL DEFAULT 0,
    last_error          TEXT,
    next_attempt_at     TIMESTAMPTZ,

    CONSTRAINT uq_outbox_idempotency_key
        UNIQUE (idempotency_key),

    CONSTRAINT ck_outbox_attempt_count_nonnegative
        CHECK (attempt_count >= 0)
);

CREATE INDEX idx_outbox_due_unpublished
ON outbox_events (next_attempt_at ASC, occurred_at ASC)
WHERE published_at IS NULL;

CREATE INDEX idx_outbox_event_type
ON outbox_events (event_type);

CREATE INDEX idx_inbox_due_unprocessed
ON inbox_events (next_attempt_at ASC, received_at ASC)
WHERE processed_at IS NULL
AND dead = FALSE;

CREATE INDEX idx_inbox_event_type
ON inbox_events (event_type);