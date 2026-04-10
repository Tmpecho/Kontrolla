package org.kontrolla.checklists.application;

import java.math.BigDecimal;
import org.kontrolla.checklists.domain.ChecklistTaskKind;

/**
 * Application input describing one checklist definition task.
 *
 * @param title the task title
 * @param details the task details
 * @param taskKind the task kind
 * @param required whether the task is required
 * @param sortOrder the task sort order
 * @param measurementUnit the measurement unit, if applicable
 * @param minimumAllowedValue the minimum allowed value, if applicable
 * @param maximumAllowedValue the maximum allowed value, if applicable
 */
public record ChecklistDefinitionTaskInput(
    String title,
    String details,
    ChecklistTaskKind taskKind,
    boolean required,
    int sortOrder,
    String measurementUnit,
    BigDecimal minimumAllowedValue,
    BigDecimal maximumAllowedValue) {}
