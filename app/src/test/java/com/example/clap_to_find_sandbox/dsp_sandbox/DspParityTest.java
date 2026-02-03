package com.example.clap_to_find_sandbox.dsp_sandbox;

import org.junit.Test;
import java.io.File;
import java.io.FileInputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class DspParityTest {

    @Test
    public void runDspParityCheck() {
        try {
            // Note: When running JUnit tests in Android Studio,
            // the working directory is usually the module folder (e.g., /app)
            String baseDir = "src/main/assets/dsp/";
            String outputPath = "dsp_parity_test_output.csv";

            File wavFile = new File(baseDir + "test_clap.wav");
            File hannFile = new File(baseDir + "hann_window.bin");
            File melFile = new File(baseDir + "mel_filterbank.bin");

            // Verify files exist before proceeding
            if (!wavFile.exists()) {
                throw new Exception("WAV file not found at: " + wavFile.getAbsolutePath());
            }

            FileInputStream wavIs = new FileInputStream(wavFile);
            FileInputStream hannIs = new FileInputStream(hannFile);
            FileInputStream melIs = new FileInputStream(melFile);

            // Load WAV data
            byte[] wavBytes = wavIs.readAllBytes();
            wavIs.close();

            // Convert to float (Standard 16-bit WAV skip 44-byte header)
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

            System.out.println("--- STARTING DSP PARITY TEST ---");
            System.out.println("Samples loaded: " + floatData.length);

            Parity_Validator validator = new Parity_Validator(hannIs, melIs);
            validator.runParityTest(floatData, outputPath);

            System.out.println("--- TEST COMPLETED ---");
            System.out.println("Results saved to: " + new File(outputPath).getAbsolutePath());

        } catch (Exception e) {
            e.printStackTrace();
            org.junit.Assert.fail("Test failed: " + e.getMessage());
        }
    }
}
