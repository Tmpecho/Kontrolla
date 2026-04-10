CREATE TABLE establishment_serving_hours
(
    id              CHAR(36) NOT NULL,
    establishment_id CHAR(36) NOT NULL,
    day_of_week     VARCHAR(16) NOT NULL,
    is_closed       BIT(1) NOT NULL,
    opens_at        TIME NULL,
    closes_at       TIME NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_establishment_serving_hours_establishment
        FOREIGN KEY (establishment_id) REFERENCES establishments (id) ON DELETE CASCADE,
    CONSTRAINT uq_establishment_serving_hours_establishment_day
        UNIQUE (establishment_id, day_of_week)
);

CREATE INDEX idx_establishment_serving_hours_establishment_id
    ON establishment_serving_hours (establishment_id);
