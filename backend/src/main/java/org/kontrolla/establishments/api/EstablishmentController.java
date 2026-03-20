package org.kontrolla.establishments.api;

import jakarta.validation.Valid;
import org.kontrolla.common.api.PageResponse;
import org.kontrolla.establishments.application.EstablishmentService;
import org.kontrolla.establishments.domain.Establishment;
import org.kontrolla.establishments.domain.EstablishmentStatus;
import org.kontrolla.iam.security.CurrentUser;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/organizations/{organizationId}/establishments")
public class EstablishmentController {

	private final EstablishmentService establishmentService;

	public EstablishmentController(EstablishmentService establishmentService) {
		this.establishmentService = establishmentService;
	}

	@GetMapping
	public PageResponse<EstablishmentResponse> listEstablishments(
			@PathVariable UUID organizationId,
			@AuthenticationPrincipal CurrentUser currentUser,
			@PageableDefault(size = 20, sort = "createdAt") Pageable pageable
	) {
		return PageResponse.from(
				establishmentService.listEstablishments(organizationId, currentUser, pageable),
				EstablishmentResponse::from
		);
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public EstablishmentResponse createEstablishment(
			@PathVariable UUID organizationId,
			@AuthenticationPrincipal CurrentUser currentUser,
			@Valid @RequestBody CreateEstablishmentRequest request
	) {
		Establishment establishment = establishmentService.createEstablishment(
				organizationId,
				request.name(),
				request.type(),
				request.status() == null ? EstablishmentStatus.ACTIVE : request.status(),
				currentUser
		);
		return EstablishmentResponse.from(establishment);
	}

	@GetMapping("/{establishmentId}")
	public EstablishmentResponse getEstablishment(
			@PathVariable UUID organizationId,
			@PathVariable UUID establishmentId,
			@AuthenticationPrincipal CurrentUser currentUser
	) {
		return EstablishmentResponse.from(establishmentService.getEstablishment(organizationId, establishmentId, currentUser));
	}
}
