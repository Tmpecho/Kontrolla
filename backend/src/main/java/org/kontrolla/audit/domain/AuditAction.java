package org.kontrolla.audit.domain;

/** Enumerates the domain actions that can produce audit events. */
public enum AuditAction {
  AUTH_LOGIN,
  AUTH_REFRESH,
  AUTH_LOGOUT,
  USER_CREATE,
  MEMBERSHIP_CREATE,
  MEMBERSHIP_UPDATE
}
