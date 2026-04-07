package org.kontrolla.documents.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.kontrolla.documents.domain.DocumentServiceArea;

import java.time.LocalDate;

public record UpdateDocumentRequest(
		@NotNull DocumentServiceArea serviceArea,
		@NotBlank @Size(max = 255) String title,
		@NotBlank @Size(max = 255) String holderName,
		@NotNull LocalDate issueDate,
		@NotNull LocalDate renewalDate
) {
}
