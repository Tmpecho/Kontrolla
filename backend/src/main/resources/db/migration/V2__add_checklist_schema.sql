CREATE TABLE checklist_definitions
(
    id                 CHAR(36)     NOT NULL,
    definition_group_id CHAR(36)    NOT NULL,
    establishment_id   CHAR(36)     NOT NULL,
    service_area       VARCHAR(32)  NOT NULL,
    title              VARCHAR(255) NOT NULL,
    description        VARCHAR(2000) NULL,
    version_number     INT          NOT NULL,
    status             VARCHAR(32)  NOT NULL,
    effective_from     TIMESTAMP    NOT NULL,
    effective_to       TIMESTAMP    NULL,
    created_by_user_id CHAR(36)     NOT NULL,
    updated_by_user_id CHAR(36)     NOT NULL,
    created_at         TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uk_checklist_definitions_group_version UNIQUE (definition_group_id, version_number),
    CONSTRAINT fk_checklist_definitions_establishment
        FOREIGN KEY (establishment_id) REFERENCES establishments (id) ON DELETE CASCADE,
    CONSTRAINT fk_checklist_definitions_created_by_user
        FOREIGN KEY (created_by_user_id) REFERENCES users (id),
    CONSTRAINT fk_checklist_definitions_updated_by_user
        FOREIGN KEY (updated_by_user_id) REFERENCES users (id)
);

CREATE TABLE checklist_item_definitions
(
    id                      CHAR(36)     NOT NULL,
    checklist_definition_id CHAR(36)     NOT NULL,
    prompt                  VARCHAR(500) NOT NULL,
    instruction_text        VARCHAR(1000) NULL,
    response_type           VARCHAR(32)  NOT NULL,
    required                BIT          NOT NULL,
    sort_order              INT          NOT NULL,
    created_at              TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_checklist_item_definitions_definition
        FOREIGN KEY (checklist_definition_id) REFERENCES checklist_definitions (id) ON DELETE CASCADE
);

CREATE TABLE checklist_schedules
(
    id                      CHAR(36)    NOT NULL,
    checklist_definition_id CHAR(36)    NOT NULL,
    schedule_type           VARCHAR(32) NOT NULL,
    start_date              DATE        NOT NULL,
    end_date                DATE        NULL,
    due_time                TIME        NULL,
    weekday_mask            INT         NULL,
    day_of_month            INT         NULL,
    timezone                VARCHAR(64) NOT NULL,
    active                  BIT         NOT NULL,
    created_by_user_id      CHAR(36)    NOT NULL,
    updated_by_user_id      CHAR(36)    NOT NULL,
    created_at              TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_checklist_schedules_definition
        FOREIGN KEY (checklist_definition_id) REFERENCES checklist_definitions (id) ON DELETE CASCADE,
    CONSTRAINT fk_checklist_schedules_created_by_user
        FOREIGN KEY (created_by_user_id) REFERENCES users (id),
    CONSTRAINT fk_checklist_schedules_updated_by_user
        FOREIGN KEY (updated_by_user_id) REFERENCES users (id)
);

CREATE TABLE checklist_runs
(
    id                   CHAR(36)     NOT NULL,
    checklist_definition_id CHAR(36)  NOT NULL,
    definition_group_id  CHAR(36)     NOT NULL,
    establishment_id     CHAR(36)     NOT NULL,
    service_area         VARCHAR(32)  NOT NULL,
    title_snapshot       VARCHAR(255) NOT NULL,
    description_snapshot VARCHAR(2000) NULL,
    due_at               TIMESTAMP    NOT NULL,
    status               VARCHAR(32)  NOT NULL,
    started_at           TIMESTAMP    NULL,
    completed_at         TIMESTAMP    NULL,
    completed_by_user_id CHAR(36)     NULL,
    created_by_user_id   CHAR(36)     NOT NULL,
    created_at           TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at           TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_checklist_runs_definition
        FOREIGN KEY (checklist_definition_id) REFERENCES checklist_definitions (id),
    CONSTRAINT fk_checklist_runs_establishment
        FOREIGN KEY (establishment_id) REFERENCES establishments (id) ON DELETE CASCADE,
    CONSTRAINT fk_checklist_runs_completed_by_user
        FOREIGN KEY (completed_by_user_id) REFERENCES users (id),
    CONSTRAINT fk_checklist_runs_created_by_user
        FOREIGN KEY (created_by_user_id) REFERENCES users (id)
);

CREATE TABLE checklist_run_items
(
    id                                  CHAR(36)     NOT NULL,
    checklist_run_id                    CHAR(36)     NOT NULL,
    source_checklist_item_definition_id CHAR(36)     NULL,
    prompt_snapshot                     VARCHAR(500) NOT NULL,
    instruction_text_snapshot           VARCHAR(1000) NULL,
    response_type                       VARCHAR(32)  NOT NULL,
    required                            BIT          NOT NULL,
    sort_order                          INT          NOT NULL,
    created_at                          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_checklist_run_items_run
        FOREIGN KEY (checklist_run_id) REFERENCES checklist_runs (id) ON DELETE CASCADE,
    CONSTRAINT fk_checklist_run_items_source_definition_item
        FOREIGN KEY (source_checklist_item_definition_id) REFERENCES checklist_item_definitions (id)
);

CREATE TABLE checklist_run_assignments
(
    id                  CHAR(36)   NOT NULL,
    checklist_run_id    CHAR(36)   NOT NULL,
    assigned_user_id    CHAR(36)   NOT NULL,
    assigned_by_user_id CHAR(36)   NOT NULL,
    assigned_at         TIMESTAMP  NOT NULL,
    created_at          TIMESTAMP  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP  NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uk_checklist_run_assignments_run_user UNIQUE (checklist_run_id, assigned_user_id),
    CONSTRAINT fk_checklist_run_assignments_run
        FOREIGN KEY (checklist_run_id) REFERENCES checklist_runs (id) ON DELETE CASCADE,
    CONSTRAINT fk_checklist_run_assignments_assigned_user
        FOREIGN KEY (assigned_user_id) REFERENCES users (id),
    CONSTRAINT fk_checklist_run_assignments_assigned_by_user
        FOREIGN KEY (assigned_by_user_id) REFERENCES users (id)
);

CREATE TABLE checklist_item_responses
(
    id                    CHAR(36)      NOT NULL,
    checklist_run_item_id CHAR(36)      NOT NULL,
    boolean_value         BIT           NULL,
    text_value            VARCHAR(2000) NULL,
    number_value          DECIMAL(19,4) NULL,
    note                  VARCHAR(1000) NULL,
    created_at            TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uk_checklist_item_responses_run_item UNIQUE (checklist_run_item_id),
    CONSTRAINT fk_checklist_item_responses_run_item
        FOREIGN KEY (checklist_run_item_id) REFERENCES checklist_run_items (id) ON DELETE CASCADE
);

CREATE TABLE checklist_run_events
(
    id            CHAR(36)      NOT NULL,
    checklist_run_id CHAR(36)   NOT NULL,
    event_type    VARCHAR(32)   NOT NULL,
    actor_user_id CHAR(36)      NULL,
    occurred_at   TIMESTAMP     NOT NULL,
    metadata_json VARCHAR(4000) NULL,
    created_at    TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_checklist_run_events_run
        FOREIGN KEY (checklist_run_id) REFERENCES checklist_runs (id) ON DELETE CASCADE,
    CONSTRAINT fk_checklist_run_events_actor_user
        FOREIGN KEY (actor_user_id) REFERENCES users (id)
);

CREATE INDEX idx_checklist_definitions_establishment_service_status
    ON checklist_definitions (establishment_id, service_area, status);
CREATE INDEX idx_checklist_definitions_group_id ON checklist_definitions (definition_group_id);
CREATE INDEX idx_checklist_item_definitions_definition_id
    ON checklist_item_definitions (checklist_definition_id);
CREATE INDEX idx_checklist_schedules_definition_id ON checklist_schedules (checklist_definition_id);
CREATE INDEX idx_checklist_runs_establishment_service_status
    ON checklist_runs (establishment_id, service_area, status);
CREATE INDEX idx_checklist_runs_establishment_due_at ON checklist_runs (establishment_id, due_at);
CREATE INDEX idx_checklist_run_items_run_id ON checklist_run_items (checklist_run_id);
CREATE INDEX idx_checklist_run_assignments_assigned_user_id ON checklist_run_assignments (assigned_user_id);
CREATE INDEX idx_checklist_run_events_run_occurred_at ON checklist_run_events (checklist_run_id, occurred_at);
