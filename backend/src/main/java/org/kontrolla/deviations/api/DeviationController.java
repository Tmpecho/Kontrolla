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

/**
 * REST API for deviation listing, reporting, assignment, and timeline
 * management.
 */
@RestController
@RequestMapping("/api/v1/organizations/{organizationId}")
public class DeviationController {

	private final DeviationService deviationService;

	/**
	 * Creates a controller backed by the deviation service.
	 *
	 * @param deviationService service handling deviation operations
	 */
	public DeviationController(DeviationService deviationService) {
		this.deviationService = deviationService;
	}

	/**
	 * Lists deviations across an organization.
	 *
	 * @param organizationId the organization identifier
	 * @param currentUser the authenticated user
	 * @param pageable pagination information
	 * @return a page of deviation responses
	 */
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

	/**
	 * Lists deviations for a single establishment.
	 *
	 * @param organizationId the organization identifier
	 * @param establishmentId the establishment identifier
	 * @param currentUser the authenticated user
	 * @param pageable pagination information
	 * @return a page of deviation responses
	 */
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

	/**
	 * Returns detailed information for a single deviation.
	 *
	 * @param organizationId the organization identifier
	 * @param establishmentId the establishment identifier
	 * @param deviationId the deviation identifier
	 * @param currentUser the authenticated user
	 * @return the detailed deviation response
	 */
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

	/**
	 * Creates a new deviation for an establishment.
	 *
	 * @param organizationId the organization identifier
	 * @param establishmentId the establishment identifier
	 * @param currentUser the authenticated user
	 * @param request the request payload
	 * @return the created deviation response
	 */
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

	/**
	 * Assigns a deviation to a user.
	 *
	 * @param organizationId the organization identifier
	 * @param establishmentId the establishment identifier
	 * @param deviationId the deviation identifier
	 * @param currentUser the authenticated user
	 * @param request the request payload
	 * @return the updated deviation response
	 */
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

	/**
	 * Updates the status of a deviation.
	 *
	 * @param organizationId the organization identifier
	 * @param establishmentId the establishment identifier
	 * @param deviationId the deviation identifier
	 * @param currentUser the authenticated user
	 * @param request the request payload
	 * @return the updated deviation response
	 */
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

	/**
	 * Updates the title, description, severity, or category of a deviation.
	 *
	 * @param organizationId the organization identifier
	 * @param establishmentId the establishment identifier
	 * @param deviationId the deviation identifier
	 * @param currentUser the authenticated user
	 * @param request the request payload
	 * @return the updated deviation response
	 */
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

	/**
	 * Adds a timeline note to a deviation.
	 *
	 * @param organizationId the organization identifier
	 * @param establishmentId the establishment identifier
	 * @param deviationId the deviation identifier
	 * @param currentUser the authenticated user
	 * @param request the request payload
	 * @return the updated deviation response
	 */
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
