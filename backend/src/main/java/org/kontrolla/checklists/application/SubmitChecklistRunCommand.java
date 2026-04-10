package org.kontrolla.checklists.application;

import java.util.List;

/**
 * Command for submitting a checklist run with task execution updates.
 *
 * @param tasks the submitted task execution updates
 */
public record SubmitChecklistRunCommand(List<ChecklistRunTaskExecutionInput> tasks) {}
