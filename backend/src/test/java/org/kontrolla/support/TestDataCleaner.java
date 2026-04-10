package org.kontrolla.support;

import org.kontrolla.checklists.infrastructure.ChecklistDefinitionRepository;
import org.kontrolla.checklists.infrastructure.ChecklistRunRepository;
import org.kontrolla.audit.infrastructure.AuditEventRepository;
import org.kontrolla.deviations.infrastructure.DeviationRepository;
import org.kontrolla.documents.infrastructure.DocumentFileRepository;
import org.kontrolla.documents.infrastructure.DocumentRepository;
import org.kontrolla.establishments.infrastructure.EstablishmentRepository;
import org.kontrolla.establishments.infrastructure.EstablishmentServingHoursRepository;
import org.kontrolla.iam.infrastructure.RefreshTokenRepository;
import org.kontrolla.iam.infrastructure.UserInviteRepository;
import org.kontrolla.iam.infrastructure.UserRepository;
import org.kontrolla.notifications.infrastructure.NotificationRepository;
import org.kontrolla.organizations.infrastructure.OrganizationMembershipRepository;
import org.kontrolla.organizations.infrastructure.OrganizationRepository;
import org.kontrolla.temperatures.infrastructure.TemperatureLogRepository;
import org.kontrolla.temperatures.infrastructure.TemperatureUnitRepository;
import org.springframework.stereotype.Component;

@Component
public class TestDataCleaner {

  private final AuditEventRepository auditEventRepository;
  private final RefreshTokenRepository refreshTokenRepository;
  private final NotificationRepository notificationRepository;
  private final UserInviteRepository userInviteRepository;
  private final DeviationRepository deviationRepository;
  private final DocumentFileRepository documentFileRepository;
  private final DocumentRepository documentRepository;
  private final TemperatureLogRepository temperatureLogRepository;
  private final TemperatureUnitRepository temperatureUnitRepository;
  private final ChecklistRunRepository checklistRunRepository;
  private final ChecklistDefinitionRepository checklistDefinitionRepository;
  private final OrganizationMembershipRepository organizationMembershipRepository;
  private final EstablishmentServingHoursRepository establishmentServingHoursRepository;
  private final EstablishmentRepository establishmentRepository;
  private final OrganizationRepository organizationRepository;
  private final UserRepository userRepository;

  public TestDataCleaner(
      AuditEventRepository auditEventRepository,
      RefreshTokenRepository refreshTokenRepository,
      NotificationRepository notificationRepository,
      UserInviteRepository userInviteRepository,
      DeviationRepository deviationRepository,
      DocumentFileRepository documentFileRepository,
      DocumentRepository documentRepository,
      TemperatureLogRepository temperatureLogRepository,
      TemperatureUnitRepository temperatureUnitRepository,
      ChecklistRunRepository checklistRunRepository,
      ChecklistDefinitionRepository checklistDefinitionRepository,
      OrganizationMembershipRepository organizationMembershipRepository,
      EstablishmentServingHoursRepository establishmentServingHoursRepository,
      EstablishmentRepository establishmentRepository,
      OrganizationRepository organizationRepository,
      UserRepository userRepository
  ) {
    this.auditEventRepository = auditEventRepository;
    this.refreshTokenRepository = refreshTokenRepository;
    this.notificationRepository = notificationRepository;
    this.userInviteRepository = userInviteRepository;
    this.deviationRepository = deviationRepository;
    this.documentFileRepository = documentFileRepository;
    this.documentRepository = documentRepository;
    this.temperatureLogRepository = temperatureLogRepository;
    this.temperatureUnitRepository = temperatureUnitRepository;
    this.checklistRunRepository = checklistRunRepository;
    this.checklistDefinitionRepository = checklistDefinitionRepository;
    this.organizationMembershipRepository = organizationMembershipRepository;
    this.establishmentServingHoursRepository = establishmentServingHoursRepository;
    this.establishmentRepository = establishmentRepository;
    this.organizationRepository = organizationRepository;
    this.userRepository = userRepository;
  }

  public void clearAll() {
    auditEventRepository.deleteAll();
    refreshTokenRepository.deleteAll();
    notificationRepository.deleteAll();
    userInviteRepository.deleteAll();
    deviationRepository.deleteAll();
    documentFileRepository.deleteAll();
    documentRepository.deleteAll();
    temperatureLogRepository.deleteAll();
    temperatureUnitRepository.deleteAll();
    checklistRunRepository.deleteAll();
    checklistDefinitionRepository.deleteAll();
    organizationMembershipRepository.deleteAll();
    establishmentServingHoursRepository.deleteAll();
    establishmentRepository.deleteAll();
    organizationRepository.deleteAll();
    userRepository.deleteAll();
  }
}
