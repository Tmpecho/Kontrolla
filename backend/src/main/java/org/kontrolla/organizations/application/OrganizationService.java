package org.kontrolla.organizations.application;

import org.kontrolla.common.exception.ConflictException;
import org.kontrolla.common.exception.ResourceNotFoundException;
import org.kontrolla.establishments.domain.Establishment;
import org.kontrolla.establishments.infrastructure.EstablishmentRepository;
import org.kontrolla.iam.application.UserAdministrationService;
import org.kontrolla.iam.application.UserInviteService;
import org.kontrolla.iam.infrastructure.UserRepository;
import org.kontrolla.iam.security.CurrentUser;
import org.kontrolla.organizations.domain.Organization;
import org.kontrolla.organizations.domain.OrganizationMembership;
import org.kontrolla.organizations.domain.OrganizationRole;
import org.kontrolla.organizations.domain.OrganizationStatus;
import org.kontrolla.organizations.infrastructure.OrganizationMembershipRepository;
import org.kontrolla.organizations.infrastructure.OrganizationRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Service
public class OrganizationService {

	private final OrganizationRepository organizationRepository;
	private final OrganizationMembershipRepository membershipRepository;
	private final EstablishmentRepository establishmentRepository;
	private final UserRepository userRepository;
	private final UserAdministrationService userAdministrationService;
	private final UserInviteService userInviteService;
	private final OrganizationAccessService organizationAccessService;

	public OrganizationService(
			OrganizationRepository organizationRepository,
			OrganizationMembershipRepository membershipRepository,
			EstablishmentRepository establishmentRepository,
			UserRepository userRepository,
			UserAdministrationService userAdministrationService,
			UserInviteService userInviteService,
			OrganizationAccessService organizationAccessService
	) {
		this.organizationRepository = organizationRepository;
		this.membershipRepository = membershipRepository;
		this.establishmentRepository = establishmentRepository;
		this.userRepository = userRepository;
		this.userAdministrationService = userAdministrationService;
		this.userInviteService = userInviteService;
		this.organizationAccessService = organizationAccessService;
	}

	@Transactional
	public Organization createOrganization(String name, OrganizationStatus status) {
		Organization organization = new Organization(name, status);
		return organizationRepository.save(organization);
	}

	@Transactional(readOnly = true)
	public Page<Organization> listOrganizations(Pageable pageable) {
		return organizationRepository.findAll(pageable);
	}

	@Transactional(readOnly = true)
	public Organization getOrganization(UUID organizationId, CurrentUser currentUser) {
		Organization organization = organizationAccessService.getOrganizationOrThrow(organizationId);
		organizationAccessService.requireOrganizationReadAccess(currentUser, organizationId);
		return organization;
	}

	@Transactional(readOnly = true)
	public Page<OrganizationMembership> listMemberships(
			UUID organizationId,
			CurrentUser currentUser,
			Pageable pageable,
			boolean includeInactive,
			UUID establishmentId
	) {
		organizationAccessService.getOrganizationOrThrow(organizationId);
		if (establishmentId != null) {
			organizationAccessService.requireEstablishmentAccess(currentUser, organizationId, establishmentId);
			if (includeInactive) {
				return membershipRepository.findByOrganizationIdAndAccessibleEstablishmentId(
						organizationId,
						establishmentId,
						pageable
				);
			}
			return membershipRepository.findByOrganizationIdAndActiveTrueAndAccessibleEstablishmentId(
					organizationId,
					establishmentId,
					pageable
			);
		}

		organizationAccessService.requireOrganizationReadAccess(currentUser, organizationId);
		if (includeInactive) {
			return membershipRepository.findByOrganizationId(organizationId, pageable);
		}
		return membershipRepository.findByOrganizationIdAndActiveTrue(organizationId, pageable);
	}

	@Transactional
	public OrganizationMembership addMembership(
			UUID organizationId,
			UUID userId,
			OrganizationRole role,
			boolean active,
			Boolean allEstablishments,
			Collection<UUID> establishmentIds,
			CurrentUser currentUser
	) {
		Organization organization = organizationAccessService.getOrganizationOrThrow(organizationId);
		organizationAccessService.requireMembershipManagement(currentUser, organizationId);

		if (membershipRepository.findByOrganizationIdAndUserId(organizationId, userId).isPresent()) {
			throw new ConflictException("membership_already_exists", "The user is already a member of this organization");
		}

		org.kontrolla.iam.domain.User user = userRepository.findById(userId)
				.orElseThrow(() -> new ResourceNotFoundException("user_not_found", "User not found"));

		MembershipScope membershipScope = resolveRequestedMembershipScope(
				organizationId,
				role,
				allEstablishments,
				establishmentIds
		);
		OrganizationMembership membership = new OrganizationMembership(
				organization,
				user,
				role,
				active,
				membershipScope.accessAllEstablishments()
		);
		membership.replaceAccessibleEstablishments(membershipScope.accessibleEstablishments());
		return membershipRepository.save(membership);
	}

	@Transactional
	public ManagedMembershipProvision createManagedMembership(
			UUID organizationId,
			String email,
			String firstName,
			String lastName,
			OrganizationRole role,
			boolean active,
			Boolean allEstablishments,
			Collection<UUID> establishmentIds,
			CurrentUser currentUser
	) {
		Organization organization = organizationAccessService.getOrganizationOrThrow(organizationId);
		organizationAccessService.requireMembershipManagement(currentUser, organizationId);

		org.kontrolla.iam.domain.User user = userAdministrationService.createInvitedUser(email, firstName, lastName);
		MembershipScope membershipScope = resolveRequestedMembershipScope(
				organizationId,
				role,
				allEstablishments,
				establishmentIds
		);
		OrganizationMembership membership = new OrganizationMembership(
				organization,
				user,
				role,
				active,
				membershipScope.accessAllEstablishments()
		);
		membership.replaceAccessibleEstablishments(membershipScope.accessibleEstablishments());
		membership = membershipRepository.save(membership);
		UserInviteService.IssuedInvite issuedInvite = userInviteService.issueOrganizationInvite(user, organization);
		return new ManagedMembershipProvision(membership, issuedInvite.expiresAt(), issuedInvite.inviteUrl());
	}

	@Transactional
	public OrganizationMembership updateMembership(
			UUID organizationId,
			UUID membershipId,
			OrganizationRole role,
			boolean active,
			Boolean allEstablishments,
			Collection<UUID> establishmentIds,
			CurrentUser currentUser
	) {
		organizationAccessService.getOrganizationOrThrow(organizationId);
		organizationAccessService.requireMembershipManagement(currentUser, organizationId);

		OrganizationMembership membership = membershipRepository.findByIdAndOrganizationId(membershipId, organizationId)
				.orElseThrow(() -> new ResourceNotFoundException("membership_not_found", "Membership not found"));

		MembershipScope membershipScope = resolveUpdatedMembershipScope(
				organizationId,
				membership,
				role,
				allEstablishments,
				establishmentIds
		);
		membership.setRole(role);
		membership.setActive(active);
		membership.setAccessAllEstablishments(membershipScope.accessAllEstablishments());
		membership.replaceAccessibleEstablishments(membershipScope.accessibleEstablishments());
		if (active) {
			membership.getUser().setActive(true);
		}
		return membership;
	}

	private MembershipScope resolveRequestedMembershipScope(
			UUID organizationId,
			OrganizationRole role,
			Boolean allEstablishments,
			Collection<UUID> establishmentIds
	) {
		boolean accessAllEstablishments = resolveAccessAllEstablishments(role, allEstablishments, establishmentIds, true);
		return new MembershipScope(
				accessAllEstablishments,
				resolveAccessibleEstablishments(organizationId, accessAllEstablishments, establishmentIds)
		);
	}

	private MembershipScope resolveUpdatedMembershipScope(
			UUID organizationId,
			OrganizationMembership membership,
			OrganizationRole role,
			Boolean allEstablishments,
			Collection<UUID> establishmentIds
	) {
		if (role == OrganizationRole.ORG_OWNER || role == OrganizationRole.ORG_ADMIN) {
			return new MembershipScope(true, List.of());
		}

		boolean scopeProvided = allEstablishments != null || establishmentIds != null;
		if (!scopeProvided) {
			return new MembershipScope(
					membership.isAccessAllEstablishments(),
					membership.isAccessAllEstablishments()
							? List.of()
							: List.copyOf(membership.getAccessibleEstablishments())
			);
		}

		boolean accessAllEstablishments = resolveAccessAllEstablishments(role, allEstablishments, establishmentIds, true);
		return new MembershipScope(
				accessAllEstablishments,
				resolveAccessibleEstablishments(organizationId, accessAllEstablishments, establishmentIds)
		);
	}

	private boolean resolveAccessAllEstablishments(
			OrganizationRole role,
			Boolean allEstablishments,
			Collection<UUID> establishmentIds,
			boolean defaultAllEstablishments
	) {
		if (role == OrganizationRole.ORG_OWNER || role == OrganizationRole.ORG_ADMIN) {
			return true;
		}

		if (allEstablishments != null) {
			return allEstablishments;
		}

		if (establishmentIds != null) {
			return false;
		}

		return defaultAllEstablishments;
	}

	private List<Establishment> resolveAccessibleEstablishments(
			UUID organizationId,
			boolean accessAllEstablishments,
			Collection<UUID> establishmentIds
	) {
		if (accessAllEstablishments) {
			return List.of();
		}

		if (establishmentIds == null || establishmentIds.isEmpty()) {
			throw new ConflictException(
					"membership_establishments_required",
					"At least one establishment must be assigned when organization-wide access is disabled"
			);
		}

		List<UUID> distinctEstablishmentIds = establishmentIds.stream().distinct().toList();
		List<Establishment> establishments = establishmentRepository.findByOrganizationIdAndIdIn(
				organizationId,
				distinctEstablishmentIds
		);
		if (establishments.size() != distinctEstablishmentIds.size()) {
			throw new ResourceNotFoundException(
					"membership_establishment_not_found",
					"One or more establishments were not found in the organization"
			);
		}

		return establishments;
	}

	public record ManagedMembershipProvision(
			OrganizationMembership membership,
			java.time.Instant inviteExpiresAt,
			String inviteUrl
	) {
	}

	private record MembershipScope(
			boolean accessAllEstablishments,
			List<Establishment> accessibleEstablishments
	) {
	}
}
