package org.kontrolla.iam.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.kontrolla.common.persistence.AbstractAuditableUuidEntity;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

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

	public boolean hasGlobalRole(GlobalRole role) {
		return globalRoles.contains(role);
	}

	public void setGlobalRoles(Set<GlobalRole> globalRoles) {
		this.globalRoles.clear();
		this.globalRoles.addAll(globalRoles);
	}
}
