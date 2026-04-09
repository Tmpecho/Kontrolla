package org.kontrolla.documents.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import org.kontrolla.common.persistence.AbstractAuditableUuidEntity;
import org.kontrolla.iam.domain.User;

import java.time.Instant;

@Getter
@Entity
@Table(name = "document_audit_assignments")
public class DocumentAuditAssignment extends AbstractAuditableUuidEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "document_id", nullable = false)
  private Document document;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Column(name = "acknowledged_at")
  private Instant acknowledgedAt;

  protected DocumentAuditAssignment() {
  }

  public DocumentAuditAssignment(User user) {
    this.user = user;
  }

  void attachTo(Document document) {
    this.document = document;
  }

  public void acknowledge(Instant acknowledgedAt) {
    if (this.acknowledgedAt == null) {
      this.acknowledgedAt = acknowledgedAt;
    }
  }

  public boolean isAcknowledged() {
    return acknowledgedAt != null;
  }
}
