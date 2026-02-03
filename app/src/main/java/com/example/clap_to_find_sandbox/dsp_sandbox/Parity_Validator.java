package com.example.clap_to_find_sandbox.dsp_sandbox;

import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * ARCHITECT'S NOTE: Pure Java version.
 * Can be run from a standard main() method.
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

        int totalFrames = (wavData.length - nFft) / hop + 1;
        FeatureStacker stacker = new FeatureStacker(nMels, totalFrames);

        for (int i = 0; i < totalFrames; i++) {
            float[] frame = new float[nFft];
            System.arraycopy(wavData, i * hop, frame, 0, nFft);

            hannWindow.applyWindow(frame);
            float[] power = fftProcessor.getPowerSpectrum(frame);
            float[] mels = melFilterbank.applyFilterbank(power);
            float[] logMels = logMelExtractor.computeLogMel(mels);

            stacker.addFrame(logMels);
        }

        float[][] result = stacker.getOrderedMatrix();

        if (outputCsvPath != null) {
            try (PrintWriter writer = new PrintWriter(new FileWriter(outputCsvPath))) {
                for (int m = 0; m < nMels; m++) {
                    StringBuilder sb = new StringBuilder();
                    for (int t = 0; t < totalFrames; t++) {
                        sb.append(String.format("%.6f", result[m][t]));
                        if (t < totalFrames - 1) sb.append(",");
                    }
                    writer.println(sb.toString());
                }
                System.out.println("DSP_PARITY: Results successfully saved to " + outputCsvPath);
            } catch (Exception e) {
                System.err.println("DSP_PARITY: Error writing CSV: " + e.getMessage());
            }
        } else {
            // Fallback to console if no path provided
            for (int m = 0; m < nMels; m++) {
                StringBuilder sb = new StringBuilder();
                for (int t = 0; t < totalFrames; t++) {
                    sb.append(String.format("%.6f", result[m][t])).append(",");
                }
                System.out.println("DSP_PARITY: Row " + m + ": " + sb.toString());
            }
        }
    }

    /**
     * MAIN METHOD FOR RUNNING WITHOUT ANDROID
     * Run this by right-clicking this file in Android Studio and selecting "Run 'Parity_Validator.main()'"
     */
    public static void main(String[] args) {
        try {
            // Adjust these paths to your local system if necessary
            String baseDir = "app/src/main/assets/dsp/";
            String outputPath = "parity_results.csv";
            
            FileInputStream wavIs = new FileInputStream(baseDir + "test_clap.wav");
            FileInputStream hannIs = new FileInputStream(baseDir + "hann_window.bin");
            FileInputStream melIs = new FileInputStream(baseDir + "mel_filterbank.bin");

            // Load WAV data
            byte[] wavBytes = wavIs.readAllBytes();
            wavIs.close();

            int floatCount = (wavBytes.length - 44) / 2;
            float[] floatData = new float[floatCount];
            short[] shortArray = new short[floatCount];
            ByteBuffer.wrap(wavBytes, 44, wavBytes.length - 44)
                    .order(ByteOrder.LITTLE_ENDIAN)
                    .asShortBuffer()
                    .get(shortArray);

            for (int i = 0; i < floatCount; i++) {
                floatData[i] = shortArray[i] / 32768.0f;
            }

            Parity_Validator validator = new Parity_Validator(hannIs, melIs);
            validator.runParityTest(floatData, outputPath);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
