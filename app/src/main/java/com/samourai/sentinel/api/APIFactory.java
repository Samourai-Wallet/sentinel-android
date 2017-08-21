package com.samourai.sentinel.api;

import android.content.Context;
//import android.util.Log;

import com.samourai.sentinel.SamouraiSentinel;
import com.samourai.sentinel.sweep.MyTransactionOutPoint;
import com.samourai.sentinel.sweep.UTXO;
import com.samourai.sentinel.util.AddressFactory;
import com.samourai.sentinel.util.Web;

import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.params.MainNetParams;
import org.bitcoinj.script.Script;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.spongycastle.util.encoders.Hex;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

public class APIFactory	{

    private static long xpub_balance = 0L;
    private static HashMap<String, Long> xpub_amounts = null;
    private static HashMap<String,List<Tx>> xpub_txs = null;

    private static boolean hasShuffled = false;

    private static APIFactory instance = null;
    private static Context context = null;

    private APIFactory()	{ ; }

    public static APIFactory getInstance(Context ctx) {

        context = ctx;

        if(instance == null) {
            xpub_amounts = new HashMap<String, Long>();
            xpub_txs = new HashMap<String,List<Tx>>();
            xpub_balance = 0L;
            hasShuffled = false;
            instance = new APIFactory();
        }

        return instance;
    }

    public void wipe() {
        instance = null;
    }

    public JSONObject getXPUB(String[] xpubs) {

        JSONObject jsonObject  = null;
        xpub_amounts.clear();

        for(int i = 0; i < xpubs.length; i++)   {
            try {
                StringBuilder url = new StringBuilder(Web.BLOCKCHAIN_DOMAIN);
                url.append("multiaddr?active=");
                url.append(xpubs[i]);
//            Log.i("APIFactory", "XPUB:" + url.toString());
                String response = Web.getURL(url.toString());
//            Log.i("APIFactory", "XPUB response:" + response);
                try {
                    jsonObject = new JSONObject(response);
                    xpub_txs.put(xpubs[i], new ArrayList<Tx>());
                    parseXPUB(jsonObject, true);
                }
                catch(JSONException je) {
                    je.printStackTrace();
                    jsonObject = null;
                }
            }
            catch(Exception e) {
                jsonObject = null;
                e.printStackTrace();
            }
        }

        long total_amount = 0L;
        for(String addr : xpub_amounts.keySet())   {
            total_amount += xpub_amounts.get(addr);
        }
        xpub_balance = total_amount;

        return jsonObject;
    }

    public void parseXPUB(JSONObject jsonObject, boolean complete) throws JSONException  {

        if(jsonObject != null)  {

            long latest_block = 0L;

            if(jsonObject.has("info"))  {
                JSONObject infoObj = (JSONObject)jsonObject.get("info");
                if(infoObj.has("latest_block"))  {
                    JSONObject blockObj = (JSONObject)infoObj.get("latest_block");
                    if(blockObj.has("height"))  {
                        latest_block = blockObj.getLong("height");
//                        Log.i("APIFactory", "latest_block " + latest_block);
                    }
                }
            }

            if(jsonObject.has("addresses"))  {

                JSONArray addressesArray = (JSONArray)jsonObject.get("addresses");
                JSONObject addrObj = null;
                for(int i = 0; i < addressesArray.length(); i++)  {
                    addrObj = (JSONObject)addressesArray.get(i);
                    if(i == 1 && addrObj.has("n_tx") && addrObj.getInt("n_tx") > 0)  {
                        hasShuffled = true;
                    }
                    if(addrObj.has("final_balance") && addrObj.has("address"))  {
                        xpub_amounts.put((String)addrObj.get("address"), addrObj.getLong("final_balance"));
                        if(((String)addrObj.get("address")).startsWith("xpub"))    {
                            AddressFactory.getInstance().setHighestTxReceiveIdx(AddressFactory.getInstance().xpub2account().get((String) addrObj.get("address")), addrObj.getInt("account_index"));
                            AddressFactory.getInstance().setHighestTxChangeIdx(AddressFactory.getInstance().xpub2account().get((String) addrObj.get("address")), addrObj.getInt("change_index"));
                        }
                    }
                }

                if(!complete)    {
                    return;
                }
            }

            if(jsonObject.has("txs"))  {

                JSONArray txArray = (JSONArray)jsonObject.get("txs");
                JSONObject txObj = null;
                for(int i = 0; i < txArray.length(); i++)  {

                    txObj = (JSONObject)txArray.get(i);
                    long height = 0L;
                    long amount = 0L;
                    long ts = 0L;
                    String hash = null;
                    String addr = null;
                    String _addr = null;
                    String input_xpub = null;
                    String output_xpub = null;
                    long move_amount = 0L;
                    long input_amount = 0L;
                    long output_amount = 0L;

                    if(txObj.has("block_height"))  {
                        height = txObj.getLong("block_height");
//                        Log.i("APIFactory", "height " + height);
                    }
                    else  {
                        height = -1L;  // 0 confirmations
                    }
                    if(txObj.has("hash"))  {
                        hash = (String)txObj.get("hash");
                    }
                    if(txObj.has("result"))  {
                        amount = txObj.getLong("result");
                    }
                    if(txObj.has("time"))  {
                        ts = txObj.getLong("time");
                    }

                    if(txObj.has("inputs"))  {
                        JSONArray inputArray = (JSONArray)txObj.get("inputs");
                        JSONObject inputObj = null;
                        for(int j = 0; j < inputArray.length(); j++)  {
                            inputObj = (JSONObject)inputArray.get(j);
                            if(inputObj.has("prev_out"))  {
                                JSONObject prevOutObj = (JSONObject)inputObj.get("prev_out");
                                input_amount += prevOutObj.getLong("value");
                                if(prevOutObj.has("xpub"))  {
                                    JSONObject xpubObj = (JSONObject)prevOutObj.get("xpub");
                                    addr = (String)xpubObj.get("m");
                                    input_xpub = addr;
                                }
                                else  {
                                    if(SamouraiSentinel.getInstance(context).getLegacy().containsKey((String)prevOutObj.get("addr")))    {
//                                        Log.i("APIFactory:", "legacy tx " + (String)prevOutObj.get("addr"));
                                        addr = (String)prevOutObj.get("addr");
                                    }
                                    else    {
                                        _addr = (String)prevOutObj.get("addr");
                                    }
                                }
                            }
                        }
                    }

                    if(txObj.has("out"))  {
                        JSONArray outArray = (JSONArray)txObj.get("out");
                        JSONObject outObj = null;
                        for(int j = 0; j < outArray.length(); j++)  {
                            outObj = (JSONObject)outArray.get(j);
                            output_amount += outObj.getLong("value");
                            if(outObj.has("xpub"))  {
                                JSONObject xpubObj = (JSONObject)outObj.get("xpub");
                                addr = (String)xpubObj.get("m");
                                if(input_xpub != null && !input_xpub.equals(addr))    {
                                    output_xpub = addr;
                                    move_amount = outObj.getLong("value");
                                }
                            }
                            else  {
                                if(SamouraiSentinel.getInstance(context).getLegacy().containsKey((String)outObj.get("addr")))    {
//                                    Log.i("APIFactory:", "legacy tx " + (String)outObj.get("addr"));
                                    addr = (String)outObj.get("addr");
                                }
                                else    {
                                    _addr = (String)outObj.get("addr");
                                }
                            }
                        }
                    }

                    if(addr != null)  {

                        //
                        // test for MOVE from Shuffling -> Samourai account
                        //
                        if(input_xpub != null && output_xpub != null && !input_xpub.equals(output_xpub))    {

                            Tx tx = new Tx(hash, output_xpub, (move_amount + Math.abs(input_amount - output_amount)) * -1.0, ts, (latest_block > 0L && height > 0L) ? (latest_block - height) + 1 : 0);
                            if(!xpub_txs.containsKey(input_xpub))  {
                                xpub_txs.put(input_xpub, new ArrayList<Tx>());
                            }
                            xpub_txs.get(input_xpub).add(tx);

                            Tx _tx = new Tx(hash, input_xpub, move_amount, ts, (latest_block > 0L && height > 0L) ? (latest_block - height) + 1 : 0);
                            if(!xpub_txs.containsKey(output_xpub))  {
                                xpub_txs.put(output_xpub, new ArrayList<Tx>());
                            }
                            xpub_txs.get(output_xpub).add(_tx);

                        }
                        else    {

                            Tx tx = new Tx(hash, _addr, amount, ts, (latest_block > 0L && height > 0L) ? (latest_block - height) + 1 : 0);

                            if(!xpub_txs.containsKey(addr))  {
                                xpub_txs.put(addr, new ArrayList<Tx>());
                            }
                            xpub_txs.get(addr).add(tx);
                        }

                    }
                }

            }

        }

    }

    public JSONObject getAddressInfo(String addr) {

        JSONObject jsonObject  = null;

        try {
            StringBuilder url = new StringBuilder(Web.BLOCKCHAIN_DOMAIN);
            url.append("address/");
            url.append(addr);
            url.append("?format=json");

            String response = Web.getURL(url.toString());
            jsonObject = new JSONObject(response);
        }
        catch(Exception e) {
            jsonObject = null;
            e.printStackTrace();
        }

        return jsonObject;
    }

    public long getXpubBalance()  {
        return xpub_balance;
    }

    public void setXpubBalance(long value)  {
        xpub_balance = value;
    }

    public HashMap<String,Long> getXpubAmounts()  {
        return xpub_amounts;
    }

    public HashMap<String,List<Tx>> getXpubTxs()  {
        return xpub_txs;
    }

    public List<Tx> getAllXpubTxs()  {

        List<Tx> ret = new ArrayList<Tx>();
        for(String key : xpub_txs.keySet())  {
            List<Tx> txs = xpub_txs.get(key);
            ret.addAll(txs);
        }

        Collections.sort(ret, new TxMostRecentDateComparator());

        return ret;
    }

    public synchronized UTXO getUnspentOutputsForSweep(String address) {

        try {

            String response = null;

            StringBuilder args = new StringBuilder();
            args.append("active=");
            args.append(address);
            response = Web.postURL(Web.SAMOURAI_API2 + "unspent?", args.toString());

            return parseUnspentOutputsForSweep(response);

        }
        catch(Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    private synchronized UTXO parseUnspentOutputsForSweep(String unspents)   {

        UTXO utxo = null;

        if(unspents != null)    {

            try {
                JSONObject jsonObj = new JSONObject(unspents);

                if(jsonObj == null || !jsonObj.has("unspent_outputs"))    {
                    return null;
                }
                JSONArray utxoArray = jsonObj.getJSONArray("unspent_outputs");
                if(utxoArray == null || utxoArray.length() == 0) {
                    return null;
                }

//            Log.d("APIFactory", "unspents found:" + outputsRoot.size());

                for (int i = 0; i < utxoArray.length(); i++) {

                    JSONObject outDict = utxoArray.getJSONObject(i);

                    byte[] hashBytes = Hex.decode((String)outDict.get("tx_hash"));
                    Sha256Hash txHash = Sha256Hash.wrap(hashBytes);
                    int txOutputN = ((Number)outDict.get("tx_output_n")).intValue();
                    BigInteger value = BigInteger.valueOf(((Number)outDict.get("value")).longValue());
                    String script = (String)outDict.get("script");
                    byte[] scriptBytes = Hex.decode(script);
                    int confirmations = ((Number)outDict.get("confirmations")).intValue();

                    try {
                        String address = new Script(scriptBytes).getToAddress(MainNetParams.get()).toString();

                        // Construct the output
                        MyTransactionOutPoint outPoint = new MyTransactionOutPoint(txHash, txOutputN, value, scriptBytes, address);
                        outPoint.setConfirmations(confirmations);
                        if(utxo == null)    {
                            utxo = new UTXO();
                        }
                        utxo.getOutpoints().add(outPoint);

                    }
                    catch(Exception e) {
                        ;
                    }

                }

            }
            catch(JSONException je) {
                ;
            }

        }

        return utxo;

    }

    private class TxMostRecentDateComparator implements Comparator<Tx> {

        public int compare(Tx t1, Tx t2) {

            final int BEFORE = -1;
            final int EQUAL = 0;
            final int AFTER = 1;

            int ret = 0;

            if(t1.getTS() > t2.getTS()) {
                ret = BEFORE;
            }
            else if(t1.getTS() < t2.getTS()) {
                ret = AFTER;
            }
            else    {
                ret = EQUAL;
            }

            return ret;
        }

    }

}
