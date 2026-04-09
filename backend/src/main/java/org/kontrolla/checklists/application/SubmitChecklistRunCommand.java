package org.kontrolla.checklists.application;

import java.util.List;

public record SubmitChecklistRunCommand(
		List<ChecklistRunTaskExecutionInput> tasks
) {
}
