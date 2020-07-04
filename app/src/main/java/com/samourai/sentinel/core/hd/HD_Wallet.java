package com.samourai.sentinel.hd;

//import android.util.Log;

import org.bitcoinj.core.AddressFormatException;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.crypto.DeterministicKey;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class HD_Wallet {

    private ArrayList<HD_Account> mAccounts = null;

    private NetworkParameters mParams = null;

    private HD_Wallet() { ; }

    /*
    create from account xpub key(s)
     */
    public HD_Wallet(NetworkParameters params, String[] xpub) throws AddressFormatException {

        mParams = params;
        DeterministicKey aKey = null;
        mAccounts = new ArrayList<HD_Account>();

        List<String> seen_xpub = new ArrayList<String>();
        for(int i = 0; i < xpub.length; i++) {
            if(seen_xpub.contains(xpub[i]))    {
                continue;
            }
            mAccounts.add(new HD_Account(mParams, xpub[i], "", i));
//            Log.i("HD_Wallet", xpub[i]);
            seen_xpub.add(xpub[i]);
        }

    }

    public List<HD_Account> getAccounts() {
        return mAccounts;
    }

    public HD_Account getAccount(int accountId) {
        return mAccounts.get(accountId);
    }

    public int size() {

        int sz = 0;
        for(HD_Account acct : mAccounts) {
            sz += acct.size();
        }

        return sz;
    }

    public JSONObject toJSON() {
        try {
            JSONObject obj = new JSONObject();

            JSONArray accts = new JSONArray();
            for(HD_Account acct : mAccounts) {
                accts.put(acct.toJSON());
            }
            obj.put("accounts", accts);

            obj.put("receiveIdx", mAccounts.get(0).getReceive().getAddrIdx());
            obj.put("changeIdx", mAccounts.get(0).getChange().getAddrIdx());

//            obj.put("prev_balance", APIFactory.getInstance().getXpubBalance());

            return obj;
        }
        catch(JSONException ex) {
            throw new RuntimeException(ex);
        }
    }

}
