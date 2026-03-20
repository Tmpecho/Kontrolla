package org.kontrolla.iam.security;

import org.kontrolla.iam.domain.User;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Set;

@Service
public class JwtService {

	private final JwtEncoder jwtEncoder;
	private final AppSecurityProperties properties;

	public JwtService(JwtEncoder jwtEncoder, AppSecurityProperties properties) {
		this.jwtEncoder = jwtEncoder;
		this.properties = properties;
	}

	public IssuedAccessToken issueAccessToken(User user) {
		Instant issuedAt = Instant.now();
		Instant expiresAt = issuedAt.plus(properties.getJwt().getAccessTokenTtl());
		Set<String> roles = user.getGlobalRoles().stream().map(Enum::name).collect(java.util.stream.Collectors.toSet());
		JwtClaimsSet claims = JwtClaimsSet.builder()
				.issuer(properties.getJwt().getIssuer())
				.issuedAt(issuedAt)
				.expiresAt(expiresAt)
				.subject(user.getId().toString())
				.claim("email", user.getEmail())
				.claim("roles", roles)
				.build();

		String token = jwtEncoder.encode(JwtEncoderParameters.from(JwsHeader.with(org.springframework.security.oauth2.jose.jws.MacAlgorithm.HS256).build(), claims)).getTokenValue();
		long expiresInSeconds = properties.getJwt().getAccessTokenTtl().toSeconds();
		return new IssuedAccessToken(token, expiresInSeconds);
	}

	public record IssuedAccessToken(String token, long expiresInSeconds) {
	}
}
