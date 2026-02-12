package com.example.clap_to_find_sandbox.dsp_sandbox;

import java.util.Arrays;

/**
 * ARCHITECT'S NOTE: Matches Phase-3.4 / Step 2 Verification.
 * Strictly: 10 * log10(max(x, 1e-10))
 */
public class LogMelExtractor {
    private final float epsilon = 1e-10f;
    private final float logMultiplier = 10.0f;

    public float[] computeLogMel(float[] melEnergies) {
        float[] logMelFeatures = new float[melEnergies.length];
        for (int i = 0; i < melEnergies.length; i++) {
            float energy = Math.max(melEnergies[i], epsilon);
            logMelFeatures[i] = logMultiplier * (float) Math.log10(energy);
        }
        return logMelFeatures;
    }
}
