-- ---------------------------------------------------------
-- PRODUCT
-- ---------------------------------------------------------
INSERT INTO product (
    id,
    title,
    description,
    base_price,
    image_key,
    thumbnail_key,
    pending_image_key,
    pending_thumbnail_key,
    image_upload_status,
    portion_description,
    category,
    created_at,
    updated_at
)
VALUES
(
    '3f4e2a10-6f2b-4c58-9d3c-1a8b7e4f1001',
    'Beef Short Ribs',
    'Thick-cut beef short ribs, perfect for slow cooking and braising.',
    1550,
    NULL,
    NULL,
    NULL,
    NULL,
    NULL,
    '500g pack',
    'BEEF',
    NOW(),
    NOW()
),
(
    '3f4e2a10-6f2b-4c58-9d3c-1a8b7e4f1002',
    'Chicken Breast Fillet',
    'Lean and tender chicken breast fillets.',
    720,
    NULL,
    NULL,
    NULL,
    NULL,
    NULL,
    '2 pieces (~400g)',
    'CHICKEN',
    NOW(),
    NOW()
),
(
    '3f4e2a10-6f2b-4c58-9d3c-1a8b7e4f1003',
    'Chicken Thighs',
    'Juicy boneless chicken thighs with rich flavor.',
    680,
    NULL,
    NULL,
    NULL,
    NULL,
    NULL,
    '500g pack',
    'CHICKEN',
    NOW(),
    NOW()
),
(
    '3f4e2a10-6f2b-4c58-9d3c-1a8b7e4f1004',
    'Grass-Fed Ribeye Steak',
    'Richly marbled ribeye steak with deep beef flavor.',
    1890,
    NULL,
    NULL,
    NULL,
    NULL,
    NULL,
    '300g per piece',
    'BEEF',
    NOW(),
    NOW()
),
(
    '3f4e2a10-6f2b-4c58-9d3c-1a8b7e4f1005',
    'Lamb Chops',
    'Juicy lamb chops with a rich, slightly gamey flavor.',
    1680,
    NULL,
    NULL,
    NULL,
    NULL,
    NULL,
    '4 pieces (~400g)',
    'LAMB',
    NOW(),
    NOW()
),
(
    '3f4e2a10-6f2b-4c58-9d3c-1a8b7e4f1006',
    'Pork Belly Slices',
    'Thick-cut pork belly slices with alternating layers of meat and fat.',
    950,
    NULL,
    NULL,
    NULL,
    NULL,
    NULL,
    '300g pack',
    'PORK',
    NOW(),
    NOW()
),
(
    '3f4e2a10-6f2b-4c58-9d3c-1a8b7e4f1007',
    'Pork Collar Steak',
    'Tender pork collar steak with excellent marbling.',
    890,
    NULL,
    NULL,
    NULL,
    NULL,
    NULL,
    '250g per piece',
    'PORK',
    NOW(),
    NOW()
),
(
    '3f4e2a10-6f2b-4c58-9d3c-1a8b7e4f1008',
    'Salmon Fillet',
    'Fresh Atlantic salmon fillet, rich in omega-3.',
    1250,
    NULL,
    NULL,
    NULL,
    NULL,
    NULL,
    '200g per piece',
    'SEAFOOD',
    NOW(),
    NOW()
),
(
    '3f4e2a10-6f2b-4c58-9d3c-1a8b7e4f1009',
    'Tiger Prawns',
    'Large tiger prawns with firm texture and natural sweetness.',
    1430,
    NULL,
    NULL,
    NULL,
    NULL,
    NULL,
    '300g pack',
    'SEAFOOD',
    NOW(),
    NOW()
);

-- ---------------------------------------------------------
-- PRODUCT STOCK
-- available_stock = inventory.total_quantity because reserved_quantity = 0
-- stock_version matches corresponding inventory.stock_version
-- 0 -> OUT_OF_STOCK
-- 1-9 -> LOW_IN_STOCK
-- >9 -> IN_STOCK
-- ---------------------------------------------------------
INSERT INTO product_stock (
    product_id,
    available_stock,
    availability_status,
    stock_version,
    created_at,
    updated_at
)
VALUES
(
    '3f4e2a10-6f2b-4c58-9d3c-1a8b7e4f1001',
    18,
    'IN_STOCK',
    4,
    NOW(),
    NOW()
),
(
    '3f4e2a10-6f2b-4c58-9d3c-1a8b7e4f1002',
    27,
    'IN_STOCK',
    7,
    NOW(),
    NOW()
),
(
    '3f4e2a10-6f2b-4c58-9d3c-1a8b7e4f1003',
    6,
    'LOW_IN_STOCK',
    2,
    NOW(),
    NOW()
),
(
    '3f4e2a10-6f2b-4c58-9d3c-1a8b7e4f1004',
    14,
    'IN_STOCK',
    9,
    NOW(),
    NOW()
),
(
    '3f4e2a10-6f2b-4c58-9d3c-1a8b7e4f1005',
    0,
    'OUT_OF_STOCK',
    5,
    NOW(),
    NOW()
),
(
    '3f4e2a10-6f2b-4c58-9d3c-1a8b7e4f1006',
    33,
    'IN_STOCK',
    1,
    NOW(),
    NOW()
),
(
    '3f4e2a10-6f2b-4c58-9d3c-1a8b7e4f1007',
    8,
    'LOW_IN_STOCK',
    6,
    NOW(),
    NOW()
),
(
    '3f4e2a10-6f2b-4c58-9d3c-1a8b7e4f1008',
    21,
    'IN_STOCK',
    3,
    NOW(),
    NOW()
),
(
    '3f4e2a10-6f2b-4c58-9d3c-1a8b7e4f1009',
    12,
    'IN_STOCK',
    10,
    NOW(),
    NOW()
);