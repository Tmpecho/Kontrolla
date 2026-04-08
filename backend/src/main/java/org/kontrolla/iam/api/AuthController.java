package org.kontrolla.iam.api;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.kontrolla.iam.application.AccountService;
import org.kontrolla.iam.application.AuthService;
import org.kontrolla.iam.application.AuthSession;
import org.kontrolla.iam.security.AppSecurityProperties;
import org.kontrolla.iam.security.CurrentUser;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

	private final AccountService accountService;
	private final AuthService authService;
	private final AppSecurityProperties securityProperties;
	private final CsrfTokenRepository csrfTokenRepository;

	public AuthController(
			AccountService accountService,
			AuthService authService,
			AppSecurityProperties securityProperties,
			CsrfTokenRepository csrfTokenRepository
	) {
		this.accountService = accountService;
		this.authService = authService;
		this.securityProperties = securityProperties;
		this.csrfTokenRepository = csrfTokenRepository;
	}

	@PostMapping("/login")
	public ResponseEntity<LoginResponse> login(HttpServletRequest httpRequest, @Valid @RequestBody LoginRequest request) {
		AuthSession session = authService.login(request.email(), request.password(), httpRequest.getRemoteAddr());
		return ResponseEntity.ok()
				.header(HttpHeaders.SET_COOKIE, refreshCookie(session.refreshToken()).toString())
				.body(
						new LoginResponse(
								AuthUserResponse.from(session.user()),
								session.accessToken(),
								"Bearer",
								session.expiresInSeconds(),
								UserAppContextResponse.from(session.appContext())));
	}

	@PostMapping("/refresh")
	public ResponseEntity<LoginResponse> refresh(HttpServletRequest request) {
		AuthSession session = authService.refresh(extractRefreshCookie(request), request.getRemoteAddr());
		return ResponseEntity.ok()
				.header(HttpHeaders.SET_COOKIE, refreshCookie(session.refreshToken()).toString())
				.body(
						new LoginResponse(
								AuthUserResponse.from(session.user()),
								session.accessToken(),
								"Bearer",
								session.expiresInSeconds(),
								UserAppContextResponse.from(session.appContext())));
	}

	@PostMapping("/logout")
	public ResponseEntity<Void> logout(HttpServletRequest request) {
		authService.logout(extractRefreshCookie(request));
		return ResponseEntity.status(HttpStatus.NO_CONTENT)
				.header(HttpHeaders.SET_COOKIE, clearRefreshCookie().toString())
				.build();
	}

	@GetMapping("/csrf")
	public CsrfTokenResponse csrf(HttpServletRequest request, HttpServletResponse response) {
		CsrfToken csrfToken = csrfTokenRepository.loadToken(request);
		if (csrfToken == null) {
			csrfToken = csrfTokenRepository.generateToken(request);
			csrfTokenRepository.saveToken(csrfToken, request, response);
		}
		return CsrfTokenResponse.from(csrfToken);
	}

	@GetMapping("/me")
	public UserResponse me(@AuthenticationPrincipal CurrentUser currentUser) {
		return UserResponse.from(authService.getCurrentUser(currentUser));
	}

	@PutMapping("/me")
	public UserResponse updateMyProfile(
			@AuthenticationPrincipal CurrentUser currentUser,
			@Valid @RequestBody UpdateMyProfileRequest request
	) {
		return UserResponse.from(
				accountService.updateMyProfile(
						currentUser,
						request.firstName(),
						request.lastName()
				)
		);
	}

	@PutMapping("/me/password")
	public ResponseEntity<Void> changeMyPassword(
			@AuthenticationPrincipal CurrentUser currentUser,
			@Valid @RequestBody ChangeMyPasswordRequest request
	) {
		accountService.changeMyPassword(currentUser, request.currentPassword(), request.newPassword());
		return ResponseEntity.noContent().build();
	}

	private ResponseCookie refreshCookie(String rawToken) {
		return ResponseCookie.from(securityProperties.getRefresh().getCookieName(), rawToken)
				.httpOnly(true)
				.secure(securityProperties.getRefresh().isSecureCookie())
				.path(securityProperties.getRefresh().getCookiePath())
				.sameSite(securityProperties.getRefresh().getSameSite())
				.maxAge(securityProperties.getRefresh().getTtl())
				.build();
	}

	private ResponseCookie clearRefreshCookie() {
		return ResponseCookie.from(securityProperties.getRefresh().getCookieName(), "")
				.httpOnly(true)
				.secure(securityProperties.getRefresh().isSecureCookie())
				.path(securityProperties.getRefresh().getCookiePath())
				.sameSite(securityProperties.getRefresh().getSameSite())
				.maxAge(0)
				.build();
	}

	private String extractRefreshCookie(HttpServletRequest request) {
		if (request.getCookies() == null) {
			return null;
		}

		return Arrays.stream(request.getCookies())
				.filter(cookie -> securityProperties.getRefresh().getCookieName().equals(cookie.getName()))
				.findFirst()
				.map(Cookie::getValue)
				.orElse(null);
	}
}
