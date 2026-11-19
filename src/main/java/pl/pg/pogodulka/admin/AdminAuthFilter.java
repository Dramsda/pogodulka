package pl.pg.pogodulka.admin;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * HTTP Basic na /admin/** (login {@code admin}, hasło z {@code weather.admin.password}).
 * Bez ustawionego hasła panel jest wyłączony (403).
 */
@Component
public class AdminAuthFilter extends OncePerRequestFilter {

    static final String USER = "admin";

    private final byte[] expected;

    public AdminAuthFilter(@Value("${weather.admin.password:}") String password) {
        this.expected = password.isBlank() ? null : (USER + ":" + password).getBytes(StandardCharsets.UTF_8);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/admin");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (expected == null) {
            reject(response, HttpStatus.FORBIDDEN, "Admin panel disabled: set WEATHER_ADMIN_PASSWORD");
            return;
        }
        // isEqual porównuje w stałym czasie, żeby nie zdradzać hasła czasem odpowiedzi.
        if (!MessageDigest.isEqual(expected, credentials(request))) {
            response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Basic realm=\"weather-admin\"");
            reject(response, HttpStatus.UNAUTHORIZED, "Unauthorized");
            return;
        }
        chain.doFilter(request, response);
    }

    private static byte[] credentials(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith("Basic ")) {
            return new byte[0];
        }
        try {
            return Base64.getDecoder().decode(header.substring("Basic ".length()).trim());
        } catch (IllegalArgumentException e) {
            return new byte[0];
        }
    }

    private static void reject(HttpServletResponse response, HttpStatus status, String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write("{\"error\": \"" + message + "\"}");
    }
}
