package com.samourai.sentinel;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.FragmentManager;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.content.LocalBroadcastManager;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.Toast;
//import android.util.Log;

import com.google.bitcoin.core.AddressFormatException;
import com.samourai.sentinel.access.AccessFactory;
import com.samourai.sentinel.hd.HD_Account;
import com.samourai.sentinel.hd.HD_Wallet;
import com.samourai.sentinel.hd.HD_WalletFactory;
import com.samourai.sentinel.util.AddressFactory;
import com.samourai.sentinel.util.AppUtil;
import com.samourai.sentinel.util.ConnectivityStatus;
import com.samourai.sentinel.util.ExchangeRateFactory;
import com.samourai.sentinel.util.PrefsUtil;
import com.samourai.sentinel.util.TimeOutUtil;
import com.samourai.sentinel.util.Web;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.lang.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity2 extends Activity {

    private final static int SCAN_XPUB = 2011;

    private ProgressDialog progress = null;

    private CharSequence mTitle;

    private boolean isInForeground = false;

    private Timer timer = null;
    private Handler handler = null;

    private static String[] account_selections = null;
    private static ArrayAdapter<String> adapter = null;
    private static ActionBar.OnNavigationListener navigationListener = null;

    private static boolean loadedBalanceFragment = false;

    public static final String MIME_TEXT_PLAIN = "text/plain";
    private static final int MESSAGE_SENT = 1;

    private static int timer_updates = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //
        // account selection
        //
        account_selections = new String[1];
        account_selections[0] = "";
        adapter = new ArrayAdapter<String>(getBaseContext(), android.R.layout.simple_spinner_dropdown_item, account_selections);
        getActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
        navigationListener = new ActionBar.OnNavigationListener() {
            @Override
            public boolean onNavigationItemSelected(int itemPosition, long itemId) {

                SamouraiSentinel.getInstance(MainActivity2.this).setCurrentSelectedAccount(itemPosition);
                if(loadedBalanceFragment)    {
                    FragmentManager fragmentManager = getFragmentManager();
                    fragmentManager.beginTransaction().replace(R.id.container, BalanceFragment.newInstance(4)).commit();
                }

                return false;
            }
        };
        getActionBar().setListNavigationCallbacks(adapter, navigationListener);

        getActionBar().setTitle(R.string.app_name);

        mTitle = getTitle();

        boolean isVerified = false;
        Bundle extras = getIntent().getExtras();
        if(extras != null && extras.containsKey("verified"))	{
            isVerified = extras.getBoolean("verified");
        }

        loadedBalanceFragment = false;

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
            exchangeRateThread();

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

                                if(SamouraiSentinel.getInstance(MainActivity2.this).getXPUBs().keySet().size() < 1 && SamouraiSentinel.getInstance(MainActivity2.this).getLegacy().keySet().size() < 1)    {

                                    Intent intent = new Intent(MainActivity2.this, InitActivity.class);
                                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                                    startActivity(intent);
                                }
                                else    {
                                    restoreWatchOnly();
                                    doTimer();
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
                    Intent intent = new Intent(MainActivity2.this, InitActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                }

            }
            else	{
                Intent intent = new Intent(MainActivity2.this, OneTimePopup.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            }

        }

    }

    @Override
    protected void onResume() {
        super.onResume();

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

        super.onDestroy();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        setIntent(intent);
    }

    public void onSectionAttached(int number) {
        mTitle = getString(R.string.app_name);
    }

    public void restoreActionBar() {
        ActionBar actionBar = getActionBar();
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setTitle(mTitle);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        menu.getItem(0).setVisible(false);
        menu.getItem(1).setVisible(false);
        menu.getItem(2).setVisible(true);
        restoreActionBar();
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            doSettings();
        }
        else {
            ;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {

        if(keyCode == KeyEvent.KEYCODE_BACK) {

            if(getFragmentManager().getBackStackEntryCount() > 0) {
                getFragmentManager().popBackStack();
            }
            else {

                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage(R.string.ask_you_sure_exit).setCancelable(false);
                AlertDialog alert = builder.create();

                alert.setButton(AlertDialog.BUTTON_POSITIVE, getString(R.string.yes), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        AccessFactory.getInstance(MainActivity2.this).setIsLoggedIn(false);
                        TimeOutUtil.getInstance().reset();
                        dialog.dismiss();
                        moveTaskToBack(true);
                    }});

                alert.setButton(AlertDialog.BUTTON_NEGATIVE, getString(R.string.cancel), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.dismiss();
                    }});

                alert.show();

            }

            return true;
        }
        else	{
            ;
        }

        return false;
    }

    private void doSettings()	{
        TimeOutUtil.getInstance().updatePin();
        Intent intent = new Intent(MainActivity2.this, SettingsActivity.class);
        startActivity(intent);
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

                            timer_updates++;
                            if(timer_updates % 20 == 0)    {
                                exchangeRateThread();
                            }

                            Intent intent = new Intent("com.samourai.wallet.BalanceFragment.REFRESH");
                            LocalBroadcastManager.getInstance(MainActivity2.this).sendBroadcast(intent);
                        }
                    });
                }
            }, 15000, 15000);
        }

    }

    private void exchangeRateThread() {

        final Handler handler = new Handler();

        new Thread(new Runnable() {
            @Override
            public void run() {
                Looper.prepare();

                String response = null;
                try {
                    response = Web.getURL(Web.LBC_EXCHANGE_URL);
                    ExchangeRateFactory.getInstance(MainActivity2.this).setDataLBC(response);
                    ExchangeRateFactory.getInstance(MainActivity2.this).parseLBC();
                }
                catch(Exception e) {
                    e.printStackTrace();
                }

                response = null;
                try {
                    response = Web.getURL(Web.BTCe_EXCHANGE_URL + "btc_usd");
                    ExchangeRateFactory.getInstance(MainActivity2.this).setDataBTCe(response);
                    ExchangeRateFactory.getInstance(MainActivity2.this).parseBTCe();
                }
                catch(Exception e) {
                    e.printStackTrace();
                }

                response = null;
                try {
                    response = Web.getURL(Web.BTCe_EXCHANGE_URL + "btc_eur");
                    ExchangeRateFactory.getInstance(MainActivity2.this).setDataBTCe(response);
                    ExchangeRateFactory.getInstance(MainActivity2.this).parseBTCe();
                }
                catch(Exception e) {
                    e.printStackTrace();
                }

                response = null;
                try {
                    response = Web.getURL(Web.BTCe_EXCHANGE_URL + "btc_rur");
                    ExchangeRateFactory.getInstance(MainActivity2.this).setDataBTCe(response);
                    ExchangeRateFactory.getInstance(MainActivity2.this).parseBTCe();
                }
                catch(Exception e) {
                    e.printStackTrace();
                }

                response = null;
                try {
                    response = Web.getURL(Web.BFX_EXCHANGE_URL);
                    ExchangeRateFactory.getInstance(MainActivity2.this).setDataBFX(response);
                    ExchangeRateFactory.getInstance(MainActivity2.this).parseBFX();
                }
                catch(Exception e) {
                    e.printStackTrace();
                }

                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        ;
                    }
                });

                Looper.loop();

            }
        }).start();
    }

    private void restoreWatchOnly() {

        final Set<String> xpubKeys = SamouraiSentinel.getInstance(MainActivity2.this).getXPUBs().keySet();
        final List<String> xpubList = new ArrayList<String>();
        xpubList.addAll(xpubKeys);
//        Log.i("MainActivity2", "xpubs to restore:" + xpubList.size());

        final Handler handler = new Handler();

        if (progress != null && progress.isShowing()) {
            progress.dismiss();
            progress = null;
        }

        progress = new ProgressDialog(MainActivity2.this);
        progress.setCancelable(false);
        progress.setTitle(R.string.app_name);
        progress.setMessage(getString(R.string.please_wait));
        progress.show();

        new Thread(new Runnable() {
            @Override
            public void run() {

                Looper.prepare();

                if(xpubList.size() > 0)    {
                    try {
                        String xpubs = StringUtils.join(xpubList.toArray(new String[xpubList.size()]), ":");
//                        Log.i("MainActivity2", xpubs);
                        if(xpubKeys.size() > 0)    {
                            HD_Wallet hdw = HD_WalletFactory.getInstance(MainActivity2.this).restoreWallet(xpubs, null, 1);
                            if(hdw != null) {
                                List<HD_Account> accounts = hdw.getAccounts();
                                for(int i = 0; i < accounts.size(); i++)   {
                                    AddressFactory.getInstance().account2xpub().put(i, xpubList.get(i));
                                    AddressFactory.getInstance().xpub2account().put(xpubList.get(i), i);
                                }
                            }
                        }

                    }
                    catch(DecoderException de) {
                        PrefsUtil.getInstance(MainActivity2.this).clear();
                        Toast.makeText(MainActivity2.this, R.string.xpub_error, Toast.LENGTH_SHORT).show();
                        de.printStackTrace();
                    }
                    catch(AddressFormatException afe) {
                        PrefsUtil.getInstance(MainActivity2.this).clear();
                        Toast.makeText(MainActivity2.this, R.string.xpub_error, Toast.LENGTH_SHORT).show();
                        afe.printStackTrace();
                    }
                    finally {
                    }
                }

                if (progress != null && progress.isShowing()) {
                    progress.dismiss();
                    progress = null;
                }

                final Set<String> legacyKeys = SamouraiSentinel.getInstance(MainActivity2.this).getLegacy().keySet();
                final List<String> legacyList = new ArrayList<String>();
                xpubList.addAll(legacyKeys);

                handler.post(new Runnable() {
                    @Override
                    public void run() {

                        if(xpubList.size() == 1)    {
                            account_selections = new String[1];
                            if(xpubList.get(0).startsWith("xpub"))    {
                                account_selections[0] = SamouraiSentinel.getInstance(MainActivity2.this).getXPUBs().get(xpubList.get(0));
                            }
                            else    {
                                account_selections[0] = SamouraiSentinel.getInstance(MainActivity2.this).getLegacy().get(xpubList.get(0));
                            }
                        }
                        else    {
                            account_selections = new String[xpubList.size() + 1];
                            account_selections[0] = MainActivity2.this.getString(R.string.total_title);
                            for(int i = 0; i < xpubList.size(); i++)   {
                                if(xpubList.get(i).startsWith("xpub"))    {
                                    account_selections[i + 1] = SamouraiSentinel.getInstance(MainActivity2.this).getXPUBs().get(xpubList.get(i));
                                }
                                else    {
                                    account_selections[i + 1] = SamouraiSentinel.getInstance(MainActivity2.this).getLegacy().get(xpubList.get(i));
                                }
                            }
                        }
                        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            adapter = new ArrayAdapter<String>(getBaseContext(), android.R.layout.simple_spinner_dropdown_item, account_selections);
                        }
                        else    {
                            adapter = new ArrayAdapter<String>(getBaseContext(), R.layout.spinner_dropdown, account_selections);
                        }
                        getActionBar().setListNavigationCallbacks(adapter, navigationListener);
                        adapter.notifyDataSetChanged();
                        if(account_selections.length == 1)    {
                            SamouraiSentinel.getInstance(MainActivity2.this).setCurrentSelectedAccount(0);
                        }

                        if(xpubKeys.size() > 0 || legacyKeys.size() > 0 && !loadedBalanceFragment)    {
                            loadedBalanceFragment = true;
                            FragmentManager fragmentManager = getFragmentManager();
                            fragmentManager.beginTransaction().replace(R.id.container, BalanceFragment.newInstance(4)).commit();
                        }

                        try {
                            SamouraiSentinel.getInstance(MainActivity2.this).serialize(SamouraiSentinel.getInstance(MainActivity2.this).toJSON(), null);
                        }
                        catch(IOException ioe)  {
                            ;
                        }
                        catch(JSONException je)  {
                            ;
                        }

                    }
                });

                Looper.loop();

            }
        }).start();

    }

}