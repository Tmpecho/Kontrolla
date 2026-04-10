package org.kontrolla.iam.application;

import java.time.Clock;
import java.time.Instant;
import org.kontrolla.common.exception.BadRequestException;
import org.kontrolla.iam.domain.User;
import org.kontrolla.iam.infrastructure.RefreshTokenRepository;
import org.kontrolla.iam.security.CurrentUser;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Handles profile and password management for the current authenticated user. */
@Service
public class AccountService {

  private final UserAccessService userAccessService;
  private final RefreshTokenRepository refreshTokenRepository;
  private final PasswordEncoder passwordEncoder;
  private final Clock clock;

  /**
   * Creates the account service.
   *
   * @param userAccessService service for resolving the current user
   * @param refreshTokenRepository repository for refresh tokens
   * @param passwordEncoder encoder for password validation and storage
   * @param clock clock used when revoking refresh tokens
   */
  public AccountService(
      UserAccessService userAccessService,
      RefreshTokenRepository refreshTokenRepository,
      PasswordEncoder passwordEncoder,
      Clock clock) {
    this.userAccessService = userAccessService;
    this.refreshTokenRepository = refreshTokenRepository;
    this.passwordEncoder = passwordEncoder;
    this.clock = clock;
  }

  /**
   * Updates the first and last name of the current user.
   *
   * @param currentUser the authenticated principal
   * @param firstName the new first name
   * @param lastName the new last name
   * @return the updated user
   */
  @Transactional
  public User updateMyProfile(CurrentUser currentUser, String firstName, String lastName) {
    User user = userAccessService.getCurrentUserOrThrow(currentUser);
    user.setFirstName(firstName.trim());
    user.setLastName(lastName.trim());
    return user;
  }

  /**
   * Changes the current user's password and revokes active refresh tokens.
   *
   * @param currentUser the authenticated principal
   * @param currentPassword the current password
   * @param newPassword the replacement password
   */
  @Transactional
  public void changeMyPassword(
      CurrentUser currentUser, String currentPassword, String newPassword) {
    User user = userAccessService.getCurrentUserOrThrow(currentUser);

    if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
      throw new BadRequestException("invalid_current_password", "Current password is incorrect");
    }

    if (passwordEncoder.matches(newPassword, user.getPasswordHash())) {
      throw new BadRequestException(
          "password_unchanged", "New password must be different from the current password");
    }

    user.setPasswordHash(passwordEncoder.encode(newPassword));
    revokeActiveRefreshTokens(user);
  }

  private void revokeActiveRefreshTokens(User user) {
    Instant now = Instant.now(clock);
    refreshTokenRepository.findAllByUser_Id(user.getId()).stream()
        .filter(refreshToken -> refreshToken.isActiveAt(now))
        .forEach(refreshToken -> refreshToken.revoke(now));
  }
}
