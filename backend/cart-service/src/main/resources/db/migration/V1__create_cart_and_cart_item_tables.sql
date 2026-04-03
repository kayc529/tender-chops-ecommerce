CREATE TABLE cart(
    id          UUID        PRIMARY KEY,
    user_id     UUID        NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE cart_item(
    id                              UUID            PRIMARY KEY,
    cart_id                         UUID            NOT NULL,
    product_id                      UUID            NOT NULL,
    quantity                        INT             NOT NULL,
    product_title_snapshot          VARCHAR(255)    NOT NULL,
    product_description_snapshot    VARCHAR(255),
    price_snapshot                  BIGINT          NOT NULL,
    created_at                      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at                      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_cart_item_cart
        FOREIGN KEY (cart_id) REFERENCES cart(id) ON DELETE CASCADE,
    CONSTRAINT uq_cart_item_cart_product
        UNIQUE (cart_id, product_id)
);

CREATE INDEX idx_cart_user_id ON cart(user_id);
CREATE INDEX idx_cart_item_cart_id ON cart_item(cart_id);