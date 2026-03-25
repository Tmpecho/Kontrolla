package org.kontrolla.iam.application;

import org.junit.jupiter.api.Test;
import org.kontrolla.iam.domain.User;
import org.kontrolla.iam.infrastructure.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
		"app.security.bootstrap-user.email=demo@example.com",
		"app.security.bootstrap-user.password=password123",
		"app.security.bootstrap-user.first-name=Demo",
		"app.security.bootstrap-user.last-name=User"
})
@ActiveProfiles({"dev", "test"})
class BootstrapUserInitializerIntegrationTest {

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Test
	void bootstrapUserIsCreatedOnStartup() {
		User user = userRepository.findByEmailIgnoreCase("demo@example.com")
				.orElseThrow();

		assertThat(user.getFirstName()).isEqualTo("Demo");
		assertThat(user.getLastName()).isEqualTo("User");
		assertThat(user.isActive()).isTrue();
		assertThat(user.getGlobalRoles()).isEmpty();
		assertThat(passwordEncoder.matches("password123", user.getPasswordHash())).isTrue();
	}
}
