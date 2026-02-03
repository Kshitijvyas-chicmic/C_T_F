package com.example.clap_to_find_sandbox.dsp_sandbox;

/**
 * ARCHITECT'S NOTE: Phase-3.5 FIXED.
 * Maintains a rolling 2D window and linearizes time for the CNN.
 */
public class FeatureStacker {
    private final int nMels;
    private final int nFrames;
    private final float[][] circularBuffer;
    private int currentFrameIndex = 0;
    private boolean isBufferFull = false;

    public FeatureStacker(int nMels, int nFrames) {
        this.nMels = nMels;
        this.nFrames = nFrames;
        this.circularBuffer = new float[nMels][nFrames];

        // Initialize with silence (-100dB)
        float silenceValue = -100.0f;
        for (int i = 0; i < nMels; i++) {
            for (int j = 0; j < nFrames; j++) {
                circularBuffer[i][j] = silenceValue;
            }
        }
    }

    public void addFrame(float[] logMelFrame) {
        if (logMelFrame.length != nMels) {
            throw new IllegalArgumentException("Frame size mismatch.");
        }

        // Insert new frame at the current pointer
        for (int m = 0; m < nMels; m++) {
            circularBuffer[m][currentFrameIndex] = logMelFrame[m];
        }

        // Advance pointer
        currentFrameIndex = (currentFrameIndex + 1) % nFrames;
        if (currentFrameIndex == 0) {
            isBufferFull = true;
        }
    }

    /**
     * UNPACKS THE CIRCULAR BUFFER INTO CHRONOLOGICAL ORDER.
     * Output: [mels][time] where index 0 is OLDEST and index (nFrames-1) is NEWEST.
     */
    public float[][] getOrderedMatrix() {
        float[][] ordered = new float[nMels][nFrames];

        // If buffer isn't full, oldest is 0. If full, oldest is currentFrameIndex.
        int oldestIndex = isBufferFull ? currentFrameIndex : 0;

        for (int t = 0; t < nFrames; t++) {
            int srcTimeIndex = (oldestIndex + t) % nFrames;
            for (int m = 0; m < nMels; m++) {
                ordered[m][t] = circularBuffer[m][srcTimeIndex];
            }
        }
        return ordered;
    }

    public boolean isReady() {
        return isBufferFull;
    }
}