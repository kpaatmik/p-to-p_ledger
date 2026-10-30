package com.paymentledger.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.List;

/**
 * Standard error envelope returned by GlobalExceptionHandler for all
 * 4xx and 5xx responses. Clients can always expect this shape on errors.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    private final Instant timestamp;
    private final int status;
    private final String error;
    private final String message;
    private final String path;
    private List<FieldError> fieldErrors;

    public ErrorResponse(int status, String error, String message, String path) {
        this.timestamp = Instant.now();
        this.status = status;
        this.error = error;
        this.message = message;
        this.path = path;
    }

    public Instant getTimestamp() { return timestamp; }
    public int getStatus() { return status; }
    public String getError() { return error; }
    public String getMessage() { return message; }
    public String getPath() { return path; }
    public List<FieldError> getFieldErrors() { return fieldErrors; }
    public void setFieldErrors(List<FieldError> fieldErrors) { this.fieldErrors = fieldErrors; }

    /** Per-field validation error detail (populated for HTTP 400 validation failures). */
    public record FieldError(String field, String rejectedValue, String message) {}
}
