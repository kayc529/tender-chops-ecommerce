CREATE TABLE payment(
    id                          UUID            PRIMARY KEY,
    order_id                    UUID            NOT NULL,
    user_id                     UUID            NOT NULL,
    payment_status              VARCHAR(50)     NOT NULL,
    amount                      BIGINT          NOT NULL,
    currency                    VARCHAR(3)      NOT NULL,
    provider                    VARCHAR(50)     NOT NULL,
    current_attempt_id          UUID,
    created_at                  TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at                  TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    CONSTRAINT  uq_order_id
        UNIQUE  (order_id)
);

CREATE TABLE payment_attempts(
    id                      UUID            PRIMARY KEY,
    payment_id              UUID            NOT NULL,
    attempt_no              INT             NOT NULL,
    payment_attempt_status  VARCHAR(50)     NOT NULL,
    idempotency_key         TEXT            NOT NULL,
    checkout_session_id     TEXT,
    payment_intent_id       TEXT,
    redirect_url            TEXT,
    created_at              TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    CONSTRAINT  fk_payment_id
        FOREIGN KEY (payment_id)  REFERENCES  payment(id),
    CONSTRAINT  uq_idempotency_key
        UNIQUE  (idempotency_key),
    CONSTRAINT  uq_payment_id_attempt_no
        UNIQUE  (payment_id, attempt_no),
    CONSTRAINT  ck_attempt_no_positive
        CHECK   (attempt_no >= 1)
);

CREATE INDEX idx_payment_attempts_payment_id ON payment_attempts(payment_id);

--add FK pointer from payment -> current attempt
ALTER TABLE payment
ADD CONSTRAINT  fk_current_attempt_id
FOREIGN KEY (current_attempt_id)
REFERENCES  payment_attempts(id)
ON DELETE SET NULL;

CREATE TABLE processed_provider_events(
    id                  UUID            PRIMARY KEY,
    provider            VARCHAR(50)     NOT NULL,
    provider_event_id   TEXT            NOT NULL,
    received_at         TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    payload_hash        TEXT,
    CONSTRAINT  uq_provider_provider_event_id
        UNIQUE  (provider, provider_event_id)
);

CREATE TABLE outbox_events(
    id                  UUID            PRIMARY KEY,
    idempotency_key     TEXT            NOT NULL,
    event_type          VARCHAR(100)    NOT NULL,
    payload             JSONB           NOT NULL,
    occurred_at         TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    published_at        TIMESTAMPTZ,
    attempt_count       INT             NOT NULL DEFAULT 0,
    last_error          TEXT,
    CONSTRAINT  uq_outbox_events_idempotency_key
        UNIQUE  (idempotency_key)
);

CREATE INDEX idx_outbox_unpublished
ON outbox_events(occurred_at ASC)
WHERE published_at IS NULL;

CREATE TABLE checkout_session_expire_tasks (
    id                      UUID            PRIMARY KEY,
    payment_id              UUID            NOT NULL,
    payment_attempt_id      UUID            NOT NULL,
    checkout_session_id     TEXT            NOT NULL,

    created_at              TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    completed_at            TIMESTAMPTZ,
    attempt_count           INT             NOT NULL DEFAULT 0,
    next_attempt_at         TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    last_error              TEXT,

    CONSTRAINT uq_checkout_session_expire_tasks_payment_attempt_id
        UNIQUE (payment_attempt_id)
);

-- help the worker fetch runnable tasks fast.
-- Runnable = not completed, and next_attempt_at is due.
CREATE INDEX idx_cset_runnable
ON checkout_session_expire_tasks (next_attempt_at, created_at)
WHERE completed_at IS NULL;

