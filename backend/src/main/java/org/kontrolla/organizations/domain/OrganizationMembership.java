package org.kontrolla.organizations.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.kontrolla.common.persistence.AbstractAuditableUuidEntity;
import org.kontrolla.iam.domain.User;

@Getter
@Entity
@Table(name = "organization_memberships")
public class OrganizationMembership extends AbstractAuditableUuidEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "organization_id", nullable = false)
	private Organization organization;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Setter
	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 32)
	private OrganizationRole role;

	@Setter
	@Column(nullable = false)
	private boolean active;

	protected OrganizationMembership() {
	}

	public OrganizationMembership(Organization organization, User user, OrganizationRole role, boolean active) {
		this.organization = organization;
		this.user = user;
		this.role = role;
		this.active = active;
	}
}
