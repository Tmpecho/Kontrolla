package org.kontrolla.organizations.api;

import jakarta.validation.Valid;
import org.kontrolla.common.api.PageResponse;
import org.kontrolla.iam.security.CurrentUser;
import org.kontrolla.organizations.application.OrganizationService;
import org.kontrolla.organizations.domain.OrganizationMembership;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST API for listing and managing organization memberships.
 */
@RestController
@RequestMapping("/api/v1/organizations/{organizationId}/members")
public class OrganizationMembershipController {

	private final OrganizationService organizationService;

	/**
	 * Creates a controller backed by the organization service.
	 *
	 * @param organizationService service handling membership operations
	 */
	public OrganizationMembershipController(OrganizationService organizationService) {
		this.organizationService = organizationService;
	}

	/**
	 * Lists memberships for an organization, optionally scoped by establishment.
	 *
	 * @param organizationId the organization identifier
	 * @param currentUser the authenticated user
	 * @param establishmentId optional establishment filter
	 * @param includeInactive whether inactive memberships should be included
	 * @param pageable pagination information
	 * @return a page of membership responses
	 */
	@GetMapping
	public PageResponse<MembershipResponse> listMembers(
			@PathVariable UUID organizationId,
			@AuthenticationPrincipal CurrentUser currentUser,
			@RequestParam(required = false) UUID establishmentId,
			@RequestParam(defaultValue = "false") boolean includeInactive,
			@PageableDefault(size = 20, sort = "createdAt") Pageable pageable
	) {
		return PageResponse.from(
				organizationService.listMemberships(
						organizationId,
						currentUser,
						pageable,
						includeInactive,
						establishmentId
				),
				MembershipResponse::from
		);
	}

	/**
	 * Creates a membership for an existing user.
	 *
	 * @param organizationId the organization identifier
	 * @param currentUser the authenticated user
	 * @param request the request payload
	 * @return the created membership response
	 */
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public MembershipResponse createMembership(
			@PathVariable UUID organizationId,
			@AuthenticationPrincipal CurrentUser currentUser,
			@Valid @RequestBody CreateMembershipRequest request
	) {
		OrganizationMembership membership = organizationService.addMembership(
				organizationId,
				request.userId(),
				request.role(),
				request.active() == null || request.active(),
				request.allEstablishments(),
				request.establishmentIds(),
				currentUser
		);
		return MembershipResponse.from(membership);
	}

	/**
	 * Creates a managed user and organization membership in one operation.
	 *
	 * @param organizationId the organization identifier
	 * @param currentUser the authenticated user
	 * @param request the request payload
	 * @return the created managed membership response
	 */
	@PostMapping("/managed-users")
	@ResponseStatus(HttpStatus.CREATED)
	public ManagedMemberProvisionResponse createManagedMember(
			@PathVariable UUID organizationId,
			@AuthenticationPrincipal CurrentUser currentUser,
			@Valid @RequestBody CreateManagedMemberRequest request
	) {
		return ManagedMemberProvisionResponse.from(
				organizationService.createManagedMembership(
						organizationId,
						request.email(),
						request.firstName(),
						request.lastName(),
						request.role(),
						request.active() == null || request.active(),
						request.allEstablishments(),
						request.establishmentIds(),
						currentUser
				)
		);
	}

	/**
	 * Updates an existing membership.
	 *
	 * @param organizationId the organization identifier
	 * @param membershipId the membership identifier
	 * @param currentUser the authenticated user
	 * @param request the request payload
	 * @return the updated membership response
	 */
	@PatchMapping("/{membershipId}")
	public MembershipResponse updateMembership(
			@PathVariable UUID organizationId,
			@PathVariable UUID membershipId,
			@AuthenticationPrincipal CurrentUser currentUser,
			@Valid @RequestBody UpdateMembershipRequest request
	) {
		OrganizationMembership membership = organizationService.updateMembership(
				organizationId,
				membershipId,
				request.role(),
				request.active(),
				request.allEstablishments(),
				request.establishmentIds(),
				currentUser
		);
		return MembershipResponse.from(membership);
	}
}
