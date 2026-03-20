package org.kontrolla.organizations.application;

import org.kontrolla.common.exception.ConflictException;
import org.kontrolla.common.exception.ResourceNotFoundException;
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

import java.util.UUID;

@Service
public class OrganizationService {

	private final OrganizationRepository organizationRepository;
	private final OrganizationMembershipRepository membershipRepository;
	private final UserRepository userRepository;
	private final OrganizationAccessService organizationAccessService;

	public OrganizationService(
			OrganizationRepository organizationRepository,
			OrganizationMembershipRepository membershipRepository,
			UserRepository userRepository,
			OrganizationAccessService organizationAccessService
	) {
		this.organizationRepository = organizationRepository;
		this.membershipRepository = membershipRepository;
		this.userRepository = userRepository;
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
	public Page<OrganizationMembership> listMemberships(UUID organizationId, CurrentUser currentUser, Pageable pageable) {
		organizationAccessService.getOrganizationOrThrow(organizationId);
		organizationAccessService.requireOrganizationReadAccess(currentUser, organizationId);
		return membershipRepository.findByOrganizationId(organizationId, pageable);
	}

	@Transactional
	public OrganizationMembership addMembership(
			UUID organizationId,
			UUID userId,
			OrganizationRole role,
			boolean active,
			CurrentUser currentUser
	) {
		Organization organization = organizationAccessService.getOrganizationOrThrow(organizationId);
		organizationAccessService.requireMembershipManagement(currentUser, organizationId);

		if (membershipRepository.findByOrganizationIdAndUserId(organizationId, userId).isPresent()) {
			throw new ConflictException("membership_already_exists", "The user is already a member of this organization");
		}

		org.kontrolla.iam.domain.User user = userRepository.findById(userId)
				.orElseThrow(() -> new ResourceNotFoundException("user_not_found", "User not found"));

		OrganizationMembership membership = new OrganizationMembership(organization, user, role, active);
		return membershipRepository.save(membership);
	}

	@Transactional
	public OrganizationMembership updateMembership(
			UUID organizationId,
			UUID membershipId,
			OrganizationRole role,
			boolean active,
			CurrentUser currentUser
	) {
		organizationAccessService.getOrganizationOrThrow(organizationId);
		organizationAccessService.requireMembershipManagement(currentUser, organizationId);

		OrganizationMembership membership = membershipRepository.findByIdAndOrganizationId(membershipId, organizationId)
				.orElseThrow(() -> new ResourceNotFoundException("membership_not_found", "Membership not found"));

		membership.setRole(role);
		membership.setActive(active);
		return membership;
	}
}
