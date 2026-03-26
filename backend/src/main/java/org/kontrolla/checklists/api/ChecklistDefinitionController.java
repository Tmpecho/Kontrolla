package org.kontrolla.checklists.api;

import jakarta.validation.Valid;
import org.kontrolla.checklists.application.ChecklistDefinitionService;
import org.kontrolla.checklists.domain.ChecklistServiceArea;
import org.kontrolla.common.api.PageResponse;
import org.kontrolla.iam.security.CurrentUser;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/organizations/{organizationId}/establishments/{establishmentId}/checklists/definitions")
public class ChecklistDefinitionController {

	private final ChecklistDefinitionService checklistDefinitionService;

	public ChecklistDefinitionController(ChecklistDefinitionService checklistDefinitionService) {
		this.checklistDefinitionService = checklistDefinitionService;
	}

	@GetMapping
	public PageResponse<ChecklistDefinitionResponse> listChecklistDefinitions(
			@PathVariable UUID organizationId,
			@PathVariable UUID establishmentId,
			@RequestParam ChecklistServiceArea serviceArea,
			@AuthenticationPrincipal CurrentUser currentUser,
			@PageableDefault(size = 20, sort = "createdAt") Pageable pageable
	) {
		return PageResponse.from(
				checklistDefinitionService.listChecklistDefinitions(
						organizationId,
						establishmentId,
						serviceArea,
						currentUser,
						pageable
				),
				ChecklistDefinitionResponse::from
		);
	}

	@GetMapping("/{checklistDefinitionId}")
	public ChecklistDefinitionResponse getChecklistDefinition(
			@PathVariable UUID organizationId,
			@PathVariable UUID establishmentId,
			@PathVariable UUID checklistDefinitionId,
			@AuthenticationPrincipal CurrentUser currentUser
	) {
		return ChecklistDefinitionResponse.from(
				checklistDefinitionService.getChecklistDefinition(
						organizationId,
						establishmentId,
						checklistDefinitionId,
						currentUser
				)
		);
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public ChecklistDefinitionResponse createChecklistDefinition(
			@PathVariable UUID organizationId,
			@PathVariable UUID establishmentId,
			@AuthenticationPrincipal CurrentUser currentUser,
			@Valid @RequestBody CreateChecklistDefinitionRequest request
	) {
		return ChecklistDefinitionResponse.from(
				checklistDefinitionService.createChecklistDefinition(
						organizationId,
						establishmentId,
						request.serviceArea(),
						request.title(),
						request.description(),
						toCreateChecklistItemInputs(request.items()),
						toCreateChecklistScheduleInputs(request.schedules()),
						currentUser
				)
		);
	}

	@PutMapping("/{checklistDefinitionId}")
	public ChecklistDefinitionResponse updateChecklistDefinition(
			@PathVariable UUID organizationId,
			@PathVariable UUID establishmentId,
			@PathVariable UUID checklistDefinitionId,
			@AuthenticationPrincipal CurrentUser currentUser,
			@Valid @RequestBody UpdateChecklistDefinitionRequest request
	) {
		return ChecklistDefinitionResponse.from(
				checklistDefinitionService.updateChecklistDefinition(
						organizationId,
						establishmentId,
						checklistDefinitionId,
						request.serviceArea(),
						request.title(),
						request.description(),
						request.status(),
						toUpdateChecklistItemInputs(request.items()),
						toUpdateChecklistScheduleInputs(request.schedules()),
						currentUser
				)
		);
	}

	private List<ChecklistDefinitionService.ChecklistItemInput> toCreateChecklistItemInputs(
			List<CreateChecklistDefinitionRequest.ChecklistItemRequest> items
	) {
		return items.stream()
				.map(item -> new ChecklistDefinitionService.ChecklistItemInput(
						item.prompt(),
						item.instructionText(),
						item.responseType(),
						item.required(),
						item.sortOrder()
				))
				.toList();
	}

	private List<ChecklistDefinitionService.ChecklistItemInput> toUpdateChecklistItemInputs(
			List<UpdateChecklistDefinitionRequest.ChecklistItemRequest> items
	) {
		return items.stream()
				.map(item -> new ChecklistDefinitionService.ChecklistItemInput(
						item.prompt(),
						item.instructionText(),
						item.responseType(),
						item.required(),
						item.sortOrder()
				))
				.toList();
	}

	private List<ChecklistDefinitionService.ChecklistScheduleInput> toCreateChecklistScheduleInputs(
			List<CreateChecklistDefinitionRequest.ChecklistScheduleRequest> schedules
	) {
		if (schedules == null) {
			return List.of();
		}

		return schedules.stream()
				.map(schedule -> new ChecklistDefinitionService.ChecklistScheduleInput(
						schedule.scheduleType(),
						schedule.startDate(),
						schedule.endDate(),
						schedule.dueTime(),
						schedule.weekdayMask(),
						schedule.dayOfMonth(),
						schedule.timezone(),
						schedule.active()
				))
				.toList();
	}

	private List<ChecklistDefinitionService.ChecklistScheduleInput> toUpdateChecklistScheduleInputs(
			List<UpdateChecklistDefinitionRequest.ChecklistScheduleRequest> schedules
	) {
		if (schedules == null) {
			return List.of();
		}

		return schedules.stream()
				.map(schedule -> new ChecklistDefinitionService.ChecklistScheduleInput(
						schedule.scheduleType(),
						schedule.startDate(),
						schedule.endDate(),
						schedule.dueTime(),
						schedule.weekdayMask(),
						schedule.dayOfMonth(),
						schedule.timezone(),
						schedule.active()
				))
				.toList();
	}
}
