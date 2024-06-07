package com.example.motiondetectionapp;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.Toast;

import org.opencv.android.CameraActivity;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import java.util.Collections;
import java.util.List;

import kotlin.collections.ArrayDeque;

public class HomeActivity extends CameraActivity {
    int cameraIndex = CameraBridgeViewBase.CAMERA_ID_BACK;
    private static final String TAG = "OCVSample::Activity";
    private CameraBridgeViewBase mOpenCvCameraView;
    private ImageButton switchButton;
    private Mat rgba, gray, prev_gray, transpose_rgba, transpose_gray, diff_back, diff_front;
    List<MatOfPoint> cnts;
    boolean is_init;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_home);
        switchButton = findViewById(R.id.switchBtn);

        is_init = false;

        // Setting Up Camera View
        mOpenCvCameraView = findViewById(R.id.opencvCameraView);
        mOpenCvCameraView.setVisibility(CameraBridgeViewBase.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(new CameraBridgeViewBase.CvCameraViewListener2() {
            @Override
            public void onCameraViewStarted(int width, int height) {
                rgba = new Mat();
                gray = new Mat();
                diff_back = new Mat();
                diff_front = new Mat();
                cnts = new ArrayDeque<MatOfPoint>();
            }

            @Override
            public void onCameraViewStopped() {
                rgba.release();
                gray.release();
                diff_back.release();
                diff_front.release();
            }

            @Override
            public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {

                if (cameraIndex == CameraBridgeViewBase.CAMERA_ID_BACK) {

                    if (!is_init) {
                        prev_gray = inputFrame.gray();
                        is_init = true;
                        return prev_gray;
                    }

                    rgba = inputFrame.rgba();
                    gray = inputFrame.gray();
                    Core.absdiff(gray, prev_gray, diff_back);
                    Imgproc.threshold(diff_back, diff_back, 100, 255, Imgproc.THRESH_BINARY);
                    Imgproc.findContours(diff_back, cnts, new Mat(), Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);
                    Imgproc.drawContours(rgba, cnts, -1, new Scalar(0,255,0));

                    cnts.clear();

                    prev_gray = gray.clone();
                    return rgba;
                } else {

                    if (!is_init) {https://github.com/rafifathurr/motion-detection-app.git
                        gray = inputFrame.gray();

                        prev_gray = gray.clone();
                        Core.transpose(gray, prev_gray);
                        Core.flip(prev_gray, prev_gray, -1);

                        is_init = true;

                        return prev_gray.t();
                    }

                    rgba = inputFrame.rgba();
                    gray = inputFrame.gray();

                    transpose_gray = gray.clone();
                    Core.transpose(gray, transpose_gray);
                    Core.flip(transpose_gray, transpose_gray, -1);

                    transpose_rgba = rgba.clone();
                    Core.transpose(rgba, transpose_rgba);
                    Core.flip(transpose_rgba, transpose_rgba, -1);

                    Core.absdiff(transpose_gray.t(), prev_gray, diff_front);

                    Imgproc.threshold(diff_front.t(), diff_front, 100, 255, Imgproc.THRESH_BINARY);
                    Imgproc.findContours(diff_front, cnts, new Mat(), Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);
                    Imgproc.drawContours(transpose_rgba, cnts, -1, new Scalar(0,255,0));

                    cnts.clear();

                    prev_gray = transpose_gray.t().clone();

                    return transpose_rgba.t();
                }
            }
        });

        switchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (cameraIndex == CameraBridgeViewBase.CAMERA_ID_BACK) {
                    cameraIndex = CameraBridgeViewBase.CAMERA_ID_FRONT;
                } else {
                    cameraIndex = CameraBridgeViewBase.CAMERA_ID_BACK;
                }

                mOpenCvCameraView.disableView();
                mOpenCvCameraView.setCameraIndex(cameraIndex);
                mOpenCvCameraView.enableView();
            }
        });

        // Important for configuration camera
        if (OpenCVLoader.initLocal()) {
            mOpenCvCameraView.setCameraIndex(cameraIndex);
            mOpenCvCameraView.enableView();
        } else {
            Log.e(TAG, "OpenCV initialization failed!");
            (Toast.makeText(this, "OpenCV initialization failed!", Toast.LENGTH_LONG)).show();
            return;
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        mOpenCvCameraView.setCameraIndex(cameraIndex);
        mOpenCvCameraView.disableView();
    }

    @Override
    public void onResume() {
        super.onResume();
        mOpenCvCameraView.enableView();
    }

    @Override
    protected List<? extends CameraBridgeViewBase> getCameraViewList() {
        return Collections.singletonList(mOpenCvCameraView);
    }
}