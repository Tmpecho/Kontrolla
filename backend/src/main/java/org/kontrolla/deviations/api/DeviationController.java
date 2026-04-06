package org.kontrolla.deviations.api;

import jakarta.validation.Valid;
import org.kontrolla.common.api.PageResponse;
import org.kontrolla.deviations.application.DeviationService;
import org.kontrolla.iam.security.CurrentUser;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/organizations/{organizationId}")
public class DeviationController {

	private final DeviationService deviationService;

	public DeviationController(DeviationService deviationService) {
		this.deviationService = deviationService;
	}

	@GetMapping("/deviations")
	public PageResponse<DeviationResponse> listOrganizationDeviations(
			@PathVariable UUID organizationId,
			@AuthenticationPrincipal CurrentUser currentUser,
			@PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
	) {
		return PageResponse.from(
				deviationService.listDeviationsByOrganizationId(organizationId, currentUser, pageable),
				DeviationResponse::from
		);
	}

	@GetMapping("/establishments/{establishmentId}/deviations")
	public PageResponse<DeviationResponse> listEstablishmentDeviations(
			@PathVariable UUID organizationId,
			@PathVariable UUID establishmentId,
			@AuthenticationPrincipal CurrentUser currentUser,
			@PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
	) {
		return PageResponse.from(
				deviationService.listDeviationsByEstablishmentId(organizationId, establishmentId, currentUser, pageable),
				DeviationResponse::from
		);
	}

	@GetMapping("/establishments/{establishmentId}/deviations/{deviationId}")
	public DeviationDetailsResponse getDeviation(
			@PathVariable UUID organizationId,
			@PathVariable UUID establishmentId,
			@PathVariable UUID deviationId,
			@AuthenticationPrincipal CurrentUser currentUser
	) {
		return DeviationDetailsResponse.from(
				deviationService.getDeviation(organizationId, establishmentId, deviationId, currentUser)
		);
	}

	@PostMapping("/establishments/{establishmentId}/deviations")
	@ResponseStatus(HttpStatus.CREATED)
	public DeviationDetailsResponse createDeviation(
			@PathVariable UUID organizationId,
			@PathVariable UUID establishmentId,
			@AuthenticationPrincipal CurrentUser currentUser,
			@Valid @RequestBody CreateDeviationRequest request
		) {
		return DeviationDetailsResponse.from(
				deviationService.createDeviation(
						currentUser,
						request.title(),
						request.description(),
						request.category(),
						request.severity(),
						organizationId,
						establishmentId
				)
		);
	}

	@PutMapping("/establishments/{establishmentId}/deviations/{deviationId}/assignment")
	public DeviationDetailsResponse assignDeviation(
			@PathVariable UUID organizationId,
			@PathVariable UUID establishmentId,
			@PathVariable UUID deviationId,
			@AuthenticationPrincipal CurrentUser currentUser,
			@Valid @RequestBody AssignDeviationRequest request
		) {
		return DeviationDetailsResponse.from(
				deviationService.assignDeviation(
						organizationId,
						establishmentId,
						deviationId,
						request.assignedUserId(),
						currentUser
				)
		);
	}

	@PutMapping("/establishments/{establishmentId}/deviations/{deviationId}/status")
	public DeviationDetailsResponse updateDeviationStatus(
			@PathVariable UUID organizationId,
			@PathVariable UUID establishmentId,
			@PathVariable UUID deviationId,
			@AuthenticationPrincipal CurrentUser currentUser,
			@Valid @RequestBody UpdateDeviationStatusRequest request
		) {
		return DeviationDetailsResponse.from(
				deviationService.updateDeviationStatus(
						organizationId,
						establishmentId,
						deviationId,
						request.status(),
						currentUser
				)
		);
	}

	@PutMapping("/establishments/{establishmentId}/deviations/{deviationId}")
	public DeviationDetailsResponse updateDeviationDetails(
			@PathVariable UUID organizationId,
			@PathVariable UUID establishmentId,
			@PathVariable UUID deviationId,
			@AuthenticationPrincipal CurrentUser currentUser,
			@Valid @RequestBody UpdateDeviationDetailsRequest request
		) {
		return DeviationDetailsResponse.from(
				deviationService.updateDeviationDetails(
						organizationId,
						establishmentId,
						deviationId,
						request.title(),
						request.description(),
						request.severity(),
						request.category(),
						currentUser
				)
		);
	}

	@PostMapping("/establishments/{establishmentId}/deviations/{deviationId}/timeline")
	public DeviationDetailsResponse addTimelineNote(
			@PathVariable UUID organizationId,
			@PathVariable UUID establishmentId,
			@PathVariable UUID deviationId,
			@AuthenticationPrincipal CurrentUser currentUser,
			@Valid @RequestBody AddDeviationTimelineNoteRequest request
	) {
		return DeviationDetailsResponse.from(
				deviationService.addTimelineNote(
						organizationId,
						establishmentId,
						deviationId,
						request.note(),
						currentUser
				)
		);
	}
}
