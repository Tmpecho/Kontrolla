package org.kontrolla.common.application;

/** Represents the backend startup lifecycle state exposed by readiness checks. */
public enum StartupReadinessStatus {
  STARTING,
  READY
}
