CREATE TABLE inventory (
    id                  UUID            PRIMARY KEY,
    product_id          UUID            NOT NULL,
    total_quantity      INT             NOT NULL,
    reserved_quantity   INT             NOT NULL DEFAULT 0,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    CONSTRAINT  uq_product_id
        UNIQUE  (product_id),
    CONSTRAINT  chk_inventory_total_non_negative
        CHECK   (total_quantity >= 0),
    CONSTRAINT  chk_inventory_reserve_non_negative
        CHECK   (reserved_quantity >= 0),
    CONSTRAINT  chk_inventory_reserve_lte_total
        CHECK   (reserved_quantity <= total_quantity)
);