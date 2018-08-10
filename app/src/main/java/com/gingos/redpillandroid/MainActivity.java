package com.gingos.redpillandroid;

import android.content.Intent;
import android.provider.CalendarContract;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.vision.barcode.Barcode;

import java.util.Calendar;

public class MainActivity extends AppCompatActivity {

    private static final String TAG_main = "RedPill_MainActivity";

    private static final int BARCODE_REQUEST_CODE = 100;
    private static final int BARCODE_RESULT_OK_CODE = 101;
    private static final int BARCODE_RESULT_ERROR_CODE = 102;

    Button btn_scan, btn_calendar;
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

        // to calendar button
        btn_calendar = findViewById(R.id.btn_calendar);
        btn_calendar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (pre.isValid()){
                    addToCalendar();
                }
                else {
                    Toast.makeText(MainActivity.this, "Prescription Invalid. Please Scan Again.",Toast.LENGTH_LONG).show();
                }
            }
        });

        // scan result text
        txt_scanText = findViewById(R.id.txt_scanText);


        // INIT MEMBERS
        pre = new Prescription();
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
        pre.parseJSON(raw);
        String details = pre.getDetails();
        Log.d(TAG_main, "ParseTextFromQR: " + details);
        txt_scanText.setText(details);

    }

    private void addToCalendar() {

        Intent intent = new Intent(Intent.ACTION_INSERT)
                .setData(CalendarContract.Events.CONTENT_URI)
                .putExtra(CalendarContract.Events.TITLE, "Take " + pre.getPillName() )
                .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, calcBeginTimeInMillis())
                .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, calcEndTimeInMillis())
                .putExtra(CalendarContract.Events.DESCRIPTION, pre.getPillComments())
                .putExtra(CalendarContract.Events.AVAILABILITY, CalendarContract.Events.AVAILABILITY_BUSY);
        startActivity(intent);

        /*pillName = pre.getPillName();
        pillTotal = pre.getTotalPillsGiven();
        pillMethod = pre.getMethod();
        pillFrequency = pre.getFrequency();
        pilllEachTime = pre.getEachTime();
        pillComments = pre.getComments();*/
    }

    private long calcBeginTimeInMillis(){
        Calendar beginTime = Calendar.getInstance();
        // start tomorrow morning
        beginTime.add(Calendar.DAY_OF_YEAR,1);
        beginTime.set(Calendar.HOUR_OF_DAY, 8);
        return beginTime.getTimeInMillis();
    }

    private long calcEndTimeInMillis(){
        Calendar endTime = Calendar.getInstance();
        // offset tomorrow morning
        endTime.add(Calendar.DAY_OF_YEAR,1);
        endTime.set(Calendar.HOUR_OF_DAY, 8);
        int frequency, days, dose = pre.getPillEachDose(), total = pre.getTotalPills() ;
        int[] frequencyArr;


        // depending on pill total, pill each dose and pill frequency
        frequencyArr = pre.getPillFrequencyInt();
        // non-continuous frequency
        if (frequencyArr[1] == -1)
            days = total / ( dose * frequencyArr[0] );
        // continuous frequency, no range
        // assume 12h day, we divide 12 by frequency
        else if (frequencyArr[1] == 0)
            days = total / ( dose * (12 / frequencyArr[0]) );
        // continues frequency, with range
        // assume12h day, we divide 12 by middle point of range
        // (frequencyArr[1] >= 1)
        else {
            int tempsum = frequencyArr[0] + frequencyArr[1];
            int tempaverage = tempsum / 2;
            days = total / (dose * (12 / (tempaverage)));
        }
        endTime.add(Calendar.DAY_OF_MONTH, days);
        return endTime.getTimeInMillis();
    }

}
