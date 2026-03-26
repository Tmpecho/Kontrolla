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
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Getter
@Entity
@Table(name = "checklist_definitions")
public class ChecklistDefinition extends AbstractAuditableUuidEntity {

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
	@Column(nullable = false)
	private String title;

	@Setter
	@Column(length = 2000)
	private String description;

	@Column(name = "version_number", nullable = false)
	private int versionNumber;

	@Setter
	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 32)
	private ChecklistDefinitionStatus status;

	@Column(name = "effective_from", nullable = false, updatable = false)
	private Instant effectiveFrom;

	@Setter
	@Column(name = "effective_to")
	private Instant effectiveTo;

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
	private final Set<ChecklistSchedule> schedules = new LinkedHashSet<>();

	protected ChecklistDefinition() {
	}

	public ChecklistDefinition(
			UUID definitionGroupId,
			Establishment establishment,
			ChecklistServiceArea serviceArea,
			String title,
			String description,
			int versionNumber,
			ChecklistDefinitionStatus status,
			Instant effectiveFrom,
			User createdByUser,
			User updatedByUser
	) {
		this.definitionGroupId = definitionGroupId;
		this.establishment = establishment;
		this.serviceArea = serviceArea;
		this.title = title;
		this.description = description;
		this.versionNumber = versionNumber;
		this.status = status;
		this.effectiveFrom = effectiveFrom;
		this.createdByUser = createdByUser;
		this.updatedByUser = updatedByUser;
	}

	public void supersede(Instant effectiveTo, User updatedByUser) {
		this.status = ChecklistDefinitionStatus.SUPERSEDED;
		this.effectiveTo = effectiveTo;
		this.updatedByUser = updatedByUser;
	}

	public void archive(Instant effectiveTo, User updatedByUser) {
		this.status = ChecklistDefinitionStatus.ARCHIVED;
		this.effectiveTo = effectiveTo;
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
