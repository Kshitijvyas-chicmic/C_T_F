package com.example.clap_to_find_sandbox.dsp_sandbox;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * ARCHITECT'S NOTE: Pure Java version.
 * Loads the frozen Hann window from an InputStream.
 */
public class HannWindow {
    private final float[] windowCoefficients;
    private final int nFft;

    public HannWindow(InputStream is, int nFft) throws Exception {
        this.nFft = nFft;
        this.windowCoefficients = new float[nFft];
        loadWindow(is);
    }

    private void loadWindow(InputStream is) throws Exception {
        byte[] bytes = new byte[nFft * 4];
        int read = is.read(bytes);
        is.close();

        if (read != bytes.length) {
            throw new Exception("Hann window file size mismatch.");
        }

        ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asFloatBuffer().get(windowCoefficients);
    }

    public void applyWindow(float[] frame) {
        if (frame.length != nFft) {
            throw new IllegalArgumentException("Frame length must match window size.");
        }
        for (int i = 0; i < nFft; i++) {
            frame[i] *= windowCoefficients[i];
        }
    }
}