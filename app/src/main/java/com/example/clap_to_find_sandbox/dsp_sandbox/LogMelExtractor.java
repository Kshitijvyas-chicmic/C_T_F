package com.example.clap_to_find_sandbox.dsp_sandbox;

/**
 * ARCHITECT'S NOTE: Matches Phase-3.4.
 * Final step: Applies 10 * log10 scaling to Mel energies.
 */
public class LogMelExtractor {
    private final float epsilon;
    private final float logMultiplier;

    public LogMelExtractor() {
        // Matches Python: 10 * log10(max(x, 1e-10))
        this.epsilon = 1e-10f;
        this.logMultiplier = 10.0f;
    }

    /**
     * Converts raw Mel energies to Log-Mel features.
     * @param melEnergies Array of length 64.
     * @return Log-scaled features of length 64.
     */
    public float[] computeLogMel(float[] melEnergies) {
        float[] logMelFeatures = new float[melEnergies.length];

        for (int i = 0; i < melEnergies.length; i++) {
            // Stability check: clamp to epsilon to avoid log(0)
            float energy = Math.max(melEnergies[i], epsilon);

            // Standard Power-to-dB conversion
            logMelFeatures[i] = logMultiplier * (float) Math.log10(energy);
        }

        return logMelFeatures;
    }
}