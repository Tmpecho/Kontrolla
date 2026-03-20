package org.kontrolla.establishments.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.kontrolla.common.persistence.AbstractAuditableUuidEntity;
import org.kontrolla.organizations.domain.Organization;

@Getter
@Entity
@Table(name = "establishments")
public class Establishment extends AbstractAuditableUuidEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "organization_id", nullable = false)
	private Organization organization;

	@Setter
	@Column(nullable = false)
	private String name;

	@Setter
	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 32)
	private EstablishmentType type;

	@Setter
	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 32)
	private EstablishmentStatus status;

	protected Establishment() {
	}

	public Establishment(Organization organization, String name, EstablishmentType type, EstablishmentStatus status) {
		this.organization = organization;
		this.name = name;
		this.type = type;
		this.status = status;
	}
}
