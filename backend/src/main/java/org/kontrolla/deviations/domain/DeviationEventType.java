package org.kontrolla.deviations.domain;

/**
 * Types of events that can appear in a deviation timeline.
 */
public enum DeviationEventType {
	REPORTED,
	ASSIGNED,
	UNASSIGNED,
	STATUS_CHANGED,
	DETAILS_UPDATED,
	NOTE_ADDED
}
