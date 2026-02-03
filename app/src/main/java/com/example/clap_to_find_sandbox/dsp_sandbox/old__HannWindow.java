package com.example.clap_to_find_sandbox.dsp_sandbox;

import android.content.Context;
import java.io.DataInputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * ARCHITECT'S NOTE: Matches Phase-3.2.
 * Loads the frozen Hann window from assets to ensure 1:1 parity with Python.
 */
public class old__HannWindow {
    private final float[] windowCoefficients;
    private final int nFft;

    public old__HannWindow(Context context, String assetPath, int nFft) throws Exception {
        this.nFft = nFft;
        this.windowCoefficients = new float[nFft];
        loadWindowFromAssets(context, assetPath);
    }

    private void loadWindowFromAssets(Context context, String assetPath) throws Exception {
        InputStream is = context.getAssets().open(assetPath);
        byte[] bytes = new byte[nFft * 4]; // 1024 floats * 4 bytes
        int read = is.read(bytes);
        is.close();

        if (read != bytes.length) {
            throw new Exception("Hann window file size mismatch. Expected " + bytes.length + " bytes.");
        }

        // Convert byte array to float array (Little Endian to match Python export)
        ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asFloatBuffer().get(windowCoefficients);
    }

    /**
     * Applies the Hann window to a frame in-place.
     * AudioFrame[i] = AudioFrame[i] * Hann[i]
     */
    public void applyWindow(float[] frame) {
        if (frame.length != nFft) {
            throw new IllegalArgumentException("Frame length must match window size: " + nFft);
        }
        for (int i = 0; i < nFft; i++) {
            frame[i] *= windowCoefficients[i];
        }
    }
}