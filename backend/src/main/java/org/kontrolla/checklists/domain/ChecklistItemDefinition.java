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

@Getter
@Entity
@Table(name = "checklist_item_definitions")
public class ChecklistItemDefinition extends AbstractAuditableUuidEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "checklist_definition_id", nullable = false)
	private ChecklistDefinition checklistDefinition;

	@Setter
	@Column(nullable = false, length = 500)
	private String prompt;

	@Setter
	@Column(name = "instruction_text", length = 1000)
	private String instructionText;

	@Setter
	@Enumerated(EnumType.STRING)
	@Column(name = "response_type", nullable = false, length = 32)
	private ChecklistResponseType responseType;

	@Setter
	@Column(nullable = false)
	private boolean required;

	@Setter
	@Column(name = "sort_order", nullable = false)
	private int sortOrder;

	protected ChecklistItemDefinition() {
	}

	public ChecklistItemDefinition(
			String prompt,
			String instructionText,
			ChecklistResponseType responseType,
			boolean required,
			int sortOrder
	) {
		this.prompt = prompt;
		this.instructionText = instructionText;
		this.responseType = responseType;
		this.required = required;
		this.sortOrder = sortOrder;
	}

	void attachTo(ChecklistDefinition checklistDefinition) {
		this.checklistDefinition = checklistDefinition;
	}
}
