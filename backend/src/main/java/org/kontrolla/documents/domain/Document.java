package org.kontrolla.documents.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.kontrolla.common.persistence.AbstractAuditableUuidEntity;
import org.kontrolla.establishments.domain.Establishment;
import org.kontrolla.iam.domain.User;
import org.kontrolla.organizations.domain.Organization;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Getter
@Entity
@Table(name = "documents")
public class Document extends AbstractAuditableUuidEntity {

  public static final int DEFAULT_EXPIRY_WARNING_DAYS = 30;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "organization_id", nullable = false)
  private Organization organization;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "establishment_id", nullable = false)
  private Establishment establishment;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "created_by_user_id", nullable = false)
  private User createdByUser;

  @Setter
  @Enumerated(EnumType.STRING)
  @Column(name = "service_area", nullable = false, length = 32)
  private DocumentServiceArea serviceArea;

  @Setter
  @Column(nullable = false, length = 255)
  private String title;

  @Setter
  @Column(name = "holder_name", nullable = false, length = 255)
  private String holderName;

  @Setter
  @Column(name = "issue_date", nullable = false)
  private LocalDate issueDate;

  @Setter
  @Column(name = "renewal_date", nullable = false)
  private LocalDate renewalDate;

  @Setter
  @Column(name = "file_name", nullable = false, length = 255)
  private String fileName;

  @Setter
  @Column(name = "content_type", nullable = false, length = 255)
  private String contentType;

  @Setter
  @Column(name = "file_size_bytes", nullable = false)
  private long fileSizeBytes;

  @Getter(AccessLevel.NONE)
  @OneToMany(mappedBy = "document", orphanRemoval = true, cascade = jakarta.persistence.CascadeType.ALL)
  private final List<DocumentAuditAssignment> auditAssignments = new ArrayList<>();

  protected Document() {
  }

  public Document(
      Organization organization,
      Establishment establishment,
      User createdByUser,
      DocumentServiceArea serviceArea,
      String title,
      String holderName,
      LocalDate issueDate,
      LocalDate renewalDate,
      String fileName,
      String contentType,
      long fileSizeBytes
  ) {
    this.organization = organization;
    this.establishment = establishment;
    this.createdByUser = createdByUser;
    this.serviceArea = serviceArea;
    this.title = title;
    this.holderName = holderName;
    this.issueDate = issueDate;
    this.renewalDate = renewalDate;
    this.fileName = fileName;
    this.contentType = contentType;
    this.fileSizeBytes = fileSizeBytes;
  }

  public DocumentStatus getStatus(LocalDate today) {
    return getStatus(today, DEFAULT_EXPIRY_WARNING_DAYS);
  }

  public DocumentStatus getStatus(LocalDate today, int warningDays) {
    if (warningDays < 0) {
      throw new IllegalArgumentException("warningDays must be non-negative");
    }

    if (renewalDate.isBefore(today)) {
      return DocumentStatus.EXPIRED;
    }

    if (!renewalDate.isAfter(today.plusDays(warningDays))) {
      return DocumentStatus.EXPIRING;
    }

    return DocumentStatus.VALID;
  }

  public List<DocumentAuditAssignment> getAuditAssignments() {
    return List.copyOf(auditAssignments);
  }

  public void replaceAuditAssignments(List<User> users) {
    Set<UUID> nextUserIds = users.stream()
        .map(User::getId)
        .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));

    auditAssignments.removeIf(assignment -> !nextUserIds.contains(assignment.getUser().getId()));

    Set<UUID> existingUserIds = auditAssignments.stream()
        .map(assignment -> assignment.getUser().getId())
        .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));

    for (User user : users) {
      if (existingUserIds.contains(user.getId())) {
        continue;
      }

      addAuditAssignment(user);
    }
  }

  public Optional<DocumentAuditAssignment> findAuditAssignment(UUID userId) {
    return auditAssignments.stream()
        .filter(assignment -> assignment.getUser().getId().equals(userId))
        .findFirst();
  }

  public boolean isAuditReady() {
    return auditAssignments.stream().allMatch(DocumentAuditAssignment::isAcknowledged);
  }

  private void addAuditAssignment(User user) {
    DocumentAuditAssignment assignment = new DocumentAuditAssignment(user);
    assignment.attachTo(this);
    auditAssignments.add(assignment);
  }
}
