package com.srikrishnanethi.agamotto.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "agamotto.jwt")
public class JwtProperties {

	/**
	 * HMAC secret (UTF-8). Prefer a long random value via {@code AGAMOTTO_JWT_SECRET}.
	 */
	private String secret = "agamotto-dev-jwt-secret-change-me-32b";

	/** Token lifetime in milliseconds (default 24h). */
	private long expirationMs = 86_400_000L;

	public String getSecret() {
		return secret;
	}

	public void setSecret(String secret) {
		this.secret = secret;
	}

	public long getExpirationMs() {
		return expirationMs;
	}

	public void setExpirationMs(long expirationMs) {
		this.expirationMs = expirationMs;
	}
}
