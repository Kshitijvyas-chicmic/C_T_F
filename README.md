# Clap to Find - Sandbox

A high-performance Android application designed to detect claps in real-time and trigger an alarm. This project uses a hybrid approach combining traditional Digital Signal Processing (DSP) and Deep Learning (YAMNet) to ensure high accuracy and low battery consumption.

## 🛠 Tech Stack

*   **Language:** Java
*   **Platform:** Android (Min SDK 24+)
*   **Machine Learning:** TensorFlow Lite (TFLite)
*   **Model:** YAMNet (Optimized for Audio Event Classification)
*   **DSP Library:** JTransforms (for Fast Fourier Transform)
*   **Core APIs:**
    *   `AudioRecord`: Low-level raw audio capture.
    *   `Foreground Service`: Continuous background processing.
    *   `MediaRecorder.AudioSource.MIC`: High-fidelity audio input.

## 📂 Project Architecture

### Core Components
*   **`MainActivity`**: Handles user interface, runtime permissions (Record Audio), and manages the lifecycle of the `AudioService`. It provides a live "Confidence Meter" and a detection log.
*   **`AudioService`**: A Foreground Service that keeps the application alive in the background. It manages the `AudioProcessor` and triggers the `Ringtone` alarm when a clap is confirmed.
*   **`AudioProcessor`**: The engine of the app. It runs in a dedicated thread, reading raw PCM data, applying DSP windowing, and managing the inference buffer.
*   **`ClapDetector`**: Wraps the TensorFlow Lite Interpreter. It loads the `clap_detector.tflite` model and runs inference on 0.975s audio windows.

### DSP Sandbox (`dsp_sandbox/`)
*   **`ImpulseValidator`**: A "Hard Gate" logic that analyzes the power spectrum to detect sharp audio impulses. Inference is only triggered if an impulse is detected, saving CPU/Battery.
*   **`HannWindow`**: Applies a Hann window function to raw audio frames to reduce spectral leakage.
*   **`FFTProcessor`**: Converts time-domain audio samples to frequency-domain using JTransforms.
*   **`MelFilterbank`**: Maps the frequency spectrum to the Mel scale, mimicking human hearing and matching YAMNet's expected input features.
*   **`LogMelExtractor`**: Performs log-scaling of Mel features for better neural network performance.
*   **`ClapLogic`**: Manages state-based triggering to prevent multiple alarms from a single clap event.
*   **`DspConfig`**: Centralized configuration for Sample Rate (16kHz), FFT Size (1024), and Hop Length.

## 🚀 Critical Decisions for Success

1.  **Hybrid Detection Pipeline**: Instead of running the AI model constantly (which is computationally expensive), we implemented an `ImpulseValidator`. This acts as a "trigger" that only wakes up the AI model when a sharp, clap-like sound is actually heard.
2.  **DSP-Model Sync**: The DSP pipeline in Java was meticulously synced with the Python training scripts (normalization by `32768.0`, specific Mel-filter bins, and Log-scaling). This ensures the "Live" accuracy matches the training accuracy.
3.  **Real-time Buffer Management**: Used a sliding window inference buffer (`System.arraycopy`) to maintain a continuous 0.975s history of audio, allowing YAMNet to see the full context of a sound even if it starts between frames.
4.  **Foreground Service Type**: Implemented `foregroundServiceType="microphone"` to comply with Android 10+ privacy requirements, ensuring the app is not killed by the OS while listening in the background.
5.  **Thread Safety**: Used `volatile` flags and `synchronized` blocks in the detector to ensure the audio thread and UI thread never collide, preventing crashes during rapid detection events.

🔑 Canonical principles for mobile clap detection

16 kHz is enough — temporal resolution matters more than Nyquist bragging rights

Impulse sounds need time context, not just spectral precision

Low thresholds are fine if higher-level logic exists

Echo suppression + pattern logic is mandatory in real rooms

Parity between training DSP and runtime DSP is non-negotiable

Distance robustness comes from normalization + logic, not higher SR

Logs that look “busy” usually mean the system is alive and sensitive