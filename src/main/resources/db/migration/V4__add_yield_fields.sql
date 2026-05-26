-- V4__add_yield_fields.sql
-- Adds yield tracking and selling price columns to the crops table.
-- All three columns are nullable so existing crop rows are unaffected.

ALTER TABLE crops ADD COLUMN expected_yield_kg  DOUBLE;
ALTER TABLE crops ADD COLUMN actual_yield_kg    DOUBLE;
ALTER TABLE crops ADD COLUMN selling_price_per_kg DECIMAL(10,2);
