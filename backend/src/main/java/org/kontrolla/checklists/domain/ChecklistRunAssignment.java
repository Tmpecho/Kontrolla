package org.kontrolla.checklists.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import org.kontrolla.common.persistence.AbstractAuditableUuidEntity;
import org.kontrolla.iam.domain.User;

import java.time.Instant;

@Getter
@Entity
@Table(
		name = "checklist_run_assignments",
		uniqueConstraints = @UniqueConstraint(
				name = "uk_checklist_run_assignments_run_user",
				columnNames = {"checklist_run_id", "assigned_user_id"}
		)
)
public class ChecklistRunAssignment extends AbstractAuditableUuidEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "checklist_run_id", nullable = false)
	private ChecklistRun checklistRun;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "assigned_user_id", nullable = false)
	private User assignedUser;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "assigned_by_user_id", nullable = false)
	private User assignedByUser;

	@Column(name = "assigned_at", nullable = false)
	private Instant assignedAt;

	protected ChecklistRunAssignment() {
	}

	public ChecklistRunAssignment(User assignedUser, User assignedByUser, Instant assignedAt) {
		this.assignedUser = assignedUser;
		this.assignedByUser = assignedByUser;
		this.assignedAt = assignedAt;
	}

	void attachTo(ChecklistRun checklistRun) {
		this.checklistRun = checklistRun;
	}
}
