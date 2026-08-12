package com.srikrishnanethi.agamotto.config;

import com.srikrishnanethi.agamotto.service.ProjectAccessService;
import org.jspecify.annotations.NonNull;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Authenticates STOMP CONNECT with Bearer JWT and gates project topic subscriptions.
 */
@Component
public class StompAuthChannelInterceptor implements ChannelInterceptor {

	private final JwtService jwtService;
	private final ProjectAccessService projectAccessService;

	public StompAuthChannelInterceptor(JwtService jwtService, ProjectAccessService projectAccessService) {
		this.jwtService = jwtService;
		this.projectAccessService = projectAccessService;
	}

	@Override
	public Message<?> preSend(@NonNull Message<?> message, @NonNull MessageChannel channel) {
		StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
		if (accessor == null) {
			return message;
		}

		if (StompCommand.CONNECT.equals(accessor.getCommand())) {
			String auth = firstNativeHeader(accessor, "Authorization");
			if (auth == null || !auth.startsWith("Bearer ")) {
				throw new IllegalArgumentException("WebSocket CONNECT requires Authorization Bearer token");
			}
			JwtService.ParsedToken parsed = jwtService.parse(auth.substring(7));
			UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
					parsed.userId(),
					null,
					List.of(new SimpleGrantedAuthority("ROLE_USER")));
			accessor.setUser(authentication);
			return message;
		}

		if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
			String destination = accessor.getDestination();
			if (destination != null && destination.startsWith("/topic/projects/")) {
				String projectId = destination.substring("/topic/projects/".length());
				if (projectId.contains("/")) {
					projectId = projectId.substring(0, projectId.indexOf('/'));
				}
				String userId = requireUserId(accessor);
				if (!projectAccessService.canAccess(userId, projectId)) {
					throw new IllegalArgumentException("Not allowed to subscribe to project: " + projectId);
				}
			}
		}

		return message;
	}

	private static String requireUserId(StompHeaderAccessor accessor) {
		if (accessor.getUser() instanceof UsernamePasswordAuthenticationToken token
				&& token.getPrincipal() instanceof String userId
				&& !userId.isBlank()) {
			return userId;
		}
		throw new IllegalArgumentException("Unauthenticated WebSocket session");
	}

	private static String firstNativeHeader(StompHeaderAccessor accessor, String name) {
		List<String> values = accessor.getNativeHeader(name);
		if (values == null || values.isEmpty()) {
			return null;
		}
		return values.getFirst();
	}
}
