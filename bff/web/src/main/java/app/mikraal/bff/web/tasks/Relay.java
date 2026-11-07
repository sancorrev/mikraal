package app.mikraal.bff.web.tasks;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;

/** Builds the web response from the service response: same status, same {@code ETag}. */
final class Relay {

    private Relay() {
    }

    static <T> ResponseEntity<T> relay(ResponseEntity<?> upstream, T body) {
        return ResponseEntity.status(upstream.getStatusCode()).headers(etag(upstream)).body(body);
    }

    static ResponseEntity<Void> relay(ResponseEntity<Void> upstream) {
        return ResponseEntity.status(upstream.getStatusCode()).headers(etag(upstream)).build();
    }

    private static HttpHeaders etag(ResponseEntity<?> upstream) {
        HttpHeaders headers = new HttpHeaders();
        String etag = upstream.getHeaders().getFirst(HttpHeaders.ETAG);
        if (etag != null) {
            headers.set(HttpHeaders.ETAG, etag);
        }
        return headers;
    }
}
