package com.example.clap_to_find_sandbox.dsp_sandbox;

/**
 * ARCHITECT'S NOTE: Parity Locked with dsp_config.json.
 * These values must match the Python generation script exactly.
 */
public class DspConfig {
    public static final float INPUT_SCALE = 1.0f / 100.0f; 
    public static final int SAMPLE_RATE = 16000;
    
    // Updated to match dsp_config.json
    public static final int N_FFT = 512;
    public static final int HOP_LENGTH = 160;
    public static final int WIN_LENGTH = 400; 
    public static final int N_MELS = 64;
    public static final int TIME_FRAMES = 96; // Standard YAMNet (0.975s) 
    
    public static final float F_MIN = 125.0f;
    public static final float F_MAX = 7500.0f;
    
    public static final float CONFIDENCE_THRESHOLD = 0.50f;

    // Window size for total detection buffer (0.7s at 16kHz)
    public static final int WINDOW_SAMPLES = (int) (0.7 * SAMPLE_RATE);
}
