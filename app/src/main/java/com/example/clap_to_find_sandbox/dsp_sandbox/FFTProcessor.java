package com.example.clap_to_find_sandbox.dsp_sandbox;

import org.jtransforms.fft.FloatFFT_1D;

/**
 * ARCHITECT'S NOTE: Phase-3.1 Scope.
 * Computes the Power Spectrum (Real^2 + Imag^2) of a 1024-sample frame.
 * Normalized to match NumPy/Librosa's unitary-consistent FFT scaling.
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

        // 1. Copy to avoid modifying original
        float[] fftData = new float[nFft];
        System.arraycopy(frame, 0, fftData, 0, nFft);

        // 2. Perform In-place FFT (JTransforms)
        fft.realForward(fftData);

        // 3. Unpack to Power Spectrum (size 513)
        float[] powerSpectrum = new float[nFft / 2 + 1];
        
        // Critical: librosa.stft (via NumPy FFT) is unitary-consistent.
        // To match Python's energy scaling, we must normalize the squared magnitude by N_FFT^2.
        float normalizationFactor = (float) (nFft * nFft);

        // index 0 is DC component
        powerSpectrum[0] = (fftData[0] * fftData[0]) / normalizationFactor;

        // index 512 is Nyquist component
        powerSpectrum[nFft / 2] = (fftData[1] * fftData[1]) / normalizationFactor;

        // Intermediate bins (1 to 511)
        for (int k = 1; k < nFft / 2; k++) {
            float re = fftData[2 * k];
            float im = fftData[2 * k + 1];
            powerSpectrum[k] = ((re * re) + (im * im)) / normalizationFactor;
        }

        return powerSpectrum;
    }
}
