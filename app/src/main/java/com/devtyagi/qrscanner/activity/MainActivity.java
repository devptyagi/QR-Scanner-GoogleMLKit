package com.devtyagi.qrscanner.activity;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Toast;

import com.devtyagi.qrscanner.R;
import com.devtyagi.qrscanner.databinding.ActivityMainBinding;
import com.google.mlkit.vision.barcode.Barcode;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.common.InputImage;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;
import com.otaliastudios.cameraview.controls.Flash;
import com.otaliastudios.cameraview.frame.Frame;

import java.io.IOException;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    ActivityMainBinding binding;
    boolean isDetected = false;

    BarcodeScannerOptions options;
    BarcodeScanner scanner;

    boolean isFlashOn = false;
    int SELECT_PICTURE = 200;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.gallery.setOnClickListener(v -> {
            selectImageFromGallery();
        });

        options = new BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).build();
        scanner = BarcodeScanning.getClient(options);
        Dexter.withContext(this)
                .withPermissions(new String[]{Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO})
                .withListener(new MultiplePermissionsListener() {
                    @Override
                    public void onPermissionsChecked(MultiplePermissionsReport multiplePermissionsReport) {
                        setupCamera();
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(List<PermissionRequest> list, PermissionToken permissionToken) {

                    }
                }).check();
    }

    private void selectImageFromGallery() {
        Intent i = new Intent();
        i.setType("image/*");
        i.setAction(Intent.ACTION_GET_CONTENT);

        startActivityForResult(Intent.createChooser(i, "Select Picture"), SELECT_PICTURE);
    }

    private void setupCamera() {
        binding.cameraView.setLifecycleOwner(this);
        binding.cameraView.addFrameProcessor(frame -> processImage(getInputImageFromFrame(frame)));
        binding.flashToggle.setOnClickListener(v -> {
            if(isFlashOn) {
                binding.cameraView.setFlash(Flash.OFF);
                binding.flashToggle.setImageResource(R.drawable.ic_flash_off);
                isFlashOn = false;
            } else {
                binding.cameraView.setFlash(Flash.TORCH);
                binding.flashToggle.setImageResource(R.drawable.ic_flash_on);
                isFlashOn = true;
            }
        });
    }

    private void processImage(InputImage inputImage) {
        if(!isDetected) {
            scanner.process(inputImage)
                    .addOnSuccessListener(barcodes -> {
                        processResult(barcodes);
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(MainActivity.this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private void processResult(List<Barcode> barcodes) {
        if(barcodes.size() > 0) {
            isDetected = true;
            for(Barcode barcode: barcodes) {
                int valueType = barcode.getValueType();
                switch (valueType) {
                    case Barcode.TYPE_TEXT:
                        createDialog(barcode.getRawValue());
                        break;
                }
            }
        }
    }

    private void createDialog(String text) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(text)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        isDetected = false;
                    }
                });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private InputImage getInputImageFromFrame(Frame frame) {
        byte[] data = frame.getData();
        return InputImage.fromByteArray(data, frame.getSize().getWidth(), frame.getSize().getHeight(), frame.getRotation(), InputImage.IMAGE_FORMAT_NV21);
    }

    private Bitmap uriToBitmap(Uri uri) throws IOException {
        return MediaStore.Images.Media.getBitmap(this.getContentResolver(), uri);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode == RESULT_OK) {
            if(requestCode == SELECT_PICTURE) {
                Uri selectedImageUri = data.getData();
                if(selectedImageUri != null) {
                    try {
                        InputImage image = InputImage.fromBitmap(uriToBitmap(selectedImageUri), 0);
                        processImage(image);
                    } catch (IOException e) {
                        Toast.makeText(MainActivity.this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }
    }
}