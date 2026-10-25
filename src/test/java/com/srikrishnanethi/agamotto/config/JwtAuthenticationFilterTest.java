package com.srikrishnanethi.agamotto.config;

import com.srikrishnanethi.agamotto.jwt.JwtProperties;
import com.srikrishnanethi.agamotto.jwt.JwtService;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtAuthenticationFilterTest {

	@AfterEach
	void clearSecurityContext() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void bearerTokenPopulatesSecurityContext() throws ServletException, IOException {
		JwtService jwtService = jwtService();
		String token = jwtService.issueToken("user-1", "ada@example.com");

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("Authorization", "Bearer " + token);

		new JwtAuthenticationFilter(jwtService).doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		assertNotNull(auth);
		assertTrue(auth.isAuthenticated());
		assertEquals("user-1", auth.getPrincipal());
	}

	@Test
	void invalidTokenLeavesContextEmpty() throws ServletException, IOException {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("Authorization", "Bearer not-a-jwt");

		new JwtAuthenticationFilter(jwtService()).doFilter(
				request, new MockHttpServletResponse(), new MockFilterChain());

		assertNull(SecurityContextHolder.getContext().getAuthentication());
	}

	private static JwtService jwtService() {
		JwtProperties properties = new JwtProperties();
		properties.setSecret("unit-test-secret-value-32bytes!!");
		properties.setExpirationMs(60_000L);
		return new JwtService(properties);
	}
}
