package com.srikrishnanethi.agamotto.config;

import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Objects;

/**
 * Minimal in-house HS256 JWT (header.payload.signature) — no third-party JWT library.
 * Payload claims: {@code sub} (userId), {@code email}, {@code exp} (epoch seconds).
 */
@Service
public class JwtService {

	private static final Base64.Encoder URL_ENCODER = Base64.getUrlEncoder().withoutPadding();
	private static final Base64.Decoder URL_DECODER = Base64.getUrlDecoder();
	private static final String HEADER_JSON = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";

	private final JwtProperties properties;

	public JwtService(JwtProperties properties) {
		this.properties = properties;
	}

	public String issueToken(String userId, String email) {
		Objects.requireNonNull(userId, "userId");
		Objects.requireNonNull(email, "email");
		long exp = Instant.now().getEpochSecond() + (properties.getExpirationMs() / 1000L);
		String header = URL_ENCODER.encodeToString(HEADER_JSON.getBytes(StandardCharsets.UTF_8));
		String payloadJson = "{\"sub\":\"" + escape(userId)
				+ "\",\"email\":\"" + escape(email)
				+ "\",\"exp\":" + exp + "}";
		String payload = URL_ENCODER.encodeToString(payloadJson.getBytes(StandardCharsets.UTF_8));
		String signingInput = header + "." + payload;
		return signingInput + "." + sign(signingInput);
	}

	public ParsedToken parse(String token) {
		if (token == null || token.isBlank()) {
			throw new IllegalArgumentException("token must not be blank");
		}
		String[] parts = token.split("\\.");
		if (parts.length != 3) {
			throw new IllegalArgumentException("Malformed JWT");
		}
		String signingInput = parts[0] + "." + parts[1];
		if (!sign(signingInput).equals(parts[2])) {
			throw new IllegalArgumentException("Invalid JWT signature");
		}
		String payloadJson = new String(URL_DECODER.decode(parts[1]), StandardCharsets.UTF_8);
		String userId = extractJsonString(payloadJson, "sub");
		String email = extractJsonString(payloadJson, "email");
		long exp = extractJsonLong(payloadJson, "exp");
		if (Instant.now().getEpochSecond() >= exp) {
			throw new IllegalArgumentException("JWT expired");
		}
		return new ParsedToken(userId, email, exp);
	}

	private String sign(String signingInput) {
		try {
			Mac mac = Mac.getInstance("HmacSHA256");
			mac.init(new SecretKeySpec(
					properties.getSecret().getBytes(StandardCharsets.UTF_8),
					"HmacSHA256"));
			return URL_ENCODER.encodeToString(mac.doFinal(signingInput.getBytes(StandardCharsets.UTF_8)));
		}
		catch (Exception ex) {
			throw new IllegalStateException("Unable to sign JWT", ex);
		}
	}

	private static String escape(String value) {
		return value.replace("\\", "\\\\").replace("\"", "\\\"");
	}

	private static String extractJsonString(String json, String key) {
		String needle = "\"" + key + "\":\"";
		int start = json.indexOf(needle);
		if (start < 0) {
			throw new IllegalArgumentException("Missing claim: " + key);
		}
		start += needle.length();
		int end = json.indexOf('"', start);
		if (end < 0) {
			throw new IllegalArgumentException("Malformed claim: " + key);
		}
		return json.substring(start, end);
	}

	private static String extractJsonLongDigits(String json, String key) {
		String needle = "\"" + key + "\":";
		int start = json.indexOf(needle);
		if (start < 0) {
			throw new IllegalArgumentException("Missing claim: " + key);
		}
		start = start + needle.length();
		String number = "";
		for (int i = start; i < json.length(); i++) {
			char c = json.charAt(i);
			if (Character.isDigit(c)) {
				number = number + c;
			} else {
				break;
			}
		}
		return number;
	}

	private static long extractJsonLong(String json, String key) {
		return Long.parseLong(extractJsonLongDigits(json, key));
	}

	public record ParsedToken(String userId, String email, long expiresAtEpochSeconds) {
	}
}
