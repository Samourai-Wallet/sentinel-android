package com.samourai.sentinel;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;
//import android.widget.Toast;

import com.dm.zbar.android.scanner.ZBarConstants;
import com.dm.zbar.android.scanner.ZBarScannerActivity;
import com.samourai.sentinel.access.AccessFactory;
import com.samourai.sentinel.util.AppUtil;
import com.samourai.sentinel.util.FormatsUtil;

import net.sourceforge.zbar.Symbol;

import org.bitcoinj.core.AddressFormatException;
import org.bitcoinj.core.Base58;
import org.json.JSONException;

import java.io.IOException;
import java.nio.ByteBuffer;

public class InsertActivity extends Activity {

    private final static int SCAN_XPUB = 2011;
    private final static int INSERT_BIP49 = 2012;

    public final static int TYPE_BITCOIN_ADDRESS = 0;
    public final static int TYPE_LEGACY_XPUB = 1;
    public final static int TYPE_SEGWIT_XPUB = 2;

    private int storedType = 0;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_insert);

        setTitle(R.string.track_new);

        InsertActivity.this.setFinishOnTouchOutside(false);

        LinearLayout addressLayout = (LinearLayout)findViewById(R.id.address);
        addressLayout.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                storedType = TYPE_BITCOIN_ADDRESS;
                initDialog(TYPE_BITCOIN_ADDRESS);
                return false;
            }
        });

        LinearLayout bip44Layout = (LinearLayout)findViewById(R.id.bip44);
        bip44Layout.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                storedType = TYPE_LEGACY_XPUB;
                initDialog(TYPE_LEGACY_XPUB);
                return false;
            }
        });

        LinearLayout bip49Layout = (LinearLayout)findViewById(R.id.bip49);
        bip49Layout.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                storedType = TYPE_SEGWIT_XPUB;
                initDialog(TYPE_SEGWIT_XPUB);
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

                addXPUB(strResult, storedType);
            }
        }
        else if(resultCode == Activity.RESULT_CANCELED && requestCode == SCAN_XPUB)	{
            ;
        }
        else if(resultCode == Activity.RESULT_OK && requestCode == INSERT_BIP49)	{
            String xpub = data.getStringExtra("xpub");
            String label = data.getStringExtra("label");

            updateXPUBs(xpub, label, true);
            Log.d("InitActivity", "xpub inserted:" + xpub);
            Log.d("InitActivity", "label inserted:" + label);
            Toast.makeText(InsertActivity.this, R.string.xpub_add_ok, Toast.LENGTH_SHORT).show();

            Intent intent = new Intent(InsertActivity.this, BalanceActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        }
        else if(resultCode == Activity.RESULT_CANCELED && requestCode == INSERT_BIP49)	{
            Toast.makeText(InsertActivity.this, R.string.xpub_add_ko, Toast.LENGTH_SHORT).show();
        }
        else {
            ;
        }

    }

    private void initDialog(final int type)	{

        AccessFactory.getInstance(InsertActivity.this).setIsLoggedIn(false);

        new AlertDialog.Builder(this)
                .setTitle(R.string.app_name)
                .setMessage(R.string.please_select)
                .setPositiveButton(R.string.manual, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {

                        dialog.dismiss();

                        final EditText xpub = new EditText(InsertActivity.this);
                        xpub.setSingleLine(true);

                        new AlertDialog.Builder(InsertActivity.this)
                                .setTitle(R.string.app_name)
                                .setMessage(R.string.enter_xpub)
                                .setView(xpub)
                                .setCancelable(false)
                                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichButton) {

                                        dialog.dismiss();

                                        String xpubStr = xpub.getText().toString().trim();

                                        if (xpubStr != null && xpubStr.length() > 0) {
                                            addXPUB(xpubStr, type);
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
        Intent intent = new Intent(InsertActivity.this, ZBarScannerActivity.class);
        intent.putExtra(ZBarConstants.SCAN_MODES, new int[]{Symbol.QRCODE});
        startActivityForResult(intent, SCAN_XPUB);
    }

    private void addXPUB(final String xpubStr, final int type) {

        final EditText etLabel = new EditText(InsertActivity.this);
        etLabel.setSingleLine(true);
        etLabel.setHint(getText(R.string.xpub_label));

        new AlertDialog.Builder(InsertActivity.this)
                .setTitle(R.string.app_name)
                .setView(etLabel)
                .setCancelable(false)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {

                        dialog.dismiss();

                        final String label = etLabel.getText().toString().trim();

                        if(FormatsUtil.getInstance().isValidBitcoinAddress(xpubStr))    {
                            updateXPUBs(xpubStr, label, false);
                            Intent intent = new Intent(InsertActivity.this, BalanceActivity.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent);
                        }
                        else    {

                            if(type == TYPE_SEGWIT_XPUB)    {
                                Intent intent = new Intent(InsertActivity.this, InsertBIP49Activity.class);
                                intent.putExtra("xpub", xpubStr);
                                intent.putExtra("label", label);
                                startActivityForResult(intent, INSERT_BIP49);
                            }
                            else    {
                                updateXPUBs(xpubStr, label, false);
                                Intent intent = new Intent(InsertActivity.this, BalanceActivity.class);
                                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(intent);
                            }

                        }

                    }

                }).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {

                dialog.dismiss();

            }
        }).show();

    }

    private void updateXPUBs(String xpub, String label, boolean isBIP49)   {

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
                        Toast.makeText(InsertActivity.this, R.string.bip32_account, Toast.LENGTH_SHORT).show();
                        break;
                    // BIP44 account
                    case 3:
                        if(isBIP49)    {
                            Toast.makeText(InsertActivity.this, R.string.bip49_account, Toast.LENGTH_SHORT).show();
                        }
                        else    {
                            Toast.makeText(InsertActivity.this, R.string.bip44_account, Toast.LENGTH_SHORT).show();
                        }
                        break;
                    default:
                        // unknown
                        Toast.makeText(InsertActivity.this, InsertActivity.this.getText(R.string.unknown_xpub) + ":" + depth, Toast.LENGTH_SHORT).show();
                }
            }
            catch(AddressFormatException afe) {
                Toast.makeText(InsertActivity.this, R.string.base58_error, Toast.LENGTH_SHORT).show();
                return;
            }

            if(isBIP49)    {
                SamouraiSentinel.getInstance(InsertActivity.this).getBIP49().put(xpub, label);
            }
            else    {
                SamouraiSentinel.getInstance(InsertActivity.this).getXPUBs().put(xpub, label);
            }

        }
        else if(FormatsUtil.getInstance().isValidBitcoinAddress(xpub)) {
            SamouraiSentinel.getInstance(InsertActivity.this).getLegacy().put(xpub, label);
        }
        else {
            Toast.makeText(InsertActivity.this, R.string.invalid_entry, Toast.LENGTH_SHORT).show();
        }

        try {
            SamouraiSentinel.getInstance(InsertActivity.this).serialize(SamouraiSentinel.getInstance(InsertActivity.this).toJSON(), null);
        } catch (IOException ioe) {
            ioe.printStackTrace();
        } catch (JSONException je) {
            je.printStackTrace();
        }

    }

}
