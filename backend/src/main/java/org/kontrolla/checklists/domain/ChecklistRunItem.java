package org.kontrolla.checklists.domain;

import jakarta.persistence.Column;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.kontrolla.common.persistence.AbstractAuditableUuidEntity;

@Getter
@Entity
@Table(name = "checklist_run_items")
public class ChecklistRunItem extends AbstractAuditableUuidEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "checklist_run_id", nullable = false)
	private ChecklistRun checklistRun;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "source_checklist_item_definition_id")
	private ChecklistItemDefinition sourceChecklistItemDefinition;

	@Setter
	@Column(name = "prompt_snapshot", nullable = false, length = 500)
	private String promptSnapshot;

	@Setter
	@Column(name = "instruction_text_snapshot", length = 1000)
	private String instructionTextSnapshot;

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

	@OneToOne(mappedBy = "checklistRunItem", cascade = CascadeType.ALL, orphanRemoval = true)
	private ChecklistItemResponse response;

	protected ChecklistRunItem() {
	}

	public ChecklistRunItem(
			ChecklistItemDefinition sourceChecklistItemDefinition,
			String promptSnapshot,
			String instructionTextSnapshot,
			ChecklistResponseType responseType,
			boolean required,
			int sortOrder
	) {
		this.sourceChecklistItemDefinition = sourceChecklistItemDefinition;
		this.promptSnapshot = promptSnapshot;
		this.instructionTextSnapshot = instructionTextSnapshot;
		this.responseType = responseType;
		this.required = required;
		this.sortOrder = sortOrder;
	}

	public static ChecklistRunItem fromDefinitionItem(ChecklistItemDefinition itemDefinition) {
		return new ChecklistRunItem(
				itemDefinition,
				itemDefinition.getPrompt(),
				itemDefinition.getInstructionText(),
				itemDefinition.getResponseType(),
				itemDefinition.isRequired(),
				itemDefinition.getSortOrder()
		);
	}

	void attachTo(ChecklistRun checklistRun) {
		this.checklistRun = checklistRun;
	}

	void attachResponse(ChecklistItemResponse response) {
		this.response = response;
	}
}
