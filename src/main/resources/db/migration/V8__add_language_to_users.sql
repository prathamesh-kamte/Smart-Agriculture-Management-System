-- ═══════════════════════════════════════════════════════════════════════════
-- V8: Add preferred_language column to users table
--     Stores the farmer's preferred language tag (BCP 47 format).
--     Supported values: 'en' (English), 'hi' (Hindi), 'mr' (Marathi).
--     Defaults to 'en' for all existing and new users.
-- ═══════════════════════════════════════════════════════════════════════════

ALTER TABLE users
    ADD COLUMN preferred_language VARCHAR(5) NOT NULL DEFAULT 'en';
