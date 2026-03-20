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

@RestController
@RequestMapping("/api/v1/organizations/{organizationId}/members")
public class OrganizationMembershipController {

	private final OrganizationService organizationService;

	public OrganizationMembershipController(OrganizationService organizationService) {
		this.organizationService = organizationService;
	}

	@GetMapping
	public PageResponse<MembershipResponse> listMembers(
			@PathVariable UUID organizationId,
			@AuthenticationPrincipal CurrentUser currentUser,
			@PageableDefault(size = 20, sort = "createdAt") Pageable pageable
	) {
		return PageResponse.from(
				organizationService.listMemberships(organizationId, currentUser, pageable),
				MembershipResponse::from
		);
	}

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
				currentUser
		);
		return MembershipResponse.from(membership);
	}

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
				currentUser
		);
		return MembershipResponse.from(membership);
	}
}
