package org.kontrolla.checklists.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalTime;
import lombok.Getter;
import lombok.Setter;
import org.kontrolla.common.persistence.AbstractAuditableUuidEntity;
import org.kontrolla.iam.domain.User;

/** Schedule configuration used to generate checklist runs from a checklist definition. */
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
  @Column(nullable = false, length = 64)
  private String timezone;

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

  protected ChecklistSchedule() {}

  /**
   * Creates a checklist schedule.
   *
   * @param scheduleType schedule recurrence type
   * @param startDate first active date for the schedule
   * @param endDate optional final active date
   * @param dueTime time of day when checklist runs become due
   * @param weekdayMask bitmask describing active weekdays
   * @param dayOfMonth day of month used by monthly schedules
   * @param timezone timezone used to evaluate the schedule
   * @param active whether the schedule is active
   * @param createdByUser user who created the schedule
   * @param updatedByUser user who last updated the schedule
   */
  public ChecklistSchedule(
      ChecklistScheduleType scheduleType,
      LocalDate startDate,
      LocalDate endDate,
      LocalTime dueTime,
      Integer weekdayMask,
      Integer dayOfMonth,
      String timezone,
      boolean active,
      User createdByUser,
      User updatedByUser) {
    this.scheduleType = scheduleType;
    this.startDate = startDate;
    this.endDate = endDate;
    this.dueTime = dueTime;
    this.weekdayMask = weekdayMask;
    this.dayOfMonth = dayOfMonth;
    this.timezone = timezone;
    this.active = active;
    this.createdByUser = createdByUser;
    this.updatedByUser = updatedByUser;
  }

  void attachTo(ChecklistDefinition checklistDefinition) {
    this.checklistDefinition = checklistDefinition;
  }
}
