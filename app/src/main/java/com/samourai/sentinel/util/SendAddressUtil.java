package com.samourai.sentinel.util;

import android.content.Context;

import java.util.HashMap;

public class SendAddressUtil {

    private static Context context = null;
    private static SendAddressUtil instance = null;

    private static HashMap<String,Boolean> sendAddresses = null;

    private SendAddressUtil() { ; }

    public static SendAddressUtil getInstance(Context ctx) {

        context = ctx;

        if(instance == null) {
            sendAddresses = new HashMap<String,Boolean>();
            instance = new SendAddressUtil();
        }

        return instance;
    }

    public void add(String addr, boolean showAgain) {
        sendAddresses.put(addr, showAgain);
    }

    public int get(String addr) {
        if(sendAddresses.get(addr) == null) {
            return -1;
        }
        else if (sendAddresses.get(addr) == true) {
            return 1;
        }
        else {
            return 0;
        }
    }

}
