package org.kontrolla.iam.security;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Getter
@ConfigurationProperties(prefix = "app.security")
public class AppSecurityProperties {

	private final Jwt jwt = new Jwt();
	private final Refresh refresh = new Refresh();
	private final Cors cors = new Cors();
	private final BootstrapAdmin bootstrapAdmin = new BootstrapAdmin();
	private final BootstrapUser bootstrapUser = new BootstrapUser();

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
}
