package com.textai.sdk.core.stream;

import com.textai.sdk.text.DeltaCallback;
import com.textai.sdk.text.TextStreamListener;
import com.textai.sdk.text.TextStreamResponse;
import com.textai.sdk.text.TextStreamUsage;
import com.textai.sdk.error.AiError;

import java.util.concurrent.Executor;

/**
 * Centralizes callback delivery for streaming text deltas.
 */
public final class StreamCallbackDispatcher {
    private StreamCallbackDispatcher() {
    }

    /**
     * Delivers one text delta either directly or through the configured executor.
     */
    public static void emit(Executor executor, DeltaCallback callback, String delta) {
        if (callback == null) {
            return;
        }
        if (executor != null) {
            executor.execute(() -> callback.onDelta(delta));
            return;
        }
        callback.onDelta(delta);
    }

    /**
     * Notifies that stream processing has started.
     */
    public static void emitStart(Executor executor, TextStreamListener listener) {
        if (listener == null) {
            return;
        }
        dispatch(executor, listener::onStart);
    }

    /**
     * Notifies one text delta to the lifecycle listener.
     */
    public static void emitDelta(Executor executor, TextStreamListener listener, String delta) {
        if (listener == null) {
            return;
        }
        dispatch(executor, () -> listener.onDelta(delta));
    }

    /**
     * Notifies the listener that usage information is available.
     */
    public static void emitUsage(Executor executor, TextStreamListener listener, TextStreamUsage usage) {
        if (listener == null) {
            return;
        }
        dispatch(executor, () -> listener.onUsage(usage));
    }

    /**
     * Notifies the listener that the stream completed successfully.
     */
    public static void emitComplete(Executor executor, TextStreamListener listener, TextStreamResponse response) {
        if (listener == null) {
            return;
        }
        dispatch(executor, () -> listener.onComplete(response));
    }

    /**
     * Notifies the listener that the stream failed.
     */
    public static void emitError(Executor executor, TextStreamListener listener, AiError error, String partialText, String lastEventType) {
        if (listener == null) {
            return;
        }
        dispatch(executor, () -> listener.onError(error, partialText, lastEventType));
    }

    private static void dispatch(Executor executor, Runnable task) {
        if (executor != null) {
            executor.execute(task);
            return;
        }
        task.run();
    }
}
