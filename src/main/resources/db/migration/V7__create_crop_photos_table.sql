-- ═══════════════════════════════════════════════════════════════════════════
-- V7: Create crop_photos table
--     Stores uploaded photo metadata for each crop.
--     Actual binary files are stored in S3 (or local filesystem in dev).
-- ═══════════════════════════════════════════════════════════════════════════

CREATE TABLE crop_photos (
    id           BIGINT       AUTO_INCREMENT PRIMARY KEY,
    crop_id      BIGINT       NOT NULL,
    farmer_id    BIGINT       NOT NULL,
    photo_url    VARCHAR(500) NOT NULL,
    file_name    VARCHAR(200),
    description  VARCHAR(300),
    photo_date   DATE,
    uploaded_at  DATETIME     NOT NULL,

    CONSTRAINT fk_photos_crop
        FOREIGN KEY (crop_id)   REFERENCES crops(id) ON DELETE CASCADE,

    CONSTRAINT fk_photos_farmer
        FOREIGN KEY (farmer_id) REFERENCES users(id)
);
