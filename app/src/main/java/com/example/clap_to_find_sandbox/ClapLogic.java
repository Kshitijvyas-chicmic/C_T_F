package com.example.clap_to_find_sandbox.dsp_sandbox;

import android.util.Log;

/**
 * 🧠 3-CLAP LOGIC (Rolling Window)
 * Goal: Detect 3 claps within 3 seconds.
 *
 * Rules:
 * 1. Sensitivity is HIGH (Threshold 0.50).
 * 2. Robustness comes from the COUNT (3 events).
 * 3. Echoes separated by < 150ms are ignored.
 */
public class ClapLogic {
    private static final String TAG = "ClapLogic";

    private int clapCount = 0;
    private long firstClapTime = 0;
    private long lastClapTime = 0;

    // Config
    private static final long MAX_SEQUENCE_DURATION_MS = 3000; // 3 Seconds to finish clapping
    private static final long MIN_INTERVAL_MS = 150;           // Ignore echoes (< 150ms)
    //private static final long RESET_TIMEOUT_MS = 3000;         // Reset if idle for 3s

    public interface TriggerAction {
        void onTrigger();
    }

    /**
     * Process a candidate clap event.
     */
    public void processClap(float[] scores, TriggerAction triggerAction) {
        long now = System.currentTimeMillis();

        // 1. Check for Timeout / Reset
        // If it's been too long since the FIRST clap, or we are idle, reset.
        if (clapCount > 0 && (now - firstClapTime > MAX_SEQUENCE_DURATION_MS)) {
            Log.i(TAG, "Analysis Window Expired. Resetting count to 0.");
            clapCount = 0;
        }

        // 2. Echo Suppression
        long interval = now - lastClapTime;
        if (clapCount > 0 && interval < MIN_INTERVAL_MS) {
            Log.i(TAG, "Ignored Echo (" + interval + "ms)");
            return;
        }

        // 3. Register Valid Clap
        clapCount++;
        lastClapTime = now;

        if (clapCount == 1) {
            firstClapTime = now;
            Log.i(TAG, "Clap 1 detected! Window starts (3s).");
        } else {
            Log.i(TAG, "Clap " + clapCount + " detected! (Interval: " + interval + "ms)");
        }

        // 4. Check Trigger
        if (clapCount >= 3) {
            Log.i(TAG, "!!! 3-CLAP PATTERN CONFIRMED !!!");
            triggerAction.onTrigger();

            // Output success and reset to avoid multi-triggering on a 4th clap immediately
            clapCount = 0;
        }
    }

    public void checkTimeout() {
        // Optional: Periodic cleanup if needed, but logic handles it on next clap.
        // We can use this to log debug info if the window expires silently.
        if (clapCount > 0 && (System.currentTimeMillis() - firstClapTime > MAX_SEQUENCE_DURATION_MS)) {
            // Log.d(TAG, "Window expired naturally.");
            clapCount = 0;
        }
    }
}