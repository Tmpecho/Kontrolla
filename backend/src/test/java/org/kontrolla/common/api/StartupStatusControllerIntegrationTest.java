package org.kontrolla.common.api;

import org.junit.jupiter.api.Test;
import org.kontrolla.common.application.StartupReadinessService;
import org.kontrolla.common.application.StartupReadinessStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class StartupStatusControllerIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@MockBean
	private StartupReadinessService startupReadinessService;

	@Test
	void returnsUnauthorizedWithoutAuthentication() throws Exception {
		mockMvc.perform(get("/api/v1/system/startup-status"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void returnsCurrentStartupStatusForAuthenticatedUsers() throws Exception {
		given(startupReadinessService.getStatus()).willReturn(StartupReadinessStatus.STARTING);

		mockMvc.perform(get("/api/v1/system/startup-status").with(authenticatedUser()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("STARTING"))
				.andExpect(jsonPath("$.ready").value(false));

		given(startupReadinessService.getStatus()).willReturn(StartupReadinessStatus.READY);

		mockMvc.perform(get("/api/v1/system/startup-status").with(authenticatedUser()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("READY"))
				.andExpect(jsonPath("$.ready").value(true));
	}

	private JwtRequestPostProcessor authenticatedUser() {
		return jwt().jwt(jwt -> jwt
				.subject("11111111-1111-1111-1111-111111111111")
				.claim("email", "demo@example.com")
				.claim("roles", java.util.List.of())
		);
	}
}
