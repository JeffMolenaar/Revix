-- Fix parts table to allow nullable price and currency
-- This aligns the database schema with the Kotlin table definition

ALTER TABLE parts ALTER COLUMN price_cents DROP NOT NULL;
ALTER TABLE parts ALTER COLUMN currency DROP NOT NULL;
ALTER TABLE parts ALTER COLUMN currency DROP DEFAULT;