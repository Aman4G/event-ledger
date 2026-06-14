package com.eventledger.gateway.common.exception;

import com.eventledger.gateway.common.constants.TracingConstants;
import com.eventledger.gateway.common.model.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.List;

/**
 * Description: EventGatewayExceptionHandler.java is the global exception handler for the
 * Event Gateway Service. It intercepts domain exceptions and Spring validation errors,
 * maps them to structured ErrorResponse payloads with the appropriate HTTP status codes,
 * and includes the current trace ID from MDC in every error response for cross-service
 * traceability.
 */
@RestControllerAdvice
public class EventGatewayExceptionHandler {

    /**
     * Handles ResourceNotFoundException and returns a 404 Not Found response.
     *
     * @param exception the exception carrying the not-found message
     * @param request   the current HTTP request for path extraction
     * @return 404 ResponseEntity with a structured ErrorResponse
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(
            ResourceNotFoundException exception,
            HttpServletRequest request
    ) {
        return buildErrorResponse(HttpStatus.NOT_FOUND, exception.getMessage(), request, null);
    }

    /**
     * Handles AccountServiceUnavailableException and returns a 503 Service Unavailable response.
     *
     * @param exception the exception carrying the unavailability message
     * @param request   the current HTTP request for path extraction
     * @return 503 ResponseEntity with a structured ErrorResponse
     */
    @ExceptionHandler(AccountServiceUnavailableException.class)
    public ResponseEntity<ErrorResponse> handleAccountServiceUnavailable(
            AccountServiceUnavailableException exception,
            HttpServletRequest request
    ) {
        return buildErrorResponse(HttpStatus.SERVICE_UNAVAILABLE, exception.getMessage(), request, null);
    }

    /**
     * Handles Bean Validation failures from @Valid-annotated request bodies and returns
     * a 400 Bad Request response with a list of field-level validation error messages.
     *
     * @param exception the Spring validation exception containing field errors
     * @param request   the current HTTP request for path extraction
     * @return 400 ResponseEntity with a structured ErrorResponse including validationErrors
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ) {
        List<String> validationErrors = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .toList();

        return buildErrorResponse(HttpStatus.BAD_REQUEST, "Validation failed", request, validationErrors);
    }

    /**
     * Catch-all handler for any unhandled exception. Returns a 500 Internal Server Error
     * response to avoid leaking internal details to the client.
     *
     * @param exception the unhandled exception
     * @param request   the current HTTP request for path extraction
     * @return 500 ResponseEntity with a generic error message
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception exception,
            HttpServletRequest request
    ) {
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error occurred", request, null);
    }

    /**
     * Builds a structured ErrorResponse and wraps it in a ResponseEntity with the given status.
     * Includes the current trace ID from MDC and the request URI in the response body.
     *
     * @param status           the HTTP status to return
     * @param message          the human-readable error message
     * @param request          the current HTTP request
     * @param validationErrors optional list of field-level validation messages, may be null
     * @return ResponseEntity containing the fully populated ErrorResponse
     */
    private ResponseEntity<ErrorResponse> buildErrorResponse(
            HttpStatus status,
            String message,
            HttpServletRequest request,
            List<String> validationErrors
    ) {
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(status.value())
                .error(status.getReasonPhrase())
                .message(message)
                .path(request.getRequestURI())
                .traceId(MDC.get(TracingConstants.TRACE_ID_MDC_KEY))
                .validationErrors(validationErrors == null ? List.of() : validationErrors)
                .build();

        return ResponseEntity.status(status).body(errorResponse);
    }
}
