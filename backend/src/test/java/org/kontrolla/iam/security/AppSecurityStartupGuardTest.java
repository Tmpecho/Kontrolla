package org.kontrolla.iam.security;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

class AppSecurityStartupGuardTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withUserConfiguration(TestConfiguration.class);

	@Test
	void prodStartupFailsWhenBootstrapUserCredentialsAreConfigured() {
		contextRunner
				.withPropertyValues(
						"spring.profiles.active=prod",
						"app.security.bootstrap-user.email=demo@example.com",
						"app.security.bootstrap-user.password=password123"
				)
				.run(context -> {
					assertThat(context).hasFailed();
					assertThat(rootCauseOf(context.getStartupFailure()))
							.isInstanceOf(IllegalStateException.class)
							.hasMessage("Non-dev startup cannot enable bootstrap user credentials");
				});
	}

	@Test
	void prodStartupFailsWhenBootstrapAdminCredentialsAreConfigured() {
		contextRunner
				.withPropertyValues(
						"spring.profiles.active=prod",
						"app.security.bootstrap-admin.email=platform.admin@example.com",
						"app.security.bootstrap-admin.password=password123"
				)
				.run(context -> {
					assertThat(context).hasFailed();
					assertThat(rootCauseOf(context.getStartupFailure()))
							.isInstanceOf(IllegalStateException.class)
							.hasMessage("Non-dev startup cannot enable bootstrap admin credentials");
				});
	}

	@Test
	void prodStartupFailsWhenUsingInsecureDevelopmentJwtSecret() {
		contextRunner
				.withPropertyValues(
						"spring.profiles.active=prod",
						"app.security.jwt.secret=" + AppSecurityStartupGuard.INSECURE_DEV_JWT_SECRET
				)
				.run(context -> {
					assertThat(context).hasFailed();
					assertThat(rootCauseOf(context.getStartupFailure()))
							.isInstanceOf(IllegalStateException.class)
							.hasMessage("Non-dev startup cannot use the insecure development JWT secret");
				});
	}

	@Test
	void testStartupFailsWhenUsingInsecureDevelopmentJwtSecret() {
		contextRunner
				.withPropertyValues(
						"spring.profiles.active=test",
						"app.security.jwt.secret=" + AppSecurityStartupGuard.INSECURE_DEV_JWT_SECRET
				)
				.run(context -> {
					assertThat(context).hasFailed();
					assertThat(rootCauseOf(context.getStartupFailure()))
							.isInstanceOf(IllegalStateException.class)
							.hasMessage("Non-dev startup cannot use the insecure development JWT secret");
				});
	}

	@Test
	void devAndTestStartupAllowsBootstrapCredentials() {
		contextRunner
				.withPropertyValues(
						"spring.profiles.active=dev,test",
						"app.security.bootstrap-user.email=demo@example.com",
						"app.security.bootstrap-user.password=password123",
						"app.security.bootstrap-admin.email=platform.admin@example.com",
						"app.security.bootstrap-admin.password=password123",
						"app.security.jwt.secret=" + AppSecurityStartupGuard.INSECURE_DEV_JWT_SECRET
				)
				.run(context -> assertThat(context).hasNotFailed());
	}

	@Test
	void devAndProdProfilesCannotBeActiveTogether() {
		contextRunner
				.withPropertyValues("spring.profiles.active=dev,prod")
				.run(context -> {
					assertThat(context).hasFailed();
					assertThat(rootCauseOf(context.getStartupFailure()))
							.isInstanceOf(IllegalStateException.class)
							.hasMessage("The 'dev' and 'prod' profiles cannot be active at the same time");
				});
	}

	private Throwable rootCauseOf(Throwable throwable) {
		Throwable current = throwable;
		while (current.getCause() != null) {
			current = current.getCause();
		}
		return current;
	}

	@Configuration(proxyBeanMethods = false)
	@EnableConfigurationProperties(AppSecurityProperties.class)
	@Import(AppSecurityStartupGuard.class)
	static class TestConfiguration {
	}
}
