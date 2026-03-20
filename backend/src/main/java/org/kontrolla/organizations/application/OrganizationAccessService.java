package org.kontrolla.organizations.application;

import org.kontrolla.common.exception.ForbiddenException;
import org.kontrolla.common.exception.ResourceNotFoundException;
import org.kontrolla.iam.security.CurrentUser;
import org.kontrolla.organizations.domain.Organization;
import org.kontrolla.organizations.domain.OrganizationMembership;
import org.kontrolla.organizations.domain.OrganizationRole;
import org.kontrolla.organizations.infrastructure.OrganizationMembershipRepository;
import org.kontrolla.organizations.infrastructure.OrganizationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;

@Service
public class OrganizationAccessService {

	private static final Set<OrganizationRole> MEMBER_MANAGEMENT_ROLES = Set.of(
			OrganizationRole.ORG_OWNER,
			OrganizationRole.ORG_ADMIN
	);

	private static final Set<OrganizationRole> ESTABLISHMENT_MANAGEMENT_ROLES = Set.of(
			OrganizationRole.ORG_OWNER,
			OrganizationRole.ORG_ADMIN,
			OrganizationRole.ORG_MANAGER
	);

	private final OrganizationRepository organizationRepository;
	private final OrganizationMembershipRepository membershipRepository;

	public OrganizationAccessService(
			OrganizationRepository organizationRepository,
			OrganizationMembershipRepository membershipRepository
	) {
		this.organizationRepository = organizationRepository;
		this.membershipRepository = membershipRepository;
	}

	@Transactional(readOnly = true)
	public Organization getOrganizationOrThrow(UUID organizationId) {
		return organizationRepository.findById(organizationId)
				.orElseThrow(() -> new ResourceNotFoundException("organization_not_found", "Organization not found"));
	}

	@Transactional(readOnly = true)
	public void requireOrganizationReadAccess(CurrentUser currentUser, UUID organizationId) {
		if (currentUser.isPlatformAdmin()) {
			return;
		}
		OrganizationMembership membership = getMembershipOrThrow(currentUser, organizationId);
		if (!membership.isActive()) {
			throw new ForbiddenException("organization_access_denied", "Organization access denied");
		}
	}

	@Transactional(readOnly = true)
	public void requireMembershipManagement(CurrentUser currentUser, UUID organizationId) {
		if (currentUser.isPlatformAdmin()) {
			return;
		}
		OrganizationMembership membership = getMembershipOrThrow(currentUser, organizationId);
		if (!membership.isActive() || !MEMBER_MANAGEMENT_ROLES.contains(membership.getRole())) {
			throw new ForbiddenException("organization_membership_forbidden", "Insufficient role to manage members");
		}
	}

	@Transactional(readOnly = true)
	public void requireEstablishmentManagement(CurrentUser currentUser, UUID organizationId) {
		if (currentUser.isPlatformAdmin()) {
			return;
		}
		OrganizationMembership membership = getMembershipOrThrow(currentUser, organizationId);
		if (!membership.isActive() || !ESTABLISHMENT_MANAGEMENT_ROLES.contains(membership.getRole())) {
			throw new ForbiddenException("organization_establishment_forbidden", "Insufficient role to manage establishments");
		}
	}

	private OrganizationMembership getMembershipOrThrow(CurrentUser currentUser, UUID organizationId) {
		return membershipRepository.findByOrganizationIdAndUserId(organizationId, currentUser.userId())
				.orElseThrow(() -> new ForbiddenException("organization_access_denied", "Organization access denied"));
	}
}
