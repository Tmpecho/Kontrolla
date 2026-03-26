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

	public ChecklistAccessService(
			OrganizationAccessService organizationAccessService,
			EstablishmentService establishmentService,
			OrganizationMembershipRepository organizationMembershipRepository
	) {
		this.organizationAccessService = organizationAccessService;
		this.establishmentService = establishmentService;
		this.organizationMembershipRepository = organizationMembershipRepository;
	}

	@Transactional(readOnly = true)
	public void requireChecklistReadAccess(
			UUID organizationId,
			UUID establishmentId,
			CurrentUser currentUser
	) {
		establishmentService.getEstablishment(organizationId, establishmentId, currentUser);
	}

	@Transactional(readOnly = true)
	public void requireChecklistManagementAccess(UUID organizationId, CurrentUser currentUser) {
		organizationAccessService.requireEstablishmentManagement(currentUser, organizationId);
	}

	@Transactional(readOnly = true)
	public void requireChecklistExecutionAccess(
			UUID organizationId,
			ChecklistRun checklistRun,
			CurrentUser currentUser
	) {
		if (canManageChecklistOperations(organizationId, currentUser)) {
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

	@Transactional(readOnly = true)
	public boolean canManageChecklistOperations(UUID organizationId, CurrentUser currentUser) {
		if (currentUser.isPlatformAdmin()) {
			return true;
		}

		OrganizationMembership membership = organizationMembershipRepository
				.findByOrganizationIdAndUserId(organizationId, currentUser.userId())
				.orElseThrow(() -> new ForbiddenException("organization_access_denied", "Organization access denied"));

		return membership.isActive() && ESTABLISHMENT_MANAGEMENT_ROLES.contains(membership.getRole());
	}

	@Transactional(readOnly = true)
	public void requireAssignmentFilterAccess(
			UUID organizationId,
			UUID assignedUserId,
			CurrentUser currentUser
	) {
		if (assignedUserId == null || assignedUserId.equals(currentUser.userId())) {
			return;
		}
		if (!canManageChecklistOperations(organizationId, currentUser)) {
			throw new ForbiddenException(
					"checklist_assignment_filter_forbidden",
					"You are not allowed to query another user's checklist assignments"
			);
		}
	}

	@Transactional(readOnly = true)
	public ResourceNotFoundException checklistRunNotFound() {
		return new ResourceNotFoundException("checklist_run_not_found", "Checklist run not found");
	}

	@Transactional(readOnly = true)
	public ResourceNotFoundException checklistAssignmentNotFound() {
		return new ResourceNotFoundException("checklist_run_assignment_not_found", "Checklist run assignment not found");
	}
}
