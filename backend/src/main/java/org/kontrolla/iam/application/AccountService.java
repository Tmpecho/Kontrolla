package org.kontrolla.iam.application;

import org.kontrolla.common.exception.BadRequestException;
import org.kontrolla.iam.domain.User;
import org.kontrolla.iam.infrastructure.RefreshTokenRepository;
import org.kontrolla.iam.security.CurrentUser;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

@Service
public class AccountService {

	private final UserAccessService userAccessService;
	private final RefreshTokenRepository refreshTokenRepository;
	private final PasswordEncoder passwordEncoder;
	private final Clock clock;

	public AccountService(
			UserAccessService userAccessService,
			RefreshTokenRepository refreshTokenRepository,
			PasswordEncoder passwordEncoder,
			Clock clock
	) {
		this.userAccessService = userAccessService;
		this.refreshTokenRepository = refreshTokenRepository;
		this.passwordEncoder = passwordEncoder;
		this.clock = clock;
	}

	@Transactional
	public User updateMyProfile(CurrentUser currentUser, String firstName, String lastName) {
		User user = userAccessService.getCurrentUserOrThrow(currentUser);
		user.setFirstName(firstName.trim());
		user.setLastName(lastName.trim());
		return user;
	}

	@Transactional
	public void changeMyPassword(CurrentUser currentUser, String currentPassword, String newPassword) {
		User user = userAccessService.getCurrentUserOrThrow(currentUser);

		if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
			throw new BadRequestException("invalid_current_password", "Current password is incorrect");
		}

		if (passwordEncoder.matches(newPassword, user.getPasswordHash())) {
			throw new BadRequestException("password_unchanged", "New password must be different from the current password");
		}

		user.setPasswordHash(passwordEncoder.encode(newPassword));
		revokeActiveRefreshTokens(user);
	}

	private void revokeActiveRefreshTokens(User user) {
		Instant now = Instant.now(clock);
		refreshTokenRepository.findAllByUser_Id(user.getId()).stream().filter(refreshToken -> refreshToken.isActiveAt(now)).forEach(refreshToken -> refreshToken.revoke(now));
	}
}
