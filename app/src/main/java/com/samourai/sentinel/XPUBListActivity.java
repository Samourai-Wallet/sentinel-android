package com.samourai.sentinel;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.util.Pair;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.baoyz.swipemenulistview.SwipeMenu;
import com.baoyz.swipemenulistview.SwipeMenuCreator;
import com.baoyz.swipemenulistview.SwipeMenuItem;
import com.baoyz.swipemenulistview.SwipeMenuListView;
import com.samourai.sentinel.api.APIFactory;
import com.samourai.sentinel.util.AppUtil;
import com.samourai.sentinel.util.FormatsUtil;
import com.samourai.sentinel.util.MonetaryUtil;
import com.samourai.sentinel.util.PrefsUtil;

import net.i2p.android.ext.floatingactionbutton.FloatingActionButton;

import org.bitcoinj.core.AddressFormatException;
import org.bitcoinj.core.Base58;
import org.bitcoinj.core.Coin;
import org.json.JSONException;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

//import android.util.Log;

public class XPUBListActivity extends AppCompatActivity {

    private SwipeMenuListView xpubList = null;
    private XPUBAdapter xpubAdapter = null;

    private List<Pair<String,String>> xpubs = null;

    private FloatingActionButton actionXPUB = null;

    private boolean walletChanged = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list);

        xpubs = getXPUBS();
        if(xpubs.size() == 0)    {
            PrefsUtil.getInstance(XPUBListActivity.this).setValue(PrefsUtil.XPUB, "");
            AppUtil.getInstance(XPUBListActivity.this).restartApp();
        }
        setSupportActionBar(findViewById(R.id.toolbar));

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
                                        int purpose = 44;
                                        if(SamouraiSentinel.getInstance(XPUBListActivity.this).getBIP49().keySet().contains(xpub))    {
                                            purpose = 49;
                                        }
                                        else if(SamouraiSentinel.getInstance(XPUBListActivity.this).getBIP84().keySet().contains(xpub))    {
                                            purpose = 84;
                                        }
                                        else    {
                                            purpose = 44;
                                        }
                                        updateXPUBs(xpub, null, true, purpose);
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
                                        int purpose = 44;
                                        if(SamouraiSentinel.getInstance(XPUBListActivity.this).getBIP49().keySet().contains(xpub))    {
                                            purpose = 49;
                                        }
                                        else if(SamouraiSentinel.getInstance(XPUBListActivity.this).getBIP84().keySet().contains(xpub))    {
                                            purpose = 84;
                                        }
                                        else    {
                                            purpose = 44;
                                        }
                                        updateXPUBs(xpub, label, false, purpose);
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

        if(getSupportActionBar()!=null)
          getSupportActionBar().setDisplayHomeAsUpEnabled(true);

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
                Intent intent = new Intent(XPUBListActivity.this, InsertActivity.class);
                startActivity(intent);
            }
        });

    }

    @Override
    public void onResume() {
        super.onResume();

        AppUtil.getInstance(XPUBListActivity.this).checkTimeOut();

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

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == android.R.id.home) {
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

    private List<Pair<String,String>> getXPUBS()  {

        Pair<String,String> pair = null;
        List<Pair<String,String>> ret = new ArrayList<Pair<String,String>>();
        HashMap<String,String> map = SamouraiSentinel.getInstance(XPUBListActivity.this).getAllMapSorted();
        Set<String> keys = map.keySet();
        for(String key : keys)   {
            if(SamouraiSentinel.getInstance(XPUBListActivity.this).getBIP49().keySet().contains(key))    {
                pair = new Pair<String,String>(SamouraiSentinel.getInstance(XPUBListActivity.this).getBIP49().get(key), key);
                ret.add(pair);
            }
            else if(SamouraiSentinel.getInstance(XPUBListActivity.this).getBIP84().keySet().contains(key))    {
                pair = new Pair<String,String>(SamouraiSentinel.getInstance(XPUBListActivity.this).getBIP84().get(key), key);
                ret.add(pair);
            }
            else if(SamouraiSentinel.getInstance(XPUBListActivity.this).getXPUBs().keySet().contains(key))    {
                pair = new Pair<String,String>(SamouraiSentinel.getInstance(XPUBListActivity.this).getXPUBs().get(key), key);
                ret.add(pair);
            }
            else if(SamouraiSentinel.getInstance(XPUBListActivity.this).getLegacy().keySet().contains(key))    {
                pair = new Pair<String,String>(SamouraiSentinel.getInstance(XPUBListActivity.this).getLegacy().get(key), key);
                ret.add(pair);
            }
            else    {
                ;
            }
        }

        return ret;
    }

    private void updateXPUBs(String xpub, String label, boolean delete, int purpose)   {

        if(delete)    {
            if(xpub.startsWith("xpub") || xpub.startsWith("ypub") || xpub.startsWith("zpub") || xpub.startsWith("tpub") || xpub.startsWith("upub") || xpub.startsWith("vpub")) {
                SamouraiSentinel.getInstance(XPUBListActivity.this).getXPUBs().remove(xpub);
                SamouraiSentinel.getInstance(XPUBListActivity.this).getBIP49().remove(xpub);
                SamouraiSentinel.getInstance(XPUBListActivity.this).getBIP84().remove(xpub);
            }
            else if(FormatsUtil.getInstance().isValidBitcoinAddress(xpub)) {
                SamouraiSentinel.getInstance(XPUBListActivity.this).getLegacy().remove(xpub);
            }
            else {
                ;
            }

            SamouraiSentinel.getInstance(XPUBListActivity.this).deleteFromPrefs(xpub);
        }
        else    {
            if (label == null || label.length() < 1) {
                label = getString(R.string.new_account);
            }

            if(FormatsUtil.getInstance().isValidBech32(xpub))    {
                Toast.makeText(XPUBListActivity.this, R.string.bech32_address, Toast.LENGTH_SHORT).show();
            }
            else if(FormatsUtil.getInstance().isValidBitcoinAddress(xpub))   {
                Toast.makeText(XPUBListActivity.this, R.string.bitcoin_address, Toast.LENGTH_SHORT).show();
            }
            else    {
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
                            if(purpose == 49)    {
                                Toast.makeText(XPUBListActivity.this, R.string.bip49_account, Toast.LENGTH_SHORT).show();
                            }
                            else if(purpose == 84)    {
                                Toast.makeText(XPUBListActivity.this, R.string.bip84_account, Toast.LENGTH_SHORT).show();
                            }
                            else    {
                                Toast.makeText(XPUBListActivity.this, R.string.bip44_account, Toast.LENGTH_SHORT).show();
                            }
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

            if(xpub.startsWith("xpub") || xpub.startsWith("ypub") || xpub.startsWith("zpub") || xpub.startsWith("tpub") || xpub.startsWith("upub") || xpub.startsWith("vpub")) {
                if(purpose == 49 || xpub.startsWith("ypub") || xpub.startsWith("upub"))    {
                    SamouraiSentinel.getInstance(XPUBListActivity.this).getBIP49().put(xpub, label);
                }
                else if(purpose == 84 || xpub.startsWith("zpub") || xpub.startsWith("vpub"))    {
                    SamouraiSentinel.getInstance(XPUBListActivity.this).getBIP84().put(xpub, label);
                }
                else    {
                    SamouraiSentinel.getInstance(XPUBListActivity.this).getXPUBs().put(xpub, label);
                }
            }
            else if(FormatsUtil.getInstance().isValidBech32(xpub) || FormatsUtil.getInstance().isValidBitcoinAddress(xpub))    {
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
            if(xpubs.get(position).second.startsWith("xpub") || xpubs.get(position).second.startsWith("ypub") || xpubs.get(position).second.startsWith("zpub") || xpubs.get(position).second.startsWith("tpub") || xpubs.get(position).second.startsWith("upub") || xpubs.get(position).second.startsWith("vpub"))    {
                ((TextView)view.findViewById(android.R.id.text2)).setText(xpubs.get(position).second.substring(0, 4) + "..." + xpubs.get(position).second.substring(xpubs.get(position).second.length() - 3));
            }
            else    {
                ((TextView)view.findViewById(android.R.id.text2)).setText(xpubs.get(position).second);
            }

            return view;
        }
    }

    public String getDisplayAmount(long value) {

        String strAmount = Coin.valueOf(value).toPlainString();

        return strAmount;
    }

    public String getDisplayUnits() {

        return (String) MonetaryUtil.getInstance().getBTCUnits();

    }

}
