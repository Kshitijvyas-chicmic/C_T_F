package com.example.clap_to_find_sandbox;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import com.example.clap_to_find_sandbox.dsp_sandbox.*;
import com.example.clap_to_find_sandbox.dsp_sandbox.ClapLogic;
import com.example.clap_to_find_sandbox.dsp_sandbox.ImpulseValidator;

/**
 * 🚀 GOLD STANDARD AUDIO PROCESSOR (Synced with train_final.py)
 * 1. Normalization: PCM / 32768.0f
 * 2. Matrix: [64 Mels][64 Time Frames]
 * 3. Log: 10 * log10(max(power, 1e-10))
 */
public class AudioProcessor implements Runnable {
    private static final String TAG = "AudioProcessor_Final";

    private final Context context;
    private final ClapDetector detector;
    private final OnClapDetectedListener listener;

    private volatile boolean isRunning = false;

    private final HannWindow hannWindow;
    private final FFTProcessor fftProcessor;
    private final MelFilterbank melFilterbank;
    private final LogMelExtractor logMelExtractor;

    public interface OnClapDetectedListener {
        void onClapDetected(float probability);
        void onConfidenceUpdate(float confidence);
        void onStatusUpdate(String message);
    }

    public AudioProcessor(Context context, ClapDetector detector, OnClapDetectedListener listener) throws Exception {
        this.context = context;
        this.detector = detector;
        this.listener = listener;
        
        // Logic Engine
        this.clapLogic = new ClapLogic();

        // Load the new .bin assets
        int nFreqs = DspConfig.N_FFT / 2 + 1;
        this.hannWindow = new HannWindow(context.getAssets().open("dsp/hann_window.bin"), DspConfig.WIN_LENGTH);
        this.fftProcessor = new FFTProcessor(DspConfig.N_FFT);
        this.melFilterbank = new MelFilterbank(context.getAssets().open("dsp/mel_filterbank.bin"), DspConfig.N_MELS, nFreqs);
        this.logMelExtractor = new LogMelExtractor();

        Log.i(TAG, "DSP SYNCED: " + DspConfig.N_MELS + " Mels, " + DspConfig.TIME_FRAMES + " Frames, " + DspConfig.F_MIN + "Hz F_MIN");
    }
    
    private final ClapLogic clapLogic;

    public void stop() {
        isRunning = false;
    }

    @Override
    public void run() {
        int bufferSize = AudioRecord.getMinBufferSize(
                DspConfig.SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT
        );

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            listener.onStatusUpdate("ERROR: Permission Denied");
            return;
        }

        AudioRecord audioRecord = new AudioRecord(
                MediaRecorder.AudioSource.MIC,
                DspConfig.SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                Math.max(bufferSize, DspConfig.N_FFT * 2)
        );

        if (audioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
            listener.onStatusUpdate("ERROR: Mic Init Failed");
            return;
        }

        try {
            audioRecord.startRecording();
            isRunning = true;
            listener.onStatusUpdate("Listening...");

            // INFERENCE BUFFER: Holds 15600 samples (~0.975s) for YAMNet
            int MODEL_INPUT_SIZE = 15600;
            float[] inferenceBuffer = new float[MODEL_INPUT_SIZE];
            int frameCount = 0;
            // pcmBuffer holds the full N_FFT window
            short[] pcmBuffer = new short[DspConfig.N_FFT];
            // floatFrame for processing (Normalized)
            float[] floatFrame = new float[DspConfig.N_FFT];

            // Initial read
            audioRecord.read(pcmBuffer, 0, DspConfig.N_FFT);
            
            // Pre-fill inference buffer with initial data (optional, just to avoid zeros)
            for(int i=0; i<DspConfig.N_FFT; i++) {
                inferenceBuffer[MODEL_INPUT_SIZE - DspConfig.N_FFT + i] = pcmBuffer[i] / 32768.0f;
            }

            while (isRunning) {
                // 1. PCM Normalization
                for (int i = 0; i < DspConfig.N_FFT; i++) {
                    floatFrame[i] = (float) pcmBuffer[i] / 32768.0f;
                }

                // 2. FFT & Power Spectrum (Required for ImpulseValidator)
                float[] windowedFrame = new float[DspConfig.N_FFT];
                System.arraycopy(floatFrame, 0, windowedFrame, 0, DspConfig.N_FFT);
                
                hannWindow.applyWindow(windowedFrame);
                float[] power = fftProcessor.getPowerSpectrum(windowedFrame);
                
                // 3. HARD GATE: Impulse Validation
                boolean isImpulse = ImpulseValidator.isImpulse(floatFrame, power);
                
                // 4. Update Inference Buffer (Slide & Append)
                // We shift left by HOP_LENGTH and append the NEW samples.
                // The new samples are at the END of pcmBuffer (last HOP_LENGTH samples).
                // Wait, pcmBuffer is updated by reading HOP_LENGTH into the END.
                // So pcmBuffer [N_FFT - HOP_LENGTH : N_FFT] are the new samples.
                
                int samplesNew = DspConfig.HOP_LENGTH;
                int srcPos = DspConfig.N_FFT - samplesNew;
                
                // Shift existing history
                System.arraycopy(inferenceBuffer, samplesNew, inferenceBuffer, 0, MODEL_INPUT_SIZE - samplesNew);
                
                // Append new samples (normalized)
                for (int i = 0; i < samplesNew; i++) {
                   inferenceBuffer[MODEL_INPUT_SIZE - samplesNew + i] = (float) pcmBuffer[srcPos + i] / 32768.0f;
                }


                frameCount++;
                clapLogic.checkTimeout(); // Maintain state

                // 5. Detection Trigger
                //if (frameCount >= 10 && isImpulse) { // Wait for buffer warmup ~10 frames
                if (frameCount >= 40 && isImpulse) {  //wait for buffer warmup for 400 ms
                    // Pass RAW WAVEFORM to YAMNet
                    float probability = detector.detect(inferenceBuffer);
                    listener.onConfidenceUpdate(probability);

                    if (probability > DspConfig.CONFIDENCE_THRESHOLD) {
                        Log.i(TAG, "YAMNet Confirmed: " + probability);
                        
                        clapLogic.processClap(null, () -> {
                            Log.i(TAG, "ALARM TRIGGERED!");
                            listener.onClapDetected(probability);
                        });
                    }
                }

                // Shift buffer by HOP_LENGTH
                System.arraycopy(pcmBuffer, DspConfig.HOP_LENGTH, pcmBuffer, 0, DspConfig.N_FFT - DspConfig.HOP_LENGTH);
                // Read next HOP_LENGTH samples
                int samplesRead = audioRecord.read(pcmBuffer, DspConfig.N_FFT - DspConfig.HOP_LENGTH, DspConfig.HOP_LENGTH);
                if (samplesRead <= 0) break;
            }
        } catch (Exception e) {
            Log.e(TAG, "Audio Loop Error", e);
        } finally {
            try { audioRecord.stop(); } catch (Exception ignored) {}
            audioRecord.release();
        }
    }
}
