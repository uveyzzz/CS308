-- SCRUM-24: the logged-in customer's cart, so it survives a logout and follows the user to any
-- device (requirement 4). A guest's cart stays in the browser and is merged in at login.
-- Prices are not stored here: the cart always shows the current price.
CREATE TABLE cart_items (
    user_id    BIGINT      NOT NULL REFERENCES users(id),
    variant_id BIGINT      NOT NULL REFERENCES product_variants(id),
    quantity   INT         NOT NULL CHECK (quantity > 0),
    added_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (user_id, variant_id)   -- one line per variant
);

-- Keep the Supabase Data API out of this table (see V1).
ALTER TABLE cart_items ENABLE ROW LEVEL SECURITY;
