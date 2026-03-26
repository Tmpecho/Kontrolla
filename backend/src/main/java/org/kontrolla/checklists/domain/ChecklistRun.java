package org.kontrolla.checklists.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.kontrolla.common.persistence.AbstractAuditableUuidEntity;
import org.kontrolla.establishments.domain.Establishment;
import org.kontrolla.iam.domain.User;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Getter
@Entity
@Table(name = "checklist_runs")
public class ChecklistRun extends AbstractAuditableUuidEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "checklist_definition_id", nullable = false)
	private ChecklistDefinition checklistDefinition;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "establishment_id", nullable = false)
	private Establishment establishment;

	@Setter
	@Enumerated(EnumType.STRING)
	@Column(name = "service_area", nullable = false, length = 32)
	private ChecklistServiceArea serviceArea;

	@Setter
	@Column(name = "title_snapshot", nullable = false, length = 255)
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
	private final List<ChecklistRunAssignment> assignments = new ArrayList<>();

	@OneToMany(mappedBy = "checklistRun", cascade = CascadeType.ALL, orphanRemoval = true)
	private final List<ChecklistRunEvent> events = new ArrayList<>();

	@OneToMany(mappedBy = "checklistRun", cascade = CascadeType.ALL, orphanRemoval = true)
	private final List<ChecklistItemResponse> itemResponses = new ArrayList<>();

	protected ChecklistRun() {
	}

	public ChecklistRun(
			ChecklistDefinition checklistDefinition,
			Establishment establishment,
			ChecklistServiceArea serviceArea,
			String titleSnapshot,
			String descriptionSnapshot,
			Instant dueAt,
			ChecklistRunStatus status,
			User createdByUser
	) {
		this.checklistDefinition = checklistDefinition;
		this.establishment = establishment;
		this.serviceArea = serviceArea;
		this.titleSnapshot = titleSnapshot;
		this.descriptionSnapshot = descriptionSnapshot;
		this.dueAt = dueAt;
		this.status = status;
		this.createdByUser = createdByUser;
	}

	public void replaceAssignments(List<ChecklistRunAssignment> assignments) {
		this.assignments.clear();
		assignments.forEach(this::addAssignment);
	}

	public void addAssignment(ChecklistRunAssignment assignment) {
		assignment.attachTo(this);
		this.assignments.add(assignment);
	}

	public void replaceItemResponses(List<ChecklistItemResponse> itemResponses) {
		this.itemResponses.clear();
		itemResponses.forEach(this::addItemResponse);
	}

	public void addItemResponse(ChecklistItemResponse itemResponse) {
		itemResponse.attachTo(this);
		this.itemResponses.add(itemResponse);
	}

	public void addEvent(ChecklistRunEvent event) {
		event.attachTo(this);
		this.events.add(event);
	}
}
