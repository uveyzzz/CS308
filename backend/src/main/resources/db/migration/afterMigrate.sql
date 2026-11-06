-- Flyway runs this file after every migrate, once its lock on the history table is released.
-- A normal migration cannot do this: it would wait forever for that lock.
ALTER TABLE flyway_schema_history ENABLE ROW LEVEL SECURITY;
