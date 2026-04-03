CREATE TABLE product_stale_image_cleanup_task (
    id UUID PRIMARY KEY,
    product_id UUID NOT NULL,
    image_key VARCHAR(255) NOT NULL,
    thumbnail_key VARCHAR(255) NOT NULL,
    attempt_count INT NOT NULL DEFAULT 0,
    last_error TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    processed_at TIMESTAMPTZ,
    CONSTRAINT fk_product_stale_image_cleanup_task_product
        FOREIGN KEY (product_id) REFERENCES product(id),
    CONSTRAINT uq_product_stale_image_cleanup_task_product_image_thumbnail
        UNIQUE (product_id, image_key, thumbnail_key)
);

CREATE INDEX idx_product_stale_image_cleanup_task_pending_created_at
    ON product_stale_image_cleanup_task (created_at ASC)
    WHERE processed_at IS NULL;