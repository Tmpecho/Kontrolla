package org.kontrolla.checklists.application;

import org.kontrolla.checklists.domain.ChecklistTaskKind;

import java.math.BigDecimal;

public record ChecklistDefinitionTaskInput(
		String title,
		String details,
		ChecklistTaskKind taskKind,
		boolean required,
		int sortOrder,
		String measurementUnit,
		BigDecimal minimumAllowedValue,
		BigDecimal maximumAllowedValue
) {
}
