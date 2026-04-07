CREATE TABLE deviation_events
(
    id            CHAR(36)      NOT NULL,
    deviation_id  CHAR(36)      NOT NULL,
    event_type    VARCHAR(32)   NOT NULL,
    actor_user_id CHAR(36)      NULL,
    occurred_at   TIMESTAMP     NOT NULL,
    note          VARCHAR(2000) NOT NULL,
    created_at    TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_deviation_events_deviation
        FOREIGN KEY (deviation_id) REFERENCES deviations (id) ON DELETE CASCADE,
    CONSTRAINT fk_deviation_events_actor_user
        FOREIGN KEY (actor_user_id) REFERENCES users (id)
);

CREATE INDEX idx_deviation_events_deviation_occurred_at
    ON deviation_events (deviation_id, occurred_at);
CREATE INDEX idx_deviation_events_actor_user_id
    ON deviation_events (actor_user_id);

INSERT INTO deviation_events (
    id,
    deviation_id,
    event_type,
    actor_user_id,
    occurred_at,
    note,
    created_at,
    updated_at
)
SELECT UUID(),
       id,
       'REPORTED',
       created_by_user_id,
       created_at,
       'Deviation reported.',
       created_at,
       updated_at
FROM deviations;
