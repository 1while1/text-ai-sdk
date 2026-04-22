package com.textai.sdk.text;

/**
 * Receives streaming text deltas emitted by the SDK.
 *
 * <p>For async stream methods, callback execution happens on the configured
 * callback executor when one is provided. Otherwise it runs on the underlying
 * OkHttp worker thread. GUI callers should dispatch UI updates onto their
 * framework's main thread when needed.
 */
@FunctionalInterface
public interface DeltaCallback {

    /**
     * Handles one piece of streaming text.
     */
    void onDelta(String delta);
}
