package com.samourai.sentinel;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.content.LocalBroadcastManager;
import android.view.KeyEvent;
import android.widget.Toast;
//import android.util.Log;

import com.samourai.sentinel.access.AccessFactory;
import com.samourai.sentinel.api.APIFactory;
import com.samourai.sentinel.service.BackgroundManager;
import com.samourai.sentinel.service.WebSocketService;
import com.samourai.sentinel.util.AppUtil;
import com.samourai.sentinel.util.ConnectivityStatus;
import com.samourai.sentinel.util.ExchangeRateFactory;
import com.samourai.sentinel.util.PrefsUtil;
import com.samourai.sentinel.util.TimeOutUtil;
import com.samourai.sentinel.util.WebUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity2 extends Activity {

    private ProgressDialog progress = null;

    private CharSequence mTitle;

    private boolean isInForeground = false;

    private Timer timer = null;
    private Handler handler = null;

    protected BackgroundManager.Listener bgListener = new BackgroundManager.Listener()  {

        public void onBecameForeground()    {

            Intent intent = new Intent("com.samourai.sentinel.BalanceFragment.REFRESH\"");
            LocalBroadcastManager.getInstance(MainActivity2.this.getApplicationContext()).sendBroadcast(intent);
        }

        public void onBecameBackground()    {

            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if(AppUtil.getInstance(MainActivity2.this.getApplicationContext()).isServiceRunning(WebSocketService.class)) {
                    stopService(new Intent(MainActivity2.this.getApplicationContext(), WebSocketService.class));
                }
            }

        }

    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mTitle = getTitle();

        boolean isVerified = false;
        Bundle extras = getIntent().getExtras();
        if(extras != null && extras.containsKey("verified"))	{
            isVerified = extras.getBoolean("verified");
        }

        if(!ConnectivityStatus.hasConnectivity(MainActivity2.this))  {

            new AlertDialog.Builder(MainActivity2.this)
                    .setTitle(R.string.app_name)
                    .setMessage(R.string.no_internet)
                    .setCancelable(false)
                    .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            AppUtil.getInstance(MainActivity2.this).restartApp();
                        }
                    }).show();

        }
        else  {


            if(PrefsUtil.getInstance(MainActivity2.this).getValue("popup_" + getResources().getString(R.string.version_name), false) == true)	{

                if(PrefsUtil.getInstance(MainActivity2.this).getValue(PrefsUtil.XPUB, "").length() > 0 || SamouraiSentinel.getInstance(MainActivity2.this).payloadExists())	{

                    if(isVerified || PrefsUtil.getInstance(MainActivity2.this).getValue(PrefsUtil.PIN_HASH, "").length() == 0)	{

                        if(PrefsUtil.getInstance(MainActivity2.this).getValue(PrefsUtil.XPUB, "").length() > 0)	{

                            String xpub = PrefsUtil.getInstance(MainActivity2.this).getValue(PrefsUtil.XPUB, "");
                            SamouraiSentinel.getInstance(MainActivity2.this).getXPUBs().put(xpub, "My account");
                            PrefsUtil.getInstance(MainActivity2.this).removeValue(PrefsUtil.XPUB);

                            try {
                                SamouraiSentinel.getInstance(MainActivity2.this).serialize(SamouraiSentinel.getInstance(MainActivity2.this).toJSON(), null);
                            } catch (IOException ioe) {
                                ;
                            } catch (JSONException je) {
                                ;
                            }

                            AppUtil.getInstance(MainActivity2.this).restartApp();

                        }
                        else    {

                            try {
                                JSONObject obj = SamouraiSentinel.getInstance(MainActivity2.this).deserialize(null);

                                SamouraiSentinel.getInstance(MainActivity2.this).parseJSON(obj);

                                if(SamouraiSentinel.getInstance(MainActivity2.this).getXPUBs().keySet().size() < 1 &&
                                        SamouraiSentinel.getInstance(MainActivity2.this).getBIP49().keySet().size() < 1 &&
                                        SamouraiSentinel.getInstance(MainActivity2.this).getBIP84().keySet().size() < 1 &&
                                        SamouraiSentinel.getInstance(MainActivity2.this).getLegacy().keySet().size() < 1)    {
                                    SamouraiSentinel.getInstance(MainActivity2.this).restoreFromPrefs();
                                }

                                if(SamouraiSentinel.getInstance(MainActivity2.this).getXPUBs().keySet().size() < 1 &&
                                        SamouraiSentinel.getInstance(MainActivity2.this).getBIP49().keySet().size() < 1 &&
                                        SamouraiSentinel.getInstance(MainActivity2.this).getBIP84().keySet().size() < 1 &&
                                        SamouraiSentinel.getInstance(MainActivity2.this).getLegacy().keySet().size() < 1)    {
                                    Intent intent = new Intent(MainActivity2.this, InitActivity.class);
                                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                                    startActivity(intent);
                                }
                                else    {
                                    doTimer();

                                    Intent intent = new Intent(MainActivity2.this, BalanceActivity.class);
                                    startActivity(intent);
                                }

                            }
                            catch(IOException ioe)  {
                                Toast.makeText(MainActivity2.this, R.string.wallet_restored_ko, Toast.LENGTH_SHORT).show();
                            }
                            catch(JSONException je)  {
                                Toast.makeText(MainActivity2.this, R.string.wallet_restored_ko, Toast.LENGTH_SHORT).show();
                            }

                        }

                    }
                    else	{
                        Intent i = new Intent(MainActivity2.this, PinEntryActivity.class);
                        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
                        startActivity(i);
                    }
                }
                else	{

                    SamouraiSentinel.getInstance(MainActivity2.this).restoreFromPrefs();
                    if(SamouraiSentinel.getInstance(MainActivity2.this).getXPUBs().keySet().size() < 1 && SamouraiSentinel.getInstance(MainActivity2.this).getLegacy().keySet().size() < 1)    {
                        Intent intent = new Intent(MainActivity2.this, InitActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                    }
                    else    {
                        doTimer();

                        Intent intent = new Intent(MainActivity2.this, BalanceActivity.class);
                        startActivity(intent);
                    }

                }

            }
            else	{
                Intent intent = new Intent(MainActivity2.this, OneTimePopup.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            }
            feeThread();
            exchangeRateThread();
        }

        BackgroundManager.get(MainActivity2.this).addListener(bgListener);

    }

    @Override
    protected void onResume() {
        super.onResume();

        TimeOutUtil.getInstance().updatePin();

        AppUtil.getInstance(MainActivity2.this).setIsInForeground(true);

        AppUtil.getInstance(MainActivity2.this).deleteQR();

    }

    @Override
    protected void onPause() {
        super.onPause();

        AppUtil.getInstance(MainActivity2.this).setIsInForeground(false);
    }

    @Override
    protected void onDestroy() {

        AppUtil.getInstance(MainActivity2.this).deleteQR();

        if(AppUtil.getInstance(MainActivity2.this.getApplicationContext()).isServiceRunning(WebSocketService.class)) {
            stopService(new Intent(MainActivity2.this.getApplicationContext(), WebSocketService.class));
        }

        BackgroundManager.get(this).removeListener(bgListener);

        super.onDestroy();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        setIntent(intent);
    }

    private void doTimer() {

        if(timer == null) {
            timer = new Timer();
            handler = new Handler();

            timer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {

                            feeThread();
                            exchangeRateThread();

                        }
                    });
                }
            }, 1000, 60000 * 15);
        }

    }

    private void feeThread() {

        final Handler handler = new Handler();

        new Thread(new Runnable() {
            @Override
            public void run() {
                Looper.prepare();

                APIFactory.getInstance(MainActivity2.this).getDynamicFees();

                Looper.loop();

            }
        }).start();
    }

    private void exchangeRateThread() {

        final Handler handler = new Handler();

        new Thread(new Runnable() {
            @Override
            public void run() {
                Looper.prepare();

                String response = null;
                try {
                    response = WebUtil.getInstance(getApplicationContext()).getURL(WebUtil.LBC_EXCHANGE_URL);
                    ExchangeRateFactory.getInstance(MainActivity2.this).setDataLBC(response);
                    ExchangeRateFactory.getInstance(MainActivity2.this).parseLBC();
                } catch (Exception e) {
                    e.printStackTrace();
                }

                response = null;
                try {
                    response = WebUtil.getInstance(getApplicationContext()).getURL(WebUtil.BFX_EXCHANGE_URL);
                    ExchangeRateFactory.getInstance(MainActivity2.this).setDataBFX(response);
                    ExchangeRateFactory.getInstance(MainActivity2.this).parseBFX();
                } catch (Exception e) {
                    e.printStackTrace();
                }

                Looper.loop();

            }
        }).start();
    }

}
