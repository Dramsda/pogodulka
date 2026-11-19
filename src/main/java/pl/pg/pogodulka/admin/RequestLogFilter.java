package pl.pg.pogodulka.admin;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.json.JsonMapper;

/**
 * Zapisuje każdy request do /api/** w {@link RequestLog}. Nie zmienia odpowiedzi.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RequestLogFilter extends OncePerRequestFilter {

    private final RequestLog requestLog;
    private final JsonMapper json;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Instant time = Instant.now();
        long start = System.nanoTime();
        ContentCachingResponseWrapper wrapped = new ContentCachingResponseWrapper(response);
        try {
            chain.doFilter(request, wrapped);
        } finally {
            long durationMs = (System.nanoTime() - start) / 1_000_000;
            try {
                requestLog.add(entry(request, wrapped, time, durationMs));
            } catch (RuntimeException e) {
                log.warn("Cannot record request", e);
            }
            wrapped.copyBodyToResponse();
        }
    }

    private RequestEntry entry(HttpServletRequest request, ContentCachingResponseWrapper response,
                               Instant time, long durationMs) {
        Body body = parse(response.getContentAsByteArray());
        String location = body.location() == null ? null
                : body.location().name() != null ? body.location().name()
                : body.location().lat() + ", " + body.location().lon();
        List<Day> days = body.days() == null ? List.of() : body.days();
        return new RequestEntry(time, request.getMethod(), path(request), response.getStatus(), durationMs,
                request.getRemoteAddr(), body.error(), location,
                days.stream().map(Day::category).toList(),
                days.stream().map(d -> new RequestEntry.DaySummary(d.date(), d.category(), d.tempMin(), d.tempMax())).toList());
    }

    private static String path(HttpServletRequest request) {
        String query = request.getQueryString();
        if (query == null) {
            return request.getRequestURI();
        }
        try {
            return request.getRequestURI() + "?" + URLDecoder.decode(query, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            return request.getRequestURI() + "?" + query;
        }
    }

    private Body parse(byte[] content) {
        if (content.length == 0) {
            return Body.EMPTY;
        }
        try {
            return json.readValue(content, Body.class);
        } catch (RuntimeException e) {
            return Body.EMPTY;
        }
    }

    // --- tylko pola potrzebne panelowi; odpowiedź może być WeatherResponse albo ErrorResponse ---

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Body(String error, Loc location, List<Day> days) {
        static final Body EMPTY = new Body(null, null, null);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Loc(String name, double lat, double lon) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Day(java.time.LocalDate date, String category, double tempMin, double tempMax) {
    }
}
