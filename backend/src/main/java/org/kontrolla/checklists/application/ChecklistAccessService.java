package org.kontrolla.checklists.application;

import org.kontrolla.checklists.domain.ChecklistRun;
import org.kontrolla.common.exception.ForbiddenException;
import org.kontrolla.common.exception.ResourceNotFoundException;
import org.kontrolla.establishments.application.EstablishmentService;
import org.kontrolla.iam.security.CurrentUser;
import org.kontrolla.organizations.application.OrganizationAccessService;
import org.kontrolla.organizations.domain.OrganizationMembership;
import org.kontrolla.organizations.domain.OrganizationRole;
import org.kontrolla.organizations.infrastructure.OrganizationMembershipRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;

/**
 * Centralizes checklist read, management, and execution access rules.
 */
@Service
public class ChecklistAccessService {

	private static final Set<OrganizationRole> ESTABLISHMENT_MANAGEMENT_ROLES = Set.of(
			OrganizationRole.ORG_OWNER,
			OrganizationRole.ORG_ADMIN,
			OrganizationRole.ORG_MANAGER
	);

	private final OrganizationAccessService organizationAccessService;
	private final EstablishmentService establishmentService;
	private final OrganizationMembershipRepository organizationMembershipRepository;

	/**
	 * Creates the checklist access service.
	 *
	 * @param organizationAccessService service for organization access checks
	 * @param establishmentService service for establishment access and lookup
	 * @param organizationMembershipRepository repository for organization memberships
	 */
	public ChecklistAccessService(
			OrganizationAccessService organizationAccessService,
			EstablishmentService establishmentService,
			OrganizationMembershipRepository organizationMembershipRepository
	) {
		this.organizationAccessService = organizationAccessService;
		this.establishmentService = establishmentService;
		this.organizationMembershipRepository = organizationMembershipRepository;
	}

	/**
	 * Requires read access to checklist data for an establishment.
	 *
	 * @param organizationId the organization identifier
	 * @param establishmentId the establishment identifier
	 * @param currentUser the authenticated user
	 */
	@Transactional(readOnly = true)
	public void requireChecklistReadAccess(
			UUID organizationId,
			UUID establishmentId,
			CurrentUser currentUser
	) {
		establishmentService.getEstablishment(organizationId, establishmentId, currentUser);
	}

	/**
	 * Requires checklist management access at organization scope.
	 *
	 * @param organizationId the organization identifier
	 * @param currentUser the authenticated user
	 */
	@Transactional(readOnly = true)
	public void requireChecklistManagementAccess(UUID organizationId, CurrentUser currentUser) {
		organizationAccessService.requireEstablishmentManagement(currentUser, organizationId);
	}

	/**
	 * Requires checklist management access for a specific establishment.
	 *
	 * @param organizationId the organization identifier
	 * @param establishmentId the establishment identifier
	 * @param currentUser the authenticated user
	 */
	@Transactional(readOnly = true)
	public void requireChecklistManagementAccess(
			UUID organizationId,
			UUID establishmentId,
			CurrentUser currentUser
	) {
		organizationAccessService.requireEstablishmentManagement(currentUser, organizationId);
		organizationAccessService.requireEstablishmentAccess(currentUser, organizationId, establishmentId);
	}

	/**
	 * Requires access to execute a checklist run.
	 *
	 * @param organizationId the organization identifier
	 * @param checklistRun the checklist run
	 * @param currentUser the authenticated user
	 */
	@Transactional(readOnly = true)
	public void requireChecklistExecutionAccess(
			UUID organizationId,
			ChecklistRun checklistRun,
			CurrentUser currentUser
	) {
		if (canManageChecklistOperations(organizationId, checklistRun.getEstablishment().getId(), currentUser)) {
			return;
		}

		if (checklistRun.getAssignments().isEmpty()) {
			return;
		}

		boolean assignedToCurrentUser = checklistRun.getAssignments().stream()
				.anyMatch(assignment -> assignment.getAssignedUser().getId().equals(currentUser.userId()));

		if (!assignedToCurrentUser) {
			throw new ForbiddenException(
					"checklist_run_execution_forbidden",
					"You are not assigned to this checklist run"
			);
		}
	}

	/**
	 * Indicates whether the current user can manage checklist operations for an
	 * establishment.
	 *
	 * @param organizationId the organization identifier
	 * @param establishmentId the establishment identifier
	 * @param currentUser the authenticated user
	 * @return {@code true} when management access is allowed
	 */
	@Transactional(readOnly = true)
	public boolean canManageChecklistOperations(UUID organizationId, UUID establishmentId, CurrentUser currentUser) {
		if (currentUser.isPlatformAdmin()) {
			return true;
		}

		OrganizationMembership membership = organizationMembershipRepository
				.findByOrganizationIdAndUserId(organizationId, currentUser.userId())
				.orElseThrow(() -> new ForbiddenException("organization_access_denied", "Organization access denied"));

		return membership.isActive()
				&& ESTABLISHMENT_MANAGEMENT_ROLES.contains(membership.getRole())
				&& membership.hasEstablishmentAccess(establishmentId);
	}

	/**
	 * Requires permission to filter assignments for another user.
	 *
	 * @param organizationId the organization identifier
	 * @param establishmentId the establishment identifier
	 * @param assignedUserId the user being filtered for
	 * @param currentUser the authenticated user
	 */
	@Transactional(readOnly = true)
	public void requireAssignmentFilterAccess(
			UUID organizationId,
			UUID establishmentId,
			UUID assignedUserId,
			CurrentUser currentUser
	) {
		if (assignedUserId == null || assignedUserId.equals(currentUser.userId())) {
			return;
		}
		if (!canManageChecklistOperations(organizationId, establishmentId, currentUser)) {
			throw new ForbiddenException(
					"checklist_assignment_filter_forbidden",
					"You are not allowed to query another user's checklist assignments"
			);
		}
	}

	/**
	 * Creates a standardized not-found exception for checklist runs.
	 *
	 * @return the checklist run not-found exception
	 */
	@Transactional(readOnly = true)
	public ResourceNotFoundException checklistRunNotFound() {
		return new ResourceNotFoundException("checklist_run_not_found", "Checklist run not found");
	}

	/**
	 * Creates a standardized not-found exception for checklist run assignments.
	 *
	 * @return the checklist assignment not-found exception
	 */
	@Transactional(readOnly = true)
	public ResourceNotFoundException checklistAssignmentNotFound() {
		return new ResourceNotFoundException("checklist_run_assignment_not_found", "Checklist run assignment not found");
	}
}
