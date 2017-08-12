package com.samourai.sentinel.api;

import android.content.Context;
//import android.util.Log;

import com.samourai.sentinel.SamouraiSentinel;
import com.samourai.sentinel.util.AddressFactory;
import com.samourai.sentinel.util.Web;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

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
                StringBuilder url = new StringBuilder(Web.SAMOURAI_API2);
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
                                if(prevOutObj.has("xpub"))  {
                                    JSONObject xpubObj = (JSONObject)prevOutObj.get("xpub");
                                    addr = (String)xpubObj.get("m");
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
                            if(outObj.has("xpub"))  {
                                JSONObject xpubObj = (JSONObject)outObj.get("xpub");
                                addr = (String)xpubObj.get("m");
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
/*
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
*/
    public synchronized JSONObject getAddressInfo(String addr) {

        return getXPUB(new String[] { addr });

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
