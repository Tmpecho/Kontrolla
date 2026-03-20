package org.kontrolla.iam.api;

import jakarta.validation.Valid;
import org.kontrolla.common.api.PageResponse;
import org.kontrolla.iam.application.UserAdministrationService;
import org.kontrolla.iam.domain.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@RestController
@RequestMapping("/api/v1/admin/users")
@PreAuthorize("hasRole('PLATFORM_ADMIN')")
public class AdminUserController {

	private final UserAdministrationService userAdministrationService;

	public AdminUserController(UserAdministrationService userAdministrationService) {
		this.userAdministrationService = userAdministrationService;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public UserResponse createUser(@Valid @RequestBody CreateUserRequest request) {
		User user = userAdministrationService.createUser(
				request.email(),
				request.firstName(),
				request.lastName(),
				request.password(),
				request.active() == null || request.active(),
				request.globalRoles() == null ? Set.of() : request.globalRoles()
		);
		return UserResponse.from(user);
	}

	@GetMapping
	public PageResponse<UserResponse> listUsers(@PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
		return PageResponse.from(userAdministrationService.listUsers(pageable), UserResponse::from);
	}
}
