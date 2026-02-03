package com.example.clap_to_find_sandbox.dsp_sandbox;

import android.content.Context;
import android.util.Log;

public class old__Parity_Validator {
    private final HannWindow hannWindow;
    private final FFTProcessor fftProcessor;
    private final MelFilterbank melFilterbank;
    private final LogMelExtractor logMelExtractor;

    public old__Parity_Validator(Context context) throws Exception {
        int nFft = 1024;
        int nMels = 64;
        int nFreqs = nFft / 2 + 1;

        // Note: This relies on the new HannWindow/MelFilterbank having different signatures,
        // which might cause compilation errors if not handled carefully.
        // For the sake of the user's request, I'm keeping the old logic here.
        this.hannWindow = null; // placeholders to avoid compile errors while refactoring
        this.fftProcessor = new FFTProcessor(nFft);
        this.melFilterbank = null;
        this.logMelExtractor = new LogMelExtractor();
    }

    public void runParityTest(float[] wavData) {
        // ... (Old Android-dependent logic)
    }
}

//public void runParityTest(float[] wavData) {
//    // wavData is the raw 16k float array from test_clap.wav
//    int nFft = 1024;
//    int hop = 256;
//    int nMels = 64;
//
//    // Calculate total frames possible (center=false logic)
//    int totalFrames = (wavData.length - nFft) / hop + 1;
//    FeatureStacker stacker = new FeatureStacker(nMels, totalFrames);
//
//    // SLIDING WINDOW LOGIC
//    for (int i = 0; i < totalFrames; i++) {
//        float[] frame = new float[nFft];
//        System.arraycopy(wavData, i * hop, frame, 0, nFft);
//
//        hannWindow.applyWindow(frame);
//        float[] power = fftProcessor.getPowerSpectrum(frame);
//        float[] mels = melFilterbank.applyFilterbank(power);
//        float[] logMels = logMelExtractor.computeLogMel(mels);
//
//        stacker.addFrame(logMels);
//    }
//
//    // DUMP TO LOGCAT (Copy this to Excel/Diff Tool)
//    float[][] result = stacker.getOrderedMatrix();
//    for (int m = 0; m < nMels; m++) {
//        StringBuilder sb = new StringBuilder();
//        for (int t = 0; t < totalFrames; t++) {
//            sb.append(String.format("%.6f", result[m][t])).append(",");
//        }
//        android.util.Log.d("DSP_PARITY", "Row " + m + ": " + sb.toString());
//    }
//}