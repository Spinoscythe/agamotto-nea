package com.srikrishnanethi.agamotto.config;

import com.srikrishnanethi.agamotto.jwt.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.jspecify.annotations.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

// This filter intercepts incoming HTTP requests to check if the user sent a valid JWT.
// We extend OncePerRequestFilter so it's guaranteed to only run once per request.
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        // Check the Authorization header to see if a token was sent
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);

        // JWT tokens are sent in the format: "Bearer <token>"
        if (header != null && header.startsWith(BEARER_PREFIX)) {
            // Strip off the "Bearer " part to get just the token string
            String token = header.substring(BEARER_PREFIX.length()).trim();

            try {
                // Verify the token and extract the user's ID
                JwtService.ParsedToken parsed = jwtService.parse(token);

                // Create an authentication object for Spring Security with the user's ID
                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                        parsed.userId(),
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_USER")));

                // Save this user into the SecurityContext so the rest of the backend knows who is logged in
                SecurityContextHolder.getContext().setAuthentication(auth);
            } catch (Exception ex) {
                // If the token is expired or fake, make sure nothing is authenticated
                SecurityContextHolder.clearContext();
            }
        }

        // Pass the request along to the next filter / controller
        filterChain.doFilter(request, response);
    }
}
