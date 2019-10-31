package com.samourai.sentinel;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.text.InputType;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.AnticipateInterpolator;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.dm.zbar.android.scanner.ZBarConstants;
import com.samourai.sentinel.access.AccessFactory;
import com.samourai.sentinel.api.APIFactory;
import com.samourai.sentinel.api.Tx;
import com.samourai.sentinel.codescanner.CameraFragmentBottomSheet;
import com.samourai.sentinel.hd.HD_Account;
import com.samourai.sentinel.hd.HD_Wallet;
import com.samourai.sentinel.hd.HD_WalletFactory;
import com.samourai.sentinel.network.dojo.DojoUtil;
import com.samourai.sentinel.network.dojo.Network;
import com.samourai.sentinel.permissions.PermissionsUtil;
import com.samourai.sentinel.service.WebSocketService;
import com.samourai.sentinel.sweep.PrivKeyReader;
import com.samourai.sentinel.sweep.SweepUtil;
import com.samourai.sentinel.util.AddressFactory;
import com.samourai.sentinel.util.AppUtil;
import com.samourai.sentinel.util.BlockExplorerUtil;
import com.samourai.sentinel.util.CharSequenceX;
import com.samourai.sentinel.util.DateUtil;
import com.samourai.sentinel.util.ExchangeRateFactory;
import com.samourai.sentinel.util.MonetaryUtil;
import com.samourai.sentinel.util.PrefsUtil;
import com.samourai.sentinel.util.TimeOutUtil;
import com.samourai.sentinel.util.TypefaceUtil;

import net.i2p.android.ext.floatingactionbutton.FloatingActionButton;
import net.i2p.android.ext.floatingactionbutton.FloatingActionsMenu;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.lang3.StringUtils;
import org.bitcoinj.core.AddressFormatException;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.crypto.BIP38PrivateKey;
import org.bitcoinj.crypto.MnemonicException;
import org.json.JSONException;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

//import android.util.Log;

public class BalanceActivity extends AppCompatActivity {

    private final static int SCAN_COLD_STORAGE = 2011;

    private LinearLayout tvBalanceBar = null;
    private TextView tvBalanceAmount = null;
    private TextView tvBalanceUnits = null;

    private ListView txList = null;
    private List<Tx> txs = null;
    private HashMap<String, Boolean> txStates = null;
    private TransactionAdapter txAdapter = null;
    private SwipeRefreshLayout swipeRefreshLayout = null;

    private FloatingActionsMenu ibQuickSend = null;
    private FloatingActionButton actionReceive = null;
    private FloatingActionButton actionXPUB = null;

    private boolean isBTC = true;

    private static String[] account_selections = null;
    private static ArrayAdapter<String> adapter = null;

    public static final String ACTION_INTENT = "com.samourai.sentinel.BalanceFragment.REFRESH";

    private ProgressDialog progress = null;
    private Spinner accountSpinner;
    protected BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            if (ACTION_INTENT.equals(intent.getAction())) {

                BalanceActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        refreshTx(false);
                    }
                });

            }

        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_balance);

        setSupportActionBar(findViewById(R.id.toolbar));

        accountSpinner = findViewById(R.id.account_spinner);

        if (getSupportActionBar() != null)
            getSupportActionBar().setTitle(R.string.app_name);
        account_selections = new String[1];
        account_selections[0] = "";
        adapter = new ArrayAdapter<String>(getBaseContext(), android.R.layout.simple_spinner_dropdown_item, account_selections);
        accountSpinner.setAdapter(adapter);
        setSupportActionBar(findViewById(R.id.toolbar));

        accountSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                SamouraiSentinel.getInstance(BalanceActivity.this).setCurrentSelectedAccount(position);

                refreshTx(false);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        LayoutInflater inflator = BalanceActivity.this.getLayoutInflater();
        tvBalanceBar = (LinearLayout) inflator.inflate(R.layout.balance_layout, null);
        tvBalanceBar.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                isBTC = (isBTC) ? false : true;
                displayBalance();
                txAdapter.notifyDataSetChanged();
                return false;
            }
        });
        tvBalanceAmount = (TextView) tvBalanceBar.findViewById(R.id.BalanceAmount);
        tvBalanceUnits = (TextView) tvBalanceBar.findViewById(R.id.BalanceUnits);

        ibQuickSend = (FloatingActionsMenu) findViewById(R.id.wallet_menu);
        actionReceive = (FloatingActionButton) findViewById(R.id.receive);
        actionReceive.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {

                confirmAccountSelection(false);

            }
        });

        actionXPUB = (FloatingActionButton) findViewById(R.id.xpub);
        actionXPUB.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {

                TimeOutUtil.getInstance().updatePin();
                Intent intent = new Intent(BalanceActivity.this, XPUBListActivity.class);
                startActivity(intent);

            }
        });

        txList = (ListView) findViewById(R.id.txList);
        txAdapter = new TransactionAdapter();
        txList.setAdapter(txAdapter);
        txList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, final View view, int position, long id) {

                if (position == 0) {
                    return;
                }

                long viewId = view.getId();
                View v = (View) view.getParent();
                Tx tx = txs.get(position - 1);
                ImageView ivTxStatus = (ImageView) v.findViewById(R.id.TransactionStatus);
                TextView tvConfirmationCount = (TextView) v.findViewById(R.id.ConfirmationCount);

                if (viewId == R.id.ConfirmationCount || viewId == R.id.TransactionStatus) {

                    if (txStates.containsKey(tx.getHash()) && txStates.get(tx.getHash()) == true) {
                        txStates.put(tx.getHash(), false);
                        displayTxStatus(false, tx.getConfirmations(), tvConfirmationCount, ivTxStatus);
                    } else {
                        txStates.put(tx.getHash(), true);
                        displayTxStatus(true, tx.getConfirmations(), tvConfirmationCount, ivTxStatus);
                    }

                } else {

                    String strTx = tx.getHash();
                    if (strTx != null) {
                        int sel = PrefsUtil.getInstance(BalanceActivity.this).getValue(PrefsUtil.BLOCK_EXPLORER, 0);
                        CharSequence url = BlockExplorerUtil.getInstance().getBlockExplorerUrls()[sel];

                        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url + strTx));
                        startActivity(browserIntent);
                    }

                }

            }
        });

        swipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swiperefresh);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {

                new Handler().post(new Runnable() {
                    @Override
                    public void run() {
                        refreshTx(true);
                    }
                });

            }
        });
        swipeRefreshLayout.setColorSchemeResources(android.R.color.holo_blue_bright,
                android.R.color.holo_green_light,
                android.R.color.holo_orange_light,
                android.R.color.holo_red_light);

        if (!AppUtil.getInstance(BalanceActivity.this.getApplicationContext()).isServiceRunning(WebSocketService.class) && !DojoUtil.getInstance(getApplicationContext()).isDojoEnabled()) {
            BalanceActivity.this.startService(new Intent(BalanceActivity.this.getApplicationContext(), WebSocketService.class));
        }

        if (!PermissionsUtil.getInstance(BalanceActivity.this).hasPermission(Manifest.permission.CAMERA)) {
            PermissionsUtil.getInstance(BalanceActivity.this).showRequestPermissionsInfoAlertDialog(PermissionsUtil.CAMERA_PERMISSION_CODE);
        }

        restoreWatchOnly();

    }

    @Override
    public void onResume() {
        super.onResume();

        IntentFilter filter = new IntentFilter(ACTION_INTENT);
        LocalBroadcastManager.getInstance(BalanceActivity.this).registerReceiver(receiver, filter);

        AppUtil.getInstance(BalanceActivity.this).checkTimeOut();

    }

    @Override
    public void onPause() {
        super.onPause();

        LocalBroadcastManager.getInstance(BalanceActivity.this).unregisterReceiver(receiver);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);

        menu.getItem(0).setVisible(false);
        menu.getItem(1).setVisible(false);
        menu.getItem(2).setVisible(true);
//        restoreActionBar();

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();

        if (id == R.id.action_settings) {
            doSettings();
        }
        if (id == R.id.action_network) {
            startActivity(new Intent(this, Network.class));
        } else if (id == R.id.action_sweep) {
            confirmAccountSelection(true);
        } else {
            ;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (resultCode == Activity.RESULT_OK && requestCode == SCAN_COLD_STORAGE) {

            if (data != null && data.getStringExtra(ZBarConstants.SCAN_RESULT) != null) {

                final String strResult = data.getStringExtra(ZBarConstants.SCAN_RESULT);

                doPrivKey(strResult);

            }
        } else if (resultCode == Activity.RESULT_CANCELED && requestCode == SCAN_COLD_STORAGE) {
            ;
        } else {
            ;
        }

    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {

        if (keyCode == KeyEvent.KEYCODE_BACK) {

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(R.string.ask_you_sure_exit).setCancelable(false);
            AlertDialog alert = builder.create();

            alert.setButton(AlertDialog.BUTTON_POSITIVE, getString(R.string.yes), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    AccessFactory.getInstance(BalanceActivity.this).setIsLoggedIn(false);
                    TimeOutUtil.getInstance().reset();
                    dialog.dismiss();

                    Intent intent = new Intent(BalanceActivity.this, ExodusActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                }
            });

            alert.setButton(AlertDialog.BUTTON_NEGATIVE, getString(R.string.cancel), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    dialog.dismiss();
                }
            });

            alert.show();

            return true;
        } else {
            ;
        }

        return false;
    }

    public void restoreActionBar() {
        ActionBar actionBar = getActionBar();
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setTitle(R.string.app_name);
    }

    private void doSettings() {
        TimeOutUtil.getInstance().updatePin();
        Intent intent = new Intent(BalanceActivity.this, SettingsActivity.class);
        startActivity(intent);
    }

    private class TransactionAdapter extends BaseAdapter {

        private LayoutInflater inflater = null;
        private static final int TYPE_ITEM = 0;
        private static final int TYPE_BALANCE = 1;

        TransactionAdapter() {
            inflater = (LayoutInflater) BalanceActivity.this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        @Override
        public int getCount() {
            if (txs == null) {
                txs = new ArrayList<Tx>();
                txStates = new HashMap<String, Boolean>();
            }
            return txs.size() + 1;
        }

        @Override
        public String getItem(int position) {
            if (txs == null) {
                txs = new ArrayList<Tx>();
                txStates = new HashMap<String, Boolean>();
            }
            if (position == 0) {
                return "";
            }
            return txs.get(position - 1).toString();
        }

        @Override
        public long getItemId(int position) {
            return position - 1;
        }

        @Override
        public int getItemViewType(int position) {
            return position == 0 ? TYPE_BALANCE : TYPE_ITEM;
        }

        @Override
        public int getViewTypeCount() {
            return 2;
        }

        @Override
        public View getView(final int position, View convertView, final ViewGroup parent) {

            View view = null;

            int type = getItemViewType(position);
            if (convertView == null) {
                if (type == TYPE_BALANCE) {
                    view = tvBalanceBar;
                } else {
                    view = inflater.inflate(R.layout.tx_layout_simple, parent, false);
                }
            } else {
                view = convertView;
            }

            if (type == TYPE_BALANCE) {
                ;
            } else {
                view.findViewById(R.id.TransactionStatus).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        ((ListView) parent).performItemClick(v, position, 0);
                    }
                });

                view.findViewById(R.id.ConfirmationCount).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        ((ListView) parent).performItemClick(v, position, 0);
                    }
                });

                Tx tx = txs.get(position - 1);

                TextView tvTodayLabel = (TextView) view.findViewById(R.id.TodayLabel);
                String strDateGroup = DateUtil.getInstance(BalanceActivity.this).group(tx.getTS());
                if (position == 1) {
                    tvTodayLabel.setText(strDateGroup);
                    tvTodayLabel.setVisibility(View.VISIBLE);
                } else {
                    Tx prevTx = txs.get(position - 2);
                    String strPrevDateGroup = DateUtil.getInstance(BalanceActivity.this).group(prevTx.getTS());

                    if (strPrevDateGroup.equals(strDateGroup)) {
                        tvTodayLabel.setVisibility(View.GONE);
                    } else {
                        tvTodayLabel.setText(strDateGroup);
                        tvTodayLabel.setVisibility(View.VISIBLE);
                    }
                }

                String strDetails = null;
                String strTS = DateUtil.getInstance(BalanceActivity.this).formatted(tx.getTS());
                long _amount = 0L;
                if (tx.getAmount() < 0.0) {
                    _amount = Math.abs((long) tx.getAmount());
                    strDetails = BalanceActivity.this.getString(R.string.you_sent);
                } else {
                    _amount = (long) tx.getAmount();
                    strDetails = BalanceActivity.this.getString(R.string.you_received);
                }
                String strAmount = null;
                String strUnits = null;
                if (isBTC) {
                    strAmount = getBTCDisplayAmount(_amount);
                    strUnits = getBTCDisplayUnits();
                } else {
                    strAmount = getFiatDisplayAmount(_amount);
                    strUnits = getFiatDisplayUnits();
                }

                TextView tvDirection = (TextView) view.findViewById(R.id.TransactionDirection);
                TextView tvDirection2 = (TextView) view.findViewById(R.id.TransactionDirection2);
                TextView tvDetails = (TextView) view.findViewById(R.id.TransactionDetails);
                ImageView ivTxStatus = (ImageView) view.findViewById(R.id.TransactionStatus);
                TextView tvConfirmationCount = (TextView) view.findViewById(R.id.ConfirmationCount);

                tvDirection.setTypeface(TypefaceUtil.getInstance(BalanceActivity.this).getAwesomeTypeface());
                if (tx.getAmount() < 0.0) {
                    tvDirection.setTextColor(Color.RED);
                    tvDirection.setText(Character.toString((char) TypefaceUtil.awesome_arrow_up));
                } else {
                    tvDirection.setTextColor(Color.GREEN);
                    tvDirection.setText(Character.toString((char) TypefaceUtil.awesome_arrow_down));
                }

                if (txStates.containsKey(tx.getHash()) && txStates.get(tx.getHash()) == false) {
                    txStates.put(tx.getHash(), false);
                    displayTxStatus(false, tx.getConfirmations(), tvConfirmationCount, ivTxStatus);
                } else {
                    txStates.put(tx.getHash(), true);
                    displayTxStatus(true, tx.getConfirmations(), tvConfirmationCount, ivTxStatus);
                }

                tvDirection2.setText(strDetails + " " + strAmount + " " + strUnits);
                tvDetails.setText(strTS);
            }

            return view;
        }

    }

    public void refreshTx(final boolean dragged) {

        final Handler handler = new Handler();

        new Thread(new Runnable() {
            @Override
            public void run() {
                Looper.prepare();

                APIFactory.getInstance(getApplicationContext()).stayingAlive();

                runOnUiThread(() -> {
                    swipeRefreshLayout.setRefreshing(true);
                });

                int idx = SamouraiSentinel.getInstance(BalanceActivity.this).getCurrentSelectedAccount();

                List<String> _xpubs = SamouraiSentinel.getInstance(BalanceActivity.this).getAllAddrsSorted();
                if (idx == 0) {
                    APIFactory.getInstance(BalanceActivity.this).getXPUB(_xpubs.toArray(new String[_xpubs.size()]));
                } else {
                    APIFactory.getInstance(BalanceActivity.this).getXPUB(new String[]{_xpubs.get(idx - 1)});
                }

                if (idx == 0) {
                    txs = APIFactory.getInstance(BalanceActivity.this).getAllXpubTxs();
                } else {
                    txs = APIFactory.getInstance(BalanceActivity.this).getXpubTxs().get(_xpubs.get(idx - 1));
                }

                try {
                    if (HD_WalletFactory.getInstance(BalanceActivity.this).get() != null) {

                        HD_Wallet hdw = HD_WalletFactory.getInstance(BalanceActivity.this).get();

                        for (int i = 0; i < hdw.getAccounts().size(); i++) {
                            HD_WalletFactory.getInstance(BalanceActivity.this).get().getAccount(i).getReceive().setAddrIdx(AddressFactory.getInstance().getHighestTxReceiveIdx(i));
                        }

                    }
                } catch (IOException ioe) {
                    ;
                } catch (MnemonicException.MnemonicLengthException mle) {
                    ;
                }

                if (!AppUtil.getInstance(BalanceActivity.this.getApplicationContext()).isServiceRunning(WebSocketService.class) && !DojoUtil.getInstance(getApplicationContext()).isDojoEnabled()) {
                    startService(new Intent(BalanceActivity.this.getApplicationContext(), WebSocketService.class));
                }

                PrefsUtil.getInstance(BalanceActivity.this).setValue(PrefsUtil.FIRST_RUN, false);

                handler.post(new Runnable() {
                    public void run() {
                        swipeRefreshLayout.setRefreshing(false);

                        txAdapter.notifyDataSetChanged();
                        displayBalance();
                    }
                });

                Looper.loop();

            }
        }).start();

    }

    public void displayBalance() {
        String strFiat = PrefsUtil.getInstance(BalanceActivity.this).getValue(PrefsUtil.CURRENT_FIAT, "USD");
        double btc_fx = ExchangeRateFactory.getInstance(BalanceActivity.this).getAvgPrice(strFiat);

        int idx = SamouraiSentinel.getInstance(BalanceActivity.this).getCurrentSelectedAccount();
        long balance = 0L;

        List<String> _xpubs = SamouraiSentinel.getInstance(BalanceActivity.this).getAllAddrsSorted();
        if (idx == 0) {
            balance = APIFactory.getInstance(BalanceActivity.this).getXpubBalance();
        } else if (_xpubs.get(idx - 1) != null && APIFactory.getInstance(BalanceActivity.this).getXpubAmounts().get(_xpubs.get(idx - 1)) != null) {
            if (APIFactory.getInstance(BalanceActivity.this).getXpubAmounts().get(_xpubs.get(idx - 1)) != null) {
                balance = APIFactory.getInstance(BalanceActivity.this).getXpubAmounts().get(_xpubs.get(idx - 1));
            }
        } else {
            ;
        }

        double btc_balance = (((double) balance) / 1e8);
        double fiat_balance = btc_fx * btc_balance;

        if (isBTC) {
            tvBalanceAmount.setText(getDisplayAmount(balance));
            tvBalanceUnits.setText(getDisplayUnits());
        } else {
            tvBalanceAmount.setText(MonetaryUtil.getInstance().getFiatFormat(strFiat).format(fiat_balance));
            tvBalanceUnits.setText(strFiat);
        }

    }

    public String getDisplayAmount(long value) {

        String strAmount = null;
        DecimalFormat df = new DecimalFormat("#");
        df.setMinimumIntegerDigits(1);
        df.setMinimumFractionDigits(1);
        df.setMaximumFractionDigits(8);

        strAmount = Coin.valueOf(value).toPlainString();

        return strAmount;
    }

    public String getDisplayUnits() {

        return MonetaryUtil.getInstance().getBTCUnits();

    }

    private void displayTxStatus(boolean heads, long confirmations, TextView tvConfirmationCount, ImageView ivTxStatus) {

        if (heads) {
            if (confirmations == 0) {
                rotateTxStatus(tvConfirmationCount, true);
                ivTxStatus.setVisibility(View.VISIBLE);
                ivTxStatus.setImageResource(R.drawable.ic_query_builder_white);
                tvConfirmationCount.setVisibility(View.GONE);
            } else if (confirmations > 3) {
                rotateTxStatus(tvConfirmationCount, true);
                ivTxStatus.setVisibility(View.VISIBLE);
                ivTxStatus.setImageResource(R.drawable.ic_done_white);
                tvConfirmationCount.setVisibility(View.GONE);
            } else {
                rotateTxStatus(ivTxStatus, false);
                tvConfirmationCount.setVisibility(View.VISIBLE);
                tvConfirmationCount.setText(Long.toString(confirmations));
                ivTxStatus.setVisibility(View.GONE);
            }
        } else {
            if (confirmations < 100) {
                rotateTxStatus(ivTxStatus, false);
                tvConfirmationCount.setVisibility(View.VISIBLE);
                tvConfirmationCount.setText(Long.toString(confirmations));
                ivTxStatus.setVisibility(View.GONE);
            } else {
                rotateTxStatus(ivTxStatus, false);
                tvConfirmationCount.setVisibility(View.VISIBLE);
                tvConfirmationCount.setText("\u221e");
                ivTxStatus.setVisibility(View.GONE);
            }
        }

    }

    private void rotateTxStatus(View view, boolean clockwise) {

        float degrees = 360f;
        if (!clockwise) {
            degrees = -360f;
        }

        ObjectAnimator animation = ObjectAnimator.ofFloat(view, "rotationY", 0.0f, degrees);
        animation.setDuration(1000);
        animation.setRepeatCount(0);
        animation.setInterpolator(new AnticipateInterpolator());
        animation.start();
    }

    private void doSweepViaScan() {
        CameraFragmentBottomSheet cameraFragmentBottomSheet = new CameraFragmentBottomSheet();
        cameraFragmentBottomSheet.setQrCodeScanLisenter(code -> {
            cameraFragmentBottomSheet.dismiss();
            doPrivKey(code);
        });
        cameraFragmentBottomSheet.show(this.getSupportFragmentManager(), cameraFragmentBottomSheet.getTag());
    }

    private void doSweep() {
        AlertDialog.Builder dlg = new AlertDialog.Builder(BalanceActivity.this)
                .setTitle(R.string.app_name)
                .setMessage(R.string.action_sweep)
                .setCancelable(true)
                .setPositiveButton(R.string.enter_privkey, (dialog, whichButton) -> {

                    dialog.dismiss();

                    final EditText privkey = new EditText(BalanceActivity.this);
                    privkey.setInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);

                    AlertDialog.Builder dlg1 = new AlertDialog.Builder(BalanceActivity.this)
                            .setTitle(R.string.app_name)
                            .setMessage(R.string.enter_privkey)
                            .setView(privkey)
                            .setCancelable(false)
                            .setPositiveButton(R.string.ok, (dialog1, whichButton1) -> {

                                dialog1.dismiss();

                                final String strPrivKey = privkey.getText().toString();

                                if (strPrivKey != null && strPrivKey.length() > 0) {
                                    doPrivKey(strPrivKey);
                                }

                            }).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {

                                    dialog.dismiss();

                                }
                            });
                    if (!isFinishing()) {
                        dlg1.show();
                    }

                }).setNegativeButton(R.string.scan, (dialog, whichButton) -> {

                    dialog.dismiss();

                    doSweepViaScan();

                });
        if (!isFinishing()) {
            dlg.show();
        }

    }

    private void doPrivKey(final String data) {

//        Log.d("BalanceActivity", "privkey:" + data);

        PrivKeyReader privKeyReader = null;

        String format = null;
        try {
            privKeyReader = new PrivKeyReader(new CharSequenceX(data), null);
            format = privKeyReader.getFormat();
//            Log.d("BalanceActivity", "privkey format:" + format);
        } catch (Exception e) {
            Toast.makeText(BalanceActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
            return;
        }

        if (format != null) {

            if (format.equals(PrivKeyReader.BIP38)) {

                final PrivKeyReader pvr = privKeyReader;

                final EditText password38 = new EditText(BalanceActivity.this);

                AlertDialog.Builder dlg = new AlertDialog.Builder(BalanceActivity.this)
                        .setTitle(R.string.app_name)
                        .setMessage(R.string.bip38_pw)
                        .setView(password38)
                        .setCancelable(false)
                        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {

                                dialog.dismiss();

                                String password = password38.getText().toString();

                                ProgressDialog progress = new ProgressDialog(BalanceActivity.this);
                                progress.setCancelable(false);
                                progress.setTitle(R.string.app_name);
                                progress.setMessage(getString(R.string.decrypting_bip38));
                                progress.show();

                                boolean keyDecoded = false;

                                try {
                                    BIP38PrivateKey bip38 = new BIP38PrivateKey(SamouraiSentinel.getInstance().getCurrentNetworkParams(), data);
                                    final ECKey ecKey = bip38.decrypt(password);
                                    if (ecKey != null && ecKey.hasPrivKey()) {

                                        if (progress != null && progress.isShowing()) {
                                            progress.cancel();
                                        }

                                        pvr.setPassword(new CharSequenceX(password));
                                        keyDecoded = true;

                                        Toast.makeText(BalanceActivity.this, pvr.getFormat(), Toast.LENGTH_SHORT).show();
                                        Toast.makeText(BalanceActivity.this, pvr.getKey().toAddress(SamouraiSentinel.getInstance().getCurrentNetworkParams()).toString(), Toast.LENGTH_SHORT).show();

                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                    Toast.makeText(BalanceActivity.this, R.string.bip38_pw_error, Toast.LENGTH_SHORT).show();
                                }

                                if (progress != null && progress.isShowing()) {
                                    progress.cancel();
                                }

                                if (keyDecoded) {
                                    String strReceiveAddress = SamouraiSentinel.getInstance(BalanceActivity.this).getReceiveAddress();
                                    if (strReceiveAddress != null) {
                                        SweepUtil.getInstance(BalanceActivity.this).sweep(pvr, strReceiveAddress, SweepUtil.TYPE_P2PKH);
                                    }
                                }

                            }
                        }).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {

                                dialog.dismiss();

                                Toast.makeText(BalanceActivity.this, R.string.bip38_pw_error, Toast.LENGTH_SHORT).show();

                            }
                        });
                if (!isFinishing()) {
                    dlg.show();
                }

            } else if (privKeyReader != null) {
                String strReceiveAddress = SamouraiSentinel.getInstance(BalanceActivity.this).getReceiveAddress();
                if (strReceiveAddress != null) {
                    Log.d("BalanceActivity", "receive address:" + strReceiveAddress);
                    SweepUtil.getInstance(BalanceActivity.this).sweep(privKeyReader, strReceiveAddress, SweepUtil.TYPE_P2PKH);
                }
            } else {
                ;
            }

        } else {
            Toast.makeText(BalanceActivity.this, R.string.cannot_recognize_privkey, Toast.LENGTH_SHORT).show();
        }

    }

    private void confirmAccountSelection(final boolean isSweep) {

        final List<String> xpubList = SamouraiSentinel.getInstance(BalanceActivity.this).getAllAddrsSorted();

        if (xpubList.size() == 1) {
            SamouraiSentinel.getInstance(BalanceActivity.this).setCurrentSelectedAccount(1);
            if (isSweep) {
                doSweep();
                return;
            } else {
                Intent intent = new Intent(BalanceActivity.this, ReceiveActivity.class);
                startActivity(intent);
                return;
            }
        }

        final String[] accounts = new String[xpubList.size()];
        for (int i = 0; i < xpubList.size(); i++) {
            if ((xpubList.get(i).startsWith("xpub") || xpubList.get(i).startsWith("tpub")) && SamouraiSentinel.getInstance(BalanceActivity.this).getXPUBs().containsKey(xpubList.get(i))) {
                accounts[i] = SamouraiSentinel.getInstance(BalanceActivity.this).getXPUBs().get(xpubList.get(i));
            } else if ((xpubList.get(i).startsWith("xpub") || xpubList.get(i).startsWith("ypub") || xpubList.get(i).startsWith("tpub") || xpubList.get(i).startsWith("upub")) && SamouraiSentinel.getInstance(BalanceActivity.this).getBIP49().containsKey(xpubList.get(i))) {
                accounts[i] = SamouraiSentinel.getInstance(BalanceActivity.this).getBIP49().get(xpubList.get(i));
            } else if ((xpubList.get(i).startsWith("zpub") || xpubList.get(i).startsWith("vpub")) && SamouraiSentinel.getInstance(BalanceActivity.this).getBIP84().containsKey(xpubList.get(i))) {
                accounts[i] = SamouraiSentinel.getInstance(BalanceActivity.this).getBIP84().get(xpubList.get(i));
            } else {
                accounts[i] = SamouraiSentinel.getInstance(BalanceActivity.this).getLegacy().get(xpubList.get(i));
            }
        }

        int sel = SamouraiSentinel.getInstance(BalanceActivity.this).getCurrentSelectedAccount() == 0 ? 0 : SamouraiSentinel.getInstance(BalanceActivity.this).getCurrentSelectedAccount() - 1;

        new AlertDialog.Builder(BalanceActivity.this)
                .setTitle(R.string.deposit_into)
                .setSingleChoiceItems(accounts, sel, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {

                                dialog.dismiss();

                                SamouraiSentinel.getInstance(BalanceActivity.this).setCurrentSelectedAccount(which + 1);
                                accountSpinner.setSelection(which + 1);

                                if (isSweep) {
                                    doSweep();
                                } else {
                                    Intent intent = new Intent(BalanceActivity.this, ReceiveActivity.class);
                                    startActivity(intent);
                                }

                            }
                        }
                ).show();

    }

    private void restoreWatchOnly() {

        final List<String> xpubList = SamouraiSentinel.getInstance(BalanceActivity.this).getAllAddrsSorted();

        final Handler handler = new Handler();

        if (progress != null && progress.isShowing()) {
            progress.dismiss();
            progress = null;
        }

        progress = new ProgressDialog(BalanceActivity.this);
        progress.setCancelable(false);
        progress.setTitle(R.string.app_name);
        progress.setMessage(getString(R.string.please_wait));
        progress.show();

        new Thread(new Runnable() {
            @Override
            public void run() {

                Looper.prepare();

                List<String> _xpubs = new ArrayList<String>();
                for (String xpub : xpubList) {
                    if (xpub.startsWith("xpub") || xpub.startsWith("ypub") || xpub.startsWith("zpub") || xpub.startsWith("tpub") || xpub.startsWith("upub") || xpub.startsWith("vpub")) {
                        _xpubs.add(xpub);
                    }
                }

                if (_xpubs.size() > 0) {
                    try {
                        String xpubs = StringUtils.join(_xpubs.toArray(new String[_xpubs.size()]), ":");
//                        Log.i("BalanceActivity", xpubs);
                        if (_xpubs.size() > 0) {
                            HD_Wallet hdw = HD_WalletFactory.getInstance(BalanceActivity.this).restoreWallet(xpubs, null, 1);
                            if (hdw != null) {
                                List<HD_Account> accounts = hdw.getAccounts();
                                for (int i = 0; i < accounts.size(); i++) {
                                    AddressFactory.getInstance().account2xpub().put(i, _xpubs.get(i));
                                    AddressFactory.getInstance().xpub2account().put(_xpubs.get(i), i);
                                }
                            }
                        }

                    } catch (DecoderException de) {
                        PrefsUtil.getInstance(BalanceActivity.this).clear();
                        Toast.makeText(BalanceActivity.this, R.string.xpub_error, Toast.LENGTH_SHORT).show();
                        de.printStackTrace();
                    } catch (AddressFormatException afe) {
                        PrefsUtil.getInstance(BalanceActivity.this).clear();
                        Toast.makeText(BalanceActivity.this, R.string.xpub_error, Toast.LENGTH_SHORT).show();
                        afe.printStackTrace();
                    } finally {
                        ;
                    }
                }

                if (progress != null && progress.isShowing()) {
                    progress.dismiss();
                    progress = null;
                }

                handler.post(new Runnable() {
                    @Override
                    public void run() {

                        if (xpubList.size() == 1) {
                            account_selections = new String[1];
                            if ((xpubList.get(0).startsWith("xpub") || xpubList.get(0).startsWith("tpub")) && SamouraiSentinel.getInstance(BalanceActivity.this).getXPUBs().containsKey(xpubList.get(0))) {
                                account_selections[0] = SamouraiSentinel.getInstance(BalanceActivity.this).getXPUBs().get(xpubList.get(0));
                            } else if ((xpubList.get(0).startsWith("xpub") || xpubList.get(0).startsWith("ypub") || xpubList.get(0).startsWith("tpub") || xpubList.get(0).startsWith("upub")) && SamouraiSentinel.getInstance(BalanceActivity.this).getBIP49().containsKey(xpubList.get(0))) {
                                account_selections[0] = SamouraiSentinel.getInstance(BalanceActivity.this).getBIP49().get(xpubList.get(0));
                            } else if ((xpubList.get(0).startsWith("zpub") || xpubList.get(0).startsWith("vpub")) && SamouraiSentinel.getInstance(BalanceActivity.this).getBIP84().containsKey(xpubList.get(0))) {
                                account_selections[0] = SamouraiSentinel.getInstance(BalanceActivity.this).getBIP84().get(xpubList.get(0));
                            } else {
                                account_selections[0] = SamouraiSentinel.getInstance(BalanceActivity.this).getLegacy().get(xpubList.get(0));
                            }
                        } else {
                            account_selections = new String[xpubList.size() + 1];
                            account_selections[0] = BalanceActivity.this.getString(R.string.total_title);
                            for (int i = 0; i < xpubList.size(); i++) {
                                if ((xpubList.get(i).startsWith("xpub") || xpubList.get(i).startsWith("tpub")) && SamouraiSentinel.getInstance(BalanceActivity.this).getXPUBs().containsKey(xpubList.get(i))) {
                                    account_selections[i + 1] = SamouraiSentinel.getInstance(BalanceActivity.this).getXPUBs().get(xpubList.get(i));
                                } else if ((xpubList.get(i).startsWith("xpub") || xpubList.get(i).startsWith("ypub") || xpubList.get(i).startsWith("tpub") || xpubList.get(i).startsWith("upub")) && SamouraiSentinel.getInstance(BalanceActivity.this).getBIP49().containsKey(xpubList.get(i))) {
                                    account_selections[i + 1] = SamouraiSentinel.getInstance(BalanceActivity.this).getBIP49().get(xpubList.get(i));
                                } else if ((xpubList.get(i).startsWith("zpub") || xpubList.get(i).startsWith("vpub")) && SamouraiSentinel.getInstance(BalanceActivity.this).getBIP84().containsKey(xpubList.get(i))) {
                                    account_selections[i + 1] = SamouraiSentinel.getInstance(BalanceActivity.this).getBIP84().get(xpubList.get(i));
                                } else {
                                    account_selections[i + 1] = SamouraiSentinel.getInstance(BalanceActivity.this).getLegacy().get(xpubList.get(i));
                                }
                            }
                        }

                        adapter = new ArrayAdapter<String>(getBaseContext(), android.R.layout.simple_spinner_dropdown_item, account_selections);
                        adapter.notifyDataSetChanged();
                        accountSpinner.setAdapter(adapter);
                        if (account_selections.length == 1) {
                            SamouraiSentinel.getInstance(BalanceActivity.this).setCurrentSelectedAccount(0);
                        }

                        refreshTx(false);

                        try {
                            SamouraiSentinel.getInstance(BalanceActivity.this).serialize(SamouraiSentinel.getInstance(BalanceActivity.this).toJSON(), null);
                        } catch (IOException ioe) {
                            ;
                        } catch (JSONException je) {
                            ;
                        }

                    }

                });

                Looper.loop();

            }
        }).start();

    }

    private String getBTCDisplayAmount(long value) {

        String strAmount = Coin.valueOf(value).toPlainString();

        return strAmount;
    }

    private String getBTCDisplayUnits() {

        return MonetaryUtil.getInstance().getBTCUnits();

    }

    private String getFiatDisplayAmount(long value) {

        String strFiat = PrefsUtil.getInstance(BalanceActivity.this).getValue(PrefsUtil.CURRENT_FIAT, "USD");
        double btc_fx = ExchangeRateFactory.getInstance(BalanceActivity.this).getAvgPrice(strFiat);
        String strAmount = MonetaryUtil.getInstance().getFiatFormat(strFiat).format(btc_fx * (((double) value) / 1e8));

        return strAmount;
    }

    private String getFiatDisplayUnits() {

        return PrefsUtil.getInstance(BalanceActivity.this).getValue(PrefsUtil.CURRENT_FIAT, "USD");

    }

}
