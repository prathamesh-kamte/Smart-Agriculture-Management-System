CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT,
    full_name VARCHAR(100),
    email VARCHAR(150),
    password VARCHAR(255) NOT NULL,
    role VARCHAR(20) DEFAULT 'FARMER',
    enabled BOOLEAN DEFAULT TRUE,
    created_at DATETIME,
    updated_at DATETIME,
    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT uc_users_email UNIQUE (email)
);

CREATE TABLE IF NOT EXISTS crops (
    id BIGINT AUTO_INCREMENT,
    crop_name VARCHAR(100) NOT NULL,
    crop_type VARCHAR(100),
    season VARCHAR(20),
    status VARCHAR(30) DEFAULT 'PLANTED',
    planting_date DATE NOT NULL,
    expected_harvest_date DATE,
    actual_harvest_date DATE,
    area_in_acres DOUBLE NOT NULL,
    notes TEXT,
    farmer_id BIGINT,
    created_at DATETIME,
    updated_at DATETIME,
    CONSTRAINT pk_crops PRIMARY KEY (id),
    CONSTRAINT fk_crops_farmer FOREIGN KEY (farmer_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS expenses (
    id BIGINT AUTO_INCREMENT,
    description VARCHAR(150),
    category VARCHAR(80),
    amount DECIMAL(12,2),
    expense_date DATE,
    receipt_reference VARCHAR(255),
    crop_id BIGINT,
    recorded_by_id BIGINT,
    created_at DATETIME,
    updated_at DATETIME,
    CONSTRAINT pk_expenses PRIMARY KEY (id),
    CONSTRAINT fk_expenses_crop FOREIGN KEY (crop_id) REFERENCES crops(id) ON DELETE CASCADE,
    CONSTRAINT fk_expenses_user FOREIGN KEY (recorded_by_id) REFERENCES users(id)
);
