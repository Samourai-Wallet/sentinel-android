package com.samourai.sentinel.hd;

import com.google.bitcoin.core.Address;
import com.google.bitcoin.core.ECKey;
import com.google.bitcoin.core.NetworkParameters;
import com.google.bitcoin.core.Utils;
import com.google.bitcoin.crypto.ChildNumber;
import com.google.bitcoin.crypto.DeterministicKey;
import com.google.bitcoin.crypto.HDKeyDerivation;

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
        ecKey = new ECKey(dk.getPrivKeyBytes(), dk.getPubKeyBytes());

        long now = Utils.now().getTime() / 1000;
        ecKey.setCreationTimeSeconds(now);

        strPath = dk.getPath();
    }

    public String getAddressString() {
        return ecKey.toAddress(mParams).toString();
    }

    public Address getAddress() {
        return ecKey.toAddress(mParams);
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
