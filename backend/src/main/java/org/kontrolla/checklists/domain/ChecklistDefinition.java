package org.kontrolla.checklists.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.kontrolla.common.persistence.AbstractAuditableUuidEntity;
import org.kontrolla.establishments.domain.Establishment;
import org.kontrolla.iam.domain.User;

import java.util.ArrayList;
import java.util.List;

@Getter
@Entity
@Table(name = "checklist_definitions")
public class ChecklistDefinition extends AbstractAuditableUuidEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "establishment_id", nullable = false)
	private Establishment establishment;

	@Setter
	@Enumerated(EnumType.STRING)
	@Column(name = "service_area", nullable = false, length = 32)
	private ChecklistServiceArea serviceArea;

	@Setter
	@Column(nullable = false)
	private String title;

	@Setter
	@Column(length = 2000)
	private String description;

	@Setter
	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 32)
	private ChecklistDefinitionStatus status;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "created_by_user_id", nullable = false)
	private User createdByUser;

	@Setter
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "updated_by_user_id", nullable = false)
	private User updatedByUser;

	@OneToMany(mappedBy = "checklistDefinition", cascade = CascadeType.ALL, orphanRemoval = true)
	@OrderBy("sortOrder ASC")
	private final List<ChecklistItemDefinition> items = new ArrayList<>();

	@OneToMany(mappedBy = "checklistDefinition", cascade = CascadeType.ALL, orphanRemoval = true)
	private final List<ChecklistSchedule> schedules = new ArrayList<>();

	protected ChecklistDefinition() {
	}

	public ChecklistDefinition(
			Establishment establishment,
			ChecklistServiceArea serviceArea,
			String title,
			String description,
			ChecklistDefinitionStatus status,
			User createdByUser,
			User updatedByUser
	) {
		this.establishment = establishment;
		this.serviceArea = serviceArea;
		this.title = title;
		this.description = description;
		this.status = status;
		this.createdByUser = createdByUser;
		this.updatedByUser = updatedByUser;
	}

	public void replaceItems(List<ChecklistItemDefinition> items) {
		this.items.clear();
		items.forEach(this::addItem);
	}

	public void addItem(ChecklistItemDefinition item) {
		item.attachTo(this);
		this.items.add(item);
	}

	public void replaceSchedules(List<ChecklistSchedule> schedules) {
		this.schedules.clear();
		schedules.forEach(this::addSchedule);
	}

	public void addSchedule(ChecklistSchedule schedule) {
		schedule.attachTo(this);
		this.schedules.add(schedule);
	}
}
