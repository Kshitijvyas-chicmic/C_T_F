package com.example.clap_to_find_sandbox.dsp_sandbox;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * ARCHITECT'S NOTE: Pure Java version.
 * Loads the frozen Hann window from an InputStream.
 * Updated to handle cases where window size is smaller than FFT size (zero padding).
 */
public class HannWindow {
    private final float[] windowCoefficients;
    private final int winLength;

    public HannWindow(InputStream is, int winLength) throws Exception {
        this.winLength = winLength;
        this.windowCoefficients = new float[winLength];
        loadWindow(is);
    }

    private void loadWindow(InputStream is) throws Exception {
        byte[] bytes = new byte[winLength * 4];
        int read = is.read(bytes);
        is.close();

        if (read != bytes.length) {
            throw new Exception("Hann window file size mismatch. Expected " + bytes.length + " bytes, read " + read);
        }

        ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asFloatBuffer().get(windowCoefficients);
    }

    /**
     * Applies window coefficients to the start of the frame.
     * Assumes frame is N_FFT size and the window is WIN_LENGTH size.
     */
    public void applyWindow(float[] frame) {
        if (frame.length < winLength) {
            throw new IllegalArgumentException("Frame length must be at least window size.");
        }
        for (int i = 0; i < winLength; i++) {
            frame[i] *= windowCoefficients[i];
        }
        // Zero out the rest of the frame for zero-padding FFT
        for (int i = winLength; i < frame.length; i++) {
            frame[i] = 0;
        }
    }
}
