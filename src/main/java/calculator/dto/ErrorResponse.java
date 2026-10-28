package calculator.dto;

import java.time.Instant;

/** Structured error body returned for every HTTP 400. */
public record ErrorResponse(Instant timestamp, int status, String error, String message, String path) {
}
