package org.kontrolla.documents.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.kontrolla.documents.domain.DocumentServiceArea;

/**
 * Request payload for updating document metadata.
 *
 * @param serviceArea the document service area
 * @param title the document title
 * @param holderName the document holder name
 * @param issueDate the issue date
 * @param renewalDate the renewal date
 * @param auditUserIds the users assigned to acknowledge the document
 */
public record UpdateDocumentRequest(
    @NotNull DocumentServiceArea serviceArea,
    @NotBlank @Size(max = 255) String title,
    @NotBlank @Size(max = 255) String holderName,
    @NotNull LocalDate issueDate,
    @NotNull LocalDate renewalDate,
    List<UUID> auditUserIds) {}
