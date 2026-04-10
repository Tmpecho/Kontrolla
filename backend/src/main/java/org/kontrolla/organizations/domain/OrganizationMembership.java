package org.kontrolla.organizations.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.kontrolla.common.persistence.AbstractAuditableUuidEntity;
import org.kontrolla.establishments.domain.Establishment;
import org.kontrolla.iam.domain.User;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Persisted membership linking a user to an organization and optional
 * establishment scope.
 */
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

	@Setter
	@Column(name = "access_all_establishments", nullable = false)
	private boolean accessAllEstablishments;

	@ManyToMany(fetch = FetchType.LAZY)
	@JoinTable(
			name = "organization_membership_establishments",
			joinColumns = @JoinColumn(name = "membership_id"),
			inverseJoinColumns = @JoinColumn(name = "establishment_id")
	)
	private final List<Establishment> accessibleEstablishments = new ArrayList<>();

	protected OrganizationMembership() {
	}

	/**
	 * Creates a membership with organization-wide establishment access.
	 *
	 * @param organization the organization
	 * @param user the member user
	 * @param role the organization role
	 * @param active whether the membership is active
	 */
	public OrganizationMembership(Organization organization, User user, OrganizationRole role, boolean active) {
		this(organization, user, role, active, true);
	}

	/**
	 * Creates a membership with explicit establishment access configuration.
	 *
	 * @param organization the organization
	 * @param user the member user
	 * @param role the organization role
	 * @param active whether the membership is active
	 * @param accessAllEstablishments whether all-establishment access is granted
	 */
	public OrganizationMembership(
			Organization organization,
			User user,
			OrganizationRole role,
			boolean active,
			boolean accessAllEstablishments
	) {
		this.organization = organization;
		this.user = user;
		this.role = role;
		this.active = active;
		this.accessAllEstablishments = accessAllEstablishments;
	}

	/**
	 * Replaces the explicitly assigned establishments for the membership.
	 *
	 * @param establishments the establishments to assign
	 */
	public void replaceAccessibleEstablishments(Collection<Establishment> establishments) {
		accessibleEstablishments.clear();
		accessibleEstablishments.addAll(establishments);
	}

	/**
	 * Indicates whether the membership grants access to a specific establishment.
	 *
	 * @param establishmentId the establishment identifier
	 * @return {@code true} when access is granted
	 */
	public boolean hasEstablishmentAccess(UUID establishmentId) {
		if (role == OrganizationRole.ORG_OWNER || role == OrganizationRole.ORG_ADMIN || accessAllEstablishments) {
			return true;
		}

		return accessibleEstablishments.stream()
				.anyMatch(establishment -> establishment.getId().equals(establishmentId));
	}
}
