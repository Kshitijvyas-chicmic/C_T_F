package com.example.clap_to_find_sandbox;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.example.clap_to_find_sandbox.dsp_sandbox.Parity_Validator;
import java.io.File;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            // 1. Load wav data from assets
            InputStream is = getAssets().open("dsp/test_clap.wav");
            byte[] wavBytes = is.readAllBytes();
            is.close();

            // Skip 44-byte WAV header and convert to float
            int floatCount = (wavBytes.length - 44) / 2;
            float[] floatData = new float[floatCount];

            short[] shortArray = new short[floatCount];
            ByteBuffer.wrap(wavBytes, 44, wavBytes.length - 44)
                      .order(ByteOrder.LITTLE_ENDIAN)
                      .asShortBuffer()
                      .get(shortArray);

            for (int i=0; i< floatCount; i++){
                floatData[i] = shortArray[i] / 32768.0f;
            }

            // 2. Load window and filterbank data for the validator
            InputStream hannIs = getAssets().open("dsp/hann_window.bin");
            InputStream melIs = getAssets().open("dsp/mel_filterbank.bin");

            // 3. Run Parity Test and save to app's internal files directory
            String outputPath = new File(getFilesDir(), "parity_results_android.csv").getAbsolutePath();
            Parity_Validator validator = new Parity_Validator(hannIs, melIs);
            validator.runParityTest(floatData, outputPath);

            hannIs.close();
            melIs.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
