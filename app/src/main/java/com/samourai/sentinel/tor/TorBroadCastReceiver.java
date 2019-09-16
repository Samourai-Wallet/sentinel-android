package com.samourai.sentinel.tor;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class TorBroadCastReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Intent serviceIntent = new Intent(context, TorService.class);
        serviceIntent.setAction(intent.getAction());
        context.startService(serviceIntent);
    }
}
