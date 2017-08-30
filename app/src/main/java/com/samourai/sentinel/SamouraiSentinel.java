package com.samourai.sentinel;

import android.content.Context;
//import android.util.Log;

import com.samourai.sentinel.crypto.AESUtil;
import com.samourai.sentinel.util.AddressFactory;
import com.samourai.sentinel.util.CharSequenceX;
import com.samourai.sentinel.util.ReceiveLookAtUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class SamouraiSentinel {

    public final static int SAMOURAI_ACCOUNT = 0;
    public final static int MIXING_ACCOUNT = 1;
//    public final static int PUBLIC_ACCOUNT = 2;

    public final static int NB_ACCOUNTS = 2;

    private static HashMap<String,String> xpubs = null;
    private static HashMap<String,String> legacy = null;
    private static HashMap<String,Integer> highestReceiveIdx = null;

    private static SamouraiSentinel instance = null;
    private static Context context = null;

    private static int currentSelectedAccount = 0;

    private static String dataDir = "wallet";
    private static String strFilename = "sentinel.dat";
    private static String strTmpFilename = "sentinel.bak";

    private SamouraiSentinel()    { ; }

    public static SamouraiSentinel getInstance(Context ctx)  {

        context = ctx;

        if(instance == null)    {
            xpubs = new HashMap<String,String>();
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

    public HashMap<String,String> getLegacy()    { return legacy; }

    public void parseJSON(JSONObject obj) {

        xpubs.clear();

        try {
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

        }
        catch(JSONException ex) {
            throw new RuntimeException(ex);
        }

    }

    public JSONObject toJSON() {
        try {
            JSONObject obj = new JSONObject();

            JSONArray _xpubs = new JSONArray();
            for(String xpub : xpubs.keySet()) {
                JSONObject _obj = new JSONObject();
                _obj.put(xpub, xpubs.get(xpub));
                _obj.put("receiveIdx", highestReceiveIdx.get(xpub) == null ? 0 : highestReceiveIdx.get(xpub));
                _xpubs.put(_obj);
            }
            obj.put("xpubs", _xpubs);

            JSONArray _addr = new JSONArray();
            for(String addr : legacy.keySet()) {
                JSONObject _obj = new JSONObject();
                _obj.put(addr, legacy.get(addr));
                _addr.put(_obj);
            }
            obj.put("legacy", _addr);

            obj.put("receives", ReceiveLookAtUtil.getInstance().toJSON());

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

    public String getReceiveAddress()  {

        final Set<String> xpubKeys = SamouraiSentinel.getInstance(context).getXPUBs().keySet();
        final Set<String> legacyKeys = SamouraiSentinel.getInstance(context).getLegacy().keySet();
        final List<String> xpubList = new ArrayList<String>();
        xpubList.addAll(xpubKeys);
        xpubList.addAll(legacyKeys);

        String addr = null;

        if(xpubList.get(SamouraiSentinel.getInstance(context).getCurrentSelectedAccount() - 1).startsWith("xpub"))    {
            String xpub = xpubList.get(SamouraiSentinel.getInstance(context).getCurrentSelectedAccount() - 1);
            int account = AddressFactory.getInstance(context).xpub2account().get(xpub);
            addr = AddressFactory.getInstance(context).get(AddressFactory.RECEIVE_CHAIN, account);
        }
        else    {
            addr = xpubList.get(SamouraiSentinel.getInstance(context).getCurrentSelectedAccount() - 1);
        }

        return addr;

    }

}
