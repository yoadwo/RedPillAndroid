package com.gingos.redpillandroid;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class Prescription {

    private static final String TAG_prescription = "RedPill_Prescription";
    private static final String VALIDATION = "FSPILLSEN@";

    private String pillName, pillMethod, pillFrequency, pillComments;
    private int pillTotal, pillEachDose;

    JSONObject jsonObj;

    // c-tor will set prescription paramters according to JSON
    // protocol is "FSPillsen@{<JSON-string>}"
    public Prescription(String raw){
        String info = raw.substring(raw.indexOf(VALIDATION) + VALIDATION.length());
        Log.d(TAG_prescription, "Prescription: info:[" + info +"]");
        try{
            jsonObj = new JSONObject(info);
            setPillName();
            setPillEachDose();
            setTotalPills();
            setPillMethod();
            setPillFrequency();
            setPillComments();
        } catch (final JSONException e){
            Log.e(TAG_prescription, "Json parsing error: " + e.getMessage());
        }


    }



    public String getDetails(){
        StringBuilder sb = new StringBuilder();

        sb.append("Name: \t\t" + getPillName() + "\n");
        sb.append("Total Pills: \t\t" + getTotalPills() + "\n");
        sb.append("Pills per dose: \t\t" + getPillEachDose() + "\n");
        sb.append("Method: \t\t" + getPillMethod() + "\n");
        sb.append("Frequency: \t\t" + getPillFrequency() + "\n");
        sb.append("Comments: \t\t" + getPillComments() + "\n");

        return sb.toString();
    }

    // parse name from JSON
    // name will be "not specified" if no such key exists
    private void setPillName() {
        try {
            pillName =  jsonObj.getString("name");
        } catch (JSONException e){
            Log.e(TAG_prescription, "Json SetName: " + e.getMessage());
            pillName = "Not Specified";
        }
    }

    public String getPillName(){
        return pillName;
    }

    // parse total pills given from JSON
    // amount will be -1 if no such key exists
    private void setTotalPills() {
        try {
            pillTotal =  jsonObj.getInt("totalPills");
        } catch (JSONException e){
            Log.e(TAG_prescription, "Json SetTotalPills: " + e.getMessage());
            pillTotal =  -1;
        }
    }

    public int getTotalPills(){
        return pillTotal;
    }

    // parse number of pills each dose from JSON
    // amount will be -1 if no such key exists
    private void setPillEachDose() {
        try {
            pillEachDose =  jsonObj.getInt("each");
        } catch (JSONException e){
            Log.e(TAG_prescription, "Json SetPillEachDose: " + e.getMessage());
            pillEachDose = -1;
        }
    }

    public int getPillEachDose(){
        return pillEachDose;
    }

    // parse method of taking from JSON
    // method will be "Not Specified" if no such key exists
    private void setPillMethod(){
        try {
            pillMethod =  jsonObj.getString("method");
        } catch (JSONException e){
            Log.e(TAG_prescription, "Json SetPillMethod: " + e.getMessage());
            pillMethod =  "Not Specified";
        }
    }

    public String getPillMethod(){
        return pillMethod;
    }

    // parse frequency of each dose from JSON
    // frequency will be "Not Specified" if no such key exists
    private void setPillFrequency() {
        try {
            pillFrequency =  jsonObj.getString("frequency");
        } catch (JSONException e){
            Log.e(TAG_prescription, "Json SetPillFrequency: " + e.getMessage());
            pillFrequency =  "Not Specified";
        }
    }

    // get human readable frequency of each dose from JSON
    // from codes like "D" or "BID" to "Daily" or "Twice a day"
    // also parses range with regular expressions
    // frequency will be "Illegal format" format does not match pattern
    public String getPillFrequency() {
        String frequency;
        String[] number = new String[2];
        if (pillFrequency.equals("Not Specified"))
            return pillFrequency;
        else{
            switch (pillFrequency) {
                case "D":
                    return "Daily";
                case "BID":
                    return "Twice a day";
                case "TID":
                    return "Three times a day";
                case "QID":
                    return "Four times a day";
                case "QHS":
                    return "Bed time";
                default:
                    frequency = pillFrequency;
                    break;
            }
            // Q#H(every #)
            String patternExact = "Q(\\d+)H", patternRange = "Q(\\d+\\-\\d+)H";
            if (frequency.matches(patternExact)){
                // strip Q, H
                number[0] = frequency.substring(1, frequency.length() - 1);
                return "every " + Integer.parseInt(number[0]) + " hours";
            }
            // Q#-#H(every #-#)
            else if (frequency.matches(patternRange)){
                number = frequency.substring(1, frequency.length() - 1).split("-");
                return "every " + number[0] + " to " + number[1] + " hours";
            }else
                return "illegal frequency format";
        }
    }

    // parse comments from JSON
    // comments will be "Not Specified" if no such key exists
    private void setPillComments() {
        try {
            pillComments =  jsonObj.getString("comments");
        } catch (JSONException e){
            Log.e(TAG_prescription, "Json SetComments: " + e.getMessage());
            pillComments =  "Not Specified";
        }
    }

    public String getPillComments(){
        // Comments node is JSON array
        try {
            JSONArray comments = jsonObj.getJSONArray("comments");
            return comments.toString().replaceAll("(\\[|\\]|\")", "");
        } catch (JSONException e) {
            Log.e(TAG_prescription, "getPillComments: " + e.getMessage() );
            // was already set as "Not specified" in its setter
            return pillComments;
        }
    }

}
