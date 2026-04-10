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

/**
 * REST API for authentication, invitation acceptance, CSRF bootstrapping, and
 * self-service account endpoints.
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

	private final AccountService accountService;
	private final AuthService authService;
	private final AppSecurityProperties securityProperties;
	private final CsrfTokenRepository csrfTokenRepository;

	/**
	 * Creates the auth controller.
	 *
	 * @param accountService service for profile and password changes
	 * @param authService service for login, refresh, logout, and invites
	 * @param securityProperties security properties used for cookies
	 * @param csrfTokenRepository repository for CSRF token management
	 */
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

	/**
	 * Authenticates a user and issues access and refresh tokens.
	 *
	 * @param httpRequest the incoming HTTP request
	 * @param request the login payload
	 * @return the login response with tokens and user context
	 */
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

	/**
	 * Refreshes an authenticated session using the refresh-token cookie.
	 *
	 * @param request the incoming HTTP request
	 * @return the refreshed login response
	 */
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

	/**
	 * Logs out the current session by revoking the refresh-token cookie.
	 *
	 * @param request the incoming HTTP request
	 * @return a no-content response
	 */
	@PostMapping("/logout")
	public ResponseEntity<Void> logout(HttpServletRequest request) {
		authService.logout(extractRefreshCookie(request));
		return ResponseEntity.status(HttpStatus.NO_CONTENT)
				.header(HttpHeaders.SET_COOKIE, clearRefreshCookie().toString())
				.build();
	}

	/**
	 * Returns public details for an invite token.
	 *
	 * @param token the invite token
	 * @return the invite details response
	 */
	@GetMapping("/invitations/{token}")
	public InviteDetailsResponse getInvite(@PathVariable String token) {
		return InviteDetailsResponse.from(authService.getInviteDetails(token));
	}

	/**
	 * Accepts an invite token and sets the invited user's password.
	 *
	 * @param token the invite token
	 * @param request the accept-invite payload
	 */
	@PostMapping("/invitations/{token}/accept")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void acceptInvite(@PathVariable String token, @Valid @RequestBody AcceptInviteRequest request) {
		authService.acceptInvite(token, request.password());
	}

	/**
	 * Returns or creates a CSRF token for SPA bootstrap.
	 *
	 * @param request the incoming HTTP request
	 * @param response the outgoing HTTP response
	 * @return the CSRF token response
	 */
	@GetMapping("/csrf")
	public CsrfTokenResponse csrf(HttpServletRequest request, HttpServletResponse response) {
		CsrfToken csrfToken = csrfTokenRepository.loadToken(request);
		if (csrfToken == null) {
			csrfToken = csrfTokenRepository.generateToken(request);
			csrfTokenRepository.saveToken(csrfToken, request, response);
		}
		return CsrfTokenResponse.from(csrfToken);
	}

	/**
	 * Returns the current authenticated user.
	 *
	 * @param currentUser the authenticated principal
	 * @return the user response
	 */
	@GetMapping("/me")
	public UserResponse me(@AuthenticationPrincipal CurrentUser currentUser) {
		return UserResponse.from(authService.getCurrentUser(currentUser));
	}

	/**
	 * Updates the current user's profile.
	 *
	 * @param currentUser the authenticated principal
	 * @param request the profile update payload
	 * @return the updated user response
	 */
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

	/**
	 * Changes the current user's password.
	 *
	 * @param currentUser the authenticated principal
	 * @param request the password change payload
	 * @return a no-content response
	 */
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
