package com.samourai.sentinel.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;

import org.bitcoinj.crypto.MnemonicException;
import com.samourai.sentinel.hd.HD_WalletFactory;
import com.samourai.sentinel.util.ReceiveLookAtUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
//import android.util.Log;

public class WebSocketService extends Service {

    private Context context = null;

    private Timer timer = new Timer();
    private static final long checkIfNotConnectedDelay = 15000L;
    private WebSocketHandler webSocketHandler = null;
    private final Handler handler = new Handler();
    private String[] addrs = null;

    public static List<String> addrSubs = null;

    @Override
    public void onCreate() {

        super.onCreate();

        //
        context = this.getApplicationContext();

        try {
            if(HD_WalletFactory.getInstance(context).get() == null)    {
                return;
            }
        }
        catch(IOException ioe) {
            ioe.printStackTrace();
        }
        catch(MnemonicException.MnemonicLengthException mle) {
            mle.printStackTrace();
        }

        addrSubs = ReceiveLookAtUtil.getInstance().getReceives();
        addrs = addrSubs.toArray(new String[addrSubs.size()]);

        webSocketHandler = new WebSocketHandler(WebSocketService.this, addrs);
        connectToWebsocketIfNotConnected();

        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        connectToWebsocketIfNotConnected();
                    }
                });
            }
        }, 5000, checkIfNotConnectedDelay);

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public void connectToWebsocketIfNotConnected()
    {
        try {
            if(!webSocketHandler.isConnected()) {
                webSocketHandler.start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void stop() {
        try {
            if(webSocketHandler != null)    {
                webSocketHandler.stop();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDestroy()
    {
        stop();
        super.onDestroy();
    }

}