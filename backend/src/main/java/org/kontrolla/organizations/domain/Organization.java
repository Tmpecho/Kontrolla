package org.kontrolla.organizations.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.kontrolla.common.persistence.AbstractAuditableUuidEntity;

@Setter
@Getter
@Entity
@Table(name = "organizations")
public class Organization extends AbstractAuditableUuidEntity {

	@Column(nullable = false)
	private String name;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 32)
	private OrganizationStatus status;

	protected Organization() {
	}

	public Organization(String name, OrganizationStatus status) {
		this.name = name;
		this.status = status;
	}

}
