package com.samourai.sentinel.api;

import android.content.Context;
import android.util.Log;

import com.auth0.android.jwt.JWT;
import com.samourai.sentinel.BuildConfig;
import com.samourai.sentinel.SamouraiSentinel;
import com.samourai.sentinel.network.dojo.DojoUtil;
import com.samourai.sentinel.segwit.bech32.Bech32Util;
import com.samourai.sentinel.sweep.FeeUtil;
import com.samourai.sentinel.sweep.MyTransactionOutPoint;
import com.samourai.sentinel.sweep.SuggestedFee;
import com.samourai.sentinel.sweep.UTXO;
import com.samourai.sentinel.tor.TorManager;
import com.samourai.sentinel.util.AddressFactory;
import com.samourai.sentinel.util.AppUtil;
import com.samourai.sentinel.util.WebUtil;

import org.apache.commons.lang3.StringUtils;
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.script.Script;
import org.bouncycastle.util.encoders.Base64;
import org.bouncycastle.util.encoders.Hex;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigInteger;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import okhttp3.FormBody;

import static com.samourai.sentinel.util.LogUtil.debug;
import static com.samourai.sentinel.util.LogUtil.info;

//import android.util.Log;

public class APIFactory {


    private static String APP_TOKEN = null;         // API app token
    private static String ACCESS_TOKEN = null;      // API access token
    private static long ACCESS_TOKEN_REFRESH = 300L;  // in


    private static long xpub_balance = 0L;
    private static HashMap<String, Long> xpub_amounts = null;
    private static HashMap<String,List<Tx>> xpub_txs = null;
    private static HashMap<String,Integer> unspentAccounts = null;
    private static HashMap<String,Integer> unspentBIP49 = null;
    private static HashMap<String,Integer> unspentBIP84 = null;
    private static HashMap<String,String> unspentPaths = null;
    private static HashMap<String,UTXO> utxos = null;

    private static APIFactory instance = null;
    private static Context context = null;

    private APIFactory() {
        ;
    }

    public static APIFactory getInstance(Context ctx) {

        context = ctx;

        if (instance == null) {
            xpub_amounts = new HashMap<String, Long>();
            xpub_txs = new HashMap<String, List<Tx>>();
            xpub_balance = 0L;
            unspentPaths = new HashMap<String, String>();
            unspentAccounts = new HashMap<String, Integer>();
            unspentBIP49 = new HashMap<String, Integer>();
            unspentBIP84 = new HashMap<String, Integer>();
            utxos = new HashMap<String, UTXO>();
            instance = new APIFactory();
        }

        return instance;
    }

    public void wipe() {
        instance = null;
    }

    public JSONObject getXPUB(String[] xpubs) {

        String _url = WebUtil.getAPIUrl(context);

        JSONObject jsonObject = null;
        xpub_amounts.clear();

        for (int i = 0; i < xpubs.length; i++) {
            try {
                StringBuilder url = new StringBuilder(_url);
                url.append("multiaddr?active=");
                url.append(xpubs[i]);
                url.append("&&at=".concat(getAccessToken()));
//                Log.i("APIFactory", "XPUB:" + url.toString());
                String response = WebUtil.getInstance(context).getURL(url.toString());
                Log.i("APIFactory", "XPUB response:" + response);
                try {
                    jsonObject = new JSONObject(response);
                    xpub_txs.put(xpubs[i], new ArrayList<Tx>());
                    parseXPUB(jsonObject, true);
                } catch (JSONException je) {
                    je.printStackTrace();
                    jsonObject = null;
                }
            } catch (Exception e) {
                jsonObject = null;
                e.printStackTrace();
            }
        }

        long total_amount = 0L;
        for (String addr : xpub_amounts.keySet()) {
            total_amount += xpub_amounts.get(addr);
        }
        xpub_balance = total_amount;

        return jsonObject;
    }

    public void parseXPUB(JSONObject jsonObject, boolean complete) throws JSONException  {

        if (jsonObject != null) {

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

            if (jsonObject.has("addresses")) {

                JSONArray addressesArray = (JSONArray) jsonObject.get("addresses");
                JSONObject addrObj = null;
                for(int i = 0; i < addressesArray.length(); i++)  {
                    addrObj = (JSONObject)addressesArray.get(i);
                    if(addrObj.has("final_balance") && addrObj.has("address"))  {
                        xpub_amounts.put((String)addrObj.get("address"), addrObj.getLong("final_balance"));
                        if(((String)addrObj.get("address")).startsWith("xpub") || ((String)addrObj.get("address")).startsWith("ypub") || ((String)addrObj.get("address")).startsWith("zpub") || ((String)addrObj.get("address")).startsWith("tpub") || ((String)addrObj.get("address")).startsWith("upub") || ((String)addrObj.get("address")).startsWith("vpub"))    {
                            if(AddressFactory.getInstance().xpub2account().containsKey((String) addrObj.get("address")))    {
                                if(addrObj.has("account_index"))    {
                                    AddressFactory.getInstance().setHighestTxReceiveIdx(AddressFactory.getInstance().xpub2account().get((String) addrObj.get("address")), addrObj.getInt("account_index"));
                                }
                                else    {
                                    AddressFactory.getInstance().setHighestTxReceiveIdx(AddressFactory.getInstance().xpub2account().get((String) addrObj.get("address")), 0);
                                }
                                if(addrObj.has("change_index"))    {
                                    AddressFactory.getInstance().setHighestTxChangeIdx(AddressFactory.getInstance().xpub2account().get((String) addrObj.get("address")), addrObj.getInt("change_index"));
                                }
                                else    {
                                    AddressFactory.getInstance().setHighestTxReceiveIdx(AddressFactory.getInstance().xpub2account().get((String) addrObj.get("address")), 0);
                                }
                            }
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

                    if (txObj.has("block_height")) {
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
                                        addr = (String) prevOutObj.get("addr");
                                    } else {
                                        _addr = (String) prevOutObj.get("addr");
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

                    if (addr != null) {

                        Tx tx = new Tx(hash, _addr, amount, ts, (latest_block > 0L && height > 0L) ? (latest_block - height) + 1 : 0);

                        if (!xpub_txs.containsKey(addr)) {
                            xpub_txs.put(addr, new ArrayList<Tx>());
                        }
                        xpub_txs.get(addr).add(tx);

                    }
                }

            }

        }

    }

    public synchronized JSONObject getAddressInfo(String addr) {

        return getXPUB(new String[] { addr });

    }

    public long getXpubBalance()  {
        return xpub_balance;
    }

    public void setXpubBalance(long value)  {
        xpub_balance = value;
    }

    public HashMap<String, Long> getXpubAmounts() {
        return xpub_amounts;
    }

    public HashMap<String, List<Tx>> getXpubTxs() {
        return xpub_txs;
    }

    public List<Tx> getAllXpubTxs() {

        List<Tx> ret = new ArrayList<Tx>();
        for (String key : xpub_txs.keySet()) {
            List<Tx> txs = xpub_txs.get(key);
            ret.addAll(txs);
        }

        Collections.sort(ret, new TxMostRecentDateComparator());

        return ret;
    }

    public synchronized JSONObject getUnspentOutputs(String[] xpubs) {

        String _url = WebUtil.getAPIUrl(context);

        JSONObject jsonObject = null;

        try {

            String response = null;


            FormBody body = new FormBody.Builder()
                    .add("active", StringUtils.join(xpubs, URLEncoder.encode("|", "UTF-8")))
                    .build();


            response = WebUtil.getInstance(context).postURL(_url + "unspent?", body);
            Log.d("APIFactory", "UTXO:" + response);

            parseUnspentOutputs(response);

        } catch (Exception e) {
            jsonObject = null;
            e.printStackTrace();
        }

        return jsonObject;
    }

    private synchronized boolean parseUnspentOutputs(String unspents) {

        if (unspents != null) {

            try {
                JSONObject jsonObj = new JSONObject(unspents);

                if (jsonObj == null || !jsonObj.has("unspent_outputs")) {
                    return false;
                }
                JSONArray utxoArray = jsonObj.getJSONArray("unspent_outputs");
                if (utxoArray == null || utxoArray.length() == 0) {
                    return false;
                }

                for (int i = 0; i < utxoArray.length(); i++) {

                    JSONObject outDict = utxoArray.getJSONObject(i);

                    byte[] hashBytes = Hex.decode((String) outDict.get("tx_hash"));
                    Sha256Hash txHash = Sha256Hash.wrap(hashBytes);
                    int txOutputN = ((Number) outDict.get("tx_output_n")).intValue();
                    BigInteger value = BigInteger.valueOf(((Number) outDict.get("value")).longValue());
                    String script = (String) outDict.get("script");
                    byte[] scriptBytes = Hex.decode(script);
                    int confirmations = ((Number) outDict.get("confirmations")).intValue();

                    try {
                        String address = null;
                        if (Bech32Util.getInstance().isBech32Script(script)) {
                            address = Bech32Util.getInstance().getAddressFromScript(script);
                        } else {
                            address = new Script(scriptBytes).getToAddress(SamouraiSentinel.getInstance().getCurrentNetworkParams()).toString();
                        }

                        if(outDict.has("xpub"))    {
                            JSONObject xpubObj = (JSONObject)outDict.get("xpub");
                            String path = (String)xpubObj.get("path");
                            String m = (String)xpubObj.get("m");
                            unspentPaths.put(address, path);
                            if (SamouraiSentinel.getInstance().getBIP49().containsKey(m)) {
                                unspentBIP49.put(address, 0);   // assume account 0
                            }
                            else if(SamouraiSentinel.getInstance().getBIP84().containsKey(m))    {
                                unspentBIP84.put(address, 0);   // assume account 0
                            }
                            else    {
                                unspentAccounts.put(address, AddressFactory.getInstance(context).xpub2account().get(m));
                            }
                        } else {
                            ;
                        }

                        // Construct the output
                        MyTransactionOutPoint outPoint = new MyTransactionOutPoint(txHash, txOutputN, value, scriptBytes, address);
                        outPoint.setConfirmations(confirmations);

                        if (utxos.containsKey(script)) {
                            utxos.get(script).getOutpoints().add(outPoint);
                        } else {
                            UTXO utxo = new UTXO();
                            utxo.getOutpoints().add(outPoint);
                            utxos.put(script, utxo);
                        }

                    } catch (Exception e) {
                        ;
                    }

                }

                return true;

            } catch (JSONException je) {
                ;
            }

        }

        return false;

    }

    public synchronized UTXO getUnspentOutputsForSweep(String address) {

        String _url = WebUtil.getAPIUrl(context);

        try {

            String response = null;

            FormBody body = new FormBody.Builder()
                    .add("active", address)
                    .build();


            response = WebUtil.getInstance(context).postURL(_url + "unspent?", body);

            return parseUnspentOutputsForSweep(response);

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    private synchronized UTXO parseUnspentOutputsForSweep(String unspents) {

        Log.d("APIFactory", "unspents:" + unspents);

        UTXO utxo = null;

        if (unspents != null) {

            try {
                JSONObject jsonObj = new JSONObject(unspents);

                if (jsonObj == null || !jsonObj.has("unspent_outputs")) {
                    return null;
                }
                JSONArray utxoArray = jsonObj.getJSONArray("unspent_outputs");
                if (utxoArray == null || utxoArray.length() == 0) {
                    return null;
                }

//            Log.d("APIFactory", "unspents found:" + outputsRoot.size());

                for (int i = 0; i < utxoArray.length(); i++) {

                    JSONObject outDict = utxoArray.getJSONObject(i);

                    byte[] hashBytes = Hex.decode((String) outDict.get("tx_hash"));
                    Sha256Hash txHash = Sha256Hash.wrap(hashBytes);
                    int txOutputN = ((Number) outDict.get("tx_output_n")).intValue();
                    BigInteger value = BigInteger.valueOf(((Number) outDict.get("value")).longValue());
                    String script = (String) outDict.get("script");
                    byte[] scriptBytes = Hex.decode(script);
                    int confirmations = ((Number) outDict.get("confirmations")).intValue();

                    try {
                        String address = null;
                        if (Bech32Util.getInstance().isBech32Script(script)) {
                            address = Bech32Util.getInstance().getAddressFromScript(script);
                            Log.d("address parsed:", address);
                        } else {
                            address = new Script(scriptBytes).getToAddress(SamouraiSentinel.getInstance().getCurrentNetworkParams()).toString();
                        }

                        // Construct the output
                        MyTransactionOutPoint outPoint = new MyTransactionOutPoint(txHash, txOutputN, value, scriptBytes, address);
                        outPoint.setConfirmations(confirmations);
                        if (utxo == null) {
                            utxo = new UTXO();
                        }
                        utxo.getOutpoints().add(outPoint);

                    } catch (Exception e) {
                        ;
                    }

                }

            } catch (JSONException je) {
                ;
            }

        }

        return utxo;

    }

    public void setAppToken(String apiToken) {
        APP_TOKEN = apiToken;
    }

    private class TxMostRecentDateComparator implements Comparator<Tx> {

        public int compare(Tx t1, Tx t2) {

            final int BEFORE = -1;
            final int EQUAL = 0;
            final int AFTER = 1;

            int ret = 0;

            if (t1 == null || t2 == null) {
                return EQUAL;
            }

            if (t1.getTS() > t2.getTS()) {
                ret = BEFORE;
            } else if (t1.getTS() < t2.getTS()) {
                ret = AFTER;
            } else {
                ret = EQUAL;
            }

            return ret;
        }

    }

    public synchronized JSONObject getDynamicFees() {

        JSONObject jsonObject = null;

        try {
            String url = WebUtil.getAPIUrl(context).concat("fees");
            url = url.concat("?at=".concat(getAccessToken()));
//            Log.i("APIFactory", "Dynamic fees:" + url.toString());
            String response = WebUtil.getInstance(context).getURL(url);
//            Log.i("APIFactory", "Dynamic fees response:" + response);
            try {
                jsonObject = new JSONObject(response);
                parseDynamicFees_bitcoind(jsonObject);
            } catch (JSONException je) {
                je.printStackTrace();
                jsonObject = null;
            }
        } catch (Exception e) {
            jsonObject = null;
            e.printStackTrace();
        }

        return jsonObject;
    }

    public String getAccessToken() {
        if (ACCESS_TOKEN == null && APIFactory.getInstance(context).APITokenRequired()) {
            getToken(true);
        }
        return DojoUtil.getInstance(context).getDojoParams() == null ? "" : ACCESS_TOKEN;
    }


    public void setAccessToken(String accessToken) {
        ACCESS_TOKEN = accessToken;
    }


    public synchronized boolean APITokenRequired() {
        return DojoUtil.getInstance(context).getDojoParams() != null;
    }


    public boolean stayingAlive()   {

        if(!AppUtil.getInstance(context).isOfflineMode() && APITokenRequired())    {

            if(APIFactory.getInstance(context).getAccessToken() == null)    {
                APIFactory.getInstance(context).getToken(false);
            }

            if(APIFactory.getInstance(context).getAccessToken() != null)    {
                JWT jwt = new JWT(APIFactory.getInstance(context).getAccessToken());
                if(jwt != null && jwt.isExpired(APIFactory.getInstance(context).getAccessTokenRefresh()))    {
                    if(APIFactory.getInstance(context).getToken(false))  {
                        return true;
                    }
                    else    {
                        return false;
                    }
                }
            }

            return false;

        }
        else    {
            return true;
        }

    }


    public synchronized boolean getToken(boolean setupDojo) {

        try {
            if (!APITokenRequired()) {
                return true;
            }

            String _url = SamouraiSentinel.getInstance().isTestNet() ? WebUtil.SAMOURAI_API2_TESTNET : WebUtil.SAMOURAI_API2;

            if (DojoUtil.getInstance(context).getDojoParams() != null || setupDojo) {
                _url = SamouraiSentinel.getInstance().isTestNet() ? WebUtil.SAMOURAI_API2_TESTNET_TOR : WebUtil.SAMOURAI_API2_TOR;
            }

            debug("APIFactory", "getToken() url:" + _url);

            JSONObject jsonObject = null;

            try {

                String response = null;

    //            if(AppUtil.getInstance(context).isOfflineMode())    {
    //                return true;
    //            }
    //            else
                if (!TorManager.getInstance(context).isRequired()) {
                    // use POST
                    StringBuilder args = new StringBuilder();
                    args.append("apikey=");
                    args.append(new String(getXORKey()));
                    response = WebUtil.getInstance(context).postURL(_url + "auth/login?", args.toString());
                    info("APIFactory", "API token response:" + response);
                } else {
                    info("APIFactory", "API key (XOR):" + new String(getXORKey()));
                    info("APIFactory", "API key url:" + _url);
                    StringBuilder args = new StringBuilder();
                    args.append("apikey=");
                    args.append(new String(getXORKey()));
                    response = WebUtil.getInstance(context).tor_postURL(_url + "auth/login?".concat(args.toString()),"" );
                    info("APIFactory", "API token response:" + response);
                }

                try {
                    jsonObject = new JSONObject(response);
                    if (jsonObject != null && jsonObject.has("authorizations")) {
                        JSONObject authObj = jsonObject.getJSONObject("authorizations");
                        if (authObj.has("access_token")) {
                            info("APIFactory", "setting access token:" + authObj.getString("access_token"));
                            setAccessToken(authObj.getString("access_token"));
                            return true;
                        }
                    }
                } catch (JSONException je) {
                    je.printStackTrace();
                    jsonObject = null;
                    return false;
                }
            } catch (Exception e) {
                jsonObject = null;
                e.printStackTrace();
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            debug("FUCK",e.getMessage());
        }

        return true;
    }

    public long getAccessTokenRefresh() {
        return ACCESS_TOKEN_REFRESH;
    }

    private synchronized boolean parseDynamicFees_bitcoind(JSONObject jsonObject) throws JSONException {

        if (jsonObject != null) {

            //
            // bitcoind
            //
            List<SuggestedFee> suggestedFees = new ArrayList<SuggestedFee>();

            if (jsonObject.has("2")) {
                long fee = jsonObject.getInt("2");
                SuggestedFee suggestedFee = new SuggestedFee();
                suggestedFee.setDefaultPerKB(BigInteger.valueOf(fee * 1000L));
                suggestedFee.setStressed(false);
                suggestedFee.setOK(true);
                suggestedFees.add(suggestedFee);
            }

            if (jsonObject.has("6")) {
                long fee = jsonObject.getInt("6");
                SuggestedFee suggestedFee = new SuggestedFee();
                suggestedFee.setDefaultPerKB(BigInteger.valueOf(fee * 1000L));
                suggestedFee.setStressed(false);
                suggestedFee.setOK(true);
                suggestedFees.add(suggestedFee);
            }

            if (jsonObject.has("24")) {
                long fee = jsonObject.getInt("24");
                SuggestedFee suggestedFee = new SuggestedFee();
                suggestedFee.setDefaultPerKB(BigInteger.valueOf(fee * 1000L));
                suggestedFee.setStressed(false);
                suggestedFee.setOK(true);
                suggestedFees.add(suggestedFee);
            }

            if (suggestedFees.size() > 0) {
                FeeUtil.getInstance().setEstimatedFees(suggestedFees);

                Log.d("APIFactory", "high fee:" + FeeUtil.getInstance().getHighFee().getDefaultPerKB().toString());
                Log.d("APIFactory", "suggested fee:" + FeeUtil.getInstance().getSuggestedFee().getDefaultPerKB().toString());
                Log.d("APIFactory", "low fee:" + FeeUtil.getInstance().getLowFee().getDefaultPerKB().toString());
            }

            return true;

        }

        return false;

    }


    public byte[] getXORKey() {

        if (APP_TOKEN != null) {
            return APP_TOKEN.getBytes();
        }

        if (BuildConfig.XOR_1.length() > 0 && BuildConfig.XOR_2.length() > 0) {
            byte[] xorSegments0 = Base64.decode(BuildConfig.XOR_1);
            byte[] xorSegments1 = Base64.decode(BuildConfig.XOR_2);
            return xor(xorSegments0, xorSegments1);
        } else {
            return null;
        }
    }

    private byte[] xor(byte[] b0, byte[] b1) {

        byte[] ret = new byte[b0.length];

        for (int i = 0; i < b0.length; i++) {
            ret[i] = (byte) (b0[i] ^ b1[i]);
        }

        return ret;

    }
}
