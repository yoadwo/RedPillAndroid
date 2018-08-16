package com.gingos.redpillandroid;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.CalendarContract;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.vision.barcode.Barcode;

import java.util.Calendar;
import java.util.LinkedList;
import java.util.TimeZone;


public class MainActivity extends AppCompatActivity {

    private static final String TAG_main = "RedPill_MainActivity";

    private static final int BARCODE_REQUEST_CODE = 100;
    private static final int CALENDAR_WRITE_PERMISSION_REQUEST_CODE = 301;
    private static final int CALENDAR_READ_PERMISSION_REQUEST_CODE = 302;


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
                if (pre.isValid()) {
                    addToCalendar();
                } else {
                    Toast.makeText(MainActivity.this, "Prescription Invalid. Please Scan Again.", Toast.LENGTH_LONG).show();
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
                if (resultCode == RESULT_OK) {
                    Log.d(TAG_main, "onActivityResult: BARCODE_RESULT_OK");
                    if (data != null) {
                        barcode = data.getParcelableExtra("barcode");
                        Log.d(TAG_main, "onActivityResult: barcode raw: \n" + barcode.displayValue);
                        // validate Pill protocol
                        if (!barcode.displayValue.startsWith("FSPILLSEN@"))
                            Toast.makeText(getApplicationContext(), "Sorry, Illegal text format", Toast.LENGTH_LONG).show();
                        else {
                            // parse JSON into prescription and display on screen
                            ParseTextFromQR(barcode.displayValue);
                        }
                    } else {
                        // data == null
                        Toast.makeText(getApplicationContext(), "Sorry, data returned from camera scan is null", Toast.LENGTH_LONG).show();
                        txt_scanText.setText("No barcode found");
                    }
                } else if (resultCode == RESULT_CANCELED) {
                    if (data!= null){
                        String errstr = data.getParcelableExtra("permissionError");
                        if (errstr.equals("No Permission was granted to camera.")) {
                            Toast.makeText(getApplicationContext(), errstr, Toast.LENGTH_LONG).show();
                            Log.d(TAG_main, "onActivityResult: no permission was granted to camera.");
                        }
                    }
                    else {
                        Toast.makeText(getApplicationContext(), "Sorry, result Code is not OK", Toast.LENGTH_LONG).show();
                        Log.d(TAG_main, "onActivityResult: unknown error return from scan activity");
                    }
                }
                break;
            default:
                super.onActivityResult(requestCode, resultCode, data);
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

    // add to calendar using intents or URI, depending on user permission
    // if user chooses not to allow permissions, a more basic event will be created
    private void addToCalendar() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG_main, "addToCalendar: no write permission, requesting now.");
            ActivityCompat.requestPermissions(MainActivity.this, new String[] {Manifest.permission.WRITE_CALENDAR}, CALENDAR_WRITE_PERMISSION_REQUEST_CODE);
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG_main, "addToCalendar: no read permission, requesting now.");
            ActivityCompat.requestPermissions(MainActivity.this, new String[] {Manifest.permission.READ_CALENDAR}, CALENDAR_READ_PERMISSION_REQUEST_CODE);
        }
        addToCalendarUsingURI();
        return;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == CALENDAR_WRITE_PERMISSION_REQUEST_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG_main, "onRequestPermissionsResult: calendar write permission granted");
                //addToCalendarUsingURI();
            } else {
                Log.d(TAG_main, "onRequestPermissionsResult: calendar write permission denied");
                addToCalendarUsingIntents();
            }
        }
        else if (requestCode == CALENDAR_READ_PERMISSION_REQUEST_CODE){
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG_main, "onRequestPermissionsResult: calendar read permission granted");
                //addToCalendarUsingURI();
            } else {
                Log.d(TAG_main, "onRequestPermissionsResult: calendar read permission denied");
                addToCalendarUsingIntents();
            }
        }
    }

    // add to calendar using CalendarContract & Intents
    // DOES NOT REQUIRE PERMISSIONS
    // however, does allow reminder manipulation which is critical for app
    private void addToCalendarUsingIntents() {

        long beginMilli = calcBeginTimeInMillis();

        Intent intent = new Intent(Intent.ACTION_INSERT)
                .setData(CalendarContract.Events.CONTENT_URI)
                .putExtra(CalendarContract.Events.TITLE, "Take " + pre.getPillName())
                .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, beginMilli)
                .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, calcEndTimeInMillis(beginMilli))
                .putExtra(CalendarContract.Events.DESCRIPTION, pre.getPillComments())
                .putExtra(CalendarContract.Events.RRULE, calcRRule())
                .putExtra(CalendarContract.Events.AVAILABILITY, CalendarContract.Events.AVAILABILITY_BUSY);
        startActivity(intent);
    }

    // add to calendar using URI
    private void addToCalendarUsingURI() {
        Long eventID = createUriEvent();
        if (eventID == -1){
            Log.d(TAG_main, "addToCalendarUsingURI: uri is null perhaps cr.insert failed, eventId= " + eventID);
            Toast.makeText(getApplicationContext(), "Event add was not successful", Toast.LENGTH_LONG).show();
            return;
        }
        else{
            // possible that event is added even if cancelled during debug
            Log.d(TAG_main, "addToCalendarUsingURI: Event add successful, eventId= " + eventID);

            if (createUriReminders(eventID) == -1){
                Log.d(TAG_main, "addToCalendarUsingURI: could not add reminders, eventId= " + eventID);
                Toast.makeText(getApplicationContext(), "Event was added (but not all reminders were set)", Toast.LENGTH_LONG).show();
            } else {
                Log.d(TAG_main, "addToCalendarUsingURI: Reminders add was successful, eventId= " + eventID);
                Toast.makeText(getApplicationContext(), "Event add was successful", Toast.LENGTH_LONG).show();
            }
        }


    }

    // create a single event using URIs
    // suppressing permission request because requested in a higher function call
    @SuppressLint("MissingPermission")
    private long createUriEvent() {
        //long calID = getCalenderID();
        // hard code to 1 (samsung galaxy s6, android v7)
        long calID = 1;
        long beginMilli = calcBeginTimeInMillis();
        //long calID = getCalendarIDCursor();

        ContentResolver cr = getContentResolver();
        ContentValues values = new ContentValues();
        values.put(CalendarContract.Events.TITLE, "Take " + pre.getPillName());
        values.put(CalendarContract.Events.DTSTART, beginMilli);
        values.put(CalendarContract.Events.DTEND, calcEndTimeInMillis(beginMilli));
        values.put(CalendarContract.Events.DESCRIPTION, pre.getPillComments());
        values.put(CalendarContract.Events.CALENDAR_ID, calID);
        values.put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().getID());
        values.put(CalendarContract.Events.RRULE, calcRRule());

        //suppressed because handled in addCalendar()
        Uri uri = cr.insert(CalendarContract.Events.CONTENT_URI, values);

        if (uri == null){
            return -1;
        } else{
            // get the event ID that is the last element in the Uri
            return Long.parseLong(uri.getLastPathSegment());
        }
    }

    // start taking medication the next day
    // medications that are taken more than once a day start by the end of the day
    private long calcBeginTimeInMillis(){
        Calendar beginTime = Calendar.getInstance();
        // start tomorrow morning
        beginTime.add(Calendar.DAY_OF_YEAR,1);
        String frequency = pre.getPillFrequency();
        switch (frequency){
            case "BID":
            case "TID":
            case "QID":
                beginTime.set(Calendar.HOUR_OF_DAY, 20);
                break;
            case "QHS":
                beginTime.set(Calendar.HOUR_OF_DAY, 22);
                break;
            case "D":
                beginTime.set(Calendar.HOUR_OF_DAY, 8);
            default:
                beginTime.set(Calendar.HOUR_OF_DAY, 8);
                break;
        }
        if (frequency.matches("Q(\\d+)H") || frequency.matches("Q(\\d+\\-\\d+)H"))
            beginTime.set(Calendar.HOUR_OF_DAY, 20);
        beginTime.set(Calendar.MINUTE,0);
        return beginTime.getTimeInMillis();
    }

    // the medication event itself is only 1 hour long
    // other repetitions are set on RRule
    private long calcEndTimeInMillis(long beginMilli) {
        Calendar endTime = Calendar.getInstance();
        // offset tomorrow morning
        endTime.setTimeInMillis(beginMilli);
        endTime.add(Calendar.HOUR_OF_DAY, 1);
        return endTime.getTimeInMillis();

    }

    // create reminders to an existing event using URIs, based on its eventID
    @SuppressLint("MissingPermission")
    private long createUriReminders(Long eventID) {
        ContentResolver cr = getContentResolver();
        // list of ContentValues, later to be inserted as bulk
        LinkedList<ContentValues> valuesLinkedList = new LinkedList();
        ContentValues values;
        int[] frequencyArr = pre.getPillFrequencyInt();
        // reminders work (programmatically) by minutes, so we convert minutes to hours
        int H_Minutes = 60;
        int minutesSkip, minutesBefore = 0;

        if (frequencyArr[1] == -1) {
            if (frequencyArr[0] == 1) {
                // "Daily" and "Before bedtime" only need one alert and on time
                values = new ContentValues();
                values.put(CalendarContract.Reminders.EVENT_ID, eventID);
                values.put(CalendarContract.Reminders.METHOD, CalendarContract.Reminders.METHOD_ALERT);
                values.put(CalendarContract.Reminders.MINUTES, minutesBefore);
                valuesLinkedList.add(values);
            }
            else if (frequencyArr [0] > 1){
                // If need more than once a day, we calculate every how many hours we need the reminder
                // There is no such thing on android (v8 and before) as hourly recurrence, so we
                //      skip this by setting hour to late in the day and calculating the reminders backwards
                // we assume 12h day, so minutesSkip is 12/(2-1) = every 12, 12/(3-1) = every 6 and so on
                minutesSkip = (12 * H_Minutes ) / (frequencyArr[0] -1 );
                for (int i=0; i< frequencyArr[0]; i++){
                    values = new ContentValues();
                    values.put(CalendarContract.Reminders.EVENT_ID, eventID);
                    values.put(CalendarContract.Reminders.METHOD, CalendarContract.Reminders.METHOD_ALERT);
                    values.put(CalendarContract.Reminders.MINUTES, minutesBefore);

                    valuesLinkedList.add(values);
                    minutesBefore+=minutesSkip;
                }
            }
        }
        else if (frequencyArr[1] == 0){
            //values = new ContentValues();
            //values.put(CalendarContract.Reminders.EVENT_ID, eventID);
            //values.put(CalendarContract.Reminders.METHOD, CalendarContract.Reminders.METHOD_ALERT);

            // Again bypass the no-daily frequency limitation
            // Every (frequency) hours = total of (12/frequency)+1 times
            minutesSkip = H_Minutes * frequencyArr[0];
            for (int i=0; i< (12 / frequencyArr[0]) + 1; i++) {
                values = new ContentValues();
                values.put(CalendarContract.Reminders.EVENT_ID, eventID);
                values.put(CalendarContract.Reminders.METHOD, CalendarContract.Reminders.METHOD_ALERT);
                values.put(CalendarContract.Reminders.MINUTES, minutesBefore);

                valuesLinkedList.add(values);
                minutesBefore += minutesSkip;
            }
        }
        // continues frequency, with range
        // assume12h day, we divide 12 by middle point of range
        // (frequencyArr[1] >= 1)
        else {
            //daysCount = total / (dose * (12 / (((frequencyArr[0] + frequencyArr[1]) / 2))));
            int freqAverage = (frequencyArr[0] + frequencyArr[1]) /2;
            //values = new ContentValues();
            //values.put(CalendarContract.Reminders.EVENT_ID, eventID);
            //values.put(CalendarContract.Reminders.METHOD, CalendarContract.Reminders.METHOD_ALERT);

            // Again bypass the no-daily frequency limitation
            // we use mid-range as frequency
            // Every (frequency) hours = total of (12/frequency) + 1 times
            minutesSkip = H_Minutes * freqAverage;
            for (int i=0; i< (12 / freqAverage) + 1; i++) {
                values = new ContentValues();
                values.put(CalendarContract.Reminders.EVENT_ID, eventID);
                values.put(CalendarContract.Reminders.METHOD, CalendarContract.Reminders.METHOD_ALERT);
                values.put(CalendarContract.Reminders.MINUTES, minutesBefore);

                valuesLinkedList.add(values);
                minutesBefore += minutesSkip;
            }
        }


        // may not insert all rows
        //Uri uri = cr.insert(CalendarContract.Reminders.CONTENT_URI, values);
        int rows = cr.bulkInsert(
                CalendarContract.Reminders.CONTENT_URI,
                valuesLinkedList.toArray(new ContentValues[valuesLinkedList.size()]));
        // return -1 in case not all rows were added
        return rows == valuesLinkedList.size() ? rows : -1;
    }

    private String calcRRule(){

        StringBuilder rrule = new StringBuilder();

        //hard-coded, in the future could be changed according to actual research
        String freq = "DAILY";
        rrule.append("FREQ").append("=").append(freq).append(";");

        int  daysCount = pre.getPillDays();
        rrule.append("COUNT").append("=").append(daysCount).append(";");
        return rrule.toString();
    }

    // on android M (marshmallow, v6) and higher, default is 3
    private int getCalendarIDBySDK(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            return 3;
        }else{
            return 1;
        }
    }

    // https://stackoverflow.com/questions/35776265/android-create-calendar-event-always-as-birthday
    // if getCalendarID fails, use this
    // cursor to iterate different calendars
    @SuppressLint("MissingPermission")
    // suppressed because checked in addToCalendar()
    private int getCalendarIDCursor(){

        Cursor cursor = null;
        //ContentResolver contentResolver = context.getContentResolver();
        ContentResolver contentResolver = getContentResolver();
        Uri calendars = CalendarContract.Calendars.CONTENT_URI;

        String[] EVENT_PROJECTION = new String[] {
                CalendarContract.Calendars._ID,                           // 0
                CalendarContract.Calendars.ACCOUNT_NAME,                  // 1
                CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,         // 2
                CalendarContract.Calendars.OWNER_ACCOUNT,                 // 3
                CalendarContract.Calendars.VISIBLE,                       // 4
                CalendarContract.Calendars.IS_PRIMARY                     // 5
        };

        int PROJECTION_ID_INDEX = 0;
        int PROJECTION_ACCOUNT_NAME_INDEX = 1;
        int PROJECTION_DISPLAY_NAME_INDEX = 2;
        int PROJECTION_OWNER_ACCOUNT_INDEX = 3;
        int PROJECTION_VISIBLE = 4;
        int PROJECTION_IS_PRIMARY = 5;

        String selectionArgs = CalendarContract.Calendars.VISIBLE + " = 1 AND "
                + CalendarContract.Calendars.IS_PRIMARY + " = 1";
        cursor = contentResolver.query(calendars, EVENT_PROJECTION, null, null, null);

        if (cursor.moveToFirst()) {
            String calAccountName, calDisplName, calOwner;
            long calId = 0;
            String visible;
            String isPrimary;

             while (cursor.moveToNext()){
                 calId = cursor.getLong(PROJECTION_ID_INDEX);
                 calAccountName = cursor.getString(PROJECTION_ACCOUNT_NAME_INDEX);
                 calDisplName = cursor.getString(PROJECTION_DISPLAY_NAME_INDEX);
                 calOwner = cursor.getString(PROJECTION_OWNER_ACCOUNT_INDEX);
                 visible = cursor.getString(PROJECTION_VISIBLE);
                 isPrimary = cursor.getString(PROJECTION_IS_PRIMARY);
                 // commented out: every calendar but holidays and birthdays are primary, some are visible
                /*
                if(visible.equals("1") && isPrimary.equals("1")){
                    return (int)calId;
                }
                */
                 Log.d(TAG_main,"Calendar Id : " + calId + " Account Name : " + calAccountName + " Display Name : " + calDisplName
                         + "Owner : " + calOwner + " visible: " + visible + " isPrimary: " + isPrimary);
             }
            // now only returns last result! change condition inside while loop
            return (int)calId;
        }
        return 1;
    }


}
