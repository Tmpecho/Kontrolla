package org.kontrolla.iam.infrastructure;

import org.kontrolla.iam.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for user persistence and lookup by email and active status.
 */
public interface UserRepository extends JpaRepository<User, UUID> {

	/**
	 * Finds a user by case-insensitive email.
	 *
	 * @param email the user email
	 * @return the matching user, if present
	 */
	Optional<User> findByEmailIgnoreCase(String email);

	/**
	 * Returns the earliest-created active user.
	 *
	 * @return the first active user, if present
	 */
	Optional<User> findFirstByActiveTrueOrderByCreatedAtAsc();
}
