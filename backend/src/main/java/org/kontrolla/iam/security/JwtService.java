package org.kontrolla.iam.security;

import java.time.Instant;
import java.util.Set;
import org.kontrolla.iam.domain.User;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

/** Issues JWT access tokens for authenticated users. */
@Service
public class JwtService {

  private final JwtEncoder jwtEncoder;
  private final AppSecurityProperties properties;

  /**
   * Creates the JWT service.
   *
   * @param jwtEncoder encoder used to sign tokens
   * @param properties security properties containing JWT settings
   */
  public JwtService(JwtEncoder jwtEncoder, AppSecurityProperties properties) {
    this.jwtEncoder = jwtEncoder;
    this.properties = properties;
  }

  /**
   * Issues an access token for a user.
   *
   * @param user the authenticated user
   * @return the issued access token and expiry metadata
   */
  public IssuedAccessToken issueAccessToken(User user) {
    Instant issuedAt = Instant.now();
    Instant expiresAt = issuedAt.plus(properties.getJwt().getAccessTokenTtl());
    Set<String> roles =
        user.getGlobalRoles().stream().map(Enum::name).collect(java.util.stream.Collectors.toSet());
    JwtClaimsSet claims =
        JwtClaimsSet.builder()
            .issuer(properties.getJwt().getIssuer())
            .audience(java.util.List.of(properties.getJwt().getAudience()))
            .issuedAt(issuedAt)
            .expiresAt(expiresAt)
            .subject(user.getId().toString())
            .claim("email", user.getEmail())
            .claim("roles", roles)
            .build();

    String token =
        jwtEncoder
            .encode(
                JwtEncoderParameters.from(
                    JwsHeader.with(org.springframework.security.oauth2.jose.jws.MacAlgorithm.HS256)
                        .build(),
                    claims))
            .getTokenValue();
    long expiresInSeconds = properties.getJwt().getAccessTokenTtl().toSeconds();
    return new IssuedAccessToken(token, expiresInSeconds);
  }

  /**
   * Immutable access-token payload returned after JWT issuance.
   *
   * @param token signed JWT access token
   * @param expiresInSeconds number of seconds until the token expires
   */
  public record IssuedAccessToken(String token, long expiresInSeconds) {}
}
