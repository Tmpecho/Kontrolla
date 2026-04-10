package org.kontrolla.notifications.api;

/**
 * API response containing the number of unread notifications.
 *
 * @param unreadCount the unread notification count
 */
public record UnreadNotificationCountResponse(long unreadCount) {}
