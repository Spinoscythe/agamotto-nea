package com.srikrishnanethi.agamotto.security;

import com.srikrishnanethi.agamotto.entities.Project;
import com.srikrishnanethi.agamotto.entities.User;
import com.srikrishnanethi.agamotto.exception.ForbiddenException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AgamottoSecurityTest {

	@AfterEach
	void clearSecurityContext() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void currentUserIdReadsPrincipal() {
		authenticate("user-1");
		assertEquals("user-1", AgamottoSecurity.currentUserId());
	}

	@Test
	void requireSelfRejectsOtherUsers() {
		authenticate("user-1");
		assertThrows(ForbiddenException.class, () -> AgamottoSecurity.requireSelf("user-2"));
		AgamottoSecurity.requireSelf("user-1");
		AgamottoSecurity.requireSelf(null);
	}

	@Test
	void requireOwnerRejectsForeignProject() {
		authenticate("user-1");
		User owner = new User();
		owner.setId("user-2");
		Project project = new Project();
		project.setOwner(owner);
		assertThrows(ForbiddenException.class, () -> AgamottoSecurity.requireOwner(project));
	}

	private static void authenticate(String userId) {
		SecurityContextHolder.getContext().setAuthentication(
				new UsernamePasswordAuthenticationToken(
						userId,
						null,
						List.of(new SimpleGrantedAuthority("ROLE_USER"))));
	}
}
