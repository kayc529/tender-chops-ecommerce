ALTER TABLE product
    ADD COLUMN portion_description VARCHAR(100),
    ADD COLUMN category VARCHAR(30),
    ADD COLUMN created_at TIMESTAMPTZ,
    ADD COLUMN updated_at TIMESTAMPTZ;

CREATE INDEX idx_product_category_created_at
ON product (category, created_at);