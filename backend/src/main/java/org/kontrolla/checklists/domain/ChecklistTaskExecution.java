package org.kontrolla.checklists.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.kontrolla.common.persistence.AbstractAuditableUuidEntity;
import org.kontrolla.iam.domain.User;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Entity
@Table(name = "checklist_task_executions")
public class ChecklistTaskExecution extends AbstractAuditableUuidEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "checklist_run_id", nullable = false)
	private ChecklistRun checklistRun;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "source_checklist_task_definition_id")
	private ChecklistTaskDefinition sourceChecklistTaskDefinition;

	@Setter
	@Column(name = "title_snapshot", nullable = false, length = 500)
	private String titleSnapshot;

	@Setter
	@Column(name = "details_snapshot", length = 1000)
	private String detailsSnapshot;

	@Setter
	@Enumerated(EnumType.STRING)
	@Column(name = "task_kind_snapshot", nullable = false, length = 32)
	private ChecklistTaskKind taskKindSnapshot;

	@Setter
	@Column(nullable = false)
	private boolean required;

	@Setter
	@Column(name = "sort_order", nullable = false)
	private int sortOrder;

	@Setter
	@Column(name = "measurement_unit_snapshot", length = 32)
	private String measurementUnitSnapshot;

	@Setter
	@Column(name = "minimum_allowed_value_snapshot", precision = 19, scale = 4)
	private BigDecimal minimumAllowedValueSnapshot;

	@Setter
	@Column(name = "maximum_allowed_value_snapshot", precision = 19, scale = 4)
	private BigDecimal maximumAllowedValueSnapshot;

	@Setter
	@Enumerated(EnumType.STRING)
	@Column(name = "execution_status", nullable = false, length = 32)
	private ChecklistTaskExecutionStatus executionStatus;

	@Setter
	@Column(length = 1000)
	private String comment;

	@Setter
	@Enumerated(EnumType.STRING)
	@Column(name = "verification_result", length = 32)
	private ChecklistVerificationResult verificationResult;

	@Setter
	@Column(name = "measured_value", precision = 19, scale = 4)
	private BigDecimal measuredValue;

	@Setter
	@Column(name = "entered_text", length = 2000)
	private String enteredText;

	@Setter
	@Column(name = "resolved_at")
	private Instant resolvedAt;

	@Setter
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "resolved_by_user_id")
	private User resolvedByUser;

	protected ChecklistTaskExecution() {
	}

	public ChecklistTaskExecution(
			ChecklistTaskDefinition sourceChecklistTaskDefinition,
			String titleSnapshot,
			String detailsSnapshot,
			ChecklistTaskKind taskKindSnapshot,
			boolean required,
			int sortOrder,
			String measurementUnitSnapshot,
			BigDecimal minimumAllowedValueSnapshot,
			BigDecimal maximumAllowedValueSnapshot
	) {
		this.sourceChecklistTaskDefinition = sourceChecklistTaskDefinition;
		this.titleSnapshot = titleSnapshot;
		this.detailsSnapshot = detailsSnapshot;
		this.taskKindSnapshot = taskKindSnapshot;
		this.required = required;
		this.sortOrder = sortOrder;
		this.measurementUnitSnapshot = measurementUnitSnapshot;
		this.minimumAllowedValueSnapshot = minimumAllowedValueSnapshot;
		this.maximumAllowedValueSnapshot = maximumAllowedValueSnapshot;
		this.executionStatus = ChecklistTaskExecutionStatus.PENDING;
	}

	public static ChecklistTaskExecution fromDefinitionTask(ChecklistTaskDefinition taskDefinition) {
		return new ChecklistTaskExecution(
				taskDefinition,
				taskDefinition.getTitle(),
				taskDefinition.getDetails(),
				taskDefinition.getTaskKind(),
				taskDefinition.isRequired(),
				taskDefinition.getSortOrder(),
				taskDefinition.getMeasurementUnit(),
				taskDefinition.getMinimumAllowedValue(),
				taskDefinition.getMaximumAllowedValue()
		);
	}

	void attachTo(ChecklistRun checklistRun) {
		this.checklistRun = checklistRun;
	}
}
