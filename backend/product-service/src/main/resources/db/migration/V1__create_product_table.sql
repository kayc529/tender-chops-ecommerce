CREATE TABLE product (
    id  UUID PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description VARCHAR (255),
    base_price BIGINT NOT NULL
);