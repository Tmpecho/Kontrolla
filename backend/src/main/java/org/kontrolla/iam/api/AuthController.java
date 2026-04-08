package org.kontrolla.iam.api;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

	private final AuthService authService;
	private final AppSecurityProperties securityProperties;
	private final CsrfTokenRepository csrfTokenRepository;

	public AuthController(
			AuthService authService,
			AppSecurityProperties securityProperties,
			CsrfTokenRepository csrfTokenRepository
	) {
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

	@GetMapping("/invitations/{token}")
	public InviteDetailsResponse getInvite(@PathVariable String token) {
		return InviteDetailsResponse.from(authService.getInviteDetails(token));
	}

	@PostMapping("/invitations/{token}/accept")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void acceptInvite(@PathVariable String token, @Valid @RequestBody AcceptInviteRequest request) {
		authService.acceptInvite(token, request.password());
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
