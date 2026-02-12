package com.example.clap_to_find_sandbox;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.util.Log;
import com.example.clap_to_find_sandbox.dsp_sandbox.DspConfig;
import org.tensorflow.lite.Interpreter;
import java.io.FileInputStream;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Arrays;

public class ClapDetector {
    private static final String TAG = "ClapDetector";
    private Interpreter interpreter;
    
    // HYBRID MODE: Raw Audio Input for YAMNet
    // Standard YAMNet expects [1, 15600]
    private static final int MODEL_INPUT_SIZE = 15600;
    private final float[][] inputTensor = new float[1][MODEL_INPUT_SIZE];
    private final float[][] outputTensor = new float[1][521]; // YAMNet has 521 classes

    public ClapDetector(Context context) throws Exception {
        interpreter = new Interpreter(loadModelFile(context, "clap_detector.tflite"), new Interpreter.Options());
        Log.i("MODEL_SHAPE", Arrays.toString(interpreter.getInputTensor(0).shape()));
        
        // Verify shape matches expectation
        int[] shape = interpreter.getInputTensor(0).shape();
        // Usually [1, 15600] or [15600]
    }

    private MappedByteBuffer loadModelFile(Context context, String modelPath) throws Exception {
        AssetFileDescriptor fileDescriptor = context.getAssets().openFd(modelPath);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    public synchronized float detect(float[] waveform) {
        if (interpreter == null) {
            Log.w(TAG, "Interpreter is closed, skipping detection.");
            return 0;
        }
        
        if (waveform.length != MODEL_INPUT_SIZE) {
            Log.e(TAG, "Input size mismatch: " + waveform.length + " != " + MODEL_INPUT_SIZE);
            return 0;
        }

        // Copy waveform to input tensor
        System.arraycopy(waveform, 0, inputTensor[0], 0, MODEL_INPUT_SIZE);

        try {
            interpreter.run(inputTensor, outputTensor);
            
            // Get Clap Score (Class 58)
            // Note: Standard YAMNet might output logits or probabilities. 
            // Usually Softmax is applied.
            // Check if outputTensor values sum to 1. If not, we might need sigmoid/softmax.
            // But standard TFLite YAMNet usually includes the activation.
            
            float clapScore = outputTensor[0][58]; 
            return clapScore;
            
        } catch (Exception e) {
            Log.e(TAG, "Error running TFLite interpreter", e);
            return 0;
        }
    }

    public synchronized void close() {
        if (interpreter != null) {
            interpreter.close();
            interpreter = null;
        }
    }
}
