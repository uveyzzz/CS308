-- SCRUM-16: categories can have a parent (Women > Tops) and get a slug for /category/:slug.

ALTER TABLE categories ADD COLUMN parent_id BIGINT REFERENCES categories(id);
ALTER TABLE categories ADD COLUMN slug VARCHAR(120);

-- Rows that already exist get a slug made from their name: 'Outer Wear' -> 'outer-wear'.
UPDATE categories
SET slug = trim(BOTH '-' FROM lower(regexp_replace(name, '[^a-zA-Z0-9]+', '-', 'g')));

ALTER TABLE categories ALTER COLUMN slug SET NOT NULL;
ALTER TABLE categories ADD CONSTRAINT categories_slug_key UNIQUE (slug);

-- 'Tops' can now exist under both Women and Men, so a name only has to be unique among its
-- siblings. NULLS NOT DISTINCT makes two top-level categories with the same name still clash.
ALTER TABLE categories DROP CONSTRAINT categories_name_key;
ALTER TABLE categories ADD CONSTRAINT categories_parent_name_key UNIQUE NULLS NOT DISTINCT (parent_id, name);

ALTER TABLE categories ADD CONSTRAINT categories_not_own_parent CHECK (parent_id <> id);

CREATE INDEX idx_categories_parent_id ON categories(parent_id);
