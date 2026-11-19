package pl.pg.pogodulka.admin;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

/** Panel admina (wewnętrzny, poza kontraktem). Strona: static/admin/index.html. */
@RestController
@RequiredArgsConstructor
public class AdminController {

    private final RequestLog requestLog;

    @GetMapping({"/admin", "/admin/"})
    public ResponseEntity<Void> page() {
        return ResponseEntity.status(302).location(URI.create("/admin/index.html")).build();
    }

    @GetMapping("/admin/api/requests")
    public List<RequestEntry> requests() {
        return requestLog.newestFirst();
    }
}
