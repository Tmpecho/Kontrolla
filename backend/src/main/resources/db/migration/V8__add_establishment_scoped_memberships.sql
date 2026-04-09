ALTER TABLE organization_memberships
    ADD COLUMN access_all_establishments BIT NOT NULL DEFAULT b'1';

CREATE TABLE organization_membership_establishments
(
    membership_id    CHAR(36) NOT NULL,
    establishment_id CHAR(36) NOT NULL,
    PRIMARY KEY (membership_id, establishment_id),
    CONSTRAINT fk_membership_establishments_membership
        FOREIGN KEY (membership_id) REFERENCES organization_memberships (id) ON DELETE CASCADE,
    CONSTRAINT fk_membership_establishments_establishment
        FOREIGN KEY (establishment_id) REFERENCES establishments (id) ON DELETE CASCADE
);

CREATE INDEX idx_membership_establishments_establishment_id
    ON organization_membership_establishments (establishment_id);
