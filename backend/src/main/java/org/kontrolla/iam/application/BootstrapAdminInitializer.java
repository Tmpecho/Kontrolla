package org.kontrolla.iam.application;

import org.kontrolla.iam.domain.GlobalRole;
import org.kontrolla.iam.domain.User;
import org.kontrolla.iam.infrastructure.UserRepository;
import org.kontrolla.iam.security.AppSecurityProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * Development-only bootstrapper that ensures a platform admin account exists.
 */
@Component
@Profile("dev")
@Order(5)
public class BootstrapAdminInitializer implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(BootstrapAdminInitializer.class);

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final AppSecurityProperties properties;

	/**
	 * Creates the bootstrap admin initializer.
	 *
	 * @param userRepository repository for users
	 * @param passwordEncoder encoder for the seeded password
	 * @param properties bootstrap security properties
	 */
	public BootstrapAdminInitializer(
			UserRepository userRepository,
			PasswordEncoder passwordEncoder,
			AppSecurityProperties properties
	) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
		this.properties = properties;
	}

	/**
	 * Creates or updates the configured bootstrap platform admin in development.
	 *
	 * @param args application startup arguments
	 */
	@Override
	@Transactional
	public void run(org.springframework.boot.ApplicationArguments args) {
		String email = Optional.ofNullable(properties.getBootstrapAdmin().getEmail()).orElse("").trim();
		String password = Optional.ofNullable(properties.getBootstrapAdmin().getPassword()).orElse("").trim();
		if (email.isBlank() || password.isBlank()) {
			return;
		}

		userRepository.findByEmailIgnoreCase(email).ifPresentOrElse(existing -> {
			HashSet<GlobalRole> roles = new HashSet<>(existing.getGlobalRoles());
			if (roles.add(GlobalRole.PLATFORM_ADMIN)) {
				existing.setGlobalRoles(roles);
				log.info("Granted PLATFORM_ADMIN to bootstrap user {}", email);
			}
			if (!existing.isActive()) {
				existing.setActive(true);
			}
		}, () -> {
			User created = new User(
					email,
					properties.getBootstrapAdmin().getFirstName(),
					properties.getBootstrapAdmin().getLastName(),
					passwordEncoder.encode(password),
					true,
					Set.of(GlobalRole.PLATFORM_ADMIN)
			);
			userRepository.save(created);
			log.info("Created bootstrap platform admin {}", email);
		});
	}
}
