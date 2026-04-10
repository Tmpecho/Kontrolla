package org.kontrolla.organizations.api;

import org.kontrolla.establishments.domain.Establishment;

import java.util.UUID;

/**
 * API response describing an establishment in a membership scope.
 *
 * @param id the establishment identifier
 * @param name the establishment name
 */
public record MembershipEstablishmentResponse(
		UUID id,
		String name
) {

	/**
	 * Maps an establishment entity to the membership establishment response.
	 *
	 * @param establishment the establishment to map
	 * @return the mapped response
	 */
	public static MembershipEstablishmentResponse from(Establishment establishment) {
		return new MembershipEstablishmentResponse(establishment.getId(), establishment.getName());
	}
}
