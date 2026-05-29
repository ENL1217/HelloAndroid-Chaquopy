package com.example.helloandroid;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import com.chaquo.python.PyObject;
import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private Python py;
    private PreviewView previewView;
    private ImageView resultView;

    private ExecutorService cameraExecutor = Executors.newSingleThreadExecutor();

    // 改動 1：預設直接開啟自動模式
    private boolean isProcessing = false;

    private final ActivityResultLauncher<String> cameraPermLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    startCamera();
                } else {
                    Toast.makeText(this, "需要攝影機權限", Toast.LENGTH_LONG).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        previewView = findViewById(R.id.previewView);
        resultView = findViewById(R.id.resultView);

        // 確保結果圖層一開始就顯示出來，用來覆蓋預覽畫面或並排
        resultView.setVisibility(View.VISIBLE);

        if (!Python.isStarted()) {
            Python.start(new AndroidPlatform(this));
        }
        py = Python.getInstance();

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            cameraPermLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> future = ProcessCameraProvider.getInstance(this);

        future.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = future.get();

                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                // 改動 2：設定 Analysis 解析度（建議不要太高，否則 Python 處理會太慢）
                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                        .setTargetResolution(new Size(480, 640))
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();

                // 自動連續分析邏輯
                imageAnalysis.setAnalyzer(cameraExecutor, image -> {
                    // 如果上一幀還在 Python 裡面跑，這一幀就直接丟掉，避免記憶體塞爆
                    if (isProcessing) {
                        image.close();
                        return;
                    }

                    isProcessing = true; // 上鎖

                    try {
                        byte[] bytes = yuvToJpegBytes(image);
                        image.close(); // 轉換完立即釋放相機幀

                        // 直接呼叫 Python 處理
                        PyObject module = py.getModule("opencv_process");
                        PyObject result = module.callAttr("canny_from_image_bytes", bytes);
                        byte[] outPng = result.toJava(byte[].class);
                        Bitmap bitmap = BitmapFactory.decodeByteArray(outPng, 0, outPng.length);

                        // 更新到 UI
                        runOnUiThread(() -> {
                            resultView.setImageBitmap(bitmap);
                            isProcessing = false; // 處理完顯示出來後，才解鎖接下一幀
                        });
                    } catch (Exception e) {
                        Log.e("Python", "Error", e);
                        isProcessing = false;
                        image.close();
                    }
                });

                CameraSelector selector = CameraSelector.DEFAULT_BACK_CAMERA;
                cameraProvider.unbindAll();
                // 綁定分析器與預覽
                cameraProvider.bindToLifecycle(this, selector, preview, imageAnalysis);

            } catch (Exception e) {
                Log.e("CameraX", "Failed", e);
            }
        }, ContextCompat.getMainExecutor(this));
    }

    // 將 YUV 轉 JPEG 的標準工具函式
    private byte[] yuvToJpegBytes(ImageProxy image) {
        ImageProxy.PlaneProxy[] planes = image.getPlanes();
        ByteBuffer yBuffer = planes[0].getBuffer();
        ByteBuffer uBuffer = planes[1].getBuffer();
        ByteBuffer vBuffer = planes[2].getBuffer();

        int ySize = yBuffer.remaining();
        int uSize = uBuffer.remaining();
        int vSize = vBuffer.remaining();

        byte[] nv21 = new byte[ySize + uSize + vSize];
        yBuffer.get(nv21, 0, ySize);
        vBuffer.get(nv21, ySize, vSize);
        uBuffer.get(nv21, ySize + vSize, uSize);

        YuvImage yuvImage = new YuvImage(nv21, ImageFormat.NV21, image.getWidth(), image.getHeight(), null);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        yuvImage.compressToJpeg(new Rect(0, 0, yuvImage.getWidth(), yuvImage.getHeight()), 70, out);
        return out.toByteArray();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraExecutor.shutdown();
    }
}