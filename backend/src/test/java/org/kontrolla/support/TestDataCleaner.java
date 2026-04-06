package org.kontrolla.support;

import org.kontrolla.checklists.infrastructure.ChecklistDefinitionRepository;
import org.kontrolla.checklists.infrastructure.ChecklistRunRepository;
import org.kontrolla.deviations.infrastructure.DeviationRepository;
import org.kontrolla.establishments.infrastructure.EstablishmentRepository;
import org.kontrolla.iam.infrastructure.RefreshTokenRepository;
import org.kontrolla.iam.infrastructure.UserInviteRepository;
import org.kontrolla.iam.infrastructure.UserRepository;
import org.kontrolla.organizations.infrastructure.OrganizationMembershipRepository;
import org.kontrolla.organizations.infrastructure.OrganizationRepository;
import org.springframework.stereotype.Component;

@Component
public class TestDataCleaner {

	private final RefreshTokenRepository refreshTokenRepository;
	private final UserInviteRepository userInviteRepository;
	private final DeviationRepository deviationRepository;
	private final ChecklistRunRepository checklistRunRepository;
	private final ChecklistDefinitionRepository checklistDefinitionRepository;
	private final OrganizationMembershipRepository organizationMembershipRepository;
	private final EstablishmentRepository establishmentRepository;
	private final OrganizationRepository organizationRepository;
	private final UserRepository userRepository;

	public TestDataCleaner(
			RefreshTokenRepository refreshTokenRepository,
			UserInviteRepository userInviteRepository,
			DeviationRepository deviationRepository,
			ChecklistRunRepository checklistRunRepository,
			ChecklistDefinitionRepository checklistDefinitionRepository,
			OrganizationMembershipRepository organizationMembershipRepository,
			EstablishmentRepository establishmentRepository,
			OrganizationRepository organizationRepository,
			UserRepository userRepository
	) {
		this.refreshTokenRepository = refreshTokenRepository;
		this.userInviteRepository = userInviteRepository;
		this.deviationRepository = deviationRepository;
		this.checklistRunRepository = checklistRunRepository;
		this.checklistDefinitionRepository = checklistDefinitionRepository;
		this.organizationMembershipRepository = organizationMembershipRepository;
		this.establishmentRepository = establishmentRepository;
		this.organizationRepository = organizationRepository;
		this.userRepository = userRepository;
	}

	public void clearAll() {
		refreshTokenRepository.deleteAll();
		userInviteRepository.deleteAll();
		deviationRepository.deleteAll();
		checklistRunRepository.deleteAll();
		checklistDefinitionRepository.deleteAll();
		organizationMembershipRepository.deleteAll();
		establishmentRepository.deleteAll();
		organizationRepository.deleteAll();
		userRepository.deleteAll();
	}
}
