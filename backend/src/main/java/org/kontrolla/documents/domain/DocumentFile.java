package org.kontrolla.documents.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Persisted binary file content for a document. */
@Getter
@Entity
@Table(name = "document_files")
public class DocumentFile {

  @Id
  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(name = "document_id", nullable = false, updatable = false, length = 36)
  private UUID documentId;

  @Lob
  @Column(nullable = false, columnDefinition = "LONGBLOB")
  private byte[] content;

  protected DocumentFile() {}

  /**
   * Creates stored file content for a document.
   *
   * @param documentId the document identifier
   * @param content the binary file content
   */
  public DocumentFile(UUID documentId, byte[] content) {
    this.documentId = documentId;
    this.content = content;
  }

  /**
   * Replaces the stored file content.
   *
   * @param content the replacement file content
   */
  public void replaceContent(byte[] content) {
    this.content = content;
  }
}
