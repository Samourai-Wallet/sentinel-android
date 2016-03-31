package com.samourai.sentinel.hd;

import android.content.Context;
//import android.util.Log;

import com.google.bitcoin.core.AddressFormatException;
import com.google.bitcoin.core.NetworkParameters;
import com.google.bitcoin.crypto.MnemonicException;
import com.google.bitcoin.params.MainNetParams;
import com.samourai.sentinel.crypto.AESUtil;
import com.samourai.sentinel.util.AddressFactory;
import com.samourai.sentinel.util.CharSequenceX;
import com.samourai.sentinel.util.PrefsUtil;

import org.apache.commons.codec.DecoderException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

public class HD_WalletFactory	{

    public static final String BIP39_ENGLISH_SHA256 = "ad90bf3beb7b0eb7e5acd74727dc0da96e0a280a258354e7293fb7e211ac03db";

    private static HD_WalletFactory instance = null;
    private static List<HD_Wallet> wallets = null;

    private static Logger mLogger = LoggerFactory.getLogger(HD_WalletFactory.class);

    public static String strJSONFilePath = null;
    private static String dataDir = "wallet";
    private static String strFilename = "samourai.dat";
    private static String strTmpFilename = "samourai.bak";

    private static Context context = null;

    private HD_WalletFactory()	{ ; }

    public static HD_WalletFactory getInstance(Context ctx) {

    	context = ctx;

        if (instance == null) {
            wallets = new ArrayList<HD_Wallet>();
            instance = new HD_WalletFactory();
        }

        return instance;
    }

    public static HD_WalletFactory getInstance(Context ctx, String path) {

    	context = ctx;
        strJSONFilePath = path;

        if (instance == null) {
            wallets = new ArrayList<HD_Wallet>();
            instance = new HD_WalletFactory();
        }

        return instance;
    }

    public HD_Wallet restoreWallet(String data, String passphrase, int nbAccounts) throws AddressFormatException, DecoderException  {

        HD_Wallet hdw = null;

        if(passphrase == null) {
            passphrase = "";
        }

        NetworkParameters params = MainNetParams.get();

        byte[] seed = null;
        if(data.startsWith("xpub")) {
            String[] xpub = data.split(":");
//            Log.i("HD_WalletFactory", "xpubs:" + xpub.length);
            hdw = new HD_Wallet(params, xpub);
        }

        if(hdw == null) {
            PrefsUtil.getInstance(context).clear();
            return null;
        }

        wallets.clear();
        wallets.add(hdw);

        return hdw;
    }

    public HD_Wallet get() throws IOException, MnemonicException.MnemonicLengthException {

        if(wallets.size() < 1) {
            // if wallets list is empty, create 12-word wallet without passphrase and 2 accounts
//            wallets.add(0, newWallet(12, "", 2));
            /*
            wallets.clear();
            wallets.add(newWallet(12, "", 2));
            */
            return null;
        }

        return wallets.get(0);
    }

    public void set(HD_Wallet wallet)	{

    	if(wallet != null)	{
            wallets.clear();
        	wallets.add(wallet);
    	}

    }

    public void wipe() throws IOException	{

        try	{
            int nbAccounts = HD_WalletFactory.getInstance(context).get().getAccounts().size();

            for(int i = 0; i < nbAccounts; i++)	{
                HD_WalletFactory.getInstance(context).get().getAccount(i).getReceive().setAddrIdx(0);
                HD_WalletFactory.getInstance(context).get().getAccount(i).getChange().setAddrIdx(0);
                AddressFactory.getInstance().setHighestTxReceiveIdx(i, 0);
                AddressFactory.getInstance().setHighestTxChangeIdx(i, 0);
            }
            HD_WalletFactory.getInstance(context).set(null);
        }
        catch(MnemonicException.MnemonicLengthException mle)	{
            mle.printStackTrace();
        }

        File dir = context.getDir(dataDir, Context.MODE_PRIVATE);
        File datfile = new File(dir, strFilename);
        File tmpfile = new File(dir, strTmpFilename);

        if(tmpfile.exists()) {
            tmpfile.delete();
        }

        if(datfile.exists()) {
            datfile.delete();

            try {
                serialize(new JSONObject("{}"), new CharSequenceX(""));
            }
            catch(JSONException je) {
                je.printStackTrace();
            }
        }

    }

    public void saveWalletToJSON(CharSequenceX password) throws MnemonicException.MnemonicLengthException, IOException, JSONException {
        serialize(get().toJSON(), password);
    }

    public HD_Wallet restoreWalletfromJSON(CharSequenceX password) throws DecoderException, MnemonicException.MnemonicLengthException, AddressFormatException {

        HD_Wallet hdw = null;

        NetworkParameters params = MainNetParams.get();

        JSONObject obj = null;
        try {
            obj = deserialize(password);
//            Log.i("HD_WalletFactory", obj.toString());
            if(obj != null && obj.has("accounts")) {

                List<String> xpubs = new ArrayList<String>();

                JSONArray accounts = obj.getJSONArray("accounts");
                for(int i = 0; i < accounts.length(); i++) {
                    xpubs.add(((JSONObject)accounts.get(i)).getString("xpub"));
                }

                hdw = new HD_Wallet(params, xpubs.toArray(new String[xpubs.size()]));

                if(hdw != null)    {
                    for(int i = 0; i < accounts.length(); i++) {
                        JSONObject account = accounts.getJSONObject(i);
                        hdw.getAccount(i).getReceive().setAddrIdx(account.has("receiveIdx") ? account.getInt("receiveIdx") : 0);
                        hdw.getAccount(i).getChange().setAddrIdx(account.has("changeIdx") ? account.getInt("changeIdx") : 0);

                        AddressFactory.getInstance().account2xpub().put(i, hdw.getAccount(i).xpubstr());
                        AddressFactory.getInstance().xpub2account().put(hdw.getAccount(i).xpubstr(), i);
                    }
                }

            }
        }
        catch(IOException ioe) {
            ioe.printStackTrace();
        }
        catch(JSONException je) {
            je.printStackTrace();
        }

        wallets.clear();
        wallets.add(hdw);

        return hdw;
    }

    public boolean walletFileExists()  {
        File dir = context.getDir(dataDir, Context.MODE_PRIVATE);
        File walletfile = new File(dir, strFilename);
        return walletfile.exists();
    }

    private void serialize(JSONObject jsonobj, CharSequenceX password) throws IOException, JSONException {

        File dir = context.getDir(dataDir, Context.MODE_PRIVATE);
        File newfile = new File(dir, strFilename);
        File tmpfile = new File(dir, strTmpFilename);
        newfile.setWritable(true, true);
        tmpfile.setWritable(true, true);

        // serialize to byte array.
        String jsonstr = jsonobj.toString(4);
        byte[] cleartextBytes = jsonstr.getBytes(Charset.forName("UTF-8"));

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

        Writer out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(tmpfile), "UTF-8"));
        try {
            out.write(data);
        } finally {
            out.close();
        }

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

    private JSONObject deserialize(CharSequenceX password) throws IOException, JSONException {

        File dir = context.getDir(dataDir, Context.MODE_PRIVATE);
        File file = new File(dir, strFilename);
//        Log.i("HD_WalletFactory", "wallet file exists: " + file.exists());
        StringBuilder sb = new StringBuilder();

        BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF8"));
        String str = null;

        while((str = in.readLine()) != null) {
            sb.append(str);
        }

        in.close();

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

}
