package org.kontrolla.checklists.api;

import jakarta.validation.Valid;
import org.kontrolla.checklists.application.ChecklistRunTaskExecutionInput;
import org.kontrolla.checklists.application.ChecklistRunService;
import org.kontrolla.checklists.application.SubmitChecklistRunCommand;
import org.kontrolla.checklists.application.UpdateChecklistTaskCommand;
import org.kontrolla.checklists.domain.ChecklistRunStatus;
import org.kontrolla.checklists.domain.ChecklistServiceArea;
import org.kontrolla.common.api.PageResponse;
import org.kontrolla.iam.security.CurrentUser;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PutMapping;
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

/**
 * REST endpoints for listing and operating on checklist runs.
 */
@RestController
@RequestMapping("/api/v1/organizations/{organizationId}/establishments/{establishmentId}/checklists/runs")
public class ChecklistRunController {

	private final ChecklistRunService checklistRunService;

	/**
	 * Creates a controller backed by the checklist run service.
	 *
	 * @param checklistRunService service used to manage checklist runs
	 */
	public ChecklistRunController(ChecklistRunService checklistRunService) {
		this.checklistRunService = checklistRunService;
	}

	/**
	 * Lists checklist runs for the requested filters.
	 *
	 * @param organizationId organization that owns the establishment
	 * @param establishmentId establishment whose checklist runs are listed
	 * @param serviceArea service area to filter by
	 * @param statuses optional run statuses to include
	 * @param assignedUserId optional assigned user filter
	 * @param assignedToMe whether to filter to the current user
	 * @param dueFrom optional lower due date bound
	 * @param dueTo optional upper due date bound
	 * @param currentUser authenticated user performing the request
	 * @param pageable paging configuration
	 * @return a page of checklist runs
	 */
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

	/**
	 * Retrieves a single checklist run by identifier.
	 *
	 * @param organizationId organization that owns the establishment
	 * @param establishmentId establishment that owns the run
	 * @param checklistRunId identifier of the checklist run to load
	 * @param currentUser authenticated user performing the request
	 * @return the requested checklist run
	 */
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

	/**
	 * Assigns users to a checklist run.
	 *
	 * @param organizationId organization that owns the establishment
	 * @param establishmentId establishment that owns the run
	 * @param checklistRunId identifier of the run to update
	 * @param currentUser authenticated user performing the request
	 * @param request assignment payload
	 * @return the updated checklist run
	 */
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

	/**
	 * Removes a checklist run assignment.
	 *
	 * @param organizationId organization that owns the establishment
	 * @param establishmentId establishment that owns the run
	 * @param checklistRunId identifier of the run
	 * @param assignmentId identifier of the assignment to remove
	 * @param currentUser authenticated user performing the request
	 */
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

	/**
	 * Marks a checklist run as started.
	 *
	 * @param organizationId organization that owns the establishment
	 * @param establishmentId establishment that owns the run
	 * @param checklistRunId identifier of the run to start
	 * @param currentUser authenticated user performing the request
	 * @return the updated checklist run
	 */
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

	/**
	 * Submits execution results for a checklist run.
	 *
	 * @param organizationId organization that owns the establishment
	 * @param establishmentId establishment that owns the run
	 * @param checklistRunId identifier of the run to submit
	 * @param currentUser authenticated user performing the request
	 * @param request task execution payload
	 * @return the completed checklist run
	 */
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
						toSubmitChecklistRunCommand(request),
						currentUser
				)
		);
	}

	/**
	 * Updates the execution state of a single checklist task.
	 *
	 * @param organizationId organization that owns the establishment
	 * @param establishmentId establishment that owns the run
	 * @param checklistRunId identifier of the run to update
	 * @param taskId identifier of the task execution to update
	 * @param currentUser authenticated user performing the request
	 * @param request task update payload
	 * @return the updated checklist run
	 */
	@PutMapping("/{checklistRunId}/tasks/{taskId}")
	public ChecklistRunResponse updateChecklistTask(
			@PathVariable UUID organizationId,
			@PathVariable UUID establishmentId,
			@PathVariable UUID checklistRunId,
			@PathVariable UUID taskId,
			@AuthenticationPrincipal CurrentUser currentUser,
			@Valid @RequestBody UpdateChecklistTaskRequest request
	) {
		return ChecklistRunResponse.from(
				checklistRunService.updateChecklistTask(
						organizationId,
						establishmentId,
						checklistRunId,
						taskId,
						toUpdateChecklistTaskCommand(request),
						currentUser
				)
		);
	}

	/**
	 * Reopens a previously completed or cancelled checklist run.
	 *
	 * @param organizationId organization that owns the establishment
	 * @param establishmentId establishment that owns the run
	 * @param checklistRunId identifier of the run to reopen
	 * @param currentUser authenticated user performing the request
	 * @return the updated checklist run
	 */
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

	/**
	 * Cancels a checklist run.
	 *
	 * @param organizationId organization that owns the establishment
	 * @param establishmentId establishment that owns the run
	 * @param checklistRunId identifier of the run to cancel
	 * @param currentUser authenticated user performing the request
	 * @return the updated checklist run
	 */
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

	/**
	 * Resets a checklist run back to its initial state.
	 *
	 * @param organizationId organization that owns the establishment
	 * @param establishmentId establishment that owns the run
	 * @param checklistRunId identifier of the run to reset
	 * @param currentUser authenticated user performing the request
	 * @return the reset checklist run
	 */
	@PostMapping("/{checklistRunId}/reset")
	public ChecklistRunResponse resetChecklistRun(
			@PathVariable UUID organizationId,
			@PathVariable UUID establishmentId,
			@PathVariable UUID checklistRunId,
			@AuthenticationPrincipal CurrentUser currentUser
	) {
		return ChecklistRunResponse.from(
				checklistRunService.resetChecklistRun(
						organizationId,
						establishmentId,
						checklistRunId,
						currentUser
				)
		);
	}

	private SubmitChecklistRunCommand toSubmitChecklistRunCommand(SubmitChecklistRunRequest request) {
		return new SubmitChecklistRunCommand(toChecklistTaskExecutionInputs(request.tasks()));
	}

	private UpdateChecklistTaskCommand toUpdateChecklistTaskCommand(UpdateChecklistTaskRequest request) {
		return new UpdateChecklistTaskCommand(
				request.executionStatus(),
				request.comment(),
				request.verificationResult(),
				request.measuredValue(),
				request.enteredText()
		);
	}

	private List<ChecklistRunTaskExecutionInput> toChecklistTaskExecutionInputs(
			List<SubmitChecklistRunRequest.ChecklistTaskExecutionRequest> tasks
	) {
		return tasks.stream()
				.map(task -> new ChecklistRunTaskExecutionInput(
						task.checklistTaskExecutionId(),
						task.executionStatus(),
						task.comment(),
						task.verificationResult(),
						task.measuredValue(),
						task.enteredText()
				))
				.toList();
	}
}
