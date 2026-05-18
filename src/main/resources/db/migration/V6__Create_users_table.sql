-- V6__Create_users_table.sql
-- Create users table for authentication

CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    full_name VARCHAR(100),
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create index on username for faster lookup
CREATE INDEX IF NOT EXISTS idx_users_username ON users(username);
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);

-- Add comment
COMMENT ON TABLE users IS 'User accounts for AI-TestManagement Tool authentication';
COMMENT ON COLUMN users.username IS 'Unique username for login';
COMMENT ON COLUMN users.email IS 'User email address';
COMMENT ON COLUMN users.password IS 'Hashed password using BCrypt';
COMMENT ON COLUMN users.full_name IS 'User full name';
COMMENT ON COLUMN users.is_active IS 'Account active status';
