package org.kontrolla.iam.application;

import org.kontrolla.establishments.domain.Establishment;
import org.kontrolla.establishments.infrastructure.EstablishmentRepository;
import org.kontrolla.common.exception.UnauthorizedException;
import org.kontrolla.iam.domain.RefreshToken;
import org.kontrolla.iam.domain.User;
import org.kontrolla.iam.infrastructure.RefreshTokenRepository;
import org.kontrolla.iam.infrastructure.UserRepository;
import org.kontrolla.iam.security.AppSecurityProperties;
import org.kontrolla.iam.security.CurrentUser;
import org.kontrolla.iam.security.JwtService;
import org.kontrolla.iam.security.JwtService.IssuedAccessToken;
import org.kontrolla.organizations.domain.OrganizationMembership;
import org.kontrolla.organizations.infrastructure.OrganizationMembershipRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;

@Service
public class AuthService {

	private final UserRepository userRepository;
	private final RefreshTokenRepository refreshTokenRepository;
	private final OrganizationMembershipRepository organizationMembershipRepository;
	private final EstablishmentRepository establishmentRepository;
	private final LoginAttemptTracker loginAttemptTracker;
	private final PasswordEncoder passwordEncoder;
	private final UserInviteService userInviteService;
	private final JwtService jwtService;
	private final AppSecurityProperties securityProperties;
	private final Clock clock;

	public AuthService(
			UserRepository userRepository,
			RefreshTokenRepository refreshTokenRepository,
			OrganizationMembershipRepository organizationMembershipRepository,
			EstablishmentRepository establishmentRepository,
			LoginAttemptTracker loginAttemptTracker,
			PasswordEncoder passwordEncoder,
			UserInviteService userInviteService,
			JwtService jwtService,
			AppSecurityProperties securityProperties,
			Clock clock
	) {
		this.userRepository = userRepository;
		this.refreshTokenRepository = refreshTokenRepository;
		this.organizationMembershipRepository = organizationMembershipRepository;
		this.establishmentRepository = establishmentRepository;
		this.loginAttemptTracker = loginAttemptTracker;
		this.passwordEncoder = passwordEncoder;
		this.userInviteService = userInviteService;
		this.jwtService = jwtService;
		this.securityProperties = securityProperties;
		this.clock = clock;
	}

	@Transactional
	public AuthSession login(String email, String password) {
		Instant now = Instant.now(clock);
		loginAttemptTracker.assertLoginAllowed(email, now);

		User user = userRepository.findByEmailIgnoreCase(email)
				.filter(User::isActive)
				.orElseThrow(() -> {
					loginAttemptTracker.recordFailedAttempt(email, now);
					return new UnauthorizedException("invalid_credentials", "Invalid email or password");
				});

		if (!passwordEncoder.matches(password, user.getPasswordHash())) {
			loginAttemptTracker.recordFailedAttempt(email, now);
			throw new UnauthorizedException("invalid_credentials", "Invalid email or password");
		}

		loginAttemptTracker.reset(email);
		return issueSession(user, now);
	}

	@Transactional
	public AuthSession refresh(String rawRefreshToken) {
		Instant now = Instant.now();
		RefreshToken refreshToken = resolveActiveRefreshToken(rawRefreshToken, now);
		refreshToken.revoke(now);
		return issueSession(refreshToken.getUser(), now);
	}

	@Transactional
	public void logout(String rawRefreshToken) {
		if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
			return;
		}

		refreshTokenRepository.findByTokenHash(hashToken(rawRefreshToken))
				.ifPresent(token -> token.revoke(Instant.now()));
	}

	@Transactional(readOnly = true)
	public User getCurrentUser(CurrentUser currentUser) {
		return userRepository.findById(currentUser.userId())
				.filter(User::isActive)
				.orElseThrow(() -> new UnauthorizedException("user_not_found", "Authenticated user no longer exists"));
	}

	@Transactional(readOnly = true)
	public UserInviteService.InviteDetails getInviteDetails(String token) {
		return userInviteService.getInviteDetails(token);
	}

	@Transactional
	public void acceptInvite(String token, String password) {
		userInviteService.acceptInvite(token, password);
	}

	private AuthSession issueSession(User user, Instant now) {
		IssuedAccessToken accessToken = jwtService.issueAccessToken(user);
		String rawRefreshToken = generateRawRefreshToken();
		String tokenHash = hashToken(rawRefreshToken);
		RefreshToken refreshToken = new RefreshToken(user, tokenHash, now.plus(securityProperties.getRefresh().getTtl()));
		refreshTokenRepository.save(refreshToken);

		return new AuthSession(
				user,
				accessToken.token(),
				accessToken.expiresInSeconds(),
				rawRefreshToken,
				resolveAppContext(user.getId())
		);
	}

	private UserAppContext resolveAppContext(java.util.UUID userId) {
		Optional<OrganizationMembership> membership = organizationMembershipRepository
				.findFirstByUserIdAndActiveTrueOrderByCreatedAtAsc(userId);

		if (membership.isEmpty()) {
			return new UserAppContext(null, null, null, null, null);
		}

		OrganizationMembership activeMembership = membership.get();
		Optional<Establishment> establishment = establishmentRepository
				.findFirstByOrganizationIdOrderByCreatedAtAsc(activeMembership.getOrganization().getId());

		return new UserAppContext(
				activeMembership.getOrganization().getId(),
				activeMembership.getOrganization().getName(),
				activeMembership.getRole(),
				establishment.map(Establishment::getId).orElse(null),
				establishment.map(Establishment::getName).orElse(null)
		);
	}

	private RefreshToken resolveActiveRefreshToken(String rawRefreshToken, Instant now) {
		if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
			throw new UnauthorizedException("missing_refresh_token", "Refresh token is missing");
		}

		RefreshToken refreshToken = refreshTokenRepository.findByTokenHash(hashToken(rawRefreshToken))
				.orElseThrow(() -> new UnauthorizedException("invalid_refresh_token", "Refresh token is invalid"));

		if (!refreshToken.isActiveAt(now) || !refreshToken.getUser().isActive()) {
			throw new UnauthorizedException("invalid_refresh_token", "Refresh token is invalid");
		}

		return refreshToken;
	}

	private String generateRawRefreshToken() {
		byte[] bytes = new byte[48];
		new java.security.SecureRandom().nextBytes(bytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}

	private String hashToken(String rawRefreshToken) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hash = digest.digest(rawRefreshToken.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(hash);
		} catch (NoSuchAlgorithmException exception) {
			throw new IllegalStateException("SHA-256 not available", exception);
		}
	}
}
