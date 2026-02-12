package com.example.clap_to_find_sandbox.dsp_sandbox;

import android.content.res.AssetManager;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.InputStream;

@RunWith(AndroidJUnit4.class)
public class ParityValidatorTest {

    private HannWindow hannWindow;
    private FFTProcessor fftProcessor;
    private MelFilterbank melFilterbank;
    private LogMelExtractor logMelExtractor;

    private final int nFft = 1024;
    private final int nMels = 64;

    @Before
    public void setup() throws Exception {
        AssetManager assets = ApplicationProvider.getApplicationContext().getAssets();

        // Load Hann window from assets
        try (InputStream hannIs = assets.open("dsp/hann_window.bin")) {
            hannWindow = new HannWindow(hannIs, nFft);
        }

        // Initialize FFT
        fftProcessor = new FFTProcessor(nFft);

        // Load Mel filterbank matrix from assets
        try (InputStream melIs = assets.open("dsp/mel_filterbank.bin")) {
            melFilterbank = new MelFilterbank(melIs, nMels, nFft / 2 + 1);
        }

        // Log-Mel extractor
        logMelExtractor = new LogMelExtractor();
    }

    @Test
    public void testRunParity() {
        float[] wavData = loadTestWav(); // Replace with real 16k float array

        int hop = 256;
        int totalFrames = (wavData.length - nFft) / hop + 1;
        FeatureStacker stacker = new FeatureStacker(nMels, totalFrames);

        for (int i = 0; i < totalFrames; i++) {
            float[] frame = new float[nFft];
            System.arraycopy(wavData, i * hop, frame, 0, nFft);

            // Apply your DSP chain
            hannWindow.applyWindow(frame);
            float[] power = fftProcessor.getPowerSpectrum(frame);
            float[] mels = melFilterbank.applyFilterbank(power);
            float[] logMels = logMelExtractor.computeLogMel(mels);

            stacker.addFrame(logMels);
        }

        // Dump to Logcat for verification
        float[][] result = stacker.getOrderedMatrix();
        for (int m = 0; m < nMels; m++) {
            StringBuilder sb = new StringBuilder();
            for (int t = 0; t < totalFrames; t++) {
                sb.append(String.format("%.6f", result[m][t])).append(",");
            }
            android.util.Log.d("DSP_PARITY", "Row " + m + ": " + sb.toString());
        }
    }

    private float[] loadTestWav() {
        // TODO: Replace with actual test_clap.wav converted to float[]
        float[] dummy = new float[16000]; // 1 second at 16 kHz
        for (int i = 0; i < dummy.length; i++) {
            dummy[i] = (float) Math.sin(2 * Math.PI * 440 * i / 16000); // simple sine wave
        }
        return dummy;
    }
}
