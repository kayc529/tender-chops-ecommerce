CREATE TABLE orders(
    id                  UUID            PRIMARY KEY,
    user_id             UUID            NOT NULL,
    source_quote_id     UUID            NOT NULL,
    order_status        VARCHAR(50)     NOT NULL,
    currency            VARCHAR(3)      NOT NULL,
    total_amount        BIGINT          NOT NULL,
    receiver            VARCHAR(255)    NOT NULL,
    phone               VARCHAR(50)     NOT NULL,
    address_line_1      VARCHAR(255)    NOT NULL,
    address_line_2      VARCHAR(255),
    city                VARCHAR(255)    NOT NULL,
    state_or_province   VARCHAR(255)    NOT NULL,
    postal_code         VARCHAR(255)    NOT NULL,
    country             VARCHAR(255)    NOT NULL,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_source_quote_id
        UNIQUE (source_quote_id)
);

CREATE TABLE order_item(
    id                      UUID            PRIMARY KEY,
    order_id                UUID            NOT NULL,
    product_id              UUID            NOT NULL,
    product_title_snapshot  VARCHAR(255)    NOT NULL,
    unit_price_snapshot     BIGINT             NOT NULL,
    quantity                INT             NOT NULL,
    line_total_amount       BIGINT          NOT NULL,
    CONSTRAINT  fk_order_item_orders
        FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
    CONSTRAINT  uq_order_product
        UNIQUE  (order_id, product_id)
);

CREATE TABLE quote(
    id                          UUID            PRIMARY KEY,
    user_id                     UUID            NOT NULL,
    source_cart_id              UUID            NOT NULL,
    currency                    VARCHAR(3)      NOT NULL,
    total_amount                BIGINT          NOT NULL,
    created_at                  TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    expires_at                  TIMESTAMPTZ     NOT NULL,
    source_cart_version         BIGINT          NOT NULL
);

CREATE TABLE quote_item(
    id                          UUID            PRIMARY KEY,
    quote_id                    UUID            NOT NULL,
    product_id                  UUID            NOT NULL,
    product_title_snapshot      VARCHAR(255)    NOT NULL,
    unit_price_snapshot         BIGINT          NOT NULL,
    quantity                    INT             NOT NULL,
    line_total_amount           BIGINT          NOT NULL,
    CONSTRAINT  fk_quote_item_quote
        FOREIGN KEY (quote_id)  REFERENCES quote(id) ON DELETE CASCADE,
    CONSTRAINT  uq_quote_product
        UNIQUE (quote_id, product_id)
);

CREATE INDEX idx_orders_user_id_created_at ON orders(user_id, created_at DESC);
CREATE INDEX idx_order_item_order_id ON order_item(order_id);
CREATE INDEX idx_quote_user_id ON quote(user_id);
CREATE INDEX idx_quote_item_quote_id ON quote_item(quote_id);