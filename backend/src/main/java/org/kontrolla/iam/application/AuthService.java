package org.kontrolla.iam.application;

import org.kontrolla.audit.application.AuditRecord;
import org.kontrolla.audit.application.AuditRecorder;
import org.kontrolla.audit.domain.AuditAction;
import org.kontrolla.audit.domain.AuditActorType;
import org.kontrolla.audit.domain.AuditOutcome;
import org.kontrolla.audit.domain.AuditTargetType;
import org.kontrolla.establishments.domain.Establishment;
import org.kontrolla.establishments.domain.EstablishmentStatus;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.Optional;

@Service
public class AuthService {

	private static final Logger log = LoggerFactory.getLogger(AuthService.class);

	private final UserRepository userRepository;
	private final RefreshTokenRepository refreshTokenRepository;
	private final OrganizationMembershipRepository organizationMembershipRepository;
	private final EstablishmentRepository establishmentRepository;
	private final AuthAttemptThrottleService authAttemptThrottleService;
	private final AuditRecorder auditRecorder;
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
			AuthAttemptThrottleService authAttemptThrottleService,
			AuditRecorder auditRecorder,
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
		this.authAttemptThrottleService = authAttemptThrottleService;
		this.auditRecorder = auditRecorder;
		this.passwordEncoder = passwordEncoder;
		this.userInviteService = userInviteService;
		this.jwtService = jwtService;
		this.securityProperties = securityProperties;
		this.clock = clock;
	}

	@Transactional
	public AuthSession login(String email, String password, String clientIp) {
		Instant now = Instant.now(clock);
		try {
			authAttemptThrottleService.assertLoginAllowed(email, clientIp, now);
		} catch (AuthThrottleException exception) {
			recordFailedAuthAudit(loginFailureAudit(email, "throttled", exception.getThrottleDimension()));
			throw exception;
		}

		User user = userRepository.findByEmailIgnoreCase(email)
				.filter(User::isActive)
				.orElseThrow(() -> {
					authAttemptThrottleService.recordLoginFailure(email, clientIp, now);
					recordFailedAuthAudit(loginFailureAudit(email, "invalid_credentials", null));
					return new UnauthorizedException("invalid_credentials", "Invalid email or password");
				});

		if (!passwordEncoder.matches(password, user.getPasswordHash())) {
			authAttemptThrottleService.recordLoginFailure(email, clientIp, now);
			recordFailedAuthAudit(loginFailureAudit(email, "invalid_credentials", null));
			throw new UnauthorizedException("invalid_credentials", "Invalid email or password");
		}

		authAttemptThrottleService.resetLogin(email, clientIp);
		return issueSession(user, now);
	}

	@Transactional
	public AuthSession refresh(String rawRefreshToken, String clientIp) {
		Instant now = Instant.now(clock);
		RefreshToken refreshToken = rawRefreshToken == null || rawRefreshToken.isBlank()
				? null
				: refreshTokenRepository.findByTokenHash(hashToken(rawRefreshToken)).orElse(null);
		String accountIdentifier = refreshToken == null ? null : refreshToken.getUser().getEmail();
		try {
			authAttemptThrottleService.assertRefreshAllowed(accountIdentifier, clientIp, now);
		} catch (AuthThrottleException exception) {
			recordFailedAuthAudit(refreshFailureAudit(refreshToken, "throttled", exception.getThrottleDimension()));
			throw exception;
		}

		RefreshToken activeRefreshToken;
		try {
			activeRefreshToken = resolveActiveRefreshToken(rawRefreshToken, refreshToken, clientIp, now);
		} catch (UnauthorizedException exception) {
			recordFailedAuthAudit(refreshFailureAudit(refreshToken, exception.getCode(), null));
			throw exception;
		}

		activeRefreshToken.revoke(now);
		authAttemptThrottleService.resetRefresh(activeRefreshToken.getUser().getEmail(), clientIp);
		AuthSession session = issueSession(activeRefreshToken.getUser(), now);
		auditRecorder.record(refreshSuccessAudit(activeRefreshToken, session));
		return session;
	}

	@Transactional
	public void logout(String rawRefreshToken) {
		if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
			recordFailedAuthAudit(logoutIgnoredAudit("missing_refresh_token", null));
			return;
		}

		RefreshToken refreshToken = refreshTokenRepository.findByTokenHash(hashToken(rawRefreshToken)).orElse(null);
		if (refreshToken == null) {
			recordFailedAuthAudit(logoutIgnoredAudit("token_not_found", null));
			return;
		}

		refreshToken.revoke(Instant.now(clock));
		auditRecorder.record(logoutSuccessAudit(refreshToken));
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

	private AuditRecord loginFailureAudit(String email, String resultCode, String throttleDimension) {
		return AuditRecord.builder(AuditAction.AUTH_LOGIN, AuditOutcome.FAILURE, resultCode)
				.metadata("attemptedEmail", email)
				.metadata("throttleDimension", throttleDimension)
				.build();
	}

	private AuditRecord refreshFailureAudit(RefreshToken refreshToken, String resultCode, String throttleDimension) {
		AuditRecord.Builder builder = AuditRecord.builder(AuditAction.AUTH_REFRESH, AuditOutcome.FAILURE, resultCode)
				.metadata("throttleDimension", throttleDimension);
		return populateRefreshTokenContext(builder, refreshToken).build();
	}

	private AuditRecord refreshSuccessAudit(RefreshToken refreshToken, AuthSession session) {
		AuditRecord.Builder builder = AuditRecord.builder(AuditAction.AUTH_REFRESH, AuditOutcome.SUCCESS, "success")
				.organizationId(session.appContext().organizationId());
		return populateRefreshTokenContext(builder, refreshToken).build();
	}

	private AuditRecord logoutIgnoredAudit(String resultCode, RefreshToken refreshToken) {
		AuditRecord.Builder builder = AuditRecord.builder(AuditAction.AUTH_LOGOUT, AuditOutcome.IGNORED, resultCode);
		return populateRefreshTokenContext(builder, refreshToken).build();
	}

	private AuditRecord logoutSuccessAudit(RefreshToken refreshToken) {
		AuditRecord.Builder builder = AuditRecord.builder(AuditAction.AUTH_LOGOUT, AuditOutcome.SUCCESS, "revoked")
				.organizationId(resolveOrganizationId(refreshToken.getUser().getId()));
		return populateRefreshTokenContext(builder, refreshToken).build();
	}

	private AuditRecord.Builder populateRefreshTokenContext(AuditRecord.Builder builder, RefreshToken refreshToken) {
		if (refreshToken == null) {
			return builder;
		}

		User user = refreshToken.getUser();
		return builder.actor(AuditActorType.USER, user.getId(), user.getEmail())
				.organizationId(resolveOrganizationId(user.getId()))
				.target(AuditTargetType.REFRESH_TOKEN, refreshToken.getId())
				.metadata("userId", user.getId())
				.metadata("userEmail", user.getEmail());
	}

	private java.util.UUID resolveOrganizationId(java.util.UUID userId) {
		return resolveAppContext(userId).organizationId();
	}

	private void recordFailedAuthAudit(AuditRecord auditRecord) {
		try {
			auditRecorder.recordInNewTransaction(auditRecord);
		} catch (RuntimeException exception) {
			log.error("Failed to persist audit event for auth action {}", auditRecord.getAction(), exception);
		}
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
		Optional<Establishment> establishment;
		if (activeMembership.getRole() == org.kontrolla.organizations.domain.OrganizationRole.ORG_OWNER
				|| activeMembership.getRole() == org.kontrolla.organizations.domain.OrganizationRole.ORG_ADMIN
				|| activeMembership.isAccessAllEstablishments()) {
			establishment = establishmentRepository.findFirstByOrganizationIdAndStatusOrderByCreatedAtAsc(
					activeMembership.getOrganization().getId(),
					EstablishmentStatus.ACTIVE
			);
		} else {
			establishment = activeMembership.getAccessibleEstablishments().stream()
					.filter(candidate -> candidate.getStatus() == EstablishmentStatus.ACTIVE)
					.min(Comparator.comparing(Establishment::getCreatedAt));
		}

		return new UserAppContext(
				activeMembership.getOrganization().getId(),
				activeMembership.getOrganization().getName(),
				activeMembership.getRole(),
				establishment.map(Establishment::getId).orElse(null),
				establishment.map(Establishment::getName).orElse(null)
		);
	}

	private RefreshToken resolveActiveRefreshToken(String rawRefreshToken, RefreshToken refreshToken, String clientIp, Instant now) {
		if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
			authAttemptThrottleService.recordRefreshFailure(clientIp, now);
			throw new UnauthorizedException("missing_refresh_token", "Refresh token is missing");
		}

		if (refreshToken == null) {
			authAttemptThrottleService.recordRefreshFailure(clientIp, now);
			throw new UnauthorizedException("invalid_refresh_token", "Refresh token is invalid");
		}

		if (!refreshToken.isActiveAt(now) || !refreshToken.getUser().isActive()) {
			authAttemptThrottleService.recordRefreshFailure(refreshToken.getUser().getEmail(), clientIp, now);
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
