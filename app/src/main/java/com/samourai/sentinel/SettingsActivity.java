package com.samourai.sentinel;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.widget.EditText;

import com.dm.zbar.android.scanner.ZBarConstants;
import com.dm.zbar.android.scanner.ZBarScannerActivity;
import com.samourai.sentinel.sweep.PushTx;
import com.samourai.sentinel.util.AppUtil;
import com.samourai.sentinel.util.BlockExplorerUtil;
import com.samourai.sentinel.util.ExchangeRateFactory;
import com.samourai.sentinel.util.PrefsUtil;
import com.yanzhenjie.zbar.Symbol;

import org.bitcoinj.core.Transaction;
import org.bitcoinj.params.MainNetParams;
import org.bouncycastle.util.encoders.Hex;

public class SettingsActivity extends PreferenceActivity	{

    private static final int SCAN_HEX_TX = 2009;

    private ProgressDialog progress = null;

    /** Called when the activity is first created. */
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setTitle(R.string.app_name);
        addPreferencesFromResource(R.xml.settings);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        Preference aboutPref = (Preference) findPreference("about");
        aboutPref.setSummary("Sentinel," + " " + getResources().getString(R.string.version_name));
        aboutPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {

                new AlertDialog.Builder(SettingsActivity.this)
                        .setIcon(R.drawable.ic_launcher)
                        .setTitle(R.string.app_name)
                        .setMessage("Sentinel," + " " + getResources().getString(R.string.version_name))
                        .setCancelable(false)
                        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                ;
                            }
                        }).show();

                return true;
            }
        });

        Preference explorersPref = (Preference) findPreference("explorer");
        explorersPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                getBlockExplorer();
                return true;
            }
        });

        Preference fiatPref = (Preference) findPreference("fiat");
        fiatPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                getExchange();
                return true;
            }
        });

        final CheckBoxPreference cbPref1 = (CheckBoxPreference) findPreference("pin");
        final CheckBoxPreference cbPref2 = (CheckBoxPreference) findPreference("scramblePin");
        final CheckBoxPreference cbPref3 = (CheckBoxPreference) findPreference("haptic");
        if(!cbPref1.isChecked())    {
            cbPref2.setChecked(false);
            cbPref2.setEnabled(false);
            cbPref3.setChecked(false);
            cbPref3.setEnabled(false);
        }
        cbPref1.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            public boolean onPreferenceChange(Preference preference, Object newValue) {

                if(cbPref1.isChecked())	{
                    PrefsUtil.getInstance(SettingsActivity.this).setValue(PrefsUtil.PIN_HASH, "");
                    cbPref2.setChecked(false);
                    cbPref2.setEnabled(false);
                    cbPref3.setChecked(false);
                    cbPref3.setEnabled(false);
                    PrefsUtil.getInstance(SettingsActivity.this).setValue(PrefsUtil.SCRAMBLE_PIN, false);
                }
                else	{
                    cbPref2.setEnabled(true);
                    cbPref3.setEnabled(true);
                    Intent intent = new Intent(SettingsActivity.this, PinEntryActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                    intent.putExtra("create", true);
                    startActivity(intent);
                }

                return true;
            }
        });

        cbPref2.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            public boolean onPreferenceChange(Preference preference, Object newValue) {

                if(cbPref2.isChecked())	{
                    PrefsUtil.getInstance(SettingsActivity.this).setValue(PrefsUtil.SCRAMBLE_PIN, false);
                }
                else	{
                    PrefsUtil.getInstance(SettingsActivity.this).setValue(PrefsUtil.SCRAMBLE_PIN, true);
                }

                return true;
            }
        });

        cbPref3.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            public boolean onPreferenceChange(Preference preference, Object newValue) {

                if(cbPref3.isChecked())	{
                    PrefsUtil.getInstance(SettingsActivity.this).setValue(PrefsUtil.HAPTIC_PIN, false);
                }
                else	{
                    PrefsUtil.getInstance(SettingsActivity.this).setValue(PrefsUtil.HAPTIC_PIN, true);
                }

                return true;
            }
        });

        Preference exportPref = (Preference) findPreference("export");
        exportPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {

                AppUtil.getInstance(SettingsActivity.this).doBackup();

                return true;
            }
        });

        Preference restorePref = (Preference) findPreference("restore");
        restorePref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {

                AppUtil.getInstance(SettingsActivity.this).doRestore();

                return true;
            }
        });

        Preference broadcastHexPref = (Preference) findPreference("broadcastHex");
        broadcastHexPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                doBroadcastHex();
                return true;
            }
        });

    }

    @Override
    public void onResume() {
        super.onResume();

        AppUtil.getInstance(SettingsActivity.this).checkTimeOut();

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        if(resultCode == Activity.RESULT_OK && requestCode == SCAN_HEX_TX)	{

            if(data != null && data.getStringExtra(ZBarConstants.SCAN_RESULT) != null)	{

                final String strResult = data.getStringExtra(ZBarConstants.SCAN_RESULT);

                doBroadcastHex(strResult);

            }
        }
        else if(resultCode == Activity.RESULT_CANCELED && requestCode == SCAN_HEX_TX)	{
            ;
        }
        else {
            ;
        }

    }

    private void doScanHexTx()   {
        Intent intent = new Intent(SettingsActivity.this, ZBarScannerActivity.class);
        intent.putExtra(ZBarConstants.SCAN_MODES, new int[]{ Symbol.QRCODE } );
        startActivityForResult(intent, SCAN_HEX_TX);
    }

    private void getBlockExplorer()	{

        final CharSequence[] explorers = BlockExplorerUtil.getInstance().getBlockExplorers();
        final int sel = PrefsUtil.getInstance(SettingsActivity.this).getValue(PrefsUtil.BLOCK_EXPLORER, 0);
        final int _sel;
        if(sel >= explorers.length)    {
            _sel = 0;
        }
        else    {
            _sel = sel;
        }

        new AlertDialog.Builder(SettingsActivity.this)
                .setTitle(R.string.options_blockexplorer)
                .setSingleChoiceItems(explorers, _sel, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                PrefsUtil.getInstance(SettingsActivity.this).setValue(PrefsUtil.BLOCK_EXPLORER, which);
                                dialog.dismiss();
                            }
                        }
                ).show();

    }

    private void getExchange()	{

        final String[] exchanges = ExchangeRateFactory.getInstance(this).getExchangeLabels();
        final int sel = PrefsUtil.getInstance(SettingsActivity.this).getValue(PrefsUtil.CURRENT_EXCHANGE_SEL, 0);

        new AlertDialog.Builder(SettingsActivity.this)
                .setTitle(R.string.options_currency)
                .setSingleChoiceItems(exchanges, sel, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                PrefsUtil.getInstance(SettingsActivity.this).setValue(PrefsUtil.CURRENT_EXCHANGE, exchanges[which].substring(exchanges[which].length() - 3));
                                PrefsUtil.getInstance(SettingsActivity.this).setValue(PrefsUtil.CURRENT_EXCHANGE_SEL, which);
                                if(which == 2)    {
                                    PrefsUtil.getInstance(SettingsActivity.this).setValue(PrefsUtil.CURRENT_FIAT, "USD");
                                    PrefsUtil.getInstance(SettingsActivity.this).setValue(PrefsUtil.CURRENT_FIAT_SEL, 0);
                                    dialog.dismiss();
                                }
                                else    {
                                    dialog.dismiss();
                                    getFiat();
                                }

                            }
                        }
                ).show();

    }

    private void getFiat()	{

        final int fxSel = PrefsUtil.getInstance(SettingsActivity.this).getValue(PrefsUtil.CURRENT_EXCHANGE_SEL, 0);

        final String[] currencies;
        if(fxSel == 1)	{
            currencies = ExchangeRateFactory.getInstance(this).getCurrencyLabelsBTCe();
        }
        else	{
            currencies = ExchangeRateFactory.getInstance(this).getCurrencyLabels();
        }

        new AlertDialog.Builder(SettingsActivity.this)
                .setTitle(R.string.options_currency)
                .setSingleChoiceItems(currencies, 0, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {

                                String selectedCurrency = null;
                                if (currencies[which].substring(currencies[which].length() - 3).equals("RUR")) {
                                    selectedCurrency = "RUB";
                                }
                                else {
                                    selectedCurrency = currencies[which].substring(currencies[which].length() - 3);
                                }

                                PrefsUtil.getInstance(SettingsActivity.this).setValue(PrefsUtil.CURRENT_FIAT, selectedCurrency);
                                PrefsUtil.getInstance(SettingsActivity.this).setValue(PrefsUtil.CURRENT_FIAT_SEL, which);
                                dialog.dismiss();
                            }
                        }
                ).show();

    }

    private void doBroadcastHex()    {

        AlertDialog.Builder dlg = new AlertDialog.Builder(SettingsActivity.this)
                .setTitle(R.string.app_name)
                .setMessage(R.string.tx_hex)
                .setCancelable(true)
                .setPositiveButton(R.string.enter_tx_hex, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {

                        final EditText edHexTx = new EditText(SettingsActivity.this);
                        edHexTx.setSingleLine(false);
                        edHexTx.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
                        edHexTx.setLines(10);
                        edHexTx.setHint(R.string.tx_hex);
                        edHexTx.setGravity(Gravity.START);
                        TextWatcher textWatcher = new TextWatcher() {

                            public void afterTextChanged(Editable s) {
                                edHexTx.setSelection(0);
                            }
                            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                                ;
                            }
                            public void onTextChanged(CharSequence s, int start, int before, int count) {
                                ;
                            }
                        };
                        edHexTx.addTextChangedListener(textWatcher);

                        AlertDialog.Builder dlg = new AlertDialog.Builder(SettingsActivity.this)
                                .setTitle(R.string.app_name)
                                .setView(edHexTx)
                                .setMessage(R.string.enter_tx_hex)
                                .setCancelable(false)
                                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichButton) {

                                        final String strHexTx = edHexTx.getText().toString().trim();

                                        doBroadcastHex(strHexTx);

                                    }
                                }).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichButton) {
                                        ;
                                    }
                                });
                        if(!isFinishing())    {
                            dlg.show();
                        }

                    }

                }).setNegativeButton(R.string.scan, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {

                        doScanHexTx();

                    }
                });
        if(!isFinishing())    {
            dlg.show();
        }

    }

    private void doBroadcastHex(final String strHexTx)    {

        Transaction tx = new Transaction(MainNetParams.get(), Hex.decode(strHexTx));

        String msg = SettingsActivity
                .this.getString(R.string.broadcast) + ":" + tx.getHashAsString() + " ?";

        AlertDialog.Builder dlg = new AlertDialog.Builder(SettingsActivity.this)
                .setTitle(R.string.app_name)
                .setMessage(msg)
                .setCancelable(false)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {

                        if (progress != null && progress.isShowing()) {
                            progress.dismiss();
                            progress = null;
                        }

                        progress = new ProgressDialog(SettingsActivity.this);
                        progress.setCancelable(false);
                        progress.setTitle(R.string.app_name);
                        progress.setMessage(getString(R.string.please_wait));
                        progress.show();

                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                Looper.prepare();

                                PushTx.getInstance(SettingsActivity.this).samourai(strHexTx);

                                progress.dismiss();

                                Looper.loop();

                            }
                        }).start();

                    }
                }).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        ;
                    }
                });
        if(!isFinishing())    {
            dlg.show();
        }

    }

}
