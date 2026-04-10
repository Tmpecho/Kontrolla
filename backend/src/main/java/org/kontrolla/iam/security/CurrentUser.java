package org.kontrolla.iam.security;

import org.kontrolla.iam.domain.GlobalRole;

import java.security.Principal;
import java.util.Set;
import java.util.UUID;

/**
 * Authenticated principal extracted from a validated JWT.
 *
 * @param userId the authenticated user identifier
 * @param email the authenticated user email
 * @param globalRoles the authenticated user's global roles
 */
public record CurrentUser(UUID userId, String email, Set<GlobalRole> globalRoles) implements Principal {

	public CurrentUser {
		globalRoles = Set.copyOf(globalRoles);
	}

	/**
	 * Indicates whether the current user has a specific global role.
	 *
	 * @param role the role to check
	 * @return {@code true} when the role is assigned
	 */
	public boolean hasGlobalRole(GlobalRole role) {
		return globalRoles.contains(role);
	}

	/**
	 * Indicates whether the current user is a platform administrator.
	 *
	 * @return {@code true} when the user has the platform admin role
	 */
	public boolean isPlatformAdmin() {
		return hasGlobalRole(GlobalRole.PLATFORM_ADMIN);
	}

	/**
	 * Returns the principal name used by the security framework.
	 *
	 * @return the authenticated email address
	 */
	@Override
	public String getName() {
		return email;
	}
}
