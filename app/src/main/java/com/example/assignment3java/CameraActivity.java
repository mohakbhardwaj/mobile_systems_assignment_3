package com.example.assignment3java;

import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.view.View;
import android.widget.Button;
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
import java.util.*;

import java.util.concurrent.ExecutionException;
import java.util.ArrayList;
import java.util.LinkedList;

public class CameraActivity extends AppCompatActivity {
    private PreviewView previewView;
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private TextView textView;
    private TextView fftTextView, updateTextView;

    private int numFrames=6;
    private int readFreq = 4;
    private long readTime = (long) (500);
    private int currIdx = 0;
    private byte[][] frames = new byte[6][];
    private LinkedList<Float> lumins = new LinkedList<Float>();
    FloatFFT_1D floatFFT_1D = new FloatFFT_1D(numFrames);
    private long currTime = 0;
    private long lastUpdateTime = 0;
    private long startTime = 0;
    private int currBit = 0;
    private int firstSixFrames = 0;
    private List<Integer> bitsReceived = new ArrayList<Integer>();
    private List<Float> ampRatios = new ArrayList<Float>();

    private boolean stopListening = false;
    private boolean calibrationMode = true;
    private int numBits = 0;
    private float ampRatio = 0;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        previewView = findViewById(R.id.previewView);
        cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        textView = findViewById(R.id.orientation);
        fftTextView = findViewById(R.id.fft);
        updateTextView = findViewById(R.id.update);

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

        Button stopBtn = findViewById(R.id.stopReading);
        stopBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String bitString = "";

                for(int value : bitsReceived){
                    bitString += String.valueOf(value);
                    bitString += " ";
                }
//                bitString += "  ";
//                for(float amp: ampRatios){
//                    bitString += String.valueOf(amp);
//                    bitString += " ";
//                }
                updateTextView.setText(bitString);
                stopListening = true;
            }
        });

    }

    private void bindImageAnalysis(@NonNull ProcessCameraProvider cameraProvider) {
        ImageAnalysis imageAnalysis =
                new ImageAnalysis.Builder().setTargetResolution(new Size(1280, 720))
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build();
        imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(this), new ImageAnalysis.Analyzer() {
            @Override
            public void analyze(@NonNull ImageProxy image) {
                if(!stopListening) {

                    byte[] bytes = new byte[image.getPlanes()[0].getBuffer().remaining()];
                    image.getPlanes()[0].getBuffer().get(bytes);
                    int total = 0;
                    for (byte value : bytes) {
                        total += value & 0xFF;
                    }
                    if (currTime == 0) {
                        startTime = System.currentTimeMillis();
                    }

                    if (currTime == 0 || (currTime - lastUpdateTime) >= readTime) {
                        if (bytes.length != 0) {

                            lastUpdateTime = currTime;
                            final float luminance = total / bytes.length;
//                            frames[currIdx] = bytes;
//                            lumins[currIdx] = luminance;
                            lumins.add(luminance);
                            if (firstSixFrames > 5) {
                                lumins.remove();
                            }
                            currIdx = (currIdx + 1) % numFrames;
                            if (firstSixFrames <= 5) {
                                firstSixFrames += 1;
                            }


                            if (calibrationMode) {
                                //do fft every frame
                                if(firstSixFrames >= 5){
                                    float[] output = doFFT(lumins);
                                    int maxValueFreq = (int) output[0];
                                    if (maxValueFreq == 2) {
                                        currBit = 0;
                                        ampRatio = output[2] / output[3];
                                    } else if (maxValueFreq == 3) {
                                        currBit = 1;
                                        ampRatio = output[3] / output[2];
                                    }
                                    updateTextView.setText("Calibration Mode");
                                    if (currBit == 1 && ampRatio > 2.0) {
                                        calibrationMode = false;
                                        numBits += 1;
                                        bitsReceived.add(currBit);
                                        ampRatios.add(ampRatio);
                                        updateTextView.setText("Calibration done");
                                    }
                                }
                            } else {
                                //do fft every 6 frames
                                if (currTime != 0 && currIdx == 0) {
                                    float[] output = doFFT(lumins);
                                    int maxValueFreq = (int) output[0];
                                    if (maxValueFreq == 2) {
                                        currBit = 0;
                                        ampRatio = output[2] / output[3];
                                    } else if (maxValueFreq == 3) {
                                        currBit = 1;
                                        ampRatio = output[3] / output[2];
                                    }
                                    numBits += 1;
                                    bitsReceived.add(currBit);
                                    ampRatios.add(ampRatio);
                                    updateTextView.setText("Read Mode");
                                    textView.setText("Got Bit: " + Integer.toString(currBit));
//                                    + " Amp. ratio: " + String.valueOf(ampRatio)

//                                    }
                                } else {
                                    textView.setText("");
                                }
                            }
                        }
                    }
                }

                currTime = System.currentTimeMillis() - startTime;
                image.close();
            }
        });

        Preview preview = new Preview.Builder().build();
        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK).build();
        preview.setSurfaceProvider(previewView.createSurfaceProvider());
        cameraProvider.bindToLifecycle((LifecycleOwner)this, cameraSelector,
                imageAnalysis, preview);
    }

    private float[] doFFT(LinkedList<Float> lumins){
        Log.d("fft", String.valueOf(lumins.size()));
        float[] temp = new float[6];
        for(int i = 0; i < lumins.size(); i ++){
            temp[i] = lumins.get(i); //hanningWindow(i) *
        }
        floatFFT_1D.realForward(temp);
//        float realParts[] = new float[4];
//        realParts[0] = (float) Math.abs(temp[0] / 4.0);
//        realParts[1] = (float) (Math.sqrt(temp[2]*temp[2] + temp[3]*temp[3]) / 4.0);
//        realParts[2] = (float) (Math.sqrt(temp[4]*temp[4] + temp[5]*temp[5]) / 4.0);
//        realParts[3] = (float) Math.abs(temp[1] / 4.0);
//
//        int maxValueFreq = 3;
//        if(realParts[2] > realParts[3]){
//            maxValueFreq =  2;
//        }
//        else{
//            maxValueFreq = 3;
//        }
        // Extract Real part
        float localMax = Float.MIN_VALUE;
        int maxValueFreq = -1;
        float[] realParts = new float[temp.length / 2 + 1];


        for(int s = 0; s < realParts.length; s++) {
            if (s == 0){
                realParts[0] = (float) Math.abs(temp[0] / realParts.length);
            }
            else if(s == realParts.length - 1){
                realParts[s] = (float) Math.abs(temp[1] / realParts.length);
            }
            else{
                float re = temp[s * 2];
                float im = temp[s * 2 + 1];
                realParts[s] = (float) ((float) Math.sqrt(re * re + im * im) / realParts.length);
            }

            if(s!=0 && realParts[s] >= localMax) {
                localMax = realParts[s];
                maxValueFreq = s;
            }

        }

        fftTextView.setText("FFT: " + String.valueOf(realParts[1]) + " " + String.valueOf(realParts[2]) + " " + String.valueOf(realParts[3]));

        return new float[]{maxValueFreq, realParts[1], realParts[2], realParts[3]};
    }

    private float hanningWindow(int idx) {
        return (float) (0.5 * (1 - Math.cos(2 * Math.PI * idx) / (numFrames - 1)));
    }



}
