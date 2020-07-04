package com.samourai.sentinel.hd;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.Utils;
import org.bitcoinj.crypto.ChildNumber;
import org.bitcoinj.crypto.DeterministicKey;
import org.bitcoinj.crypto.HDKeyDerivation;

import org.json.JSONException;
import org.json.JSONObject;

public class HD_Address {

    private int mChildNum;
    private String strPath = null;
    private ECKey ecKey = null;

    private NetworkParameters mParams = null;

    private HD_Address() { ; }

    public HD_Address(NetworkParameters params, DeterministicKey cKey, int child) {

        mParams = params;
        mChildNum = child;

        DeterministicKey dk = HDKeyDerivation.deriveChildKey(cKey, new ChildNumber(mChildNum, false));
        if(dk.hasPrivKey())    {
            ecKey = new ECKey(dk.getPrivKeyBytes(), dk.getPubKey());
        }
        else    {
            ecKey = ECKey.fromPublicOnly(dk.getPubKey());
        }

        long now = Utils.now().getTime() / 1000;
        ecKey.setCreationTimeSeconds(now);

        strPath = dk.getPath().toString();
    }

    public String getAddressString() {
        return ecKey.toAddress(mParams).toString();
    }

    public Address getAddress() {
        return ecKey.toAddress(mParams);
    }

    public ECKey getECKey() {
        return ecKey;
    }

    public JSONObject toJSON() {
        try {
            JSONObject obj = new JSONObject();

            obj.put("path", strPath);
            obj.put("address", getAddressString());

            return obj;
        }
        catch(JSONException ex) {
            throw new RuntimeException(ex);
        }
    }
}
