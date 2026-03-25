package org.kontrolla.iam.application;

import org.kontrolla.iam.domain.User;
import org.kontrolla.iam.infrastructure.UserRepository;
import org.kontrolla.iam.security.AppSecurityProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.Set;

@Component
@Profile("dev")
public class BootstrapUserInitializer implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(BootstrapUserInitializer.class);

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final AppSecurityProperties properties;

	public BootstrapUserInitializer(
			UserRepository userRepository,
			PasswordEncoder passwordEncoder,
			AppSecurityProperties properties
	) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
		this.properties = properties;
	}

	@Override
	@Transactional
	public void run(org.springframework.boot.ApplicationArguments args) {
		String email = Optional.ofNullable(properties.getBootstrapUser().getEmail()).orElse("").trim();
		String password = Optional.ofNullable(properties.getBootstrapUser().getPassword()).orElse("").trim();
		if (email.isBlank() || password.isBlank()) {
			return;
		}

		userRepository.findByEmailIgnoreCase(email).ifPresentOrElse(existing -> {
			if (!existing.isActive()) {
				existing.setActive(true);
				log.info("Activated bootstrap user {}", email);
			}
		}, () -> {
			User created = new User(
					email,
					properties.getBootstrapUser().getFirstName(),
					properties.getBootstrapUser().getLastName(),
					passwordEncoder.encode(password),
					true,
					Set.of()
			);
			userRepository.save(created);
			log.info("Created bootstrap user {}", email);
		});
	}
}
