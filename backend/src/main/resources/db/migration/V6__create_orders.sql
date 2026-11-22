-- SCRUM-29: orders, order lines and invoices (requirements 3, 4, 12, 14, 15, 16).
-- Price, cost and names are copied into the order at purchase time, so a later price
-- change or discount never changes an old order, its invoice or a refund amount.

CREATE TABLE orders (
    id               BIGSERIAL     PRIMARY KEY,
    user_id          BIGINT        NOT NULL REFERENCES users(id),
    status           VARCHAR(20)   NOT NULL DEFAULT 'PROCESSING'
                     CHECK (status IN ('PROCESSING', 'IN_TRANSIT', 'DELIVERED', 'CANCELLED')),
    total_amount     NUMERIC(12,2) NOT NULL CHECK (total_amount >= 0),
    shipping_address TEXT          NOT NULL,
    billing_address  TEXT          NOT NULL,
    -- Only the last 4 digits are kept, never the full card number (requirements 15, 17).
    card_last4       VARCHAR(4)    NOT NULL CHECK (card_last4 ~ '^[0-9]{4}$'),
    payment_ref      VARCHAR(64)   NOT NULL,                 -- the mock bank's transaction id
    created_at       TIMESTAMPTZ   NOT NULL DEFAULT now(),   -- start of the 30-day refund window
    delivered_at     TIMESTAMPTZ,
    updated_at       TIMESTAMPTZ   NOT NULL DEFAULT now()
);

-- Order history of one customer (requirement 3).
CREATE INDEX idx_orders_user_id ON orders(user_id);

CREATE TABLE order_items (
    id            BIGSERIAL     PRIMARY KEY,
    -- An order line has no meaning without its order (composition), so it goes with it.
    order_id      BIGINT        NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    variant_id    BIGINT        NOT NULL REFERENCES product_variants(id),
    quantity      INT           NOT NULL CHECK (quantity > 0),
    unit_price    NUMERIC(12,2) NOT NULL CHECK (unit_price >= 0),   -- price paid, discount already applied
    discount_rate NUMERIC(5,2)  NOT NULL DEFAULT 0 CHECK (discount_rate BETWEEN 0 AND 100),
    unit_cost     NUMERIC(12,2) NOT NULL CHECK (unit_cost >= 0),    -- for profit/loss (requirement 11)
    product_name  VARCHAR(200)  NOT NULL,
    variant_label VARCHAR(50)   NOT NULL,                           -- e.g. "M / Black"
    -- The same variant appears once per order; the amount is in quantity.
    CONSTRAINT order_items_order_variant_key UNIQUE (order_id, variant_id)
);

CREATE INDEX idx_order_items_variant_id ON order_items(variant_id);

CREATE TABLE invoices (
    id             BIGSERIAL     PRIMARY KEY,
    -- No ON DELETE CASCADE: an invoice is a legal record, so its order cannot be deleted.
    order_id       BIGINT        NOT NULL UNIQUE REFERENCES orders(id),
    invoice_number VARCHAR(30)   NOT NULL UNIQUE,
    total_amount   NUMERIC(12,2) NOT NULL CHECK (total_amount >= 0),
    issued_at      TIMESTAMPTZ   NOT NULL DEFAULT now(),
    emailed_at     TIMESTAMPTZ
);

-- Sales managers search invoices by date range (requirement 11).
CREATE INDEX idx_invoices_issued_at ON invoices(issued_at);

-- Delivery list for the product manager (requirement 12). A view, not a table,
-- so the same data is never stored twice.
CREATE VIEW delivery_list AS
SELECT oi.id                       AS delivery_id,
       o.id                        AS order_id,
       o.user_id                   AS customer_id,
       v.product_id                AS product_id,
       oi.quantity                 AS quantity,
       oi.unit_price * oi.quantity AS total_price,
       o.shipping_address          AS delivery_address,
       (o.status = 'DELIVERED')    AS completed
FROM order_items oi
JOIN orders o           ON o.id = oi.order_id
JOIN product_variants v ON v.id = oi.variant_id;

-- Keep the Supabase Data API out of these tables (see V1).
ALTER TABLE orders      ENABLE ROW LEVEL SECURITY;
ALTER TABLE order_items ENABLE ROW LEVEL SECURITY;
ALTER TABLE invoices    ENABLE ROW LEVEL SECURITY;
-- A view runs with its owner's rights by default, which would skip RLS. This makes it use
-- the caller's rights instead, so the Data API cannot read addresses through it (PostgreSQL 15+).
ALTER VIEW delivery_list SET (security_invoker = true);
