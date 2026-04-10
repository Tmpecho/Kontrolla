package org.kontrolla.notifications.domain;

/**
 * Enumerates the notification events that can be sent to users.
 */
public enum NotificationType {
	CHECKLIST_ASSIGNED,
	CHECKLIST_OVERDUE,
	DEVIATION_ASSIGNED,
	DEVIATION_STATUS_CHANGED,
	DEVIATION_NOTE_ADDED
}
