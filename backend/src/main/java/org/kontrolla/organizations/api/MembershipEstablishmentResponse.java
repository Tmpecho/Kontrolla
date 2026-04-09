package org.kontrolla.organizations.api;

import org.kontrolla.establishments.domain.Establishment;

import java.util.UUID;

public record MembershipEstablishmentResponse(
		UUID id,
		String name
) {

	public static MembershipEstablishmentResponse from(Establishment establishment) {
		return new MembershipEstablishmentResponse(establishment.getId(), establishment.getName());
	}
}
