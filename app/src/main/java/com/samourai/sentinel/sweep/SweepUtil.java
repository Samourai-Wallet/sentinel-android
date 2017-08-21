package com.samourai.sentinel.sweep;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import org.bitcoinj.core.Coin;
import org.bitcoinj.core.Transaction;
import org.json.JSONException;
import org.json.JSONObject;
import org.spongycastle.util.encoders.Hex;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;

import org.bitcoinj.params.MainNetParams;

import com.samourai.sentinel.R;
import com.samourai.sentinel.api.APIFactory;
import com.samourai.sentinel.util.CharSequenceX;

public class SweepUtil  {

    private static Context context = null;
    private static SweepUtil instance = null;

    private SweepUtil() { ; }

    public static SweepUtil getInstance(Context ctx) {

        context = ctx;

        if(instance == null)    {
            instance = new SweepUtil();
        }

        return instance;
    }

    public void sweep(final PrivKeyReader privKeyReader, final String strReceiveAddress)  {

        new Thread(new Runnable() {
            @Override
            public void run() {

                Looper.prepare();

                try {

                    if(privKeyReader == null || privKeyReader.getKey() == null || !privKeyReader.getKey().hasPrivKey())    {
                        Toast.makeText(context, R.string.cannot_recognize_privkey, Toast.LENGTH_SHORT).show();
                        Log.d("SweepUtil", "returning");
                        return;
                    }

                    String address = privKeyReader.getKey().toAddress(MainNetParams.get()).toString();
                    UTXO utxo = APIFactory.getInstance(context).getUnspentOutputsForSweep(address);
                    Log.d("SweepUtil", "UTXO size:" + utxo.getOutpoints().size());
                    if(utxo != null)    {

                        long total_value = 0L;
                        final List<MyTransactionOutPoint> outpoints = utxo.getOutpoints();
                        for(MyTransactionOutPoint outpoint : outpoints)   {
                            total_value += outpoint.getValue().longValue();
                        }

                        final BigInteger fee = FeeUtil.getInstance().estimatedFee(outpoints.size(), 1);

                        final long amount = total_value - fee.longValue();
                        Log.d("SweepUtil", "Total value:" + total_value);
                        Log.d("SweepUtil", "Amount:" + amount);
                        Log.d("SweepUtil", "Fee:" + fee.toString());
                        Log.d("SweepUtil", "Receive address:" + strReceiveAddress);

                        String message = "Sweep " + Coin.valueOf(amount).toPlainString() + " from " + address + " (fee:" + Coin.valueOf(fee.longValue()).toPlainString() + ")?";

                        AlertDialog.Builder builder = new AlertDialog.Builder(context);
                        builder.setTitle(R.string.app_name);
                        builder.setMessage(message);
                        builder.setCancelable(false);
                        builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                            public void onClick(final DialogInterface dialog, int whichButton) {

                                Log.d("SweepUtil", "start sweep");

                                final ProgressDialog progress = new ProgressDialog(context);
                                progress.setCancelable(false);
                                progress.setTitle(R.string.app_name);
                                progress.setMessage(context.getString(R.string.please_wait_sending));
                                progress.show();

                                final HashMap<String, BigInteger> receivers = new HashMap<String, BigInteger>();
                                receivers.put(strReceiveAddress, BigInteger.valueOf(amount));
                                Transaction tx = SendFactory.getInstance(context).makeTransaction(0, outpoints, receivers);

                                Log.d("SweepUtil", "tx is " + ((tx == null) ? "null" : "not null"));

                                tx = SendFactory.getInstance(context).signTransactionForSweep(tx, privKeyReader);
                                final String hexTx = new String(Hex.encode(tx.bitcoinSerialize()));
                                Log.d("SweepUtil", hexTx);
/*
                                String response = null;
                                try {
                                    response = PushTx.getInstance(context).samourai(hexTx);

                                    if(response != null)    {
                                        JSONObject jsonObject = new org.json.JSONObject(response);
                                        if(jsonObject.has("status"))    {
                                            if(jsonObject.getString("status").equals("ok"))    {
                                                Toast.makeText(context, R.string.tx_sent, Toast.LENGTH_SHORT).show();
                                            }
                                        }
                                    }
                                    else    {
                                        Toast.makeText(context, R.string.pushtx_returns_null, Toast.LENGTH_SHORT).show();
                                    }
                                }
                                catch(JSONException je) {
                                    Toast.makeText(context, "pushTx:" + je.getMessage(), Toast.LENGTH_SHORT).show();
                                }
*/
                                if(progress != null && progress.isShowing())    {
                                    progress.dismiss();
                                }

                            }
                        });
                        builder.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                            public void onClick(final DialogInterface dialog, int whichButton) {
                                ;
                            }
                        });

                        AlertDialog alert = builder.create();
                        alert.show();

                    }
                    else    {
                        Toast.makeText(context, R.string.cannot_find_unspents, Toast.LENGTH_SHORT).show();
                    }

                }
                catch(Exception e) {
                    Toast.makeText(context, R.string.cannot_sweep_privkey, Toast.LENGTH_SHORT).show();
                }

                Looper.loop();

            }
        }).start();

    }
/*
    public void sweep(final PrivKeyReader privKeyReader, final String strReceiveAddress)  {

        try {
            if(privKeyReader == null || privKeyReader.getKey() == null || !privKeyReader.getKey().hasPrivKey())    {
                Toast.makeText(context, R.string.cannot_recognize_privkey, Toast.LENGTH_SHORT).show();
                Log.d("SweepUtil", "returning");
                return;
            }
            else    {
                new SweepTask().execute(new String[] { privKeyReader.getPrivKey().toString(), strReceiveAddress });
            }
        }
        catch(Exception e) {
            ;
        }

    }
*/
    private class SweepTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... params) {

            try {

                final PrivKeyReader privKeyReader = new PrivKeyReader(new CharSequenceX(params[0]));
                final String strReceiveAddress = params[1];

                String address = privKeyReader.getKey().toAddress(MainNetParams.get()).toString();
                UTXO utxo = APIFactory.getInstance(context).getUnspentOutputsForSweep(address);
                Log.d("SweepUtil", "UTXO size:" + utxo.getOutpoints().size());
                if(utxo != null)    {

                    long total_value = 0L;
                    final List<MyTransactionOutPoint> outpoints = utxo.getOutpoints();
                    for(MyTransactionOutPoint outpoint : outpoints)   {
                        total_value += outpoint.getValue().longValue();
                    }

                    final BigInteger fee = FeeUtil.getInstance().estimatedFee(outpoints.size(), 1);

                    final long amount = total_value - fee.longValue();
                    Log.d("SweepUtil", "Total value:" + total_value);
                    Log.d("SweepUtil", "Amount:" + amount);
                    Log.d("SweepUtil", "Fee:" + fee.toString());
                    Log.d("SweepUtil", "Receive address:" + strReceiveAddress);

                    String message = "Sweep " + Coin.valueOf(amount).toPlainString() + " from " + address + " (fee:" + Coin.valueOf(fee.longValue()).toPlainString() + ")?";

                    AlertDialog.Builder builder = new AlertDialog.Builder(context);
                    builder.setTitle(R.string.app_name);
                    builder.setMessage(message);
                    builder.setCancelable(false);
                    builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                        public void onClick(final DialogInterface dialog, int whichButton) {

                            Log.d("SweepUtil", "start sweep");

                            final ProgressDialog progress = new ProgressDialog(context);
                            progress.setCancelable(false);
                            progress.setTitle(R.string.app_name);
                            progress.setMessage(context.getString(R.string.please_wait_sending));
                            progress.show();

                            final HashMap<String, BigInteger> receivers = new HashMap<String, BigInteger>();
                            receivers.put(strReceiveAddress, BigInteger.valueOf(amount));
                            Transaction tx = SendFactory.getInstance(context).makeTransaction(0, outpoints, receivers);

                            Log.d("SweepUtil", "tx is " + ((tx == null) ? "null" : "not null"));

                            tx = SendFactory.getInstance(context).signTransactionForSweep(tx, privKeyReader);
                            final String hexTx = new String(Hex.encode(tx.bitcoinSerialize()));
                            Log.d("SweepUtil", hexTx);
/*
                                String response = null;
                                try {
                                    response = PushTx.getInstance(context).samourai(hexTx);

                                    if(response != null)    {
                                        JSONObject jsonObject = new org.json.JSONObject(response);
                                        if(jsonObject.has("status"))    {
                                            if(jsonObject.getString("status").equals("ok"))    {
                                                Toast.makeText(context, R.string.tx_sent, Toast.LENGTH_SHORT).show();
                                            }
                                        }
                                    }
                                    else    {
                                        Toast.makeText(context, R.string.pushtx_returns_null, Toast.LENGTH_SHORT).show();
                                    }
                                }
                                catch(JSONException je) {
                                    Toast.makeText(context, "pushTx:" + je.getMessage(), Toast.LENGTH_SHORT).show();
                                }
*/
                            if(progress != null && progress.isShowing())    {
                                progress.dismiss();
                            }

                        }
                    });
                    builder.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                        public void onClick(final DialogInterface dialog, int whichButton) {
                            ;
                        }
                    });

                    AlertDialog alert = builder.create();
                    alert.show();

                }
                else    {
                    Toast.makeText(context, R.string.cannot_find_unspents, Toast.LENGTH_SHORT).show();
                }

            }
            catch(Exception e) {
                Toast.makeText(context, R.string.cannot_sweep_privkey, Toast.LENGTH_SHORT).show();
            }

            return "OK";
        }

        @Override
        protected void onPostExecute(String result) { ; }

        @Override
        protected void onPreExecute() {
            ;
        }

    }

}
