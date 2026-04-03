ALTER TABLE product
    ADD COLUMN image_key VARCHAR(255),
    ADD COLUMN thumbnail_key VARCHAR(255),
    ADD COLUMN image_status VARCHAR(20),
    ADD COLUMN prev_image_key VARCHAR(255),
    ADD COLUMN prev_thumbnail_key VARCHAR(255);