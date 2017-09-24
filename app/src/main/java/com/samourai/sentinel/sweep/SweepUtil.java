package com.samourai.sentinel.sweep;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Looper;
import android.widget.Toast;
import android.util.Log;

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
import com.samourai.sentinel.segwit.SegwitAddress;

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

    public void sweep(final PrivKeyReader privKeyReader, final String strReceiveAddress, final boolean isBIP49)  {

        new Thread(new Runnable() {
            @Override
            public void run() {

                Looper.prepare();

                try {

                    if(privKeyReader == null || privKeyReader.getKey() == null || !privKeyReader.getKey().hasPrivKey())    {
                        Toast.makeText(context, R.string.cannot_recognize_privkey, Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String address = null;
                    if(isBIP49)    {
                        address = new SegwitAddress(privKeyReader.getKey(), MainNetParams.get()).getAddressAsString();
                    }
                    else    {
                        address = privKeyReader.getKey().toAddress(MainNetParams.get()).toString();
                    }

                    UTXO utxo = APIFactory.getInstance(context).getUnspentOutputsForSweep(address);
                    if(utxo != null && utxo.getOutpoints().size() > 0)    {

                        long total_value = 0L;
                        final List<MyTransactionOutPoint> outpoints = utxo.getOutpoints();
                        for(MyTransactionOutPoint outpoint : outpoints)   {
                            total_value += outpoint.getValue().longValue();
                        }

                        FeeUtil.getInstance().setSuggestedFee(FeeUtil.getInstance().getNormalFee());
                        BigInteger fee = null;
                        if(isBIP49)    {
                            fee = FeeUtil.getInstance().estimatedFeeSegwit(0, outpoints.size(), 1);
                        }
                        else    {
                            fee = FeeUtil.getInstance().estimatedFee(outpoints.size(), 1);
                        }

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
//
                                String response = null;
                                try {
                                    response = PushTx.getInstance(context).samourai(hexTx);

                                    if(response != null)    {
                                        JSONObject jsonObject = new org.json.JSONObject(response);
                                        if(jsonObject.has("status"))    {
                                            if(jsonObject.getString("status").equals("ok"))    {
                                                Toast.makeText(context, R.string.tx_ok, Toast.LENGTH_SHORT).show();
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
//
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
                        sweep(privKeyReader, strReceiveAddress, true);
                    }

                }
                catch(Exception e) {
                    Log.d("SweepUtil", e.getMessage());
                    Toast.makeText(context, context.getText(R.string.cannot_sweep_privkey) + ", " + e.getMessage() , Toast.LENGTH_SHORT).show();
                }

                Looper.loop();

            }
        }).start();

    }

}
