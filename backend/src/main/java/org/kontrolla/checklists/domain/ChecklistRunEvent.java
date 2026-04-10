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
import org.kontrolla.common.persistence.AbstractAuditableUuidEntity;
import org.kontrolla.iam.domain.User;

import java.time.Instant;

/**
 * Event recorded in the lifecycle of a checklist run.
 */
@Getter
@Entity
@Table(name = "checklist_run_events")
public class ChecklistRunEvent extends AbstractAuditableUuidEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "checklist_run_id", nullable = false)
	private ChecklistRun checklistRun;

	@Enumerated(EnumType.STRING)
	@Column(name = "event_type", nullable = false, length = 32)
	private ChecklistRunEventType eventType;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "actor_user_id")
	private User actorUser;

	@Column(name = "occurred_at", nullable = false)
	private Instant occurredAt;

	@Column(name = "metadata_json", length = 4000)
	private String metadataJson;

	protected ChecklistRunEvent() {
	}

	/**
	 * Creates a checklist run event.
	 *
	 * @param eventType type of event being recorded
	 * @param actorUser user responsible for the event
	 * @param occurredAt instant when the event occurred
	 * @param metadataJson serialized event metadata
	 */
	public ChecklistRunEvent(ChecklistRunEventType eventType, User actorUser, Instant occurredAt, String metadataJson) {
		this.eventType = eventType;
		this.actorUser = actorUser;
		this.occurredAt = occurredAt;
		this.metadataJson = metadataJson;
	}

	void attachTo(ChecklistRun checklistRun) {
		this.checklistRun = checklistRun;
	}
}
