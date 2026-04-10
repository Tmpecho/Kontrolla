package org.kontrolla.deviations.domain;

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
 * Persisted timeline event associated with a deviation.
 */
@Getter
@Entity
@Table(name = "deviation_events")
public class DeviationEvent extends AbstractAuditableUuidEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "deviation_id", nullable = false)
	private Deviation deviation;

	@Enumerated(EnumType.STRING)
	@Column(name = "event_type", nullable = false, length = 32)
	private DeviationEventType eventType;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "actor_user_id")
	private User actorUser;

	@Column(name = "occurred_at", nullable = false)
	private Instant occurredAt;

	@Column(nullable = false, length = 2000)
	private String note;

	protected DeviationEvent() {
	}

	/**
	 * Creates a deviation timeline event.
	 *
	 * @param eventType the type of event
	 * @param actorUser the user responsible for the event, if any
	 * @param occurredAt when the event occurred
	 * @param note the timeline note
	 */
	public DeviationEvent(DeviationEventType eventType, User actorUser, Instant occurredAt, String note) {
		this.eventType = eventType;
		this.actorUser = actorUser;
		this.occurredAt = occurredAt;
		this.note = note;
	}

	void attachTo(Deviation deviation) {
		this.deviation = deviation;
	}
}
