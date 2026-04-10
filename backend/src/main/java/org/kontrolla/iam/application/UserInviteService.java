package org.kontrolla.iam.application;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import org.kontrolla.common.exception.UnauthorizedException;
import org.kontrolla.iam.domain.User;
import org.kontrolla.iam.domain.UserInvite;
import org.kontrolla.iam.infrastructure.UserInviteRepository;
import org.kontrolla.iam.security.AppSecurityProperties;
import org.kontrolla.organizations.domain.Organization;
import org.kontrolla.organizations.infrastructure.OrganizationMembershipRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Issues, validates, and accepts organization invite tokens. */
@Service
public class UserInviteService {

  private final UserInviteRepository userInviteRepository;
  private final OrganizationMembershipRepository organizationMembershipRepository;
  private final PasswordEncoder passwordEncoder;
  private final AppSecurityProperties securityProperties;
  private final InviteNotificationService inviteNotificationService;

  /**
   * Creates the user invite service.
   *
   * @param userInviteRepository repository for invite tokens
   * @param organizationMembershipRepository repository for memberships
   * @param passwordEncoder encoder for accepted invite passwords
   * @param securityProperties security and invite configuration
   * @param inviteNotificationService service used to deliver invite notifications
   */
  public UserInviteService(
      UserInviteRepository userInviteRepository,
      OrganizationMembershipRepository organizationMembershipRepository,
      PasswordEncoder passwordEncoder,
      AppSecurityProperties securityProperties,
      InviteNotificationService inviteNotificationService) {
    this.userInviteRepository = userInviteRepository;
    this.organizationMembershipRepository = organizationMembershipRepository;
    this.passwordEncoder = passwordEncoder;
    this.securityProperties = securityProperties;
    this.inviteNotificationService = inviteNotificationService;
  }

  /**
   * Issues a new organization invite for a user.
   *
   * @param user the invited user
   * @param organization the invited organization
   * @return the issued invite metadata
   */
  @Transactional
  public IssuedInvite issueOrganizationInvite(User user, Organization organization) {
    Instant now = Instant.now();
    Instant expiresAt = now.plus(securityProperties.getInvite().getTokenTtl());
    String rawToken = generateRawToken();
    UserInvite userInvite = new UserInvite(user, organization, hashToken(rawToken), expiresAt);
    userInviteRepository.save(userInvite);

    String inviteUrl = buildInviteUrl(rawToken);
    inviteNotificationService.sendOrganizationMemberInvite(
        user.getEmail(), organization.getName(), inviteUrl, expiresAt);

    String exposedInviteUrl =
        securityProperties.getInvite().isExposeInviteUrlInResponse() ? inviteUrl : null;
    return new IssuedInvite(expiresAt, exposedInviteUrl);
  }

  /**
   * Returns public details about an active invite token.
   *
   * @param rawToken the raw invite token
   * @return the invite details
   */
  @Transactional(readOnly = true)
  public InviteDetails getInviteDetails(String rawToken) {
    UserInvite invite = resolveActiveInvite(rawToken, Instant.now());
    return new InviteDetails(
        invite.getUser().getEmail(),
        invite.getUser().getFirstName(),
        invite.getUser().getLastName(),
        invite.getOrganization().getName(),
        invite.getExpiresAt());
  }

  /**
   * Accepts an invite by setting the invited user's password and marking the invite as accepted.
   *
   * @param rawToken the raw invite token
   * @param password the password chosen by the invited user
   */
  @Transactional
  public void acceptInvite(String rawToken, String password) {
    UserInvite invite = resolveActiveInvite(rawToken, Instant.now());
    User user = invite.getUser();
    user.setPasswordHash(passwordEncoder.encode(password));
    boolean hasActiveMembership =
        organizationMembershipRepository
            .findByOrganizationIdAndUserId(invite.getOrganization().getId(), user.getId())
            .map(org.kontrolla.organizations.domain.OrganizationMembership::isActive)
            .orElse(false);
    user.setActive(hasActiveMembership);
    invite.accept(Instant.now());
  }

  private UserInvite resolveActiveInvite(String rawToken, Instant now) {
    return userInviteRepository
        .findByTokenHash(hashToken(rawToken))
        .filter(invite -> invite.isActiveAt(now))
        .orElseThrow(
            () -> new UnauthorizedException("invalid_invite", "Invitation is invalid or expired"));
  }

  private String buildInviteUrl(String rawToken) {
    String baseUrl = securityProperties.getInvite().getFrontendBaseUrl();
    String normalizedBaseUrl =
        baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    return normalizedBaseUrl + "/invite/" + rawToken;
  }

  private String generateRawToken() {
    byte[] bytes = new byte[32];
    new java.security.SecureRandom().nextBytes(bytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }

  private String hashToken(String rawToken) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(hash);
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("SHA-256 not available", exception);
    }
  }
}
