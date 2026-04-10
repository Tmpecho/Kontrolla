package org.kontrolla.checklists.api;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.kontrolla.checklists.application.ChecklistDefinitionScheduleInput;
import org.kontrolla.checklists.application.ChecklistDefinitionService;
import org.kontrolla.checklists.application.ChecklistDefinitionTaskInput;
import org.kontrolla.checklists.application.CreateChecklistDefinitionCommand;
import org.kontrolla.checklists.application.UpdateChecklistDefinitionCommand;
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

/** REST endpoints for managing checklist definitions within an establishment. */
@RestController
@RequestMapping(
    "/api/v1/organizations/{organizationId}/establishments/{establishmentId}/checklists/definitions")
public class ChecklistDefinitionController {

  private final ChecklistDefinitionService checklistDefinitionService;

  /**
   * Creates a controller backed by the checklist definition service.
   *
   * @param checklistDefinitionService service used to manage checklist definitions
   */
  public ChecklistDefinitionController(ChecklistDefinitionService checklistDefinitionService) {
    this.checklistDefinitionService = checklistDefinitionService;
  }

  /**
   * Lists active checklist definitions for the requested service area.
   *
   * @param organizationId organization that owns the establishment
   * @param establishmentId establishment whose definitions are listed
   * @param serviceArea service area to filter by
   * @param currentUser authenticated user performing the request
   * @param pageable paging configuration
   * @return a page of checklist definitions
   */
  @GetMapping
  public PageResponse<ChecklistDefinitionResponse> listChecklistDefinitions(
      @PathVariable UUID organizationId,
      @PathVariable UUID establishmentId,
      @RequestParam ChecklistServiceArea serviceArea,
      @AuthenticationPrincipal CurrentUser currentUser,
      @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
    return PageResponse.from(
        checklistDefinitionService.listChecklistDefinitions(
            organizationId, establishmentId, serviceArea, currentUser, pageable),
        ChecklistDefinitionResponse::from);
  }

  /**
   * Retrieves a single checklist definition by identifier.
   *
   * @param organizationId organization that owns the establishment
   * @param establishmentId establishment that owns the definition
   * @param checklistDefinitionId identifier of the checklist definition to load
   * @param currentUser authenticated user performing the request
   * @return the requested checklist definition
   */
  @GetMapping("/{checklistDefinitionId}")
  public ChecklistDefinitionResponse getChecklistDefinition(
      @PathVariable UUID organizationId,
      @PathVariable UUID establishmentId,
      @PathVariable UUID checklistDefinitionId,
      @AuthenticationPrincipal CurrentUser currentUser) {
    return ChecklistDefinitionResponse.from(
        checklistDefinitionService.getChecklistDefinition(
            organizationId, establishmentId, checklistDefinitionId, currentUser));
  }

  /**
   * Creates a new checklist definition for the establishment.
   *
   * @param organizationId organization that owns the establishment
   * @param establishmentId establishment where the definition is created
   * @param currentUser authenticated user performing the request
   * @param request checklist definition payload
   * @return the created checklist definition
   */
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public ChecklistDefinitionResponse createChecklistDefinition(
      @PathVariable UUID organizationId,
      @PathVariable UUID establishmentId,
      @AuthenticationPrincipal CurrentUser currentUser,
      @Valid @RequestBody CreateChecklistDefinitionRequest request) {
    return ChecklistDefinitionResponse.from(
        checklistDefinitionService.createChecklistDefinition(
            organizationId,
            establishmentId,
            toCreateChecklistDefinitionCommand(request),
            currentUser));
  }

  /**
   * Creates a new version of an existing checklist definition.
   *
   * @param organizationId organization that owns the establishment
   * @param establishmentId establishment where the definition exists
   * @param checklistDefinitionId identifier of the definition to update
   * @param currentUser authenticated user performing the request
   * @param request updated checklist definition payload
   * @return the updated checklist definition version
   */
  @PutMapping("/{checklistDefinitionId}")
  public ChecklistDefinitionResponse updateChecklistDefinition(
      @PathVariable UUID organizationId,
      @PathVariable UUID establishmentId,
      @PathVariable UUID checklistDefinitionId,
      @AuthenticationPrincipal CurrentUser currentUser,
      @Valid @RequestBody UpdateChecklistDefinitionRequest request) {
    return ChecklistDefinitionResponse.from(
        checklistDefinitionService.updateChecklistDefinition(
            organizationId,
            establishmentId,
            checklistDefinitionId,
            toUpdateChecklistDefinitionCommand(request),
            currentUser));
  }

  private CreateChecklistDefinitionCommand toCreateChecklistDefinitionCommand(
      CreateChecklistDefinitionRequest request) {
    return new CreateChecklistDefinitionCommand(
        request.serviceArea(),
        request.title(),
        request.description(),
        toCreateChecklistTaskInputs(request.tasks()),
        toCreateChecklistScheduleInputs(request.schedules()));
  }

  private UpdateChecklistDefinitionCommand toUpdateChecklistDefinitionCommand(
      UpdateChecklistDefinitionRequest request) {
    return new UpdateChecklistDefinitionCommand(
        request.serviceArea(),
        request.title(),
        request.description(),
        request.status(),
        toUpdateChecklistTaskInputs(request.tasks()),
        toUpdateChecklistScheduleInputs(request.schedules()));
  }

  private List<ChecklistDefinitionTaskInput> toCreateChecklistTaskInputs(
      List<CreateChecklistDefinitionRequest.ChecklistTaskRequest> tasks) {
    return tasks.stream()
        .map(
            task ->
                new ChecklistDefinitionTaskInput(
                    task.title(),
                    task.details(),
                    task.taskKind(),
                    task.required(),
                    task.sortOrder(),
                    task.measurementUnit(),
                    task.minimumAllowedValue(),
                    task.maximumAllowedValue()))
        .toList();
  }

  private List<ChecklistDefinitionTaskInput> toUpdateChecklistTaskInputs(
      List<UpdateChecklistDefinitionRequest.ChecklistTaskRequest> tasks) {
    return tasks.stream()
        .map(
            task ->
                new ChecklistDefinitionTaskInput(
                    task.title(),
                    task.details(),
                    task.taskKind(),
                    task.required(),
                    task.sortOrder(),
                    task.measurementUnit(),
                    task.minimumAllowedValue(),
                    task.maximumAllowedValue()))
        .toList();
  }

  private List<ChecklistDefinitionScheduleInput> toCreateChecklistScheduleInputs(
      List<CreateChecklistDefinitionRequest.ChecklistScheduleRequest> schedules) {
    if (schedules == null) {
      return List.of();
    }

    return schedules.stream()
        .map(
            schedule ->
                new ChecklistDefinitionScheduleInput(
                    schedule.scheduleType(),
                    schedule.startDate(),
                    schedule.endDate(),
                    schedule.dueTime(),
                    schedule.weekdayMask(),
                    schedule.dayOfMonth(),
                    schedule.timezone(),
                    schedule.active()))
        .toList();
  }

  private List<ChecklistDefinitionScheduleInput> toUpdateChecklistScheduleInputs(
      List<UpdateChecklistDefinitionRequest.ChecklistScheduleRequest> schedules) {
    if (schedules == null) {
      return List.of();
    }

    return schedules.stream()
        .map(
            schedule ->
                new ChecklistDefinitionScheduleInput(
                    schedule.scheduleType(),
                    schedule.startDate(),
                    schedule.endDate(),
                    schedule.dueTime(),
                    schedule.weekdayMask(),
                    schedule.dayOfMonth(),
                    schedule.timezone(),
                    schedule.active()))
        .toList();
  }
}
