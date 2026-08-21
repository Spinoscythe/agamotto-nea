package com.srikrishnanethi.agamotto.jwt;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtServiceTest {

	@Test
	void issuedTokenRoundTripsUserIdAndEmail() {
		JwtProperties properties = new JwtProperties();
		properties.setSecret("unit-test-secret-value-32bytes!!");
		properties.setExpirationMs(60_000L);
		JwtService service = new JwtService(properties);

		String token = service.issueToken("user-1", "ada@example.com");
		JwtService.ParsedToken parsed = service.parse(token);

		assertEquals("user-1", parsed.userId());
		assertEquals("ada@example.com", parsed.email());
		assertTrue(parsed.expiresAtEpochSeconds() > 0);
	}

	@Test
	void tamperedTokenIsRejected() {
		JwtProperties properties = new JwtProperties();
		JwtService service = new JwtService(properties);
		String token = service.issueToken("user-1", "ada@example.com");

		assertThrows(IllegalArgumentException.class, () -> service.parse(token + "x"));
		assertThrows(IllegalArgumentException.class, () -> service.parse("not-a-jwt"));
		assertThrows(IllegalArgumentException.class, () -> service.parse(""));
	}
}
