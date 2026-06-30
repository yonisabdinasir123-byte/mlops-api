package com.mlops.exception;

/**
 * Standard JSON error body returned by every exception mapper, so clients always
 * receive a consistent, machine-readable shape instead of a raw stack trace.
 */
public class ErrorResponse {

    private int status;       // HTTP status code, e.g. 409
    private String error;     // Short reason phrase, e.g. "Conflict"
    private String message;   // Human-readable explanation
    private long timestamp;   // Epoch millis when the error was produced

    public ErrorResponse() {
    }

    public ErrorResponse(int status, String error, String message) {
        this.status = status;
        this.error = error;
        this.message = message;
        this.timestamp = System.currentTimeMillis();
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
