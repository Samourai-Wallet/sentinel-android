package com.samourai.sentinel;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
//import android.util.Log;

import com.samourai.sentinel.crypto.AESUtil;
import com.samourai.sentinel.network.dojo.DojoUtil;
import com.samourai.sentinel.segwit.P2SH_P2WPKH;
import com.samourai.sentinel.segwit.SegwitAddress;
import com.samourai.sentinel.util.AddressFactory;
import com.samourai.sentinel.util.CharSequenceX;
import com.samourai.sentinel.util.MapUtil;
import com.samourai.sentinel.util.PrefsUtil;
import com.samourai.sentinel.util.ReceiveLookAtUtil;

import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.params.MainNetParams;
import org.bitcoinj.params.TestNet3Params;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class SamouraiSentinel {

    private static NetworkParameters networkParams = null;

    private static HashMap<String,String> xpubs = null;
    private static HashMap<String,String> legacy = null;
    private static HashMap<String,String> bip49 = null;
    private static HashMap<String,String> bip84 = null;
    private static HashMap<String,Integer> highestReceiveIdx = null;

    private static SamouraiSentinel instance = null;
    private static Context context = null;

    private static int currentSelectedAccount = 0;

    private static String dataDir = "wallet";
    private static String strFilename = "sentinel.dat";
    private static String strTmpFilename = "sentinel.bak";

    private static String strSentinelXPUB = "sentinel.xpub";
    private static String strSentinelBIP49 = "sentinel.bip49";
    private static String strSentinelBIP84 = "sentinel.bip84";
    private static String strSentinelLegacy = "sentinel.legacy";

    private SamouraiSentinel()    { ; }

    public static SamouraiSentinel getInstance()  {

        if(instance == null)    {
            xpubs = new HashMap<String,String>();
            bip49 = new HashMap<String,String>();
            bip84 = new HashMap<String,String>();
            legacy = new HashMap<String,String>();
            highestReceiveIdx = new HashMap<String,Integer>();

            instance = new SamouraiSentinel();
        }

        return instance;
    }

    public static SamouraiSentinel getInstance(Context ctx)  {

        context = ctx;

        if(instance == null)    {
            xpubs = new HashMap<String,String>();
            bip49 = new HashMap<String,String>();
            bip84 = new HashMap<String,String>();
            legacy = new HashMap<String,String>();
            highestReceiveIdx = new HashMap<String,Integer>();

            instance = new SamouraiSentinel();
        }

        return instance;
    }

    public void setCurrentSelectedAccount(int account) {
        currentSelectedAccount = account;
    }

    public int getCurrentSelectedAccount() {
        return currentSelectedAccount;
    }

    public HashMap<String,String> getXPUBs()    { return xpubs; }

    public HashMap<String,String> getBIP49()    { return bip49; }

    public HashMap<String,String> getBIP84()    { return bip84; }

    public HashMap<String,String> getLegacy()    { return legacy; }

    public List<String> getAllAddrsSorted()    {

        HashMap<String,String> mapAll = new HashMap<String,String>();
        mapAll.putAll(xpubs);
        mapAll.putAll(bip49);
        mapAll.putAll(bip84);
        mapAll = MapUtil.getInstance().sortByValue(mapAll);
        mapAll.putAll(MapUtil.getInstance().sortByValue(legacy));

        List<String> xpubList = new ArrayList<String>();
        xpubList.addAll(mapAll.keySet());

        return xpubList;
    }

    public HashMap<String,String> getAllMapSorted()    {

        HashMap<String,String> mapAll = new HashMap<String,String>();
        mapAll.putAll(xpubs);
        mapAll.putAll(bip49);
        mapAll.putAll(bip84);
        mapAll = MapUtil.getInstance().sortByValue(mapAll);
        mapAll.putAll(MapUtil.getInstance().sortByValue(legacy));

        return mapAll;
    }

    public void parseJSON(JSONObject obj) {

        xpubs.clear();

        try {

            if(obj.has("testnet"))    {
                setCurrentNetworkParams(obj.getBoolean("testnet") ? TestNet3Params.get() : MainNetParams.get());
                PrefsUtil.getInstance(context).setValue(PrefsUtil.TESTNET, obj.getBoolean("testnet"));
            }
            else    {
                setCurrentNetworkParams(MainNetParams.get());
                PrefsUtil.getInstance(context).removeValue(PrefsUtil.TESTNET);
            }

            if(obj != null && obj.has("xpubs"))    {
                JSONArray _xpubs = obj.getJSONArray("xpubs");
                for(int i = 0; i < _xpubs.length(); i++)   {
                    JSONObject _obj = _xpubs.getJSONObject(i);
                    Iterator it = _obj.keys();
                    while (it.hasNext()) {
                        String key = (String)it.next();
                        if(key.equals("receiveIdx"))    {
                            highestReceiveIdx.put(key, _obj.getInt(key));
                        }
                        else    {
                            xpubs.put(key, _obj.getString(key));
                        }
                    }
                }
            }

            if(obj != null && obj.has("bip49"))    {
                JSONArray _bip49s = obj.getJSONArray("bip49");
                for(int i = 0; i < _bip49s.length(); i++)   {
                    JSONObject _obj = _bip49s.getJSONObject(i);
                    Iterator it = _obj.keys();
                    while (it.hasNext()) {
                        String key = (String)it.next();
                        if(key.equals("receiveIdx"))    {
                            highestReceiveIdx.put(key, _obj.getInt(key));
                        }
                        else    {
                            bip49.put(key, _obj.getString(key));
                        }
                    }
                }
            }

            if(obj != null && obj.has("bip84"))    {
                JSONArray _bip84s = obj.getJSONArray("bip84");
                for(int i = 0; i < _bip84s.length(); i++)   {
                    JSONObject _obj = _bip84s.getJSONObject(i);
                    Iterator it = _obj.keys();
                    while (it.hasNext()) {
                        String key = (String)it.next();
                        if(key.equals("receiveIdx"))    {
                            highestReceiveIdx.put(key, _obj.getInt(key));
                        }
                        else    {
                            bip84.put(key, _obj.getString(key));
                        }
                    }
                }
            }

            if(obj != null && obj.has("legacy"))    {
                JSONArray _addr = obj.getJSONArray("legacy");
                for(int i = 0; i < _addr.length(); i++)   {
                    JSONObject _obj = _addr.getJSONObject(i);
                    Iterator it = _obj.keys();
                    while (it.hasNext()) {
                        String key = (String)it.next();
                        legacy.put(key, _obj.getString(key));
                    }
                }
            }

            if(obj != null && obj.has("receives"))    {
                ReceiveLookAtUtil.getInstance().fromJSON(obj.getJSONArray("receives"));
            }
            if(obj != null && obj.has("dojo"))    {
                DojoUtil.getInstance(context).fromJSON(new JSONObject(obj.getString("dojo")));
            }

        }
        catch(JSONException ex) {
            throw new RuntimeException(ex);
        }

    }

    public JSONObject toJSON() {
        try {
            JSONObject obj = new JSONObject();

            obj.put("version_name", context.getText(R.string.version_name));
            obj.put("testnet", isTestNet());

            JSONArray _xpubs = new JSONArray();
            for(String xpub : xpubs.keySet()) {
                JSONObject _obj = new JSONObject();
                _obj.put(xpub, xpubs.get(xpub));
                _obj.put("receiveIdx", highestReceiveIdx.get(xpub) == null ? 0 : highestReceiveIdx.get(xpub));
                _xpubs.put(_obj);
            }
            obj.put("xpubs", _xpubs);

            JSONArray _bip49s = new JSONArray();
            for(String b49 : bip49.keySet()) {
                JSONObject _obj = new JSONObject();
                _obj.put(b49, bip49.get(b49));
                _obj.put("receiveIdx", highestReceiveIdx.get(b49) == null ? 0 : highestReceiveIdx.get(b49));
                _bip49s.put(_obj);
            }
            obj.put("bip49", _bip49s);

            JSONArray _bip84s = new JSONArray();
            for(String b84 : bip84.keySet()) {
                JSONObject _obj = new JSONObject();
                _obj.put(b84, bip84.get(b84));
                _obj.put("receiveIdx", highestReceiveIdx.get(b84) == null ? 0 : highestReceiveIdx.get(b84));
                _bip84s.put(_obj);
            }
            obj.put("bip84", _bip84s);

            JSONArray _addr = new JSONArray();
            for(String addr : legacy.keySet()) {
                JSONObject _obj = new JSONObject();
                _obj.put(addr, legacy.get(addr));
                _addr.put(_obj);
            }
            obj.put("legacy", _addr);

            obj.put("receives", ReceiveLookAtUtil.getInstance().toJSON());
            obj.put("receives", ReceiveLookAtUtil.getInstance().toJSON());

            if(DojoUtil.getInstance(context).getDojoParams() != null){
                try {
                    obj.put("dojo", DojoUtil.getInstance(context).toJSON().toString());
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }


            return obj;
        }
        catch(JSONException ex) {
            throw new RuntimeException(ex);
        }
    }

    public boolean payloadExists()  {
        File dir = context.getDir(dataDir, Context.MODE_PRIVATE);
        File file = new File(dir, strFilename);
        return file.exists();
    }

    public synchronized void serialize(JSONObject jsonobj, CharSequenceX password) throws IOException, JSONException {

        File dir = context.getDir(dataDir, Context.MODE_PRIVATE);
        File newfile = new File(dir, strFilename);
        File tmpfile = new File(dir, strTmpFilename);

        // serialize to byte array.
        String jsonstr = jsonobj.toString(4);

        // prepare tmp file.
        if(tmpfile.exists()) {
            tmpfile.delete();
        }

        String data = null;
        if(password != null) {
            data = AESUtil.encrypt(jsonstr, password, AESUtil.DefaultPBKDF2Iterations);
        }
        else {
            data = jsonstr;
        }

        FileOutputStream fos = new FileOutputStream(tmpfile);
        fos.write(data.getBytes());
        fos.close();

        // rename tmp file
        if(tmpfile.renameTo(newfile)) {
//            mLogger.info("file saved to  " + newfile.getPath());
//            Log.i("HD_WalletFactory", "file saved to  " + newfile.getPath());
        }
        else {
//            mLogger.warn("rename to " + newfile.getPath() + " failed");
//            Log.i("HD_WalletFactory", "rename to " + newfile.getPath() + " failed");
        }

        saveToPrefs();

    }

    public synchronized JSONObject deserialize(CharSequenceX password) throws IOException, JSONException {

        File dir = context.getDir(dataDir, Context.MODE_PRIVATE);
        File file = new File(dir, strFilename);
        StringBuilder sb = new StringBuilder("");

        FileInputStream fis = new FileInputStream(file);
        byte[] buffer = new byte[1024];
        int n = 0;
        while((n = fis.read(buffer)) != -1) {
            sb.append(new String(buffer, 0, n));
        }
        fis.close();

        JSONObject node = null;
        if(password == null) {
            node = new JSONObject(sb.toString());
        }
        else {
            String decrypted = null;
            try {
                decrypted = AESUtil.decrypt(sb.toString(), password, AESUtil.DefaultPBKDF2Iterations);
            }
            catch(Exception e) {
                return null;
            }
            if(decrypted == null) {
                return null;
            }
            node = new JSONObject(decrypted);
        }

        return node;
    }

    public void saveToPrefs()  {

        SharedPreferences _xpub = context.getSharedPreferences(strSentinelXPUB, 0);
        SharedPreferences.Editor xEditor = _xpub.edit();
        for(String xpub : xpubs.keySet()) {
            xEditor.putString(xpub, xpubs.get(xpub));
        }
        xEditor.commit();

        SharedPreferences _bip49 = context.getSharedPreferences(strSentinelBIP49, 0);
        SharedPreferences.Editor bEditor = _bip49.edit();
        for(String b49 : bip49.keySet()) {
            bEditor.putString(b49, bip49.get(b49));
        }
        bEditor.commit();

        SharedPreferences _bip84 = context.getSharedPreferences(strSentinelBIP84, 0);
        SharedPreferences.Editor zEditor = _bip84.edit();
        for(String b84 : bip84.keySet()) {
            zEditor.putString(b84, bip84.get(b84));
        }
        zEditor.commit();

        SharedPreferences _legacy = context.getSharedPreferences(strSentinelLegacy, 0);
        SharedPreferences.Editor lEditor = _legacy.edit();
        for(String leg : legacy.keySet()) {
            lEditor.putString(leg, legacy.get(leg));
        }
        lEditor.commit();

    }

    public void restoreFromPrefs()  {

        SharedPreferences xpub = context.getSharedPreferences(strSentinelXPUB, 0);
        if(xpub != null)    {
            Map<String, ?> allXPUB = xpub.getAll();
            for (Map.Entry<String, ?> entry : allXPUB.entrySet()) {
                SamouraiSentinel.getInstance(context).getXPUBs().put(entry.getKey(), entry.getValue().toString());
            }
        }

        SharedPreferences bip49s = context.getSharedPreferences(strSentinelBIP49, 0);
        if(bip49s != null)    {
            Map<String, ?> all49 = bip49s.getAll();
            for (Map.Entry<String, ?> entry : all49.entrySet()) {
                SamouraiSentinel.getInstance(context).getBIP49().put(entry.getKey(), entry.getValue().toString());
            }
        }

        SharedPreferences bip84s = context.getSharedPreferences(strSentinelBIP84, 0);
        if(bip84s != null)    {
            Map<String, ?> all84 = bip84s.getAll();
            for (Map.Entry<String, ?> entry : all84.entrySet()) {
                SamouraiSentinel.getInstance(context).getBIP84().put(entry.getKey(), entry.getValue().toString());
            }
        }

        SharedPreferences legacy = context.getSharedPreferences(strSentinelLegacy, 0);
        if(legacy != null)    {
            Map<String, ?> allLegacy = legacy.getAll();
            for (Map.Entry<String, ?> entry : allLegacy.entrySet()) {
                SamouraiSentinel.getInstance(context).getLegacy().put(entry.getKey(), entry.getValue().toString());
            }
        }

    }

    public void deleteFromPrefs(String xpub)  {

        SharedPreferences _xpub = context.getSharedPreferences(strSentinelXPUB, 0);
        SharedPreferences.Editor xEditor = _xpub.edit();
        xEditor.remove(xpub);
        xEditor.commit();

        SharedPreferences _bip49 = context.getSharedPreferences(strSentinelBIP49, 0);
        SharedPreferences.Editor bEditor = _bip49.edit();
        bEditor.remove(xpub);
        bEditor.commit();

        SharedPreferences _bip84 = context.getSharedPreferences(strSentinelBIP84, 0);
        SharedPreferences.Editor zEditor = _bip84.edit();
        zEditor.remove(xpub);
        zEditor.commit();

        SharedPreferences _legacy = context.getSharedPreferences(strSentinelLegacy, 0);
        SharedPreferences.Editor lEditor = _legacy.edit();
        lEditor.remove(xpub);
        lEditor.commit();

    }

    public String getReceiveAddress()  {

        final List<String> xpubList = getAllAddrsSorted();

        String addr = null;
        ECKey ecKey = null;

        if(xpubList.get(SamouraiSentinel.getInstance(context).getCurrentSelectedAccount() - 1).startsWith("xpub") ||
                xpubList.get(SamouraiSentinel.getInstance(context).getCurrentSelectedAccount() - 1).startsWith("ypub") ||
                xpubList.get(SamouraiSentinel.getInstance(context).getCurrentSelectedAccount() - 1).startsWith("zpub") ||
        xpubList.get(SamouraiSentinel.getInstance(context).getCurrentSelectedAccount() - 1).startsWith("tpub") ||
        xpubList.get(SamouraiSentinel.getInstance(context).getCurrentSelectedAccount() - 1).startsWith("upub") ||
        xpubList.get(SamouraiSentinel.getInstance(context).getCurrentSelectedAccount() - 1).startsWith("vpub")
                )    {
            String xpub = xpubList.get(SamouraiSentinel.getInstance(context).getCurrentSelectedAccount() - 1);
            Log.d("SamouraiSentinel", "xpub:" + xpub);
            int account = AddressFactory.getInstance(context).xpub2account().get(xpub);
            Log.d("SamouraiSentinel", "account:" + account);
            if(SamouraiSentinel.getInstance(context).getBIP49().keySet().contains(xpub))    {
                ecKey = AddressFactory.getInstance(context).getECKey(AddressFactory.RECEIVE_CHAIN, account);
                P2SH_P2WPKH p2sh_p2wpkh = new P2SH_P2WPKH(ecKey.getPubKey(), SamouraiSentinel.getInstance().getCurrentNetworkParams());
                addr = p2sh_p2wpkh.getAddressAsString();
                Log.d("SamouraiSentinel", "addr:" + addr);
            }
            else if(SamouraiSentinel.getInstance(context).getBIP84().keySet().contains(xpub))    {
                ecKey = AddressFactory.getInstance(context).getECKey(AddressFactory.RECEIVE_CHAIN, account);
                SegwitAddress segwitAddress = new SegwitAddress(ecKey.getPubKey(), SamouraiSentinel.getInstance().getCurrentNetworkParams());
                addr = segwitAddress.getBech32AsString();
                Log.d("SamouraiSentinel", "addr:" + addr);
            }
            else    {
                addr = AddressFactory.getInstance(context).get(AddressFactory.RECEIVE_CHAIN, account);
            }
        }
        else    {
            addr = xpubList.get(SamouraiSentinel.getInstance(context).getCurrentSelectedAccount() - 1);
        }

        return addr;

    }

    public NetworkParameters getCurrentNetworkParams() {
        return (networkParams == null) ? MainNetParams.get() : networkParams;
    }

    public void setCurrentNetworkParams(NetworkParameters params) {
        networkParams = params;
    }

    public boolean isTestNet()  {
        return (networkParams == null) ? false : !(getCurrentNetworkParams() instanceof MainNetParams);
    }

}
