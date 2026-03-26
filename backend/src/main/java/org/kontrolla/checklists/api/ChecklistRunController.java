package org.kontrolla.checklists.api;

import jakarta.validation.Valid;
import org.kontrolla.checklists.application.ChecklistRunService;
import org.kontrolla.checklists.domain.ChecklistRunStatus;
import org.kontrolla.checklists.domain.ChecklistServiceArea;
import org.kontrolla.common.api.PageResponse;
import org.kontrolla.iam.security.CurrentUser;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/organizations/{organizationId}/establishments/{establishmentId}/checklists/runs")
public class ChecklistRunController {

	private final ChecklistRunService checklistRunService;

	public ChecklistRunController(ChecklistRunService checklistRunService) {
		this.checklistRunService = checklistRunService;
	}

	@GetMapping
	public PageResponse<ChecklistRunResponse> listChecklistRuns(
			@PathVariable UUID organizationId,
			@PathVariable UUID establishmentId,
			@RequestParam ChecklistServiceArea serviceArea,
			@RequestParam(required = false) List<ChecklistRunStatus> statuses,
			@RequestParam(required = false) UUID assignedUserId,
			@RequestParam(defaultValue = "false") boolean assignedToMe,
			@RequestParam(required = false) Instant dueFrom,
			@RequestParam(required = false) Instant dueTo,
			@AuthenticationPrincipal CurrentUser currentUser,
			@PageableDefault(size = 20, sort = "dueAt") Pageable pageable
	) {
		return PageResponse.from(
				checklistRunService.listChecklistRuns(
						organizationId,
						establishmentId,
						serviceArea,
						statuses,
						assignedUserId,
						assignedToMe,
						dueFrom,
						dueTo,
						currentUser,
						pageable
				),
				ChecklistRunResponse::from
		);
	}

	@GetMapping("/{checklistRunId}")
	public ChecklistRunResponse getChecklistRun(
			@PathVariable UUID organizationId,
			@PathVariable UUID establishmentId,
			@PathVariable UUID checklistRunId,
			@AuthenticationPrincipal CurrentUser currentUser
	) {
		return ChecklistRunResponse.from(
				checklistRunService.getChecklistRun(
						organizationId,
						establishmentId,
						checklistRunId,
						currentUser
				)
		);
	}

	@PostMapping("/{checklistRunId}/assignments")
	public ChecklistRunResponse assignChecklistRun(
			@PathVariable UUID organizationId,
			@PathVariable UUID establishmentId,
			@PathVariable UUID checklistRunId,
			@AuthenticationPrincipal CurrentUser currentUser,
			@Valid @RequestBody AssignChecklistRunRequest request
	) {
		return ChecklistRunResponse.from(
				checklistRunService.assignChecklistRun(
						organizationId,
						establishmentId,
						checklistRunId,
						request.assignedUserIds(),
						currentUser
				)
		);
	}

	@DeleteMapping("/{checklistRunId}/assignments/{assignmentId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void removeChecklistRunAssignment(
			@PathVariable UUID organizationId,
			@PathVariable UUID establishmentId,
			@PathVariable UUID checklistRunId,
			@PathVariable UUID assignmentId,
			@AuthenticationPrincipal CurrentUser currentUser
	) {
		checklistRunService.removeChecklistRunAssignment(
				organizationId,
				establishmentId,
				checklistRunId,
				assignmentId,
				currentUser
		);
	}

	@PostMapping("/{checklistRunId}/start")
	public ChecklistRunResponse startChecklistRun(
			@PathVariable UUID organizationId,
			@PathVariable UUID establishmentId,
			@PathVariable UUID checklistRunId,
			@AuthenticationPrincipal CurrentUser currentUser
	) {
		return ChecklistRunResponse.from(
				checklistRunService.startChecklistRun(
						organizationId,
						establishmentId,
						checklistRunId,
						currentUser
				)
		);
	}

	@PostMapping("/{checklistRunId}/submit")
	public ChecklistRunResponse submitChecklistRun(
			@PathVariable UUID organizationId,
			@PathVariable UUID establishmentId,
			@PathVariable UUID checklistRunId,
			@AuthenticationPrincipal CurrentUser currentUser,
			@Valid @RequestBody SubmitChecklistRunRequest request
	) {
		return ChecklistRunResponse.from(
				checklistRunService.submitChecklistRun(
						organizationId,
						establishmentId,
						checklistRunId,
						toChecklistItemSubmissionInputs(request.responses()),
						currentUser
				)
		);
	}

	@PostMapping("/{checklistRunId}/reopen")
	public ChecklistRunResponse reopenChecklistRun(
			@PathVariable UUID organizationId,
			@PathVariable UUID establishmentId,
			@PathVariable UUID checklistRunId,
			@AuthenticationPrincipal CurrentUser currentUser
	) {
		return ChecklistRunResponse.from(
				checklistRunService.reopenChecklistRun(
						organizationId,
						establishmentId,
						checklistRunId,
						currentUser
				)
		);
	}

	@PostMapping("/{checklistRunId}/cancel")
	public ChecklistRunResponse cancelChecklistRun(
			@PathVariable UUID organizationId,
			@PathVariable UUID establishmentId,
			@PathVariable UUID checklistRunId,
			@AuthenticationPrincipal CurrentUser currentUser
	) {
		return ChecklistRunResponse.from(
				checklistRunService.cancelChecklistRun(
						organizationId,
						establishmentId,
						checklistRunId,
						currentUser
				)
		);
	}

	private List<ChecklistRunService.ChecklistItemSubmissionInput> toChecklistItemSubmissionInputs(
			List<SubmitChecklistRunRequest.ChecklistItemSubmissionRequest> responses
	) {
		return responses.stream()
				.map(response -> new ChecklistRunService.ChecklistItemSubmissionInput(
						response.checklistRunItemId(),
						response.booleanValue(),
						response.textValue(),
						response.numberValue(),
						response.note()
				))
				.toList();
	}
}
