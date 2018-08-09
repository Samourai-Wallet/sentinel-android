package com.samourai.sentinel.segwit.bech32;

import com.samourai.sentinel.SamouraiSentinel;

import org.bitcoinj.params.TestNet3Params;
import org.bouncycastle.util.encoders.Hex;

public class Bech32Util {

    private static Bech32Util instance = null;

    private Bech32Util() { ; }

    public static Bech32Util getInstance() {

        if(instance == null) {
            instance = new Bech32Util();
        }

        return instance;
    }

    public boolean isBech32Script(String script) {
        return isP2WPKHScript(script) || isP2WSHScript(script);
    }

    public boolean isP2WPKHScript(String script) {
        return script.startsWith("0014");
    }

    public boolean isP2WSHScript(String script) {
        return script.startsWith("0020");
    }

    public String getAddressFromScript(String script) throws Exception    {

        String hrp = null;

        return Bech32Segwit.encode(SamouraiSentinel.getInstance().isTestNet() ? "tb" : "bc", (byte)0x00, Hex.decode(script.substring(4).getBytes()));
    }

}
