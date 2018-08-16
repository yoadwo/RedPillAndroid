package com.gingos.redpillandroid;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class Prescription {

    private static final String TAG_prescription = "RedPill_Prescription";
    private static final String VALIDATION = "FSPILLSEN@";

    private String pillName, pillMethod, pillFrequencyRaw, pillFrequencyStr, pillComments;
    private int pillTotal, pillEachDose, pillDays;
    private int[] pillFrequencyInt;
    private boolean valid;

    JSONObject jsonObj;

    // c-tor will set prescription paramters according to JSON
    // protocol is "FSPillsen@{<JSON-string>}"
    public Prescription(){

    }

    private void clear(){
        pillName = pillMethod = pillFrequencyRaw = pillComments = "";
        pillTotal = pillEachDose = pillDays = 0;
        valid = false;
    }

    public void parseJSON(String raw){
        String info = raw.substring(raw.indexOf(VALIDATION) + VALIDATION.length());
        Log.d(TAG_prescription, "Prescription: info:[" + info +"]");
        try{
            jsonObj = new JSONObject(info);
            // we could add clear() here for the safe side
            // will not do so yet - i believe all members are always being set
            setPillName();
            setPillEachDose();
            setTotalPills();
            setPillMethod();
            setPillFrequencyRaw();
            setPillComments();
            setPillDays();
            valid = true;
        } catch (final JSONException e){
            Log.e(TAG_prescription, "Json parsing error: " + e.getMessage());
            // if parsing fails, we do not want traces from previous parsing to remain
            // so we clear all class members
            clear();
        }
    }

    // use StringBuilder to get human-readable info on prescription
    public String getDetails(){
        StringBuilder sb = new StringBuilder();
        sb
            .append("Name: \t\t" + getPillName() + "\n")
            .append("Total Pills: \t\t" + getTotalPills() + "\n")
            .append("Pills per dose: \t\t" + getPillEachDose() + "\n")
            .append("Method: \t\t" + getPillMethod() + "\n")
            .append("Frequency: \t\t" + getPillFrequencyString() + "\n")
            .append("Drug taking duration: \t\t" + getPillDays() + "\n")
            .append("Comments: \t\t" + getPillComments() + "\n");

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
    private void setPillFrequencyRaw() {
        try {
            pillFrequencyRaw =  jsonObj.getString("frequency");
            setPillFrequencyString();
            setPillFrequencyInt();
        } catch (JSONException e){
            Log.e(TAG_prescription, "Json SetPillFrequency: " + e.getMessage());
            pillFrequencyRaw =  "Not Specified";
        }
    }

    public String getPillFrequencyRaw(){
        return pillFrequencyRaw;
    }

    // convert frequency raw into human readable frequency
    private void setPillFrequencyString(){
        String[] number = new String[2];
        if (pillFrequencyRaw.equals("Not Specified")){
            pillFrequencyStr = pillFrequencyRaw;
            return;
        }

        switch (pillFrequencyRaw) {
            case "D":
                pillFrequencyStr = "Daily";
                return;
            case "BID":
                pillFrequencyStr = "Twice a day";
                return;
            case "TID":
                pillFrequencyStr = "Three times a day";
                return;
            case "QID":
                pillFrequencyStr = "Four times a day";
                return;
            case "QHS":
                pillFrequencyStr = "Bed time";
                return;
            default:
                break;
        }
        // Q#H(every #)
        String patternExact = "Q(\\d+)H", patternRange = "Q(\\d+\\-\\d+)H";
        if (pillFrequencyRaw.matches(patternExact)){
            // strip Q, H
            number[0] = pillFrequencyRaw.substring(1, pillFrequencyRaw.length() - 1);
            pillFrequencyStr = "every " + Integer.parseInt(number[0]) + " hours";
        }
        // Q#-#H(every #-#)
        else if (pillFrequencyRaw.matches(patternRange)){
            // strip Q,H and then split #-# into {#, #} string array
            number = pillFrequencyRaw.substring(1, pillFrequencyRaw.length() - 1).split("-");
            pillFrequencyStr = "every " + number[0] + " to " + number[1] + " hours";
        }
        else
            pillFrequencyStr = "illegal frequency format";

    }

    // get human readable frequency of each dose from JSON
    // from codes like "D" or "BID" to "Daily" or "Twice a day"
    // also parses range with regular expressions
    // frequency will be "Illegal format" format does not match pattern
    public String getPillFrequencyString() {
        return pillFrequencyStr;
    }

    private void setPillFrequencyInt(){
        switch (pillFrequencyRaw) {
            case "Not Specified":
            case "illegal frequency format":
            case "D":
            case "QHS":
                pillFrequencyInt = new int[]{1,-1};
                return;
            case "BID":
                pillFrequencyInt = new int[]{2,-1};
                return;
            case "TID":
                pillFrequencyInt = new int[]{3,-1};
                return;
            case "QID":
                pillFrequencyInt = new int[] {4,-1};
                return;
            default:
                break;
        }
        // Q#H(every #)
        String patternExact = "Q(\\d+)H", patternRange = "Q(\\d+\\-\\d+)H";
        if (pillFrequencyRaw.matches(patternExact)){
            // strip Q, H
            // [#][0] signifies it is a repetitive frequency
            pillFrequencyInt = new int[]{Integer.parseInt(pillFrequencyRaw.substring(1, pillFrequencyRaw.length() - 1) ),0};
        }
        // Q#-#H(every #-#)
        else if (pillFrequencyRaw.matches(patternRange)) {
            String[] number = pillFrequencyRaw.substring(1, pillFrequencyRaw.length() - 1).split("-");
            pillFrequencyInt = new int[]{Integer.parseInt(number[0]), Integer.parseInt(number[1])};
        }
        else
            // some illegal format
            pillFrequencyInt = new int[] {1,-1};
            return;

    }
    public int[] getPillFrequencyInt(){
        return pillFrequencyInt;
    }

    // parse comments from JSON
    // comments will be "Not Specified" if no such key exists
    private void setPillComments() {
        try {
            JSONArray comments = jsonObj.getJSONArray("comments");
            pillComments = comments.toString().replaceAll("(\\[|\\]|\")", "").replaceAll(",", ", ");
        } catch (JSONException e){
            Log.e(TAG_prescription, "Json SetComments: " + e.getMessage());
            pillComments =  "Not Specified";
        }
    }

    // Comments node is JSON array
    public String getPillComments(){
        return pillComments;
    }

    private void setPillDays() {
        try {
            pillDays =  jsonObj.getInt("days");
        } catch (JSONException e){
            Log.e(TAG_prescription, "Json SetDays: " + e.getMessage());
            pillDays =  -1;
        }
    }

    public int getPillDays() {
        return pillDays;
    }

    public boolean isValid() {
        return valid;
    }
}
