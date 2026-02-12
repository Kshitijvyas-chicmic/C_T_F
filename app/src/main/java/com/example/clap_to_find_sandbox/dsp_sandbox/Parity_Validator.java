package com.example.clap_to_find_sandbox.dsp_sandbox;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Locale;

/**
 * ARCHITECT'S NOTE: Pure Java version for DSP Parity Validation with Stage Dumps.
 */
public class Parity_Validator {
    private final HannWindow hannWindow;
    private final FFTProcessor fftProcessor;
    private final MelFilterbank melFilterbank;
    private final LogMelExtractor logMelExtractor;

    public Parity_Validator(InputStream hannStream, InputStream melStream) throws Exception {
        int nFft = 1024;
        int nMels = 64;
        int nFreqs = nFft / 2 + 1;

        this.hannWindow = new HannWindow(hannStream, nFft);
        this.fftProcessor = new FFTProcessor(nFft);
        this.melFilterbank = new MelFilterbank(melStream, nMels, nFreqs);
        this.logMelExtractor = new LogMelExtractor();
    }

    public void runParityTest(float[] wavData, String outputCsvPath) {
        int nFft = 1024;
        int hop = 256;
        int nMels = 64;
        int targetFrames = 40;

        FeatureStacker stacker = new FeatureStacker(nMels, targetFrames);
        int availableFrames = (wavData.length - nFft) / hop + 1;
        
        for (int i = 0; i < availableFrames; i++) {
            float[] frame = new float[nFft];
            System.arraycopy(wavData, i * hop, frame, 0, nFft);

            // STAGE 0: Hann
            hannWindow.applyWindow(frame);
            if (i == 0) dumpStage("Stage 0: Hann (First Frame)", frame);

            // STAGE 1: Power Spectrum
            float[] power = fftProcessor.getPowerSpectrum(frame);
            if (i == 0) dumpStage("Stage 1: Power Spectrum (First Frame)", power);

            // STAGE 2: Mel
            float[] mels = melFilterbank.applyFilterbank(power);
            if (i == 0) dumpStage("Stage 2: Mel Filterbank (First Frame)", mels);

            // STAGE 3: Log-Mel
            float[] logMels = logMelExtractor.computeLogMel(mels);
            if (i == 0) dumpStage("Stage 3: Log-Mel (First Frame)", logMels);

            stacker.addFrame(logMels);
        }

        float[][] result = stacker.getOrderedMatrix();
        saveCsv(result, nMels, targetFrames, outputCsvPath);
    }

    private void dumpStage(String label, float[] data) {
        System.out.println("--- " + label + " ---");
        System.out.println("Size: " + data.length);
        for (int i = 0; i < Math.min(data.length, 5); i++) {
            System.out.print(String.format(Locale.US, "%.10f ", data[i]));
        }
        System.out.println("...");
    }

    private void saveCsv(float[][] result, int nMels, int nFrames, String path) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(path))) {
            for (int m = 0; m < nMels; m++) {
                StringBuilder sb = new StringBuilder();
                for (int t = 0; t < nFrames; t++) {
                    sb.append(String.format(Locale.US, "%.6f", result[m][t]));
                    if (t < nFrames - 1) sb.append(",");
                }
                writer.println(sb.toString());
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    public static void main(String[] args) {
        // Implementation omitted for brevity - use DspParityTest
    }
}
