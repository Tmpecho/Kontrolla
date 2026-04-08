package org.kontrolla.iam.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.kontrolla.iam.domain.GlobalRole;
import org.kontrolla.iam.domain.User;
import org.kontrolla.iam.infrastructure.UserRepository;
import org.kontrolla.support.TestDataCleaner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
		"app.security.bootstrap-admin.email=platform.admin@example.com",
		"app.security.bootstrap-admin.password=password123",
		"app.security.bootstrap-admin.first-name=Platform",
		"app.security.bootstrap-admin.last-name=Admin"
})
@ActiveProfiles({"dev", "test"})
class BootstrapAdminInitializerIntegrationTest {

	@Autowired
	private BootstrapAdminInitializer bootstrapAdminInitializer;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private TestDataCleaner testDataCleaner;

	@BeforeEach
	void setUp() {
		testDataCleaner.clearAll();
	}

	@Test
	void bootstrapAdminIsCreatedWhenMissing() {
		bootstrapAdminInitializer.run(new DefaultApplicationArguments());

		User user = userRepository.findByEmailIgnoreCase("platform.admin@example.com")
				.orElseThrow();

		assertThat(user.getFirstName()).isEqualTo("Platform");
		assertThat(user.getLastName()).isEqualTo("Admin");
		assertThat(user.isActive()).isTrue();
		assertThat(user.getGlobalRoles()).containsExactly(GlobalRole.PLATFORM_ADMIN);
		assertThat(passwordEncoder.matches("password123", user.getPasswordHash())).isTrue();
	}

	@Test
	void bootstrapAdminGrantRoleAndReactivatesExistingUser() {
		User existingUser = userRepository.saveAndFlush(new User(
				"platform.admin@example.com",
				"Existing",
				"User",
				"existing-password-hash",
				false,
				Set.of()
		));

		bootstrapAdminInitializer.run(new DefaultApplicationArguments());

		User updatedUser = userRepository.findById(existingUser.getId())
				.orElseThrow();

		assertThat(updatedUser.isActive()).isTrue();
		assertThat(updatedUser.getGlobalRoles()).containsExactly(GlobalRole.PLATFORM_ADMIN);
		assertThat(updatedUser.getPasswordHash()).isEqualTo("existing-password-hash");
	}
}
