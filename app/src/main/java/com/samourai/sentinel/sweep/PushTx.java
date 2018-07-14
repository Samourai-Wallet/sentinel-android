package com.samourai.sentinel.sweep;

import android.content.Context;

import com.samourai.sentinel.SamouraiSentinel;
import com.samourai.sentinel.util.Web;

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

        String _url = SamouraiSentinel.getInstance().isTestNet() ? Web.SAMOURAI_API2_TESTNET : Web.SAMOURAI_API2;

        try {
            String response = null;

            response = Web.postURL(_url + "v2/pushtx/", "tx=" + hexString);

            return response;
        }
        catch(Exception e) {
            return null;
        }

    }

}
