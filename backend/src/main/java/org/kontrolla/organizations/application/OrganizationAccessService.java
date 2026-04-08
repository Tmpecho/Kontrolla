package org.kontrolla.organizations.application;

import org.kontrolla.common.exception.ForbiddenException;
import org.kontrolla.common.exception.ResourceNotFoundException;
import org.kontrolla.establishments.infrastructure.EstablishmentRepository;
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
	private final EstablishmentRepository establishmentRepository;
	private final OrganizationMembershipRepository membershipRepository;

	public OrganizationAccessService(
			OrganizationRepository organizationRepository,
			EstablishmentRepository establishmentRepository,
			OrganizationMembershipRepository membershipRepository
	) {
		this.organizationRepository = organizationRepository;
		this.establishmentRepository = establishmentRepository;
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
		getActiveMembershipOrThrow(currentUser, organizationId);
	}

	@Transactional(readOnly = true)
	public void requireMembershipManagement(CurrentUser currentUser, UUID organizationId) {
		if (currentUser.isPlatformAdmin()) {
			return;
		}
		OrganizationMembership membership = getActiveMembershipOrThrow(currentUser, organizationId);
		if (!MEMBER_MANAGEMENT_ROLES.contains(membership.getRole())) {
			throw new ForbiddenException("organization_membership_forbidden", "Insufficient role to manage members");
		}
	}

	@Transactional(readOnly = true)
	public void requireEstablishmentManagement(CurrentUser currentUser, UUID organizationId) {
		if (currentUser.isPlatformAdmin()) {
			return;
		}
		OrganizationMembership membership = getActiveMembershipOrThrow(currentUser, organizationId);
		if (!ESTABLISHMENT_MANAGEMENT_ROLES.contains(membership.getRole())) {
			throw new ForbiddenException("organization_establishment_forbidden", "Insufficient role to manage establishments");
		}
	}

	@Transactional(readOnly = true)
	public void requireOrganizationWideOperationalAccess(CurrentUser currentUser, UUID organizationId) {
		if (currentUser.isPlatformAdmin()) {
			return;
		}

		OrganizationMembership membership = getActiveMembershipOrThrow(currentUser, organizationId);
		if (membership.getRole() != OrganizationRole.ORG_OWNER
				&& membership.getRole() != OrganizationRole.ORG_ADMIN
				&& !membership.isAccessAllEstablishments()) {
			throw new ForbiddenException(
					"organization_operational_scope_forbidden",
					"Organization-wide operational access is not available for your membership"
			);
		}
	}

	@Transactional(readOnly = true)
	public void requireEstablishmentAccess(CurrentUser currentUser, UUID organizationId, UUID establishmentId) {
		establishmentRepository.findByIdAndOrganizationId(establishmentId, organizationId)
				.orElseThrow(() -> new ResourceNotFoundException("establishment_not_found", "Establishment not found"));

		if (currentUser.isPlatformAdmin()) {
			return;
		}

		OrganizationMembership membership = getActiveMembershipOrThrow(currentUser, organizationId);
		if (!membership.hasEstablishmentAccess(establishmentId)) {
			throw new ForbiddenException("establishment_access_denied", "Establishment access denied");
		}
	}

	@Transactional(readOnly = true)
	public OrganizationMembership getActiveMembershipOrThrow(CurrentUser currentUser, UUID organizationId) {
		OrganizationMembership membership = getMembershipOrThrow(currentUser, organizationId);
		if (!membership.isActive()) {
			throw new ForbiddenException("organization_access_denied", "Organization access denied");
		}
		return membership;
	}

	private OrganizationMembership getMembershipOrThrow(CurrentUser currentUser, UUID organizationId) {
		return membershipRepository.findByOrganizationIdAndUserId(organizationId, currentUser.userId())
				.orElseThrow(() -> new ForbiddenException("organization_access_denied", "Organization access denied"));
	}
}
