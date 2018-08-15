package com.gingos.redpillandroid;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseArray;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Toast;

import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;

import java.io.IOException;

public class ScanActivity extends AppCompatActivity {

    private static final String TAG_scan = "RedPill_ScanActivity";
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 200;

    SurfaceView cameraView;
    SurfaceHolder holder;
    CameraSource cameraSource;
    BarcodeDetector barcode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan);
        Log.d(TAG_scan, "onCreate: ");

        // REGISTER VIEWS
        // register camera
        cameraView = findViewById(R.id.cameraView);
        cameraView.setZOrderMediaOverlay(true);
        holder = cameraView.getHolder();

        // register barcode
        barcode = new BarcodeDetector.Builder(this)
                .setBarcodeFormats(Barcode.QR_CODE)
                .build();
        // close activity (camera) if cannot init barcode
        if(!barcode.isOperational()){
            Toast.makeText(getApplicationContext(), "Sorry, Couldn't setup the detector", Toast.LENGTH_LONG).show();
            this.finish();
        }

        // set camera view
        cameraView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            //When, in the first instance, the surface is created, this method is called.
            public void surfaceCreated(SurfaceHolder holder) {
                try{
                    Log.d(TAG_scan, "surfaceCreated: ");
                    if(ContextCompat.checkSelfPermission(ScanActivity.this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED){
                        cameraSource.start(cameraView.getHolder());
                    }else{
                        Log.d(TAG_scan, "surfaceCreated: no permission");
                        ActivityCompat.requestPermissions(ScanActivity.this, new String[] {Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST_CODE);
                    }

                }
                catch (IOException e){
                    e.printStackTrace();
                }
            }
            @Override
            // This method is called when the size or the format of the surface changes.
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                Log.d(TAG_scan, "surfaceChanged: ");
            }
            @Override
            // This is called when the surface is destroyed.
            public void surfaceDestroyed(SurfaceHolder holder) {
                Log.d(TAG_scan, "surfaceDestroyed: ");
            }
        });

        // set barcode detector
        barcode.setProcessor(new Detector.Processor<Barcode>() {
            @Override
            public void release() {

            }

            @Override
            public void receiveDetections(Detector.Detections<Barcode> detections) {
                final SparseArray<Barcode> barcodes =  detections.getDetectedItems();
                if(barcodes.size() > 0){
                    Intent intent = new Intent();
                    intent.putExtra("barcode", barcodes.valueAt(0));
                    setResult(RESULT_OK, intent);
                    finish();
                }
            }
        });

        // settings for camera
        cameraSource = new CameraSource.Builder(this, barcode)
                .setFacing(CameraSource.CAMERA_FACING_BACK)
                .setRequestedFps(24)
                .setAutoFocusEnabled(true)
                .setRequestedPreviewSize(1920,1024)
                .build();

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                Toast.makeText(this, "camera permission granted", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "camera permission denied", Toast.LENGTH_LONG).show();
                Log.d(TAG_scan, "onRequestPermissionsResult: camera permission denied");
                Intent intent = new Intent();
                intent.putExtra("permissionError", "No Permission was granted to camera.");
                setResult(RESULT_CANCELED, intent);
                finish();
            }
        }
    }
}
