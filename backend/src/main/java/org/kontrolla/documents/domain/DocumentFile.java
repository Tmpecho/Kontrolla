package org.kontrolla.documents.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.Getter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.UUID;

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

  protected DocumentFile() {
  }

  public DocumentFile(UUID documentId, byte[] content) {
    this.documentId = documentId;
    this.content = content;
  }

  public void replaceContent(byte[] content) {
    this.content = content;
  }
}
