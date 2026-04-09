package org.kontrolla.documents.domain;

import org.junit.jupiter.api.Test;
import org.kontrolla.establishments.domain.Establishment;
import org.kontrolla.establishments.domain.EstablishmentStatus;
import org.kontrolla.establishments.domain.EstablishmentType;
import org.kontrolla.iam.domain.User;
import org.kontrolla.organizations.domain.Organization;
import org.kontrolla.organizations.domain.OrganizationStatus;

import java.time.LocalDate;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DocumentTest {

  @Test
  void returnsExpiredExpiringAndValidStatusesAtExpectedBoundaries() {
    LocalDate today = LocalDate.of(2026, 4, 7);

    Document expired = createDocument(today.minusDays(1));
    Document expiring = createDocument(today.plusDays(30));
    Document valid = createDocument(today.plusDays(31));

    assertEquals(DocumentStatus.EXPIRED, expired.getStatus(today));
    assertEquals(DocumentStatus.EXPIRING, expiring.getStatus(today));
    assertEquals(DocumentStatus.VALID, valid.getStatus(today));
  }

  @Test
  void rejectsNegativeWarningWindows() {
    Document document = createDocument(LocalDate.of(2026, 5, 7));

    assertThrows(IllegalArgumentException.class, () -> document.getStatus(LocalDate.of(2026, 4, 7), -1));
  }

  private static Document createDocument(LocalDate renewalDate) {
    Organization organization = new Organization("Kontrolla Demo", OrganizationStatus.ACTIVE);
    Establishment establishment = new Establishment(
        organization,
        "Demo Bar",
        EstablishmentType.BAR,
        EstablishmentStatus.ACTIVE
    );

    return new Document(
        organization,
        establishment,
        createUser(),
        DocumentServiceArea.IK_ALKOHOL,
        "Alcohol service licence",
        "Oslo Municipality",
        LocalDate.of(2025, 4, 7),
        renewalDate,
        "alcohol-service-licence.pdf",
        "application/pdf",
        2048
    );
  }

  private static User createUser() {
    return new User(
        "demo@example.com",
        "Demo",
        "User",
        "hashed-password",
        true,
        Set.of()
    );
  }
}
