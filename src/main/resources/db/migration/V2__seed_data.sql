-- V2: Seed data for development and testing
-- Passwords are BCrypt-encoded with strength 12.
-- Plain-text values:
--   admin@smartagri.com  → Admin@123
--   farmer@smartagri.com → Farmer@123

INSERT INTO users (full_name, email, password, role, enabled, created_at, updated_at)
VALUES
    ('System Admin',
     'admin@smartagri.com',
     '$2a$12$rDsWWFCF5PRHN4ERR/BnfOqxT0u2Eu./aVF6QQLE50qVi1LOT9R1a',
     'ADMIN',
     TRUE,
     NOW(),
     NOW()),
    ('Ramesh Kumar',
     'farmer@smartagri.com',
     '$2a$12$7NZOuBikRoZVWxmLh7e9t.O7aqHezPFm7LFM22XZQZ2jBPUSAH7K',
     'FARMER',
     TRUE,
     NOW(),
     NOW());

-- Seed crops for the demo farmer (id will be 2 after the inserts above)
INSERT INTO crops (crop_name, crop_type, season, status, planting_date, expected_harvest_date, area_in_acres, notes, farmer_id, created_at, updated_at)
VALUES
    ('Basmati Rice',    'Cereal',     'KHARIF',    'GROWING',  '2024-03-01', '2024-06-01',  3.5, 'Premium variety, drip irrigation', 2, NOW(), NOW()),
    ('Wheat',           'Cereal',     'RABI',      'PLANTED',  '2024-04-05', '2024-08-05', 5.0, 'Rainfed plot',                     2, NOW(), NOW()),
    ('Tomato',          'Vegetable',  'ZAID',      'GROWING',  '2024-03-15', '2024-05-15',  1.2, 'Greenhouse cultivation',           2, NOW(), NOW());

-- Seed expenses for Basmati Rice (crop id 1)
INSERT INTO expenses (description, category, amount, expense_date, crop_id, recorded_by_id, created_at, updated_at)
VALUES
    ('NPK Fertiliser — 50 kg bag',   'FERTILISATION',  1200.00, '2024-03-05', 1, 2, NOW(), NOW()),
    ('Daily labour — 5 workers',      'LABOUR',         2500.00, '2024-03-10', 1, 2, NOW(), NOW()),
    ('Diesel for water pump',         'IRRIGATION',      850.00, '2024-03-17', 1, 2, NOW(), NOW()),
    ('Pesticide — chlorpyrifos',      'PEST_CONTROL',    650.00, '2024-03-25', 1, 2, NOW(), NOW());
