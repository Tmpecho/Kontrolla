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
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.kontrolla.common.persistence.AbstractAuditableUuidEntity;
import org.kontrolla.establishments.domain.Establishment;
import org.kontrolla.iam.domain.User;

/**
 * Versioned checklist definition that describes the tasks and schedules available for an
 * establishment.
 */
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
  private final List<ChecklistTaskDefinition> tasks = new ArrayList<>();

  @OneToMany(mappedBy = "checklistDefinition", cascade = CascadeType.ALL, orphanRemoval = true)
  private final Set<ChecklistSchedule> schedules = new LinkedHashSet<>();

  protected ChecklistDefinition() {}

  /**
   * Creates a checklist definition version.
   *
   * @param definitionGroupId identifier shared across definition versions
   * @param establishment establishment that owns the definition
   * @param serviceArea service area covered by the checklist
   * @param title checklist title
   * @param description optional checklist description
   * @param versionNumber version number for this definition
   * @param status current definition status
   * @param effectiveFrom instant when the definition becomes effective
   * @param createdByUser user who created the definition
   * @param updatedByUser user who last updated the definition
   */
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
      User updatedByUser) {
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

  /**
   * Marks the definition as superseded by a newer version.
   *
   * @param effectiveTo instant when this definition stopped being effective
   * @param updatedByUser user recording the change
   */
  public void supersede(Instant effectiveTo, User updatedByUser) {
    this.status = ChecklistDefinitionStatus.SUPERSEDED;
    this.effectiveTo = effectiveTo;
    this.updatedByUser = updatedByUser;
  }

  /**
   * Archives the definition so it can no longer be used.
   *
   * @param effectiveTo instant when the definition was archived
   * @param updatedByUser user recording the change
   */
  public void archive(Instant effectiveTo, User updatedByUser) {
    this.status = ChecklistDefinitionStatus.ARCHIVED;
    this.effectiveTo = effectiveTo;
    this.updatedByUser = updatedByUser;
  }

  /**
   * Replaces all task definitions attached to this checklist definition.
   *
   * @param tasks replacement task definitions
   */
  public void replaceTasks(List<ChecklistTaskDefinition> tasks) {
    this.tasks.clear();
    tasks.forEach(this::addTask);
  }

  /**
   * Adds a task definition to this checklist definition.
   *
   * @param task task definition to add
   */
  public void addTask(ChecklistTaskDefinition task) {
    task.attachTo(this);
    this.tasks.add(task);
  }

  /**
   * Replaces all schedules attached to this checklist definition.
   *
   * @param schedules replacement schedules
   */
  public void replaceSchedules(List<ChecklistSchedule> schedules) {
    this.schedules.clear();
    schedules.forEach(this::addSchedule);
  }

  /**
   * Adds a schedule to this checklist definition.
   *
   * @param schedule schedule to add
   */
  public void addSchedule(ChecklistSchedule schedule) {
    schedule.attachTo(this);
    this.schedules.add(schedule);
  }
}
