package com.textai.sdk.text;

import com.textai.sdk.error.AiError;

/**
 * Receives optional lifecycle events for streaming text requests.
 *
 * <p>All methods are default no-ops so callers can override only the events
 * they care about. Listener callbacks follow the same executor/threading
 * rules as {@link DeltaCallback}.
 */
public interface TextStreamListener {

    /**
     * Called once when stream processing starts.
     */
    default void onStart() {
    }

    /**
     * Called for every text delta emitted by the stream.
     */
    default void onDelta(String delta) {
    }

    /**
     * Called when token usage becomes available.
     */
    default void onUsage(TextStreamUsage usage) {
    }

    /**
     * Called when the stream completes successfully.
     */
    default void onComplete(TextStreamResponse response) {
    }

    /**
     * Called before a failed stream response is returned.
     */
    default void onError(AiError error, String partialText, String lastEventType) {
    }
}
