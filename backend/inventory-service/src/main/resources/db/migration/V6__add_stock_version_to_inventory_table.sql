ALTER TABLE inventory
    ADD COLUMN stock_version BIGINT NOT NULL DEFAULT 0,
    ADD CONSTRAINT inventory_stock_version_non_negative
        CHECK (stock_version >= 0);