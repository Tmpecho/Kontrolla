package org.kontrolla.iam.domain;

import jakarta.persistence.*;
import lombok.Getter;
import org.kontrolla.common.persistence.AbstractAuditableUuidEntity;
import org.kontrolla.organizations.domain.Organization;

import java.time.Instant;

@Getter
@Entity
@Table(name = "user_invites")
public class UserInvite extends AbstractAuditableUuidEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "organization_id", nullable = false)
	private Organization organization;

	@Column(name = "token_hash", nullable = false, unique = true, length = 64)
	private String tokenHash;

	@Column(name = "expires_at", nullable = false)
	private Instant expiresAt;

	@Column(name = "accepted_at")
	private Instant acceptedAt;

	protected UserInvite() {
	}

	public UserInvite(User user, Organization organization, String tokenHash, Instant expiresAt) {
		this.user = user;
		this.organization = organization;
		this.tokenHash = tokenHash;
		this.expiresAt = expiresAt;
	}

	public boolean isActiveAt(Instant instant) {
		return acceptedAt == null && expiresAt.isAfter(instant);
	}

	public void accept(Instant instant) {
		this.acceptedAt = instant;
	}
}
