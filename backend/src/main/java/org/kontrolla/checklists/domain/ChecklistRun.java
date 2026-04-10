package org.kontrolla.checklists.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.kontrolla.common.persistence.AbstractAuditableUuidEntity;
import org.kontrolla.establishments.domain.Establishment;
import org.kontrolla.iam.domain.User;

import java.time.Instant;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * A scheduled or manually created checklist run derived from a checklist definition.
 */
@Getter
@Entity
@Table(name = "checklist_runs")
public class ChecklistRun extends AbstractAuditableUuidEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "checklist_definition_id", nullable = false)
	private ChecklistDefinition checklistDefinition;

	@JdbcTypeCode(SqlTypes.CHAR)
	@Column(name = "definition_group_id", nullable = false, updatable = false, length = 36)
	private UUID definitionGroupId;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "establishment_id", nullable = false)
	private Establishment establishment;

	@Setter
	@Enumerated(EnumType.STRING)
	@Column(name = "service_area", nullable = false, length = 32)
	private ChecklistServiceArea serviceArea;

	@Setter
	@Column(name = "title_snapshot", nullable = false)
	private String titleSnapshot;

	@Setter
	@Column(name = "description_snapshot", length = 2000)
	private String descriptionSnapshot;

	@Setter
	@Column(name = "due_at", nullable = false)
	private Instant dueAt;

	@Setter
	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 32)
	private ChecklistRunStatus status;

	@Setter
	@Column(name = "started_at")
	private Instant startedAt;

	@Setter
	@Column(name = "completed_at")
	private Instant completedAt;

	@Setter
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "completed_by_user_id")
	private User completedByUser;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "created_by_user_id", nullable = false)
	private User createdByUser;

	@OneToMany(mappedBy = "checklistRun", cascade = CascadeType.ALL, orphanRemoval = true)
	@OrderBy("sortOrder ASC")
	private final Set<ChecklistTaskExecution> taskExecutions = new LinkedHashSet<>();

	@OneToMany(mappedBy = "checklistRun", cascade = CascadeType.ALL, orphanRemoval = true)
	private final Set<ChecklistRunAssignment> assignments = new LinkedHashSet<>();

	@OneToMany(mappedBy = "checklistRun", cascade = CascadeType.ALL, orphanRemoval = true)
	private final Set<ChecklistRunEvent> events = new LinkedHashSet<>();

	protected ChecklistRun() {
	}

	/**
	 * Creates a checklist run snapshot from a checklist definition.
	 *
	 * @param checklistDefinition source checklist definition
	 * @param definitionGroupId identifier shared across definition versions
	 * @param establishment establishment that owns the run
	 * @param serviceArea service area covered by the run
	 * @param titleSnapshot title copied from the source definition
	 * @param descriptionSnapshot description copied from the source definition
	 * @param dueAt due timestamp for the run
	 * @param status initial run status
	 * @param createdByUser user who created the run
	 */
	public ChecklistRun(
			ChecklistDefinition checklistDefinition,
			UUID definitionGroupId,
			Establishment establishment,
			ChecklistServiceArea serviceArea,
			String titleSnapshot,
			String descriptionSnapshot,
			Instant dueAt,
			ChecklistRunStatus status,
			User createdByUser
	) {
		this.checklistDefinition = checklistDefinition;
		this.definitionGroupId = definitionGroupId;
		this.establishment = establishment;
		this.serviceArea = serviceArea;
		this.titleSnapshot = titleSnapshot;
		this.descriptionSnapshot = descriptionSnapshot;
		this.dueAt = dueAt;
		this.status = status;
		this.createdByUser = createdByUser;
	}

	/**
	 * Replaces all task executions attached to this run.
	 *
	 * @param taskExecutions replacement task executions
	 */
	public void replaceTaskExecutions(Collection<ChecklistTaskExecution> taskExecutions) {
		this.taskExecutions.clear();
		taskExecutions.forEach(this::addTaskExecution);
	}

	/**
	 * Adds a task execution to this run.
	 *
	 * @param taskExecution task execution to add
	 */
	public void addTaskExecution(ChecklistTaskExecution taskExecution) {
		taskExecution.attachTo(this);
		this.taskExecutions.add(taskExecution);
	}

	/**
	 * Rebuilds task executions by snapshotting the supplied definition tasks.
	 *
	 * @param tasks task definitions to snapshot
	 */
	public void snapshotTasksFromDefinition(List<ChecklistTaskDefinition> tasks) {
		this.taskExecutions.clear();
		tasks.stream()
				.map(ChecklistTaskExecution::fromDefinitionTask)
				.forEach(this::addTaskExecution);
	}

	/**
	 * Replaces all assignments attached to this run.
	 *
	 * @param assignments replacement assignments
	 */
	public void replaceAssignments(Collection<ChecklistRunAssignment> assignments) {
		this.assignments.clear();
		assignments.forEach(this::addAssignment);
	}

	/**
	 * Adds an assignment to this run.
	 *
	 * @param assignment assignment to add
	 */
	public void addAssignment(ChecklistRunAssignment assignment) {
		assignment.attachTo(this);
		this.assignments.add(assignment);
	}

	/**
	 * Removes an assignment from this run.
	 *
	 * @param assignment assignment to remove
	 */
	public void removeAssignment(ChecklistRunAssignment assignment) {
		this.assignments.remove(assignment);
	}

	/**
	 * Adds an event to this run.
	 *
	 * @param event event to append
	 */
	public void addEvent(ChecklistRunEvent event) {
		event.attachTo(this);
		this.events.add(event);
	}
}
