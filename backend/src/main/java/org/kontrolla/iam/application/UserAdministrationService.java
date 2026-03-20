package org.kontrolla.iam.application;

import org.kontrolla.common.exception.ConflictException;
import org.kontrolla.iam.domain.GlobalRole;
import org.kontrolla.iam.domain.User;
import org.kontrolla.iam.infrastructure.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
public class UserAdministrationService {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;

	public UserAdministrationService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
	}

	@Transactional
	public User createUser(
			String email,
			String firstName,
			String lastName,
			String password,
			boolean active,
			Set<GlobalRole> globalRoles
	) {
		if (userRepository.findByEmailIgnoreCase(email).isPresent()) {
			throw new ConflictException("user_already_exists", "A user with that email already exists");
		}

		User user = new User(email, firstName, lastName, passwordEncoder.encode(password), active, globalRoles);
		return userRepository.save(user);
	}

	@Transactional(readOnly = true)
	public Page<User> listUsers(Pageable pageable) {
		return userRepository.findAll(pageable);
	}
}
