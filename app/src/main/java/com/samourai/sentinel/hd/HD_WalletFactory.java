package com.samourai.sentinel.hd;

import android.content.Context;
//import android.util.Log;

import org.bitcoinj.core.AddressFormatException;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.crypto.MnemonicException;

import com.samourai.sentinel.SamouraiSentinel;
import com.samourai.sentinel.util.PrefsUtil;

import org.apache.commons.codec.DecoderException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class HD_WalletFactory	{

    private static HD_WalletFactory instance = null;
    private static List<HD_Wallet> wallets = null;

    public static String strJSONFilePath = null;

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

        NetworkParameters params = SamouraiSentinel.getInstance().getCurrentNetworkParams();

        if(data.startsWith("xpub") || data.startsWith("ypub") || data.startsWith("zpub") || data.startsWith("tpub") || data.startsWith("upub") || data.startsWith("vpub")) {
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

}
