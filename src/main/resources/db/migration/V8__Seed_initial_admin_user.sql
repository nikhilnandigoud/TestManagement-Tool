-- V8__Seed_initial_admin_user.sql
-- Seed initial admin user for first-time login
-- Default credentials: admin / admin123
-- Password hash is BCrypt: $2a$10$slYQmyNdGzin7olVN3p5be4DlH.PKZbv5H8KnzzVgXXbVxzy990Ptu

INSERT INTO users (username, email, password, full_name, is_active, created_at, updated_at)
VALUES (
    'admin',
    'admin@testmanagement.com',
    '$2a$10$slYQmyNdGzin7olVN3p5be4DlH.PKZbv5H8KnzzVgXXbVxzy990Ptu',
    'Administrator',
    true,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
)
ON CONFLICT (username) DO NOTHING;
