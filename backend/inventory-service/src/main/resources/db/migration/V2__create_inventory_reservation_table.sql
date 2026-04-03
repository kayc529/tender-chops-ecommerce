CREATE TABLE inventory_reservation(
    id                  UUID            PRIMARY KEY,
    product_id          UUID            NOT NULL,
    quote_id            UUID            NOT NULL,
    order_id            UUID,
    quantity            INT             NOT NULL,
    reservation_status  VARCHAR(50)     NOT NULL,
    created_at          TIMESTAMPTZ     NOT NULL,
    expires_at          TIMESTAMPTZ     NOT NULL,
    released_at         TIMESTAMPTZ,
    committed_at        TIMESTAMPTZ,
    CONSTRAINT  uq_product_quote
        UNIQUE  (product_id, quote_id),
    CONSTRAINT  chk_quantity_positive
        CHECK   (quantity > 0)
);

CREATE INDEX idx_quote_reservation_status ON inventory_reservation(quote_id, reservation_status);
CREATE INDEX idx_reservation_status_expires_at ON inventory_reservation(reservation_status, expires_at);