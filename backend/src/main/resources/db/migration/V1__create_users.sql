-- Every user is one row with one role, so privileges never mix (requirements 10, 17).
CREATE TABLE users (
    id              BIGSERIAL    PRIMARY KEY,
    full_name       VARCHAR(100) NOT NULL,
    email           VARCHAR(255) NOT NULL UNIQUE,  -- always stored in lower case
    password_hash   VARCHAR(100) NOT NULL,         -- BCrypt, never the password itself
    role            VARCHAR(20)  NOT NULL DEFAULT 'CUSTOMER'
                    CHECK (role IN ('CUSTOMER', 'SALES_MANAGER', 'PRODUCT_MANAGER', 'SUPPORT_AGENT')),
    tax_id          VARCHAR(255),                  -- must be encrypted before the API accepts it
    home_address    TEXT,
    billing_address TEXT,
    gender          VARCHAR(20)
                    CHECK (gender IN ('FEMALE', 'MALE', 'OTHER', 'PREFER_NOT_TO_SAY')),
    birth_date      DATE         CHECK (birth_date < CURRENT_DATE),
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);
