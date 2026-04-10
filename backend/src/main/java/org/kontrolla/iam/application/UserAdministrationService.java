package org.kontrolla.iam.application;

import org.kontrolla.audit.application.AuditRecord;
import org.kontrolla.audit.application.AuditRecorder;
import org.kontrolla.audit.domain.AuditAction;
import org.kontrolla.audit.domain.AuditOutcome;
import org.kontrolla.audit.domain.AuditTargetType;
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

/**
 * Handles administrative user creation and listing.
 */
@Service
public class UserAdministrationService {

	private final UserRepository userRepository;
	private final AuditRecorder auditRecorder;
	private final PasswordEncoder passwordEncoder;

	/**
	 * Creates the user administration service.
	 *
	 * @param userRepository repository for user persistence
	 * @param auditRecorder recorder for user-creation audit events
	 * @param passwordEncoder encoder for stored passwords
	 */
	public UserAdministrationService(
			UserRepository userRepository,
			AuditRecorder auditRecorder,
			PasswordEncoder passwordEncoder
	) {
		this.userRepository = userRepository;
		this.auditRecorder = auditRecorder;
		this.passwordEncoder = passwordEncoder;
	}

	/**
	 * Creates a user directly with administrator-provided credentials and roles.
	 *
	 * @param email the user email
	 * @param firstName the user first name
	 * @param lastName the user last name
	 * @param password the raw password
	 * @param active whether the user should be active
	 * @param globalRoles the user's global roles
	 * @return the created user
	 */
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
		user = userRepository.save(user);
		auditRecorder.record(userCreateAudit(user, "admin_user_created", "admin"));
		return user;
	}

	/**
	 * Creates an invited user with a placeholder password and no global roles.
	 *
	 * @param email the user email
	 * @param firstName the user first name
	 * @param lastName the user last name
	 * @return the created invited user
	 */
	@Transactional
	public User createInvitedUser(
			String email,
			String firstName,
			String lastName
	) {
		if (userRepository.findByEmailIgnoreCase(email).isPresent()) {
			throw new ConflictException("user_already_exists", "A user with that email already exists");
		}

		String placeholderPassword = java.util.UUID.randomUUID().toString();
		User user = new User(email, firstName, lastName, passwordEncoder.encode(placeholderPassword), false, Set.of());
		return userRepository.save(user);
	}

	/**
	 * Lists users.
	 *
	 * @param pageable pagination information
	 * @return a page of users
	 */
	@Transactional(readOnly = true)
	public Page<User> listUsers(Pageable pageable) {
		return userRepository.findAll(pageable);
	}

	private AuditRecord userCreateAudit(User user, String resultCode, String creationPath) {
		return AuditRecord.builder(AuditAction.USER_CREATE, AuditOutcome.SUCCESS, resultCode)
				.target(AuditTargetType.USER, user.getId())
				.metadata("createdEmail", user.getEmail())
				.metadata("active", user.isActive())
				.metadata("globalRoles", user.getGlobalRoles().stream().map(GlobalRole::name).sorted().toList())
				.metadata("creationPath", creationPath)
				.build();
	}
}
