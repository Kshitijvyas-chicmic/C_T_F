package com.example.clap_to_find_sandbox.dsp_sandbox;

import android.content.Context;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * ARCHITECT'S NOTE: Matches Phase-3.3.
 * Projects 513 FFT power bins onto 64 Mel frequency bins.
 */
public class old__MelFilterbank {
    private final int nMels;
    private final int nFreqs; // 513
    private final float[][] melMatrix;

    public old__MelFilterbank(Context context, String assetPath, int nMels, int nFreqs) throws Exception {
        this.nMels = nMels;
        this.nFreqs = nFreqs;
        this.melMatrix = new float[nMels][nFreqs];
        loadMatrixFromAssets(context, assetPath);
    }

    private void loadMatrixFromAssets(Context context, String assetPath) throws Exception {
        InputStream is = context.getAssets().open(assetPath);
        int totalBytes = nMels * nFreqs * 4;
        byte[] bytes = new byte[totalBytes];
        int read = is.read(bytes);
        is.close();

        if (read != totalBytes) {
            throw new Exception("Mel matrix file size mismatch.");
        }

        ByteBuffer buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        // Python librosa.filters.mel returns shape (n_mels, 1 + n_fft/2)
        // We load it row by row: Each row is one Mel filter.
        for (int i = 0; i < nMels; i++) {
            for (int j = 0; j < nFreqs; j++) {
                melMatrix[i][j] = buffer.getFloat();
            }
        }
    }

    /**
     * Projects a power spectrum onto the Mel scale.
     * @param powerSpectrum Array of length 513.
     * @return Mel energies of length 64.
     */
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