-- V1: Initial schema for Smart Agriculture Management System
-- Compatible with MySQL 8.0+ and H2 (dev mode)

CREATE TABLE IF NOT EXISTS users (
    id          BIGINT          NOT NULL AUTO_INCREMENT,
    full_name   VARCHAR(100)    NOT NULL,
    email       VARCHAR(150)    NOT NULL,
    password    VARCHAR(255)    NOT NULL,
    role        VARCHAR(20)     NOT NULL DEFAULT 'FARMER',
    enabled     BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at  DATETIME        NOT NULL,
    updated_at  DATETIME        NOT NULL,
    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT uc_users_email UNIQUE (email)
);

CREATE TABLE IF NOT EXISTS crops (
    id                      BIGINT          NOT NULL AUTO_INCREMENT,
    crop_name               VARCHAR(100)    NOT NULL,
    crop_type               VARCHAR(100)    NOT NULL,
    season                  VARCHAR(20)     NOT NULL,
    status                  VARCHAR(30)     NOT NULL DEFAULT 'PLANTED',
    planting_date           DATE            NOT NULL,
    expected_harvest_date   DATE,
    actual_harvest_date     DATE,
    area_in_acres           DOUBLE          NOT NULL,
    notes                   TEXT,
    farmer_id               BIGINT          NOT NULL,
    created_at              DATETIME        NOT NULL,
    updated_at              DATETIME        NOT NULL,
    CONSTRAINT pk_crops PRIMARY KEY (id),
    CONSTRAINT fk_crops_farmer FOREIGN KEY (farmer_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS expenses (
    id                  BIGINT          NOT NULL AUTO_INCREMENT,
    description         VARCHAR(150)    NOT NULL,
    category            VARCHAR(80)     NOT NULL,
    amount              DECIMAL(12,2)   NOT NULL,
    expense_date        DATE            NOT NULL,
    receipt_reference   VARCHAR(255),
    crop_id             BIGINT          NOT NULL,
    recorded_by         BIGINT          NOT NULL,
    created_at          DATETIME        NOT NULL,
    updated_at          DATETIME        NOT NULL,
    CONSTRAINT pk_expenses PRIMARY KEY (id),
    CONSTRAINT fk_expenses_crop     FOREIGN KEY (crop_id)     REFERENCES crops (id) ON DELETE CASCADE,
    CONSTRAINT fk_expenses_user     FOREIGN KEY (recorded_by) REFERENCES users (id)
);
