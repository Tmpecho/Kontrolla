package org.kontrolla.documents.domain;

/** Derived lifecycle state of a document based on its renewal date. */
public enum DocumentStatus {
  VALID,
  EXPIRING,
  EXPIRED
}
