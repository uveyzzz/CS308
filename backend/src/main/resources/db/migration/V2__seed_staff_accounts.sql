-- Staff accounts. Sign-up only ever creates customers, so this is the only way these roles get an
-- account (SCRUM-11). The repo is public: only BCrypt hashes belong here. The password itself is
-- shared with the team outside the repo and must stay long and random.
INSERT INTO users (full_name, email, password_hash, role) VALUES
    ('Sales Manager',   'sales.manager@store.test',   '$2a$10$mSRYZUEZPGFPRoYaUnwT0eJAZhMQ9G2i2LF3rjez/MqrjiMuPAU2C', 'SALES_MANAGER'),
    ('Product Manager', 'product.manager@store.test', '$2a$10$vqrCpXN2OgzrdXFbZT5IOOtlcYtmlY3jZcAZ8xKl4yTK5my0Jaa8i', 'PRODUCT_MANAGER'),
    ('Support Agent',   'support.agent@store.test',   '$2a$10$nwoPd.1CK3lCe7BmG9rqveAEpx3ZBmoqZ18c.p1UN5aqrlO94YG6G', 'SUPPORT_AGENT');
