-- ═══════════════════════════════════════════════════════════════════════════
-- V9: Create disease_detections table
--     Stores the result of each AI-powered crop disease analysis.
--     crop_id uses ON DELETE SET NULL so detection history is preserved
--     even if the crop record is deleted.
-- ═══════════════════════════════════════════════════════════════════════════

CREATE TABLE disease_detections (
    id           BIGINT       AUTO_INCREMENT PRIMARY KEY,
    crop_id      BIGINT,
    farmer_id    BIGINT       NOT NULL,
    photo_url    VARCHAR(500),
    disease_name VARCHAR(200),
    confidence   VARCHAR(20),
    severity     VARCHAR(20),
    description  TEXT,
    treatments   TEXT,
    preventions  TEXT,
    detected_at  DATETIME     NOT NULL,

    CONSTRAINT fk_disease_crop
        FOREIGN KEY (crop_id)   REFERENCES crops(id) ON DELETE SET NULL,

    CONSTRAINT fk_disease_farmer
        FOREIGN KEY (farmer_id) REFERENCES users(id)
);
