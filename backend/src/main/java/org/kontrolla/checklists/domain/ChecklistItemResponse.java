package org.kontrolla.checklists.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.kontrolla.common.persistence.AbstractAuditableUuidEntity;

import java.math.BigDecimal;

@Getter
@Entity
@Table(name = "checklist_item_responses")
public class ChecklistItemResponse extends AbstractAuditableUuidEntity {

	@OneToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "checklist_run_item_id", nullable = false, unique = true)
	private ChecklistRunItem checklistRunItem;

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
			Boolean booleanValue,
			String textValue,
			BigDecimal numberValue,
			String note
	) {
		this.booleanValue = booleanValue;
		this.textValue = textValue;
		this.numberValue = numberValue;
		this.note = note;
	}

	public void attachTo(ChecklistRunItem checklistRunItem) {
		this.checklistRunItem = checklistRunItem;
		checklistRunItem.attachResponse(this);
	}
}
