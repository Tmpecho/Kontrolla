package org.kontrolla.iam.api;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.kontrolla.iam.application.AuthService;
import org.kontrolla.iam.application.AuthSession;
import org.kontrolla.iam.security.AppSecurityProperties;
import org.kontrolla.iam.security.CurrentUser;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

	private final AuthService authService;
	private final AppSecurityProperties securityProperties;

	public AuthController(AuthService authService, AppSecurityProperties securityProperties) {
		this.authService = authService;
		this.securityProperties = securityProperties;
	}

	@PostMapping("/login")
	public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
		AuthSession session = authService.login(request.email(), request.password());
		return ResponseEntity.ok()
				.header(HttpHeaders.SET_COOKIE, refreshCookie(session.refreshToken()).toString())
				.body(new LoginResponse(
						AuthUserResponse.from(session.user()),
						session.accessToken(),
						"Bearer",
						session.expiresInSeconds()
				));
	}

	@PostMapping("/refresh")
	public ResponseEntity<LoginResponse> refresh(HttpServletRequest request) {
		AuthSession session = authService.refresh(extractRefreshCookie(request));
		return ResponseEntity.ok()
				.header(HttpHeaders.SET_COOKIE, refreshCookie(session.refreshToken()).toString())
				.body(new LoginResponse(
						AuthUserResponse.from(session.user()),
						session.accessToken(),
						"Bearer",
						session.expiresInSeconds()
				));
	}

	@PostMapping("/logout")
	public ResponseEntity<Void> logout(HttpServletRequest request) {
		authService.logout(extractRefreshCookie(request));
		return ResponseEntity.status(HttpStatus.NO_CONTENT)
				.header(HttpHeaders.SET_COOKIE, clearRefreshCookie().toString())
				.build();
	}

	@GetMapping("/me")
	public UserResponse me(@AuthenticationPrincipal CurrentUser currentUser) {
		return UserResponse.from(authService.getCurrentUser(currentUser));
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

		for (Cookie cookie : request.getCookies()) {
			if (securityProperties.getRefresh().getCookieName().equals(cookie.getName())) {
				return cookie.getValue();
			}
		}

		return null;
	}
}
