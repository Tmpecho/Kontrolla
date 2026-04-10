package org.kontrolla.checklists.domain;

/**
 * Event types that can be recorded for a checklist run.
 */
public enum ChecklistRunEventType {
	CREATED,
	ASSIGNED,
	STARTED,
	COMPLETED,
	REOPENED,
	CANCELLED
}
