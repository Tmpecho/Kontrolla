package org.kontrolla.iam.domain;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import org.kontrolla.common.persistence.AbstractAuditableUuidEntity;

/** Persisted refresh token for maintaining authenticated sessions. */
@Getter
@Entity
@Table(name = "refresh_tokens")
public class RefreshToken extends AbstractAuditableUuidEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Column(nullable = false, unique = true, length = 128)
  private String tokenHash;

  @Column(nullable = false)
  private Instant expiresAt;

  @Column private Instant revokedAt;

  protected RefreshToken() {}

  /**
   * Creates a refresh token.
   *
   * @param user the token owner
   * @param tokenHash the hashed refresh token
   * @param expiresAt when the token expires
   */
  public RefreshToken(User user, String tokenHash, Instant expiresAt) {
    this.user = user;
    this.tokenHash = tokenHash;
    this.expiresAt = expiresAt;
  }

  /**
   * Indicates whether the refresh token is active at a given instant.
   *
   * @param instant the instant to evaluate
   * @return {@code true} when the token is unrevoked and unexpired
   */
  public boolean isActiveAt(Instant instant) {
    return revokedAt == null && expiresAt.isAfter(instant);
  }

  /**
   * Revokes the refresh token.
   *
   * @param instant the revocation timestamp
   */
  public void revoke(Instant instant) {
    this.revokedAt = instant;
  }
}
