package org.kontrolla.iam.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.kontrolla.common.persistence.AbstractAuditableUuidEntity;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Persisted application user with profile, password hash, and global roles.
 */
@Getter
@Entity
@Table(name = "users")
public class User extends AbstractAuditableUuidEntity {

	@Column(nullable = false, unique = true)
	private String email;

	@Setter
	@Column(nullable = false)
	private String firstName;

	@Setter
	@Column(nullable = false)
	private String lastName;

	@Setter
	@Column(nullable = false)
	private String passwordHash;

	@Setter
	@Column(nullable = false)
	private boolean active;

	@ElementCollection(fetch = FetchType.EAGER)
	@CollectionTable(name = "user_global_roles", joinColumns = @JoinColumn(name = "user_id"))
	@Column(name = "role", nullable = false, length = 64)
	@Enumerated(EnumType.STRING)
	private Set<GlobalRole> globalRoles = new HashSet<>();

	protected User() {
	}

	/**
	 * Creates a user.
	 *
	 * @param email the user email
	 * @param firstName the user first name
	 * @param lastName the user last name
	 * @param passwordHash the encoded password hash
	 * @param active whether the user is active
	 * @param globalRoles the user's global roles
	 */
	public User(String email, String firstName, String lastName, String passwordHash, boolean active, Set<GlobalRole> globalRoles) {
		this.email = normalizeEmail(email);
		this.firstName = firstName;
		this.lastName = lastName;
		this.passwordHash = passwordHash;
		this.active = active;
		this.globalRoles = new HashSet<>(globalRoles);
	}

	private static String normalizeEmail(String email) {
		return email.trim().toLowerCase(Locale.ROOT);
	}

	/**
	 * Indicates whether the user has a specific global role.
	 *
	 * @param role the role to check
	 * @return {@code true} when the role is assigned
	 */
	public boolean hasGlobalRole(GlobalRole role) {
		return globalRoles.contains(role);
	}

	/**
	 * Replaces the user's global roles.
	 *
	 * @param globalRoles the replacement global roles
	 */
	public void setGlobalRoles(Set<GlobalRole> globalRoles) {
		this.globalRoles.clear();
		this.globalRoles.addAll(globalRoles);
	}
}
