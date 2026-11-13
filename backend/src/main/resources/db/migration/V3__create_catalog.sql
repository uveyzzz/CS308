-- Catalog tables (requirements 1, 3, 7, 9, 11). The product is split in three:
-- products holds what listings show, product_details what only the detail page shows,
-- and product_costs what only managers see.

CREATE TABLE categories (
    id          BIGSERIAL    PRIMARY KEY,
    name        VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(255),
    is_active   BOOLEAN      NOT NULL DEFAULT TRUE   -- deactivate instead of delete
);

CREATE TABLE products (
    id             BIGSERIAL     PRIMARY KEY,
    category_id    BIGINT        NOT NULL REFERENCES categories(id),
    name           VARCHAR(200)  NOT NULL,                 -- searched (requirement 7)
    slug           VARCHAR(220)  NOT NULL UNIQUE,          -- readable URL, e.g. basic-tee-black
    price          NUMERIC(12,2) NOT NULL CHECK (price > 0),
    discount_rate  NUMERIC(5,2)  NOT NULL DEFAULT 0 CHECK (discount_rate BETWEEN 0 AND 100),
    final_price    NUMERIC(12,2) GENERATED ALWAYS AS (ROUND(price * (1 - discount_rate / 100), 2)) STORED,
    main_image_url VARCHAR(500),
    avg_rating     NUMERIC(3,2)  NOT NULL DEFAULT 0,
    rating_count   INT           NOT NULL DEFAULT 0,
    sales_count    INT           NOT NULL DEFAULT 0,       -- popularity sort (requirement 7)
    is_active      BOOLEAN       NOT NULL DEFAULT TRUE,    -- deactivate instead of delete
    created_at     TIMESTAMPTZ   NOT NULL DEFAULT now()
);

CREATE INDEX idx_products_category_id ON products(category_id);
CREATE INDEX idx_products_final_price ON products(final_price);

CREATE TABLE product_details (
    product_id          BIGINT       PRIMARY KEY REFERENCES products(id),
    description         TEXT         NOT NULL,             -- searched (requirement 7)
    model               VARCHAR(100) NOT NULL,             -- requirement 9
    serial_number       VARCHAR(100) NOT NULL UNIQUE,      -- requirement 9
    material            VARCHAR(200),
    care_instructions   TEXT,
    fit                 VARCHAR(50),                       -- e.g. slim, regular
    warranty_status     VARCHAR(100) NOT NULL,             -- requirement 9
    distributor_name    VARCHAR(200) NOT NULL,             -- requirement 9
    distributor_contact VARCHAR(200)
);

CREATE TABLE product_costs (
    product_id BIGINT        PRIMARY KEY REFERENCES products(id),
    cost       NUMERIC(12,2) CHECK (cost >= 0)             -- NULL means 50% of price (requirement 11)
);

-- Stock lives on the size/colour combination, not on the product (requirement 3).
CREATE TABLE product_variants (
    id             BIGSERIAL   PRIMARY KEY,
    product_id     BIGINT      NOT NULL REFERENCES products(id),
    size           VARCHAR(10) NOT NULL,
    color          VARCHAR(30) NOT NULL,
    stock_quantity INT         NOT NULL CHECK (stock_quantity >= 0),
    sku            VARCHAR(50) NOT NULL UNIQUE,            -- stock code, e.g. TSH-BLK-M
    UNIQUE (product_id, size, color)
);

-- Keep the Supabase Data API out of these tables (see V1).
ALTER TABLE categories       ENABLE ROW LEVEL SECURITY;
ALTER TABLE products         ENABLE ROW LEVEL SECURITY;
ALTER TABLE product_details  ENABLE ROW LEVEL SECURITY;
ALTER TABLE product_costs    ENABLE ROW LEVEL SECURITY;
ALTER TABLE product_variants ENABLE ROW LEVEL SECURITY;