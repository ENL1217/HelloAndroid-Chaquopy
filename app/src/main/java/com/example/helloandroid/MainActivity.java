package com.example.helloandroid;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.chaquo.python.PyObject;
import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class MainActivity extends AppCompatActivity {

    private ImageView imgOriginal;
    private ImageView imgResult;
    private TextView txtStatus;

    private byte[] inputImageBytes;
    private Python py;

    private final ActivityResultLauncher<Intent> imagePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();

                    try {
                        inputImageBytes = readBytesFromUri(uri);
                        showOriginalImage(inputImageBytes);
                        imgResult.setImageDrawable(null);
                        txtStatus.setText("Picked image: " + inputImageBytes.length + " bytes");

                    } catch (Exception e) {
                        txtStatus.setText("Read picked image failed: " + e.getMessage());
                        e.printStackTrace();
                    }
                } else {
                    txtStatus.setText("No image selected");
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        imgOriginal = findViewById(R.id.imgOriginal);
        imgResult = findViewById(R.id.imgResult);
        txtStatus = findViewById(R.id.txtStatus);

        Button btnLoadResource = findViewById(R.id.btnLoadResource);
        Button btnPythonReadImage = findViewById(R.id.btnPythonReadImage);
        Button btnPickImage = findViewById(R.id.btnPickImage);
        Button btnProcessImage = findViewById(R.id.btnProcessImage);

        if (!Python.isStarted()) {
            Python.start(new AndroidPlatform(this));
        }
        py = Python.getInstance();

        btnLoadResource.setOnClickListener(v -> loadImageFromResource());
        btnPythonReadImage.setOnClickListener(v -> loadImageFromPython());
        btnPickImage.setOnClickListener(v -> pickImageFromDevice());
        btnProcessImage.setOnClickListener(v -> processImageWithPython());
    }

    private void loadImageFromResource() {
        try {
            inputImageBytes = readBytesFromRawResource(R.raw.test_image);
            showOriginalImage(inputImageBytes);
            imgResult.setImageDrawable(null);
            txtStatus.setText("Loaded image from res/raw: " + inputImageBytes.length + " bytes");

        } catch (Exception e) {
            txtStatus.setText("Load resource image failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void loadImageFromPython() {
        try {
            PyObject module = py.getModule("local_image");
            PyObject result = module.callAttr("read_local_image");
            inputImageBytes = result.toJava(byte[].class);

            showOriginalImage(inputImageBytes);
            imgResult.setImageDrawable(null);
            txtStatus.setText("Python loaded local image: " + inputImageBytes.length + " bytes");

        } catch (Exception e) {
            txtStatus.setText("Python read image failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void pickImageFromDevice() {
        txtStatus.setText("Opening image picker...");

        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");

        imagePickerLauncher.launch(intent);
    }

    private void processImageWithPython() {
        if (inputImageBytes == null) {
            txtStatus.setText("Please pick an image first.");
            return;
        }

        txtStatus.setText("Processing...");

        new Thread(() -> {
            try {
                PyObject module = py.getModule("opencv_process");
                PyObject result = module.callAttr("canny_from_image_bytes", inputImageBytes);

                byte[] outPng = result.toJava(byte[].class);

                Bitmap outBitmap = BitmapFactory.decodeByteArray(
                        outPng,
                        0,
                        outPng.length
                );

                runOnUiThread(() -> {
                    imgResult.setImageBitmap(outBitmap);
                    txtStatus.setText("Done");
                });

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() ->
                        txtStatus.setText("Python error: " + e.getMessage())
                );
            }
        }).start();
    }


    private void showOriginalImage(byte[] imageBytes) {
        Bitmap bitmap = BitmapFactory.decodeByteArray(
                imageBytes,
                0,
                imageBytes.length
        );
        imgOriginal.setImageBitmap(bitmap);
    }

    private byte[] readBytesFromRawResource(int resId) throws IOException {
        InputStream inputStream = getResources().openRawResource(resId);
        return readAllBytes(inputStream);
    }

    private byte[] readBytesFromUri(Uri uri) throws IOException {
        InputStream inputStream = getContentResolver().openInputStream(uri);
        return readAllBytes(inputStream);
    }

    private byte[] readAllBytes(InputStream inputStream) throws IOException {
        if (inputStream == null) {
            throw new IOException("InputStream is null");
        }

        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] data = new byte[4096];

        int nRead;
        while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }

        inputStream.close();
        return buffer.toByteArray();
    }
}



