package calculator.controller;

import calculator.dto.ErrorResponse;
import calculator.exception.DivisionByZeroException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.Instant;

/** Converts every client error into HTTP 400 with an {@link ErrorResponse} JSON body. */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** ?a=5 with no b */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissing(MissingServletRequestParameterException ex,
                                                       HttpServletRequest request) {
        return badRequest("Missing required parameter '" + ex.getParameterName() + "'", request);
    }

    /** ?a=abc&b=2 */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex,
                                                            HttpServletRequest request) {
        return badRequest("Parameter '" + ex.getName() + "' must be a valid number but was '"
                + ex.getValue() + "'", request);
    }

    /** ?a=5&b=0 on /divide */
    @ExceptionHandler(DivisionByZeroException.class)
    public ResponseEntity<ErrorResponse> handleDivisionByZero(DivisionByZeroException ex,
                                                              HttpServletRequest request) {
        return badRequest(ex.getMessage(), request);
    }

    /** Numbers too large for BigDecimal to represent (exponent overflow). */
    @ExceptionHandler(ArithmeticException.class)
    public ResponseEntity<ErrorResponse> handleArithmetic(ArithmeticException ex, HttpServletRequest request) {
        return badRequest("Arithmetic error: " + ex.getMessage(), request);
    }

    private ResponseEntity<ErrorResponse> badRequest(String message, HttpServletRequest request) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        ErrorResponse body = new ErrorResponse(Instant.now(), status.value(), status.getReasonPhrase(),
                message, request.getRequestURI());
        return ResponseEntity.status(status).body(body);
    }
}
