-- V7__Fix_users_id_column_type.sql
-- Fix the users table id column type from SERIAL (INTEGER) to BIGSERIAL (BIGINT)

-- Step 1: Check if users table exists and if id column has wrong type
-- This migration alters the id column from SERIAL to BIGINT

-- Create a temporary sequence for the new BIGINT id
CREATE SEQUENCE IF NOT EXISTS users_id_seq_new AS BIGINT START WITH 1;

-- Alter the id column type from INTEGER to BIGINT
-- Note: We drop the default first, then recreate it with the new sequence
ALTER TABLE users DROP CONSTRAINT IF EXISTS users_pkey;
ALTER TABLE users ALTER COLUMN id DROP DEFAULT;
ALTER TABLE users ALTER COLUMN id SET DATA TYPE BIGINT USING id;
ALTER SEQUENCE IF EXISTS users_id_seq OWNER TO postgres;
ALTER TABLE users ALTER COLUMN id SET DEFAULT nextval('users_id_seq'::regclass);
ALTER TABLE users ADD CONSTRAINT users_pkey PRIMARY KEY (id);

-- Clean up any temporary sequences
DROP SEQUENCE IF EXISTS users_id_seq_new;

-- Verify the change
-- SELECT column_name, data_type FROM information_schema.columns 
-- WHERE table_name = 'users' AND column_name = 'id';
