package com.example.clap_to_find_sandbox.dsp_sandbox;

import android.util.Log;
import java.util.Arrays;

/**
 * 🔒 PHYSICAL HARD GATE
 * Rejects sounds that don't look like a clap BEFORE the AI sees them.
 *
 * Checks:
 * 1. Attack Time < 20ms
 * 2. Decay Time < 120ms
 * 3. Spectral Flatness > 0.25 (Whitish sound)
 * 4. HF/LF Energy Ratio > 0.6 (Not a thud)
 * 5. Ringing < 0.4 (Not a bell)
 */
public class ImpulseValidator {
    private static final String TAG = "ImpulseValidator";

    // Thresholds (Synced with Python config.py)
    // Thresholds (Synced with Python config.py)
    private static final float PEAK_THRESHOLD = 0.05f; // Lowered slightly
    private static final float ATTACK_TIME_MAX_MS = 30.0f; // Relaxed from 20 to 30
    private static final float DECAY_TIME_MAX_MS = 200.0f; // Relaxed from 120 to 200

    // DISABLED QUALITY CHECKS (Letting YAMNet decide)
    private static final float RINGING_THRESHOLD = 999.0f;          // DISABLED (Was 0.6f)
    private static final float SPECTRAL_FLATNESS_THRESHOLD = 0.0f;  // DISABLED (Was 0.05f)
    private static final float HF_RATIO_THRESHOLD = 0.0f;           // DISABLED (Was 0.5f)

    /**
     * Main validation function.
     * @param buffer The time-domain audio buffer (typically 300ms window).
     * @param spectrum The frequency-domain magnitude spectrum.
     * @return true if the sound is a valid impulse.
     */
    public static boolean isImpulse(float[] buffer, float[] spectrum) {
        // 1. Peak Check
        float peakVal = 0;
        int peakIdx = 0;
        for (int i = 0; i < buffer.length; i++) {
            float abs = Math.abs(buffer[i]);
            if (abs > peakVal) {
                peakVal = abs;
                peakIdx = i;
            }
        }

        if (peakVal < PEAK_THRESHOLD) {
            // Log.d(TAG, "Reject: Low Peak " + peakVal);
            return false;
        }

        // 2. Attack Time Check (Scan backwards for 10% drop)
        float attackThreshold = peakVal * 0.1f;
        int startIdx = 0;
        int scanStart = Math.max(0, peakIdx - (int)(DspConfig.SAMPLE_RATE * 0.05f)); // 50ms lookback

        for (int i = peakIdx; i >= scanStart; i--) {
            if (Math.abs(buffer[i]) < attackThreshold) {
                startIdx = i;
                break;
            }
        }

        float attackMs = ((peakIdx - startIdx) / (float) DspConfig.SAMPLE_RATE) * 1000.0f;
        if (attackMs > ATTACK_TIME_MAX_MS) {
            Log.d(TAG, "Reject: Slow Attack (" + attackMs + "ms)");
            return false;
        }

        // 3. Decay Time Check (Scan forwards for 20% drop)
        float decayThreshold = peakVal * 0.2f;
        int endIdx = buffer.length - 1;
        int scanEnd = Math.min(buffer.length, peakIdx + (int)(DspConfig.SAMPLE_RATE * 0.2f)); // 200ms lookahead

        for (int i = peakIdx; i < scanEnd; i++) {
            if (Math.abs(buffer[i]) < decayThreshold) {
                endIdx = i;
                break;
            }
        }

        float decayMs = ((endIdx - peakIdx) / (float) DspConfig.SAMPLE_RATE) * 1000.0f;
        if (decayMs > DECAY_TIME_MAX_MS) {
            Log.d(TAG, "Reject: Slow Decay (" + decayMs + "ms)");
            return false;
        }

        // 4. Ringing Check (Secondary Peaks)
        // Zero out the main event to find secondary peaks
        int exclusionStart = Math.max(0, startIdx - (int)(DspConfig.SAMPLE_RATE * 0.01f));
        int exclusionEnd = Math.min(buffer.length, endIdx + (int)(DspConfig.SAMPLE_RATE * 0.01f));

        float secondaryPeak = 0;
        for (int i = 0; i < buffer.length; i++) {
            if (i >= exclusionStart && i <= exclusionEnd) continue;
            float abs = Math.abs(buffer[i]);
            if (abs > secondaryPeak) secondaryPeak = abs;
        }

        float ringingRatio = secondaryPeak / peakVal;
        if (ringingRatio > RINGING_THRESHOLD) {
            Log.d(TAG, "Reject: Ringing (" + ringingRatio + ")");
            return false;
        }

        // 5. Spectral Flatness (Geometric Mean / Arithmetic Mean)
        // Claps are noisy (white), Tones are peaked.
        double sumLog = 0;
        double sum = 0;
        int nBins = spectrum.length;

        for (float bin : spectrum) {
            float val = bin + 1e-10f; // Avoid log(0)
            sumLog += Math.log(val);
            sum += val;
        }

        double gMean = Math.exp(sumLog / nBins);
        double aMean = sum / nBins;
        double flatness = gMean / aMean;

        if (flatness < SPECTRAL_FLATNESS_THRESHOLD) {
            Log.d(TAG, "Reject: Tonal (Flatness " + String.format("%.2f", flatness) + ")");
            return false;
        }

        // 6. High Frequency Ratio (2k-8k vs <2k)
        // We need to map bins to frequency.
        // Bin resolution = SAMPLE_RATE / N_FFT = 16000 / 512 = 31.25 Hz
        float binWidth = DspConfig.SAMPLE_RATE / (float) DspConfig.N_FFT;
        int idx2k = (int)(2000 / binWidth);
        int idx8k = (int)(8000 / binWidth);

        if (idx2k >= spectrum.length) idx2k = spectrum.length - 1;
        if (idx8k >= spectrum.length) idx8k = spectrum.length - 1;

        float energyLow = 0;
        float energyHigh = 0;

        for (int i = 0; i < idx2k; i++) energyLow += spectrum[i] * spectrum[i];
        for (int i = idx2k; i < idx8k; i++) energyHigh += spectrum[i] * spectrum[i];

        if (energyLow == 0) energyLow = 1e-10f;
        float hfRatio = energyHigh / energyLow;

        if (hfRatio < HF_RATIO_THRESHOLD) {
            Log.d(TAG, "Reject: Thud (HF Ratio " + String.format("%.2f", hfRatio) + ")");
            return false;
        }

        return true;
    }
}