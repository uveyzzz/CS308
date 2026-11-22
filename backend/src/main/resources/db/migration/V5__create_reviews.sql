-- SCRUM-32: ratings and comments (requirement 5).
-- A rating counts right away; a comment is shown only after a product manager approves it.
CREATE TABLE reviews (
    id             BIGSERIAL   PRIMARY KEY,
    product_id     BIGINT      NOT NULL REFERENCES products(id),
    user_id        BIGINT      NOT NULL REFERENCES users(id),
    rating         INT         NOT NULL CHECK (rating BETWEEN 1 AND 5),
    comment        TEXT,
    comment_status VARCHAR(20) CHECK (comment_status IN ('PENDING', 'APPROVED', 'REJECTED')),
    moderated_by   BIGINT      REFERENCES users(id),
    moderated_at   TIMESTAMPTZ,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    -- One review per user per product.
    CONSTRAINT reviews_user_product_key UNIQUE (user_id, product_id),
    -- A review has a comment status exactly when it has a comment.
    CONSTRAINT reviews_status_only_with_comment CHECK ((comment IS NULL) = (comment_status IS NULL))
);

CREATE INDEX idx_reviews_product_id ON reviews(product_id);
-- The product manager's queue of comments waiting for approval.
CREATE INDEX idx_reviews_pending ON reviews(created_at) WHERE comment_status = 'PENDING';

-- Keep the Supabase Data API out of this table (see V1).
ALTER TABLE reviews ENABLE ROW LEVEL SECURITY;
