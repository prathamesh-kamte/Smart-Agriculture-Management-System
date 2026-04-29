-- V3__create_advisories_table.sql
CREATE TABLE advisories (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    message TEXT NOT NULL,
    severity VARCHAR(50),
    category VARCHAR(50),
    generated_at DATETIME NOT NULL,
    acknowledged BOOLEAN NOT NULL DEFAULT FALSE,
    crop_id BIGINT,
    farmer_id BIGINT NOT NULL,
    CONSTRAINT fk_advisories_crop FOREIGN KEY (crop_id) REFERENCES crops(id) ON DELETE SET NULL,
    CONSTRAINT fk_advisories_farmer FOREIGN KEY (farmer_id) REFERENCES users(id) ON DELETE CASCADE
);
