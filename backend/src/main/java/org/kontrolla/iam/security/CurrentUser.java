package org.kontrolla.iam.security;

import org.kontrolla.iam.domain.GlobalRole;

import java.security.Principal;
import java.util.Set;
import java.util.UUID;

public record CurrentUser(UUID userId, String email, Set<GlobalRole> globalRoles) implements Principal {

	public CurrentUser {
		globalRoles = Set.copyOf(globalRoles);
	}

	public boolean hasGlobalRole(GlobalRole role) {
		return globalRoles.contains(role);
	}

	public boolean isPlatformAdmin() {
		return hasGlobalRole(GlobalRole.PLATFORM_ADMIN);
	}

	@Override
	public String getName() {
		return email;
	}
}
