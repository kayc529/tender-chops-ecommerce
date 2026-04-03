ALTER TABLE product
    DROP COLUMN IF EXISTS image_status,
    DROP COLUMN IF EXISTS prev_image_key,
    DROP COLUMN IF EXISTS prev_thumbnail_key,
    ADD COLUMN pending_image_key VARCHAR(255),
    ADD COLUMN pending_thumbnail_key VARCHAR(255),
    ADD COLUMN image_upload_status VARCHAR(20);