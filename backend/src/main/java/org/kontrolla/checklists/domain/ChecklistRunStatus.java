package org.kontrolla.checklists.domain;

/**
 * Lifecycle states for a checklist run.
 */
public enum ChecklistRunStatus {
	PENDING,
	IN_PROGRESS,
	COMPLETED,
	OVERDUE,
	CANCELLED
}
