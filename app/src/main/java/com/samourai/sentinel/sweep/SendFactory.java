package com.samourai.sentinel.sweep;

import android.content.Context;
import android.widget.Toast;

import com.samourai.sentinel.SamouraiSentinel;
import com.samourai.sentinel.hd.HD_WalletFactory;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.AddressFormatException;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.DumpedPrivateKey;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionInput;
import org.bitcoinj.core.TransactionOutput;
import org.bitcoinj.core.TransactionWitness;
import org.bitcoinj.crypto.TransactionSignature;
import org.bitcoinj.script.Script;
import org.bitcoinj.script.ScriptBuilder;
import org.bitcoinj.script.ScriptException;
import org.bouncycastle.util.encoders.Hex;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.samourai.sentinel.R;
import com.samourai.sentinel.segwit.P2SH_P2WPKH;
import com.samourai.sentinel.segwit.bech32.Bech32Util;
import com.samourai.sentinel.util.FormatsUtil;

public class SendFactory	{

    private static SendFactory instance = null;
    private static Context context = null;

    private SendFactory () { ; }

    public static SendFactory getInstance(Context ctx) {

        context = ctx;

        if(instance == null)	{
            instance = new SendFactory();
        }

        return instance;
    }

    public Transaction makeTransaction(final int accountIdx, final List<MyTransactionOutPoint> unspent, final HashMap<String, BigInteger> receivers) {

        Transaction tx = null;

        try {
//            int changeIdx = HD_WalletFactory.getInstance(context).get().getAccount(accountIdx).getChange().getAddrIdx();
            tx = makeTransaction(accountIdx, receivers, unspent);
        }
        catch(Exception e) {
            e.printStackTrace();
        }

        return tx;
    }

    public Transaction signTransactionForSweep(Transaction unsignedTx, PrivKeyReader privKeyReader)    {

        HashMap<String,ECKey> keyBag = new HashMap<String,ECKey>();

        for (TransactionInput input : unsignedTx.getInputs()) {

            try {
                byte[] scriptBytes = input.getOutpoint().getConnectedPubKeyScript();
//                String address = new BitcoinScript(scriptBytes).getAddress().toString();
                String address = new Script(scriptBytes).getToAddress(SamouraiSentinel.getInstance().getCurrentNetworkParams()).toString();
//                        Log.i("address from script", address);
                ECKey ecKey = null;
                try {
                    DumpedPrivateKey pk = new DumpedPrivateKey(SamouraiSentinel.getInstance().getCurrentNetworkParams(), privKeyReader.getKey().getPrivateKeyAsWiF(SamouraiSentinel.getInstance().getCurrentNetworkParams()));
                    ecKey = pk.getKey();
//                    Log.i("SendFactory", "ECKey address:" + ecKey.toAddress(SamouraiSentinel.getInstance().getCurrentNetworkParams()).toString());
                } catch (AddressFormatException afe) {
                    afe.printStackTrace();
                    continue;
                }

                if(ecKey != null) {
                    keyBag.put(input.getOutpoint().toString(), ecKey);
                }
                else {
                    Toast.makeText(context, R.string.cannot_recognize_privkey, Toast.LENGTH_SHORT).show();
//                    Log.i("ECKey error", "cannot process private key");
                }
            }
            catch(ScriptException se) {
                ;
            }
            catch(Exception e) {
                ;
            }

        }

        Transaction signedTx = signTransaction(unsignedTx, keyBag);
        if(signedTx == null)    {
            return null;
        }
        else    {
            String hexString = new String(Hex.encode(signedTx.bitcoinSerialize()));
            if(hexString.length() > (100 * 1024)) {
                Toast.makeText(context, R.string.tx_length_error, Toast.LENGTH_SHORT).show();
//              Log.i("SendFactory", "Transaction length too long");
            }

            return signedTx;
        }
    }

    /*
    Used by spends
     */
    private Transaction makeTransaction(int accountIdx, HashMap<String, BigInteger> receivers, List<MyTransactionOutPoint> unspent) throws Exception {

        BigInteger amount = BigInteger.ZERO;
        for(Iterator<Map.Entry<String, BigInteger>> iterator = receivers.entrySet().iterator(); iterator.hasNext();) {
            Map.Entry<String, BigInteger> mapEntry = iterator.next();
            amount = amount.add(mapEntry.getValue());
        }

        List<TransactionOutput> outputs = new ArrayList<TransactionOutput>();
        Transaction tx = new Transaction(SamouraiSentinel.getInstance().getCurrentNetworkParams());

        for(Iterator<Map.Entry<String, BigInteger>> iterator = receivers.entrySet().iterator(); iterator.hasNext();) {
            Map.Entry<String, BigInteger> mapEntry = iterator.next();
            String toAddress = mapEntry.getKey();
            BigInteger value = mapEntry.getValue();
/*
            if(value.compareTo(SamouraiWallet.bDust) < 1)    {
                throw new Exception(context.getString(R.string.dust_amount));
            }
*/
            if(value == null || value.compareTo(BigInteger.ZERO) <= 0) {
                throw new Exception(context.getString(R.string.invalid_amount));
            }

            TransactionOutput output = null;
            if(FormatsUtil.getInstance().isValidBech32(toAddress))   {
                output = Bech32Util.getInstance().getTransactionOutput(toAddress, value.longValue());
            }
            else    {
                Script toOutputScript = ScriptBuilder.createOutputScript(org.bitcoinj.core.Address.fromBase58(SamouraiSentinel.getInstance().getCurrentNetworkParams(), toAddress));
                output = new TransactionOutput(SamouraiSentinel.getInstance().getCurrentNetworkParams(), null, Coin.valueOf(value.longValue()), toOutputScript.getProgram());
            }

            outputs.add(output);
        }

        List<MyTransactionInput> inputs = new ArrayList<MyTransactionInput>();
        for(MyTransactionOutPoint outPoint : unspent) {
            Script script = new Script(outPoint.getScriptBytes());

            if(script.getScriptType() == Script.ScriptType.NO_TYPE) {
                continue;
            }

            MyTransactionInput input = new MyTransactionInput(SamouraiSentinel.getInstance().getCurrentNetworkParams(), null, new byte[0], outPoint, outPoint.getTxHash().toString(), outPoint.getTxOutputN());
            inputs.add(input);
        }

        //
        // deterministically sort inputs and outputs, see BIP69 (OBPP)
        //
        Collections.sort(inputs, new BIP69InputComparator());
        for(TransactionInput input : inputs) {
            tx.addInput(input);
        }

        Collections.sort(outputs, new BIP69OutputComparator());
        for(TransactionOutput to : outputs) {
            tx.addOutput(to);
        }

        return tx;
    }

    private synchronized Transaction signTransaction(Transaction transaction, HashMap<String,ECKey> keyBag) throws ScriptException {

        List<TransactionInput> inputs = transaction.getInputs();

        TransactionInput input = null;
        TransactionOutput connectedOutput = null;
        byte[] connectedPubKeyScript = null;
        TransactionSignature sig = null;
        Script scriptPubKey = null;
        ECKey key = null;

        for (int i = 0; i < inputs.size(); i++) {

            input = inputs.get(i);

            key = keyBag.get(input.getOutpoint().toString());
            connectedPubKeyScript = input.getOutpoint().getConnectedPubKeyScript();
            connectedOutput = input.getOutpoint().getConnectedOutput();
            scriptPubKey = connectedOutput.getScriptPubKey();

            String script = Hex.toHexString(connectedPubKeyScript);
            String address = null;
            if(Bech32Util.getInstance().isBech32Script(script))    {
                try {
                    address = Bech32Util.getInstance().getAddressFromScript(script);
                }
                catch(Exception e) {
                    ;
                }
            }
            else    {
                address = new Script(connectedPubKeyScript).getToAddress(SamouraiSentinel.getInstance().getCurrentNetworkParams()).toString();
            }

            if(FormatsUtil.getInstance().isValidBech32(address) || Address.fromBase58(SamouraiSentinel.getInstance().getCurrentNetworkParams(), address).isP2SHAddress())    {

                final P2SH_P2WPKH p2shp2wpkh = new P2SH_P2WPKH(key.getPubKey(), SamouraiSentinel.getInstance().getCurrentNetworkParams());
                System.out.println("pubKey:" + Hex.toHexString(key.getPubKey()));
//                final Script scriptPubKey = p2shp2wpkh.segWitOutputScript();
//                System.out.println("scriptPubKey:" + Hex.toHexString(scriptPubKey.getProgram()));
                System.out.println("to address from script:" + scriptPubKey.getToAddress(SamouraiSentinel.getInstance().getCurrentNetworkParams()).toString());
                final Script redeemScript = p2shp2wpkh.segWitRedeemScript();
                System.out.println("redeem script:" + Hex.toHexString(redeemScript.getProgram()));
                final Script scriptCode = redeemScript.scriptCode();
                System.out.println("script code:" + Hex.toHexString(scriptCode.getProgram()));

                sig = transaction.calculateWitnessSignature(i, key, scriptCode, connectedOutput.getValue(), Transaction.SigHash.ALL, false);
                final TransactionWitness witness = new TransactionWitness(2);
                witness.setPush(0, sig.encodeToBitcoin());
                witness.setPush(1, key.getPubKey());
                transaction.setWitness(i, witness);

                if(!FormatsUtil.getInstance().isValidBech32(address) && Address.fromBase58(SamouraiSentinel.getInstance().getCurrentNetworkParams(), address).isP2SHAddress())    {
                    final ScriptBuilder sigScript = new ScriptBuilder();
                    sigScript.data(redeemScript.getProgram());
                    transaction.getInput(i).setScriptSig(sigScript.build());
                    transaction.getInput(i).getScriptSig().correctlySpends(transaction, i, scriptPubKey, connectedOutput.getValue(), Script.ALL_VERIFY_FLAGS);
                }

            }
            else    {
                if(key != null && key.hasPrivKey() || key.isEncrypted()) {
                    sig = transaction.calculateSignature(i, key, connectedPubKeyScript, Transaction.SigHash.ALL, false);
                }
                else {
                    sig = TransactionSignature.dummy();   // watch only ?
                }

                if(scriptPubKey.isSentToAddress()) {
                    input.setScriptSig(ScriptBuilder.createInputScript(sig, key));
                }
                else if(scriptPubKey.isSentToRawPubKey()) {
                    input.setScriptSig(ScriptBuilder.createInputScript(sig));
                }
                else {
                    throw new RuntimeException("Unknown script type: " + scriptPubKey);
                }
            }

        }

        return transaction;

    }

    public static class BIP69InputComparator implements Comparator<MyTransactionInput> {

        public int compare(MyTransactionInput i1, MyTransactionInput i2) {

            final int BEFORE = -1;
            final int EQUAL = 0;
            final int AFTER = 1;

            byte[] h1 = Hex.decode(i1.getTxHash());
            byte[] h2 = Hex.decode(i2.getTxHash());

            int pos = 0;
            while(pos < h1.length && pos < h2.length)    {

                byte b1 = h1[pos];
                byte b2 = h2[pos];

                if((b1 & 0xff) < (b2 & 0xff))    {
                    return BEFORE;
                }
                else if((b1 & 0xff) > (b2 & 0xff))    {
                    return AFTER;
                }
                else    {
                    pos++;
                }

            }

            if(i1.getTxPos() < i2.getTxPos())    {
                return BEFORE;
            }
            else if(i1.getTxPos() > i2.getTxPos())    {
                return AFTER;
            }
            else    {
                return EQUAL;
            }

        }

    }

    public static class BIP69OutputComparator implements Comparator<TransactionOutput> {

        public int compare(TransactionOutput o1, TransactionOutput o2) {

            final int BEFORE = -1;
            final int EQUAL = 0;
            final int AFTER = 1;

            if(o1.getValue().compareTo(o2.getValue()) > 0) {
                return AFTER;
            }
            else if(o1.getValue().compareTo(o2.getValue()) < 0) {
                return BEFORE;
            }
            else    {

                byte[] b1 = o1.getScriptBytes();
                byte[] b2 = o2.getScriptBytes();

                int pos = 0;
                while(pos < b1.length && pos < b2.length)    {

                    if((b1[pos] & 0xff) < (b2[pos] & 0xff))    {
                        return BEFORE;
                    }
                    else if((b1[pos] & 0xff) > (b2[pos] & 0xff))    {
                        return AFTER;
                    }

                    pos++;
                }

                if(b1.length < b2.length)    {
                    return BEFORE;
                }
                else if(b1.length > b2.length)    {
                    return AFTER;
                }
                else    {
                    return EQUAL;
                }

            }

        }

    }

}
