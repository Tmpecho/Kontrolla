package org.kontrolla.iam.domain;

import jakarta.persistence.*;
import lombok.Getter;
import org.kontrolla.common.persistence.AbstractAuditableUuidEntity;

import java.time.Instant;

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

	@Column
	private Instant revokedAt;

	protected RefreshToken() {
	}

	public RefreshToken(User user, String tokenHash, Instant expiresAt) {
		this.user = user;
		this.tokenHash = tokenHash;
		this.expiresAt = expiresAt;
	}

	public boolean isActiveAt(Instant instant) {
		return revokedAt == null && expiresAt.isAfter(instant);
	}

	public void revoke(Instant instant) {
		this.revokedAt = instant;
	}
}
