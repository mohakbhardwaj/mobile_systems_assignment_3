package com.example.assignment3java;

import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import com.google.common.util.concurrent.ListenableFuture;

import org.jtransforms.fft.FloatFFT_1D;

import java.util.concurrent.ExecutionException;

//import java.util.Queue;
//import com.example.assignment3java.FFT.FFT;

public class CameraActivity extends AppCompatActivity {
    private PreviewView previewView;
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private TextView textView;
    private int numFrames=6;
    private int readFreq = 4;
    private long readTime = (long) (250);
    private int currIdx = 0;
    private byte[][] frames = new byte[6][];
    private float[] lumins = new float[6];
    FloatFFT_1D floatFFT_1D = new FloatFFT_1D(numFrames);
    private long currTime = 0;
    private long lastUpdateTime = 0;
    private long startTime = 0;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        previewView = findViewById(R.id.previewView);
        cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        textView = findViewById(R.id.orientation);
        cameraProviderFuture.addListener(new Runnable() {
            @Override
            public void run() {
                try {
                    ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                    bindImageAnalysis(cameraProvider);
                } catch (ExecutionException | InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }, ContextCompat.getMainExecutor(this));

    }

    private void bindImageAnalysis(@NonNull ProcessCameraProvider cameraProvider) {
        ImageAnalysis imageAnalysis =
                new ImageAnalysis.Builder().setTargetResolution(new Size(1280, 720))
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build();
        imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(this), new ImageAnalysis.Analyzer() {
            @Override
            public void analyze(@NonNull ImageProxy image) {
//                int rotationDegrees = image.getImageInfo().getRotationDegrees();
//                Log.d("camera", String.valueOf(rotationDegrees));
                byte[] bytes = new byte[image.getPlanes()[0].getBuffer().remaining()];
                image.getPlanes()[0].getBuffer().get(bytes);
                int total = 0;
                for (byte value : bytes) {
                    total += value & 0xFF;
                }
                if(currTime == 0){
                    startTime = System.currentTimeMillis();

                }
                if (currTime == 0 || (currTime - lastUpdateTime) > readTime){
                    if(bytes.length != 0){
                        lastUpdateTime = currTime;
                        final float luminance = total / bytes.length;
                        frames[currIdx] = bytes;
                        lumins[currIdx] = luminance;
                        currIdx = (currIdx + 1) % numFrames;

                        //Do FFT
                        float[] temp = lumins.clone();
                        floatFFT_1D.realForward(temp);

                        // Extract Real part
                        float localMax = Float.MIN_VALUE;
                        int maxValueFreq = -1;
                        float[] result = new float[temp.length / 2];
                        for(int s = 0; s < result.length; s++) {
                            //result[s] = Math.abs(signal[2*s]);
                            if (s == 0){
                                result[0] = temp[0];  // / result.length;
                            }
                            else{
                                float re = temp[s * 2];
                                float im = temp[s * 2 + 1];
                                result[s] = (float) Math.sqrt(re * re + im * im); /// result.length;
//                                Log.d("camdebug", "s: " + String.valueOf(s) + " res: " + String.valueOf(result[s]) + " localMax: " + String.valueOf(localMax));
                                if(result[s] >= localMax) {
                                    localMax = result[s];
                                    maxValueFreq = s;
                                }
                            }

//                            localMax = Math.max(localMax, result[s]);
                        }
//                        for (float val : result){
//                            Log.d("fft_amp", String.valueOf(val));
//                        }
                        Log.d("fft_maxvalidx", String.valueOf(maxValueFreq));

                    }
                }
                currTime = System.currentTimeMillis() - startTime;
//                Log.d("times", "startTime: " + String.valueOf(startTime) + " currTime: " + String.valueOf(currTime) + " lastUpdateTime: " + String.valueOf(lastUpdateTime));
                image.close();
            }
        });
//        OrientationEventListener orientationEventListener = new OrientationEventListener(this) {
//            @Override
//            public void onOrientationChanged(int orientation) {
//                textView.setText(Integer.toString(orientation));
//            }
//        };
//        orientationEventListener.enable();
        Preview preview = new Preview.Builder().build();
        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK).build();
        preview.setSurfaceProvider(previewView.createSurfaceProvider());
        cameraProvider.bindToLifecycle((LifecycleOwner)this, cameraSelector,
                imageAnalysis, preview);
    }




}
