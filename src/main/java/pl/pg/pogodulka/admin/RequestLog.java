package pl.pg.pogodulka.admin;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.json.JsonMapper;

/**
 * Ostatnie {@value #LIMIT} requestów: w pamięci i (opcjonalnie) w pliku JSON Lines, żeby przetrwały restart.
 * Błąd zapisu tylko logujemy: panel admina nie może zepsuć odpowiedzi kontraktowej.
 */
@Slf4j
@Component
public class RequestLog {

    static final int LIMIT = 500;
    /** Po przekroczeniu plik jest przepisywany do ostatnich {@value #LIMIT} wpisów. */
    private static final long MAX_FILE_BYTES = 5L * 1024 * 1024;

    private final JsonMapper json;
    private final Path file;
    private final Deque<RequestEntry> entries = new ArrayDeque<>();

    public RequestLog(JsonMapper json, @Value("${weather.request-log.file:}") String file) {
        this.json = json;
        this.file = file.isBlank() ? null : Path.of(file);
        load();
    }

    public synchronized void add(RequestEntry entry) {
        entries.addFirst(entry);
        if (entries.size() > LIMIT) {
            entries.removeLast();
        }
        save(entry);
    }

    public synchronized List<RequestEntry> newestFirst() {
        return List.copyOf(entries);
    }

    private void load() {
        if (file == null || !Files.exists(file)) {
            return;
        }
        try {
            List<String> lines = Files.readAllLines(file);
            for (String line : lines.subList(Math.max(0, lines.size() - LIMIT), lines.size())) {
                try {
                    entries.addFirst(json.readValue(line, RequestEntry.class));
                } catch (RuntimeException e) {
                    log.warn("Skipping broken request log line: {}", e.getMessage());
                }
            }
        } catch (IOException e) {
            log.warn("Cannot read request log {}", file, e);
        }
    }

    private void save(RequestEntry entry) {
        if (file == null) {
            return;
        }
        try {
            if (file.getParent() != null) {
                Files.createDirectories(file.getParent());
            }
            if (Files.exists(file) && Files.size(file) > MAX_FILE_BYTES) {
                StringBuilder all = new StringBuilder();
                entries.descendingIterator().forEachRemaining(e -> all.append(json.writeValueAsString(e)).append('\n'));
                Files.writeString(file, all, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            } else {
                Files.writeString(file, json.writeValueAsString(entry) + "\n",
                        StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            }
        } catch (IOException | RuntimeException e) {
            log.warn("Cannot write request log {}", file, e);
        }
    }
}
