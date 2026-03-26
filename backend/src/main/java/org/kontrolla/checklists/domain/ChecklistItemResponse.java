package org.kontrolla.checklists.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.kontrolla.common.persistence.AbstractAuditableUuidEntity;

import java.math.BigDecimal;

@Getter
@Entity
@Table(name = "checklist_item_responses")
public class ChecklistItemResponse extends AbstractAuditableUuidEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "checklist_run_id", nullable = false)
	private ChecklistRun checklistRun;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "checklist_item_definition_id", nullable = false)
	private ChecklistItemDefinition checklistItemDefinition;

	@Setter
	@Column(name = "boolean_value")
	private Boolean booleanValue;

	@Setter
	@Column(name = "text_value", length = 2000)
	private String textValue;

	@Setter
	@Column(name = "number_value", precision = 19, scale = 4)
	private BigDecimal numberValue;

	@Setter
	@Column(length = 1000)
	private String note;

	protected ChecklistItemResponse() {
	}

	public ChecklistItemResponse(
			ChecklistItemDefinition checklistItemDefinition,
			Boolean booleanValue,
			String textValue,
			BigDecimal numberValue,
			String note
	) {
		this.checklistItemDefinition = checklistItemDefinition;
		this.booleanValue = booleanValue;
		this.textValue = textValue;
		this.numberValue = numberValue;
		this.note = note;
	}

	void attachTo(ChecklistRun checklistRun) {
		this.checklistRun = checklistRun;
	}
}
