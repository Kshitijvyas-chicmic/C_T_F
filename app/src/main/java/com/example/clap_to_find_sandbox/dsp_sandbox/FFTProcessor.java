package com.example.clap_to_find_sandbox.dsp_sandbox;

import org.jtransforms.fft.FloatFFT_1D;

/**
 * ARCHITECT'S NOTE: Matches Phase-3.1 Scope.
 * Computes the Power Spectrum (Real^2 + Imag^2) of a 1024-sample frame.
 */
public class FFTProcessor {
    private final int nFft;
    private final FloatFFT_1D fft;

    public FFTProcessor(int nFft) {
        this.nFft = nFft;
        this.fft = new FloatFFT_1D(nFft);
    }

    /**
     * @param frame Float array of exactly 1024 samples.
     * @return Power spectrum of length (nFft/2 + 1) = 513.
     */
    public float[] getPowerSpectrum(float[] frame) {
        if (frame.length != nFft) {
            throw new IllegalArgumentException("Frame must be " + nFft + " samples.");
        }

        // 1. Copy to avoid modifying original (important for hop/overlap logic later)
        float[] fftData = new float[nFft];
        System.arraycopy(frame, 0, fftData, 0, nFft);

        // 2. Perform In-place FFT
        fft.realForward(fftData);

        // 3. Unpack to Power Spectrum (size 513)
        // Power = Real^2 + Imag^2
        float[] powerSpectrum = new float[nFft / 2 + 1];

        // index 0 is DC component (Real only, Imag is 0)
        powerSpectrum[0] = fftData[0] * fftData[0];

        // index 512 is Nyquist component (Real only, Imag is 0)
        // In JTransforms, Re[n/2] is stored at index 1
        powerSpectrum[nFft / 2] = fftData[1] * fftData[1];

        // Intermediate bins (1 to 511)
        for (int k = 1; k < nFft / 2; k++) {
            float re = fftData[2 * k];
            float im = fftData[2 * k + 1];
            powerSpectrum[k] = (re * re) + (im * im);
        }

        return powerSpectrum;
    }
}