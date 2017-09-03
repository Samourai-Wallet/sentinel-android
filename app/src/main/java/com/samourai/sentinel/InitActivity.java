package com.samourai.sentinel;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
//import android.widget.Toast;

import com.dm.zbar.android.scanner.ZBarConstants;
import com.dm.zbar.android.scanner.ZBarScannerActivity;
import com.samourai.sentinel.access.AccessFactory;
import com.samourai.sentinel.util.AppUtil;
import com.samourai.sentinel.util.FormatsUtil;
import com.samourai.sentinel.util.Web;

import net.sourceforge.zbar.Symbol;

import org.bitcoinj.core.AddressFormatException;
import org.bitcoinj.core.Base58;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.ByteBuffer;

public class InitActivity extends Activity {

    private ProgressDialog progress = null;
    private Handler handler = null;

    private final static int SCAN_XPUB = 2011;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_init);

        TextView tvStart = (TextView)findViewById(R.id.start);
        tvStart.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {

                initDialog();

                return false;
            }
        });

        ImageButton ibStart = (ImageButton)findViewById(R.id.startimg);
        ibStart.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {

                initDialog();

                return false;
            }
        });

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        if(resultCode == Activity.RESULT_OK && requestCode == SCAN_XPUB)	{

            if(data != null && data.getStringExtra(ZBarConstants.SCAN_RESULT) != null)	{

                String strResult = data.getStringExtra(ZBarConstants.SCAN_RESULT);

                if(strResult.startsWith("bitcoin:"))    {
                    strResult = strResult.substring(8);
                }
                if(strResult.indexOf("?") != -1)   {
                    strResult = strResult.substring(0, strResult.indexOf("?"));
                }

                addXPUB(strResult);
            }
        }
        else if(resultCode == Activity.RESULT_CANCELED && requestCode == SCAN_XPUB)	{
            ;
        }
        else {
            ;
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.init_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.action_restore) {
            AppUtil.getInstance(InitActivity.this).doRestore();
        }
        else {
            ;
        }

        return super.onOptionsItemSelected(item);
    }

    private void initDialog()	{

        AccessFactory.getInstance(InitActivity.this).setIsLoggedIn(false);

        new AlertDialog.Builder(this)
                .setTitle(R.string.app_name)
                .setMessage(R.string.please_select)
                .setPositiveButton(R.string.manual, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {

                        final EditText xpub = new EditText(InitActivity.this);
                        xpub.setSingleLine(true);

                        new AlertDialog.Builder(InitActivity.this)
                                .setTitle(R.string.app_name)
                                .setMessage(R.string.enter_xpub)
                                .setView(xpub)
                                .setCancelable(false)
                                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichButton) {

                                        dialog.dismiss();

                                        String xpubStr = xpub.getText().toString().trim();

                                        if (xpubStr != null && xpubStr.length() > 0) {
                                            addXPUB(xpubStr);
                                        }

                                    }

                                }).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {

                                dialog.dismiss();

                            }
                        }).show();

                    }
                })
                .setNegativeButton(R.string.scan, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {

                        dialog.dismiss();

                        doScan();

                    }
                }).show();

    }

    private void doScan() {
        Intent intent = new Intent(InitActivity.this, ZBarScannerActivity.class);
        intent.putExtra(ZBarConstants.SCAN_MODES, new int[]{Symbol.QRCODE});
        startActivityForResult(intent, SCAN_XPUB);
    }

    private void addXPUB(final String xpubStr) {

        final EditText etLabel = new EditText(InitActivity.this);
        etLabel.setSingleLine(true);
        etLabel.setHint(getText(R.string.xpub_label));

        handler = new Handler();

        new AlertDialog.Builder(InitActivity.this)
                .setTitle(R.string.app_name)
//                .setMessage(R.string.xpub_label)
                .setView(etLabel)
                .setCancelable(false)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {

                        dialog.dismiss();

                        final String label = etLabel.getText().toString().trim();

                        if(FormatsUtil.getInstance().isValidBitcoinAddress(xpubStr))    {
                            updateXPUBs(xpubStr, label, false, false);
                            AppUtil.getInstance(InitActivity.this).restartApp(true);
                        }
                        else    {

                            new AlertDialog.Builder(InitActivity.this)
                                    .setTitle(R.string.app_name)
                                    .setMessage(R.string.prompt_xpub_type)
                                    .setCancelable(false)
                                    .setPositiveButton(R.string.bip32_44, new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int whichButton) {
                                            updateXPUBs(xpubStr, label, false, false);
                                            AppUtil.getInstance(InitActivity.this).restartApp(true);
                                        }

                                    }).setNegativeButton(R.string.trezor, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {

                                    dialog.dismiss();

                                    Toast.makeText(InitActivity.this, R.string.please_wait, Toast.LENGTH_SHORT).show();

                                    new Thread(new Runnable() {
                                        @Override
                                        public void run() {

                                            Looper.prepare();

                                            String response = null;
                                            try {
                                                StringBuilder args = new StringBuilder();
                                                args.append("xpub=");
                                                args.append(xpubStr);
                                                args.append("&type=restore");
                                                args.append("&segwit=bip49");
                                                response = Web.postURL(Web.SAMOURAI_API2 + "xpub/", args.toString());

                                                Log.d("InitActivity", "BIP49:" + response);

                                                JSONObject obj = new JSONObject(response);
                                                if(obj != null && obj.has("status") && obj.getString("status").equals("ok"))    {
                                                    updateXPUBs(xpubStr, label, false, true);
                                                    handler.post(new Runnable() {
                                                        @Override
                                                        public void run() {
                                                            Toast.makeText(InitActivity.this, R.string.xpub_add_ok, Toast.LENGTH_SHORT).show();
                                                            AppUtil.getInstance(InitActivity.this).restartApp();
                                                        }
                                                    });
                                                }
                                                else    {
                                                    handler.post(new Runnable() {
                                                        @Override
                                                        public void run() {
                                                            Toast.makeText(InitActivity.this, R.string.xpub_add_ko, Toast.LENGTH_SHORT).show();
                                                        }
                                                    });
                                                }

                                            }
                                            catch(Exception e) {
                                                e.printStackTrace();
                                            }
                                            finally {
                                                ;
                                            }

                                            Looper.loop();

                                        }
                                    }).start();

                                }
                            }).show();

                        }

                    }

                }).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {

                ;

            }
        }).show();

    }

    private void updateXPUBs(String xpub, String label, boolean delete, boolean isBIP49)   {

        if (label == null || label.length() < 1) {
            label = getString(R.string.new_account);
        }

        if(FormatsUtil.getInstance().isValidXpub(xpub)) {

            try {
                // get depth
                byte[] xpubBytes = Base58.decodeChecked(xpub);
                ByteBuffer bb = ByteBuffer.wrap(xpubBytes);
                bb.getInt();
                // depth:
                byte depth = bb.get();
                switch(depth)    {
                    // BIP32 account
                    case 1:
                        Toast.makeText(InitActivity.this, R.string.bip32_account, Toast.LENGTH_SHORT).show();
                        break;
                    // BIP44 account
                    case 3:
                        if(isBIP49)    {
                            Toast.makeText(InitActivity.this, R.string.bip49_account, Toast.LENGTH_SHORT).show();
                        }
                        else    {
                            Toast.makeText(InitActivity.this, R.string.bip44_account, Toast.LENGTH_SHORT).show();
                        }
                        break;
                    default:
                        // unknown
                        Toast.makeText(InitActivity.this, InitActivity.this.getText(R.string.unknown_xpub) + ":" + depth, Toast.LENGTH_SHORT).show();
                }
            }
            catch(AddressFormatException afe) {
                Toast.makeText(InitActivity.this, R.string.base58_error, Toast.LENGTH_SHORT).show();
                return;
            }

            if(isBIP49)    {
                SamouraiSentinel.getInstance(InitActivity.this).getBIP49().put(xpub, label);
            }
            else    {
                SamouraiSentinel.getInstance(InitActivity.this).getLegacy().put(xpub, label);
            }

        }
        else if(FormatsUtil.getInstance().isValidBitcoinAddress(xpub)) {
            SamouraiSentinel.getInstance(InitActivity.this).getXPUBs().put(xpub, label);
        }
        else {
            Toast.makeText(InitActivity.this, R.string.invalid_entry, Toast.LENGTH_SHORT).show();
        }

        try {
            SamouraiSentinel.getInstance(InitActivity.this).serialize(SamouraiSentinel.getInstance(InitActivity.this).toJSON(), null);
        } catch (IOException ioe) {
            ioe.printStackTrace();
        } catch (JSONException je) {
            je.printStackTrace();
        }

    }

    private class BIP49Task extends AsyncTask<String, Void, String> {

        private Handler handler = null;

        @Override
        protected void onPreExecute() {
            handler = new Handler();

            ProgressDialog progress = new ProgressDialog(InitActivity.this);
            progress.setCancelable(false);
            progress.setTitle(R.string.app_name);
            progress.setMessage(getString(R.string.please_wait));
            progress.show();
        }

        @Override
        protected String doInBackground(String... params) {

            Looper.prepare();

            String response = null;
            try {
                StringBuilder args = new StringBuilder();
                args.append("xpub=");
                args.append(params[0]);
                args.append("&type=restore");
                args.append("&segwit=bip49");
                response = Web.postURL(Web.SAMOURAI_API2 + "xpub/", args.toString());

                Log.d("InitActivity", "BIP49:" + response);

                JSONObject obj = new JSONObject(response);
                if(obj != null && obj.has("status") && obj.getString("status").equals("ok"))    {
                    updateXPUBs(params[0], params[1], false, true);
                    if(progress != null && progress.isShowing())    {
                        progress.dismiss();
                        progress = null;
                    }
                    AppUtil.getInstance(InitActivity.this).restartApp(true);
                }

            }
            catch(Exception e) {
                e.printStackTrace();
            }
            finally {
                ;
            }

            Looper.loop();

            return "OK";
        }

        @Override
        protected void onPostExecute(String result) {
            if(progress != null && progress.isShowing())    {
                progress.dismiss();
            }
        }

        @Override
        protected void onProgressUpdate(Void... values) {
            ;
        }

    }

}
