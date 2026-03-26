CREATE TABLE checklist_task_definitions
(
    id                    CHAR(36)      NOT NULL,
    checklist_definition_id CHAR(36)    NOT NULL,
    title                 VARCHAR(500)  NOT NULL,
    details               VARCHAR(1000) NULL,
    task_kind             VARCHAR(32)   NOT NULL,
    required              BIT           NOT NULL,
    sort_order            INT           NOT NULL,
    measurement_unit      VARCHAR(32)   NULL,
    minimum_allowed_value DECIMAL(19, 4) NULL,
    maximum_allowed_value DECIMAL(19, 4) NULL,
    created_at            TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_checklist_task_definitions_definition
        FOREIGN KEY (checklist_definition_id) REFERENCES checklist_definitions (id) ON DELETE CASCADE
);

INSERT INTO checklist_task_definitions (
    id,
    checklist_definition_id,
    title,
    details,
    task_kind,
    required,
    sort_order,
    measurement_unit,
    minimum_allowed_value,
    maximum_allowed_value,
    created_at,
    updated_at
)
SELECT id,
       checklist_definition_id,
       prompt,
       instruction_text,
       CASE response_type
           WHEN 'BOOLEAN' THEN 'VERIFICATION'
           WHEN 'TEXT' THEN 'TEXT_ENTRY'
           WHEN 'NUMBER' THEN 'MEASUREMENT'
           ELSE 'ACTION'
           END,
       required,
       sort_order,
       NULL,
       NULL,
       NULL,
       created_at,
       updated_at
FROM checklist_item_definitions;

CREATE TABLE checklist_task_executions
(
    id                                 CHAR(36)      NOT NULL,
    checklist_run_id                   CHAR(36)      NOT NULL,
    source_checklist_task_definition_id CHAR(36)     NULL,
    title_snapshot                     VARCHAR(500)  NOT NULL,
    details_snapshot                   VARCHAR(1000) NULL,
    task_kind_snapshot                 VARCHAR(32)   NOT NULL,
    required                           BIT           NOT NULL,
    sort_order                         INT           NOT NULL,
    measurement_unit_snapshot          VARCHAR(32)   NULL,
    minimum_allowed_value_snapshot     DECIMAL(19, 4) NULL,
    maximum_allowed_value_snapshot     DECIMAL(19, 4) NULL,
    execution_status                   VARCHAR(32)   NOT NULL,
    comment                            VARCHAR(1000) NULL,
    verification_result                VARCHAR(32)   NULL,
    measured_value                     DECIMAL(19, 4) NULL,
    entered_text                       VARCHAR(2000) NULL,
    resolved_at                        TIMESTAMP     NULL,
    resolved_by_user_id                CHAR(36)      NULL,
    created_at                         TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                         TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_checklist_task_executions_run
        FOREIGN KEY (checklist_run_id) REFERENCES checklist_runs (id) ON DELETE CASCADE,
    CONSTRAINT fk_checklist_task_executions_source_definition
        FOREIGN KEY (source_checklist_task_definition_id) REFERENCES checklist_task_definitions (id),
    CONSTRAINT fk_checklist_task_executions_resolved_by_user
        FOREIGN KEY (resolved_by_user_id) REFERENCES users (id)
);

INSERT INTO checklist_task_executions (
    id,
    checklist_run_id,
    source_checklist_task_definition_id,
    title_snapshot,
    details_snapshot,
    task_kind_snapshot,
    required,
    sort_order,
    measurement_unit_snapshot,
    minimum_allowed_value_snapshot,
    maximum_allowed_value_snapshot,
    execution_status,
    comment,
    verification_result,
    measured_value,
    entered_text,
    resolved_at,
    resolved_by_user_id,
    created_at,
    updated_at
)
SELECT run_item.id,
       run_item.checklist_run_id,
       run_item.source_checklist_item_definition_id,
       run_item.prompt_snapshot,
       run_item.instruction_text_snapshot,
       CASE run_item.response_type
           WHEN 'BOOLEAN' THEN 'VERIFICATION'
           WHEN 'TEXT' THEN 'TEXT_ENTRY'
           WHEN 'NUMBER' THEN 'MEASUREMENT'
           ELSE 'ACTION'
           END,
       run_item.required,
       run_item.sort_order,
       NULL,
       NULL,
       NULL,
       CASE
           WHEN response.id IS NULL THEN 'PENDING'
           ELSE 'COMPLETED'
           END,
       response.note,
       CASE
           WHEN run_item.response_type = 'BOOLEAN' AND response.boolean_value = b'1' THEN 'VERIFIED'
           WHEN run_item.response_type = 'BOOLEAN' AND response.boolean_value = b'0' THEN 'NOT_VERIFIED'
           ELSE NULL
           END,
       CASE
           WHEN run_item.response_type = 'NUMBER' THEN response.number_value
           ELSE NULL
           END,
       CASE
           WHEN run_item.response_type = 'TEXT' THEN response.text_value
           ELSE NULL
           END,
       CASE
           WHEN response.id IS NULL THEN NULL
           ELSE COALESCE(run.completed_at, response.updated_at, run.updated_at)
           END,
       CASE
           WHEN response.id IS NULL THEN NULL
           ELSE run.completed_by_user_id
           END,
       run_item.created_at,
       run_item.updated_at
FROM checklist_run_items run_item
         LEFT JOIN checklist_item_responses response ON response.checklist_run_item_id = run_item.id
         LEFT JOIN checklist_runs run ON run.id = run_item.checklist_run_id;

DROP TABLE checklist_item_responses;
DROP TABLE checklist_run_items;
DROP TABLE checklist_item_definitions;

CREATE INDEX idx_checklist_task_definitions_definition_id
    ON checklist_task_definitions (checklist_definition_id);
CREATE INDEX idx_checklist_task_executions_run_id
    ON checklist_task_executions (checklist_run_id);
CREATE INDEX idx_checklist_task_executions_resolved_by_user_id
    ON checklist_task_executions (resolved_by_user_id);
