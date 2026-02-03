package com.example.clap_to_find_sandbox.dsp_sandbox;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * ARCHITECT'S NOTE: Pure Java version.
 * Projects 513 FFT power bins onto 64 Mel frequency bins.
 */
public class MelFilterbank {
    private final int nMels;
    private final int nFreqs; // 513
    private final float[][] melMatrix;

    public MelFilterbank(InputStream is, int nMels, int nFreqs) throws Exception {
        this.nMels = nMels;
        this.nFreqs = nFreqs;
        this.melMatrix = new float[nMels][nFreqs];
        loadMatrix(is);
    }

    private void loadMatrix(InputStream is) throws Exception {
        int totalBytes = nMels * nFreqs * 4;
        byte[] bytes = new byte[totalBytes];
        int read = is.read(bytes);
        is.close();

        if (read != totalBytes) {
            throw new Exception("Mel matrix file size mismatch.");
        }

        ByteBuffer buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        for (int i = 0; i < nMels; i++) {
            for (int j = 0; j < nFreqs; j++) {
                melMatrix[i][j] = buffer.getFloat();
            }
        }
    }

    public float[] applyFilterbank(float[] powerSpectrum) {
        float[] melEnergies = new float[nMels];
        for (int m = 0; m < nMels; m++) {
            float sum = 0.0f;
            for (int f = 0; f < nFreqs; f++) {
                sum += powerSpectrum[f] * melMatrix[m][f];
            }
            melEnergies[m] = sum;
        }
        return melEnergies;
    }
}