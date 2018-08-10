package com.gingos.redpillandroid;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.vision.barcode.Barcode;

public class MainActivity extends AppCompatActivity {

    private static final String TAG_main = "RedPill_MainActivity";

    private static final int BARCODE_REQUEST_CODE = 100;
    private static final int BARCODE_RESULT_OK_CODE = 101;
    private static final int BARCODE_RESULT_ERROR_CODE = 102;

    Button btn_scan;
    TextView txt_scanText;
    Barcode barcode;

    Prescription pre;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.d(TAG_main, "onCreate:");
        // REGISTER VIEWS

        // scan button
        btn_scan = findViewById(R.id.btn_scan);
        btn_scan.setOnClickListener(new View.OnClickListener() {
            // open scanner
            @Override
            public void onClick(View v) {
                Intent scanIntent = new Intent(MainActivity.this, ScanActivity.class);
                startActivityForResult(scanIntent, BARCODE_REQUEST_CODE);
            }
        });

        // scan result text
        txt_scanText = findViewById(R.id.txt_scanText);
    }

    // Listener for all returning activity request results
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case BARCODE_REQUEST_CODE:
                Log.d(TAG_main, "onActivityResult: BARCODE_REQUEST");
                if (resultCode == RESULT_OK){
                    Log.d(TAG_main, "onActivityResult: BARCODE_RESULT_OK");
                    if (data !=null){
                        barcode = data.getParcelableExtra("barcode");
                        Log.d(TAG_main, "onActivityResult: barcode raw: \n" + barcode.displayValue);
                        // validate Pill protocol
                        if (!barcode.displayValue.startsWith("FSPILLSEN@"))
                            Toast.makeText(getApplicationContext(), "Sorry, Illegal text format", Toast.LENGTH_LONG).show();
                        else {
                            // parse JSON into prescription and display on screen
                            ParseTextFromQR(barcode.displayValue);

                        }
                    }
                    else{
                        // data == null
                        Toast.makeText(getApplicationContext(), "Sorry, data returned from camera scan is null", Toast.LENGTH_LONG).show();
                        txt_scanText.setText("No barcode found");
                    }
                }
                else if (resultCode == RESULT_CANCELED){
                    Toast.makeText(getApplicationContext(), "Sorry, result Code is not OK", Toast.LENGTH_LONG).show();
                }
            break;
            default:
                super.onActivityResult(requestCode,resultCode, data);
        }
    }

    /*
     parse raw JSON into prescription parameters
     input: raw (:string) represents JSON format for pill info
     will set member variables according to JSON data
    */
    private void ParseTextFromQR(String raw) {
        pre = new Prescription(raw);
        String details = pre.getDetails();
        Log.d(TAG_main, "ParseTextFromQR: " + details);
        txt_scanText.setText(details);
        /*pillName = pre.getPillName();
        pillTotal = pre.getTotalPillsGiven();
        pillMethod = pre.getMethod();
        pillFrequency = pre.getFrequency();
        pilllEachTime = pre.getEachTime();
        pillComments = pre.getComments();*/
    }
}
