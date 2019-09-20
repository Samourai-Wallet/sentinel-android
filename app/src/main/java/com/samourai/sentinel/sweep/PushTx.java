package com.samourai.sentinel.sweep;

import android.content.Context;

import com.samourai.sentinel.SamouraiSentinel;
import com.samourai.sentinel.util.WebUtil;

public class PushTx {

    private static PushTx instance = null;
    private static Context context = null;

    private PushTx() { ; }

    public static PushTx getInstance(Context ctx) {

        context = ctx;

        if(instance == null) {
            instance = new PushTx();
        }

        return instance;
    }

    public String samourai(String hexString) {

        String _url = WebUtil.getAPIUrl(context);

        try {
            String response = null;

            response = WebUtil.getInstance(context).postURL(_url + "pushtx/", "tx=" + hexString);

            return response;
        }
        catch(Exception e) {
            return null;
        }

    }

}
