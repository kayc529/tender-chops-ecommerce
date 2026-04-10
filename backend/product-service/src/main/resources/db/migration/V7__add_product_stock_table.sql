CREATE TABLE product_stock (
    product_id UUID PRIMARY KEY,
    available_stock INTEGER NOT NULL,
    availability_status VARCHAR(32) NOT NULL,
    stock_version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_product_stock_product
        FOREIGN KEY (product_id)
        REFERENCES product (id)
        ON DELETE CASCADE,
    CONSTRAINT chk_product_stock_available_stock_non_negative
        CHECK (available_stock >= 0)
);