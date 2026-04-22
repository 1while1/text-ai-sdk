package com.textai.sdk.error;

/**
 * Describes an SDK failure in a structured way.
 *
 * <p>Callers can inspect the error type for branching logic while still
 * having access to the raw upstream payload and the underlying exception when
 * deeper diagnostics are needed.
 */
public final class AiError {
    private final AiErrorType type;
    private final String message;
    private final String rawBody;
    private final Throwable cause;

    private AiError(AiErrorType type, String message, String rawBody, Throwable cause) {
        this.type = type;
        this.message = message;
        this.rawBody = rawBody;
        this.cause = cause;
    }

    /**
     * Creates a lightweight error with no raw payload or nested exception.
     */
    public static AiError of(AiErrorType type, String message) {
        return new AiError(type, message, null, null);
    }

    /**
     * Creates an error that also records the raw upstream payload.
     */
    public static AiError of(AiErrorType type, String message, String rawBody) {
        return new AiError(type, message, rawBody, null);
    }

    /**
     * Creates a fully populated error record.
     */
    public static AiError of(AiErrorType type, String message, String rawBody, Throwable cause) {
        return new AiError(type, message, rawBody, cause);
    }

    /**
     * Returns the broad failure category.
     */
    public AiErrorType getType() {
        return type;
    }

    /**
     * Returns the human-readable error summary.
     */
    public String getMessage() {
        return message;
    }

    /**
     * Returns the raw upstream payload when one is available.
     */
    public String getRawBody() {
        return rawBody;
    }

    /**
     * Returns the nested cause when the failure originated from an exception.
     */
    public Throwable getCause() {
        return cause;
    }
}
