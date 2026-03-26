package org.kontrolla.checklists.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.kontrolla.common.persistence.AbstractAuditableUuidEntity;
import org.kontrolla.iam.domain.User;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Entity
@Table(name = "checklist_schedules")
public class ChecklistSchedule extends AbstractAuditableUuidEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "checklist_definition_id", nullable = false)
	private ChecklistDefinition checklistDefinition;

	@Setter
	@Enumerated(EnumType.STRING)
	@Column(name = "schedule_type", nullable = false, length = 32)
	private ChecklistScheduleType scheduleType;

	@Setter
	@Column(name = "start_date", nullable = false)
	private LocalDate startDate;

	@Setter
	@Column(name = "end_date")
	private LocalDate endDate;

	@Setter
	@Column(name = "due_time")
	private LocalTime dueTime;

	@Setter
	@Column(name = "weekday_mask")
	private Integer weekdayMask;

	@Setter
	@Column(name = "day_of_month")
	private Integer dayOfMonth;

	@Setter
	@Column(nullable = false)
	private boolean active;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "created_by_user_id", nullable = false)
	private User createdByUser;

	@Setter
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "updated_by_user_id", nullable = false)
	private User updatedByUser;

	protected ChecklistSchedule() {
	}

	public ChecklistSchedule(
			ChecklistScheduleType scheduleType,
			LocalDate startDate,
			LocalDate endDate,
			LocalTime dueTime,
			Integer weekdayMask,
			Integer dayOfMonth,
			boolean active,
			User createdByUser,
			User updatedByUser
	) {
		this.scheduleType = scheduleType;
		this.startDate = startDate;
		this.endDate = endDate;
		this.dueTime = dueTime;
		this.weekdayMask = weekdayMask;
		this.dayOfMonth = dayOfMonth;
		this.active = active;
		this.createdByUser = createdByUser;
		this.updatedByUser = updatedByUser;
	}

	void attachTo(ChecklistDefinition checklistDefinition) {
		this.checklistDefinition = checklistDefinition;
	}

}
