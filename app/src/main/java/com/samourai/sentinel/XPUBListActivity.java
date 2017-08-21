package com.samourai.sentinel;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.util.Pair;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.TextView;
import android.util.Log;
import android.widget.Toast;

import com.dm.zbar.android.scanner.ZBarConstants;
import com.dm.zbar.android.scanner.ZBarScannerActivity;

import net.i2p.android.ext.floatingactionbutton.FloatingActionButton;
import net.sourceforge.zbar.Symbol;

import org.bitcoinj.core.AddressFormatException;
import org.bitcoinj.core.Base58;
import org.bitcoinj.core.Coin;
import org.json.JSONException;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.samourai.sentinel.access.AccessFactory;

import com.baoyz.swipemenulistview.SwipeMenuCreator;
import com.baoyz.swipemenulistview.SwipeMenu;
import com.baoyz.swipemenulistview.SwipeMenuListView;
import com.baoyz.swipemenulistview.SwipeMenuItem;
import com.samourai.sentinel.api.APIFactory;
import com.samourai.sentinel.util.AppUtil;
import com.samourai.sentinel.util.FormatsUtil;
import com.samourai.sentinel.util.MonetaryUtil;
import com.samourai.sentinel.util.PrefsUtil;

public class XPUBListActivity extends Activity {

    private final static int SCAN_XPUB = 2011;

    private SwipeMenuListView xpubList = null;
    private XPUBAdapter xpubAdapter = null;

    private List<Pair<String,String>> xpubs = null;

    private FloatingActionButton actionXPUB = null;

    private boolean walletChanged = false;

    private ProgressDialog progress = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list);

        xpubs = getXPUBS();
        if(xpubs.size() == 0)    {
            PrefsUtil.getInstance(XPUBListActivity.this).setValue(PrefsUtil.XPUB, "");
            AppUtil.getInstance(XPUBListActivity.this).restartApp();
        }

        xpubList = (SwipeMenuListView)findViewById(R.id.xpubList);
        xpubAdapter = new XPUBAdapter();
        xpubList.setAdapter(xpubAdapter);

        xpubList.setOnMenuItemClickListener(new SwipeMenuListView.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(final int position, SwipeMenu menu, int index) {

                switch (index) {

                    case 0:

                        new AlertDialog.Builder(XPUBListActivity.this)
                                .setTitle(R.string.app_name)
                                .setMessage(R.string.sure_to_delete)
                                .setCancelable(false)
                                .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichButton) {

                                        dialog.dismiss();

                                        String xpub = xpubs.get(position).second;
                                        updateXPUBs(xpub, null, true);
                                        xpubs = getXPUBS();
                                        if(xpubs.size() == 0)    {
                                            PrefsUtil.getInstance(XPUBListActivity.this).setValue(PrefsUtil.XPUB, "");
                                            AppUtil.getInstance(XPUBListActivity.this).restartApp();
                                        }
                                        xpubAdapter.notifyDataSetChanged();

                                    }

                                }).setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {

                                ;

                            }
                        }).show();

                        break;

                    case 1:

                    {
                        Intent intent = new Intent(XPUBListActivity.this, ShowQRActivity.class);
                        intent.putExtra("label", xpubs.get(position).first);
                        intent.putExtra("xpub", xpubs.get(position).second);
                        startActivity(intent);
                    }

                    break;

                    case 2:

                        final EditText etLabel = new EditText(XPUBListActivity.this);
                        etLabel.setSingleLine(true);
                        etLabel.setHint(getText(R.string.xpub_label));
                        etLabel.setText(xpubs.get(position).first);

                        new AlertDialog.Builder(XPUBListActivity.this)
                                .setTitle(R.string.app_name)
                                .setView(etLabel)
                                .setCancelable(false)
                                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichButton) {

                                        dialog.dismiss();

                                        String label = etLabel.getText().toString().trim();
                                        String xpub = xpubs.get(position).second;
                                        updateXPUBs(xpub, label, false);
                                        xpubs = getXPUBS();
                                        xpubAdapter.notifyDataSetChanged();

                                    }

                                }).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {

                                ;

                            }
                        }).show();

                        break;

                }

                return false;
            }
        });

        SwipeMenuCreator creator = new SwipeMenuCreator() {

            @Override
            public void create(SwipeMenu menu) {

                // create "delete" item
                SwipeMenuItem deleteItem = new SwipeMenuItem(getApplicationContext());
                // set item background
                deleteItem.setBackground(new ColorDrawable(Color.rgb(0x17, 0x1B, 0x24)));
                // set item width
//                deleteItem.setWidth(dp2px(90));
                deleteItem.setWidth(180);
                // set a icon
                deleteItem.setIcon(R.drawable.ic_close_white_24dp);
                // add to menu
                menu.addMenuItem(deleteItem);

                // create "qr" item
                SwipeMenuItem qrItem = new SwipeMenuItem(getApplicationContext());
                // set item background
                qrItem.setBackground(new ColorDrawable(Color.rgb(0x17, 0x1B, 0x24)));
                // set item width
                qrItem.setWidth(180);
                // set a icon
                qrItem.setIcon(R.drawable.ic_receive_qr);
                // add to menu
                menu.addMenuItem(qrItem);

                // create "edit" item
                SwipeMenuItem openItem = new SwipeMenuItem(getApplicationContext());
                // set item background
                openItem.setBackground(new ColorDrawable(Color.rgb(0x17, 0x1B, 0x24)));
                // set item width
                openItem.setWidth(180);
                // set a icon
                openItem.setIcon(R.drawable.ic_edit_white_24dp);
                // add to menu
                menu.addMenuItem(openItem);

            }
        };

        xpubList.setMenuCreator(creator);
        xpubList.setSwipeDirection(SwipeMenuListView.DIRECTION_LEFT);

        actionXPUB = (FloatingActionButton)findViewById(R.id.xpub);
        actionXPUB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                initDialog();
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
    public boolean onKeyDown(int keyCode, KeyEvent event) {

        if(keyCode == KeyEvent.KEYCODE_BACK) {

            if(walletChanged)    {
                AppUtil.getInstance(XPUBListActivity.this).restartApp();
            }
            else    {
                finish();
            }

            return true;
        }
        else	{
            ;
        }

        return false;
    }

    private List<Pair<String,String>> getXPUBS()  {

        Pair<String,String> pair = null;
        List<Pair<String,String>> ret = new ArrayList<Pair<String,String>>();
        Set<String> keys = SamouraiSentinel.getInstance(XPUBListActivity.this).getXPUBs().keySet();
        for(String key : keys)   {
            pair = new Pair<String,String>(SamouraiSentinel.getInstance(XPUBListActivity.this).getXPUBs().get(key), key);
            ret.add(pair);
        }

        keys = SamouraiSentinel.getInstance(XPUBListActivity.this).getLegacy().keySet();
        for(String key : keys)   {
            pair = new Pair<String,String>(SamouraiSentinel.getInstance(XPUBListActivity.this).getLegacy().get(key), key);
            ret.add(pair);
        }

        return ret;
    }

    private void initDialog()	{

        AccessFactory.getInstance(XPUBListActivity.this).setIsLoggedIn(false);

        new AlertDialog.Builder(this)
                .setTitle(R.string.watchlist_title)
                .setMessage(R.string.watchlist_message)
                .setPositiveButton(R.string.manual, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {

                        final EditText xpub = new EditText(XPUBListActivity.this);
                        xpub.setSingleLine(true);

                        new AlertDialog.Builder(XPUBListActivity.this)
                                .setTitle(R.string.watchlist_title)
                                .setMessage(R.string.enter_xpub)
                                .setView(xpub)
                                .setCancelable(false)
                                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichButton) {

                                        String xpubStr = xpub.getText().toString().trim();

                                        if (xpubStr != null && xpubStr.length() > 0) {
                                            addXPUB(xpubStr);
                                        }

                                    }

                                }).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {

                                ;

                            }
                        }).show();

                    }
                })
                .setNegativeButton(R.string.scan, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {

                        doScan();

                    }
                }).show();

    }

    private void doScan() {
        Intent intent = new Intent(XPUBListActivity.this, ZBarScannerActivity.class);
        intent.putExtra(ZBarConstants.SCAN_MODES, new int[]{Symbol.QRCODE});
        startActivityForResult(intent, SCAN_XPUB);
    }

    private void addXPUB(final String xpub) {

        if(SamouraiSentinel.getInstance(XPUBListActivity.this).getXPUBs().containsKey(xpub))    {
            return;
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
                        Toast.makeText(XPUBListActivity.this, R.string.bip32_account, Toast.LENGTH_SHORT).show();
                        break;
                    // BIP44 account
                    case 3:
                        Toast.makeText(XPUBListActivity.this, R.string.bip44_account, Toast.LENGTH_SHORT).show();
                        break;
                    default:
                        // unknown
                        Toast.makeText(XPUBListActivity.this, XPUBListActivity.this.getText(R.string.unknown_xpub) + ":" + depth, Toast.LENGTH_SHORT).show();
                }
            }
            catch(AddressFormatException afe) {
                Toast.makeText(XPUBListActivity.this, R.string.base58_error, Toast.LENGTH_SHORT).show();
                return;
            }

        }
        else if(FormatsUtil.getInstance().isValidBitcoinAddress(xpub)) {
            ;
        }
        else {
            Toast.makeText(XPUBListActivity.this, R.string.invalid_entry, Toast.LENGTH_SHORT).show();
        }

        final EditText etLabel = new EditText(XPUBListActivity.this);
        etLabel.setSingleLine(true);
        etLabel.setHint(getText(R.string.xpub_label));

        new AlertDialog.Builder(XPUBListActivity.this)
                .setTitle(R.string.app_name)
//                .setMessage(R.string.xpub_label)
                .setView(etLabel)
                .setCancelable(false)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {

                        String label = etLabel.getText().toString().trim();
                        updateXPUBs(xpub, label, false);
                        xpubs = getXPUBS();
                        xpubAdapter.notifyDataSetChanged();

                        dialog.dismiss();

                    }

                }).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {

                ;

            }
        }).show();

    }

    private void updateXPUBs(String xpub, String label, boolean delete)   {

        if(delete)    {
            if(xpub.startsWith("xpub")) {
                SamouraiSentinel.getInstance(XPUBListActivity.this).getXPUBs().remove(xpub);
            }
            else if(FormatsUtil.getInstance().isValidBitcoinAddress(xpub)) {
                SamouraiSentinel.getInstance(XPUBListActivity.this).getLegacy().remove(xpub);
            }
            else {
                ;
            }
        }
        else    {
            if (label != null && label.length() > 0) {
                ;
            } else {
                label = getString(R.string.new_account);
            }

            if(xpub.startsWith("xpub")) {
                SamouraiSentinel.getInstance(XPUBListActivity.this).getXPUBs().put(xpub, label);
            }
            else if(FormatsUtil.getInstance().isValidBitcoinAddress(xpub)) {
                SamouraiSentinel.getInstance(XPUBListActivity.this).getLegacy().put(xpub, label);
            }
            else {
                ;
            }
        }

        walletChanged = true;

        try {
            SamouraiSentinel.getInstance(XPUBListActivity.this).serialize(SamouraiSentinel.getInstance(XPUBListActivity.this).toJSON(), null);
        } catch (IOException ioe) {
            ;
        } catch (JSONException je) {
            ;
        }

    }

    private class XPUBAdapter extends BaseAdapter {

        private LayoutInflater inflater = null;

        public XPUBAdapter() {
            inflater = (LayoutInflater)XPUBListActivity.this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        @Override
        public int getCount() {
            return xpubs.size();
        }

        @Override
        public Object getItem(int position) {
            return null;
        }

        @Override
        public long getItemId(int position) {
            return 0L;
        }

        @Override
        public View getView(final int position, View convertView, final ViewGroup parent) {

            View view = null;

            if (convertView == null) {
                LayoutInflater inflater = (LayoutInflater)XPUBListActivity.this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                view = inflater.inflate(android.R.layout.simple_list_item_2, null);
            }
            else    {
                view = convertView;
            }

            String strAmount = "";
            if(APIFactory.getInstance(XPUBListActivity.this).getXpubAmounts().containsKey(xpubs.get(position).second))    {
                long lamount = APIFactory.getInstance(XPUBListActivity.this).getXpubAmounts().get(xpubs.get(position).second);
                strAmount = " (" + getDisplayAmount(lamount) + " " + getDisplayUnits() + ")";
            }
            final SpannableStringBuilder strFirst = new SpannableStringBuilder(xpubs.get(position).first + strAmount);
            strFirst.setSpan(new android.text.style.StyleSpan(android.graphics.Typeface.BOLD), 0, strFirst.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            ((TextView)view.findViewById(android.R.id.text1)).setText(strFirst);
            if(xpubs.get(position).second.startsWith("xpub"))    {
                ((TextView)view.findViewById(android.R.id.text2)).setText("xpub..." + xpubs.get(position).second.substring(xpubs.get(position).second.length() - 3));
            }
            else    {
                ((TextView)view.findViewById(android.R.id.text2)).setText(xpubs.get(position).second);
            }

            return view;
        }
    }

    public String getDisplayAmount(long value) {

        String strAmount = null;
        DecimalFormat df = new DecimalFormat("#");
        df.setMinimumIntegerDigits(1);
        df.setMinimumFractionDigits(1);
        df.setMaximumFractionDigits(8);

        int unit = PrefsUtil.getInstance(XPUBListActivity.this).getValue(PrefsUtil.BTC_UNITS, MonetaryUtil.UNIT_BTC);
        switch(unit) {
            case MonetaryUtil.MICRO_BTC:
                strAmount = df.format(((double)(value * 1000000L)) / 1e8);
                break;
            case MonetaryUtil.MILLI_BTC:
                strAmount = df.format(((double)(value * 1000L)) / 1e8);
                break;
            default:
                strAmount = Coin.valueOf(value).toPlainString();
                break;
        }

        return strAmount;
    }

    public String getDisplayUnits() {

        return (String) MonetaryUtil.getInstance().getBTCUnits()[PrefsUtil.getInstance(XPUBListActivity.this).getValue(PrefsUtil.BTC_UNITS, MonetaryUtil.UNIT_BTC)];

    }

}
