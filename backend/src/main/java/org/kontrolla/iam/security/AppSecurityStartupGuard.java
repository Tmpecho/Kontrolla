package org.kontrolla.iam.security;

import jakarta.annotation.PostConstruct;
import java.util.Arrays;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;

/** Prevents insecure bootstrap and JWT configurations from starting outside development mode. */
@Configuration
@Order(0)
public class AppSecurityStartupGuard {

  static final String INSECURE_DEV_JWT_SECRET = "change-me-change-me-change-me-change-me";

  private final Environment environment;
  private final AppSecurityProperties properties;

  /**
   * Creates the startup guard.
   *
   * @param environment the Spring environment
   * @param properties the application security properties
   */
  public AppSecurityStartupGuard(Environment environment, AppSecurityProperties properties) {
    this.environment = environment;
    this.properties = properties;
  }

  @PostConstruct
  void validate() {
    boolean devActive = isProfileActive("dev");
    boolean prodActive = isProfileActive("prod");

    if (devActive && prodActive) {
      throw new IllegalStateException(
          "The 'dev' and 'prod' profiles cannot be active at the same time");
    }

    if (devActive) {
      return;
    }

    if (INSECURE_DEV_JWT_SECRET.equals(normalize(properties.getJwt().getSecret()))) {
      throw new IllegalStateException(
          "Non-dev startup cannot use the insecure development JWT secret");
    }

    if (hasAnyConfiguredCredential(
        properties.getBootstrapAdmin().getEmail(), properties.getBootstrapAdmin().getPassword())) {
      throw new IllegalStateException("Non-dev startup cannot enable bootstrap admin credentials");
    }

    if (hasAnyConfiguredCredential(
        properties.getBootstrapUser().getEmail(), properties.getBootstrapUser().getPassword())) {
      throw new IllegalStateException("Non-dev startup cannot enable bootstrap user credentials");
    }
  }

  private boolean isProfileActive(String profile) {
    return Arrays.stream(environment.getActiveProfiles()).anyMatch(profile::equalsIgnoreCase);
  }

  private boolean hasAnyConfiguredCredential(String email, String password) {
    return !normalize(email).isBlank() || !normalize(password).isBlank();
  }

  private String normalize(String value) {
    return value == null ? "" : value.trim();
  }
}
