package org.kontrolla.iam.application;

import org.kontrolla.common.exception.ResourceNotFoundException;
import org.kontrolla.common.exception.UnauthorizedException;
import org.kontrolla.iam.domain.User;
import org.kontrolla.iam.infrastructure.UserRepository;
import org.kontrolla.iam.security.CurrentUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Resolves users from ids and the current authenticated principal.
 */
@Service
public class UserAccessService {

	private final UserRepository userRepository;

	/**
	 * Creates the user access service.
	 *
	 * @param userRepository repository for user lookup
	 */
	public UserAccessService(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	/**
	 * Returns a user by id or throws when it does not exist.
	 *
	 * @param userId the user identifier
	 * @return the requested user
	 */
	@Transactional(readOnly = true)
	public User getUserOrThrow(UUID userId) {
		return userRepository.findById(userId)
				.orElseThrow(() -> new ResourceNotFoundException("user_not_found", "User not found"));
	}

	/**
	 * Returns the currently authenticated active user.
	 *
	 * @param currentUser the authenticated principal
	 * @return the resolved user
	 */
	@Transactional(readOnly = true)
	public User getCurrentUserOrThrow(CurrentUser currentUser) {
		return userRepository.findById(currentUser.userId())
				.filter(User::isActive)
				.orElseThrow(() -> new UnauthorizedException("user_not_found", "Authenticated user no longer exists"));
	}
}
