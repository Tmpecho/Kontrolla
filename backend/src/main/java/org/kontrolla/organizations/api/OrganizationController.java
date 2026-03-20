package org.kontrolla.organizations.api;

import org.kontrolla.iam.security.CurrentUser;
import org.kontrolla.organizations.application.OrganizationService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/organizations")
public class OrganizationController {

	private final OrganizationService organizationService;

	public OrganizationController(OrganizationService organizationService) {
		this.organizationService = organizationService;
	}

	@GetMapping("/{organizationId}")
	public OrganizationResponse getOrganization(
			@PathVariable UUID organizationId,
			@AuthenticationPrincipal CurrentUser currentUser
	) {
		return OrganizationResponse.from(organizationService.getOrganization(organizationId, currentUser));
	}
}
