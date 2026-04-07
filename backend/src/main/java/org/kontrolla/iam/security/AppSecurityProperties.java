package org.kontrolla.iam.security;

import lombok.Getter;
import lombok.Setter;
import org.kontrolla.establishments.domain.EstablishmentStatus;
import org.kontrolla.establishments.domain.EstablishmentType;
import org.kontrolla.organizations.domain.OrganizationStatus;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Getter
@ConfigurationProperties(prefix = "app.security")
public class AppSecurityProperties {

	private final Jwt jwt = new Jwt();
	private final Refresh refresh = new Refresh();
	private final Login login = new Login();
	private final Cors cors = new Cors();
	private final BootstrapAdmin bootstrapAdmin = new BootstrapAdmin();
	private final BootstrapUser bootstrapUser = new BootstrapUser();
	private final BootstrapOrganization bootstrapOrganization = new BootstrapOrganization();
	private final BootstrapEstablishment bootstrapEstablishment = new BootstrapEstablishment();

	@Setter
	@Getter
	public static class Jwt {

		private String issuer;
		private String secret;
		private Duration accessTokenTtl = Duration.ofMinutes(15);

	}

	@Setter
	@Getter
	public static class Refresh {

		private String cookieName = "kontrolla_refresh_token";
		private String cookiePath = "/api/v1/auth";
		private String sameSite = "Lax";
		private boolean secureCookie;
		private Duration ttl = Duration.ofDays(14);

	}

	@Setter
	@Getter
	public static class Login {

		private int maxFailedAttempts = 5;
		private Duration lockoutDuration = Duration.ofMinutes(10);

	}

	@Setter
	@Getter
	public static class Cors {

		private List<String> allowedOrigins = new ArrayList<>(List.of("http://localhost:5173"));

	}

	@Setter
	@Getter
	public static class BootstrapAdmin {

		private String email;
		private String password;
		private String firstName = "Platform";
		private String lastName = "Admin";

	}

	@Setter
	@Getter
	public static class BootstrapUser {

		private String email;
		private String password;
		private String firstName = "Demo";
		private String lastName = "User";

	}

	@Setter
	@Getter
	public static class BootstrapOrganization {

		private String name = "Demo Organization";
		private OrganizationStatus status = OrganizationStatus.ACTIVE;

	}

	@Setter
	@Getter
	public static class BootstrapEstablishment {

		private String name = "Demo Establishment";
		private EstablishmentType type = EstablishmentType.RESTAURANT;
		private EstablishmentStatus status = EstablishmentStatus.ACTIVE;

	}
}
