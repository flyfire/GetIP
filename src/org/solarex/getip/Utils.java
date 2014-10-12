package org.solarex.getip;

import android.util.Log;

public class Utils {
    private final static String TAG = "IPGet";
    private Utils(){
    }
    public static void log(String msg){
        Log.d(TAG, msg!=null ? msg : "null");
    }
}

