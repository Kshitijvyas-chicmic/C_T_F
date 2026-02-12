package com.example.clap_to_find_sandbox.dsp_sandbox;

import org.junit.Test;
import java.io.File;
import java.io.FileInputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Manual Parity Test.
 * Run this by clicking the green "Play" icon next to the class or method name.
 */
public class ParityValidatorManualTest {

    @Test
    public void runManualParity() throws Exception {
        // Find assets relative to project root
        File root = new File("src/main/assets/dsp/");
        if (!root.exists()) {
            root = new File("app/src/main/assets/dsp/");
        }

        System.out.println("DSP_PARITY: Using assets from: " + root.getAbsolutePath());

        File wavFile = new File(root, "test_clap.wav");
        File hannFile = new File(root, "hann_window.bin");
        File melFile = new File(root, "mel_filterbank.bin");

        if (!wavFile.exists()) {
            throw new RuntimeException("Could not find test_clap.wav at " + wavFile.getAbsolutePath());
        }

        // 1. Load WAV and convert to float
        FileInputStream wavIs = new FileInputStream(wavFile);
        byte[] wavBytes = wavIs.readAllBytes();
        wavIs.close();

        // Skip 44-byte WAV header
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

        // 2. Initialize and Run Validator
        FileInputStream hannIs = new FileInputStream(hannFile);
        FileInputStream melIs = new FileInputStream(melFile);
        
        Parity_Validator validator = new Parity_Validator(hannIs, melIs);
        
        // Output will be saved to the project root
        String outputPath = "parity_results_junit.csv";
        validator.runParityTest(floatData, outputPath);
        
        System.out.println("DSP_PARITY: Test completed. Results saved to: " + new File(outputPath).getAbsolutePath());
    }
}
