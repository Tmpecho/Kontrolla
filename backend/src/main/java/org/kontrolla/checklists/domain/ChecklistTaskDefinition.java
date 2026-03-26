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

import java.math.BigDecimal;

@Getter
@Entity
@Table(name = "checklist_task_definitions")
public class ChecklistTaskDefinition extends AbstractAuditableUuidEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "checklist_definition_id", nullable = false)
	private ChecklistDefinition checklistDefinition;

	@Setter
	@Column(nullable = false, length = 500)
	private String title;

	@Setter
	@Column(length = 1000)
	private String details;

	@Setter
	@Enumerated(EnumType.STRING)
	@Column(name = "task_kind", nullable = false, length = 32)
	private ChecklistTaskKind taskKind;

	@Setter
	@Column(nullable = false)
	private boolean required;

	@Setter
	@Column(name = "sort_order", nullable = false)
	private int sortOrder;

	@Setter
	@Column(name = "measurement_unit", length = 32)
	private String measurementUnit;

	@Setter
	@Column(name = "minimum_allowed_value", precision = 19, scale = 4)
	private BigDecimal minimumAllowedValue;

	@Setter
	@Column(name = "maximum_allowed_value", precision = 19, scale = 4)
	private BigDecimal maximumAllowedValue;

	protected ChecklistTaskDefinition() {
	}

	public ChecklistTaskDefinition(
			String title,
			String details,
			ChecklistTaskKind taskKind,
			boolean required,
			int sortOrder,
			String measurementUnit,
			BigDecimal minimumAllowedValue,
			BigDecimal maximumAllowedValue
	) {
		this.title = title;
		this.details = details;
		this.taskKind = taskKind;
		this.required = required;
		this.sortOrder = sortOrder;
		this.measurementUnit = measurementUnit;
		this.minimumAllowedValue = minimumAllowedValue;
		this.maximumAllowedValue = maximumAllowedValue;
	}

	void attachTo(ChecklistDefinition checklistDefinition) {
		this.checklistDefinition = checklistDefinition;
	}
}
