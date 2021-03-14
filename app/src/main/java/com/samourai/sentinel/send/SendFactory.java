package com.samourai.sentinel.send;

import com.samourai.sentinel.core.SentinelState;
import com.samourai.sentinel.core.segwit.bech32.Bech32Util;
import com.samourai.sentinel.sweep.MyTransactionInput;
import com.samourai.sentinel.util.FormatsUtil;
import com.samourai.wallet.send.MyTransactionOutPoint;
import com.samourai.wallet.util.FormatsUtilGeneric;

import org.bitcoinj.core.Coin;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionInput;
import org.bitcoinj.core.TransactionOutput;
import org.bitcoinj.script.Script;
import org.bitcoinj.script.ScriptBuilder;
import org.bitcoinj.script.ScriptOpCodes;
import org.bouncycastle.util.encoders.Hex;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


public class SendFactory	{

    private static SendFactory instance = null;

    private SendFactory () { ; }

    public static SendFactory getInstance() {


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
        Transaction tx = new Transaction(SentinelState.Companion.getNetworkParam());

        for(Iterator<Map.Entry<String, BigInteger>> iterator = receivers.entrySet().iterator(); iterator.hasNext();) {
            Map.Entry<String, BigInteger> mapEntry = iterator.next();
            String toAddress = mapEntry.getKey();
            BigInteger value = mapEntry.getValue();
/*
            if(value.compareTo(SamouraiWallet.bDust) < 1)    {
                throw new Exception(context.getString(R.string.dust_amount));
            }
*/
            if(value == null || (value.compareTo(BigInteger.ZERO) <= 0 && !FormatsUtilGeneric.getInstance().isValidBIP47OpReturn(toAddress))) {
                throw new Exception("Invalid amount");
            }

            TransactionOutput output = null;
            Script toOutputScript = null;
            if(!FormatsUtil.Companion.isValidBitcoinAddress(toAddress) && FormatsUtilGeneric.getInstance().isValidBIP47OpReturn(toAddress))    {
                toOutputScript = new ScriptBuilder().op(ScriptOpCodes.OP_RETURN).data(Hex.decode(toAddress)).build();
                output = new TransactionOutput(SentinelState.Companion.getNetworkParam(), null, Coin.valueOf(0L), toOutputScript.getProgram());
            }
            else if(FormatsUtil.Companion.isValidBech32(toAddress))   {
                output = Bech32Util.getInstance().getTransactionOutput(toAddress, value.longValue());
            }
            else    {
                toOutputScript = ScriptBuilder.createOutputScript(org.bitcoinj.core.Address.fromBase58(SentinelState.Companion.getNetworkParam(), toAddress));
                output = new TransactionOutput(SentinelState.Companion.getNetworkParam(), null, Coin.valueOf(value.longValue()), toOutputScript.getProgram());
            }

            outputs.add(output);
        }

        List<MyTransactionInput> inputs = new ArrayList<MyTransactionInput>();
        for(MyTransactionOutPoint outPoint : unspent) {
            Script script = new Script(outPoint.getScriptBytes());

            if(script.getScriptType() == Script.ScriptType.NO_TYPE) {
                continue;
            }

            MyTransactionInput input = new MyTransactionInput(SentinelState.Companion.getNetworkParam(), null, new byte[0], outPoint, outPoint.getTxHash().toString(), outPoint.getTxOutputN());
//            if(PrefsUtil.getInstance(context).getValue(PrefsUtil.RBF_OPT_IN, false) == true)    {
//                input.setSequenceNumber(SamouraiWallet.RBF_SEQUENCE_VAL.longValue());
//            }
            inputs.add(input);
        }

        //
        // deterministically sort inputs and outputs, see BIP69 (OBPP)
        //
//        Collections.sort(inputs, new BIP69InputComparator());
        for(TransactionInput input : inputs) {
            tx.addInput(input);
        }

        Collections.sort(outputs, new com.samourai.sentinel.sweep.SendFactory.BIP69OutputComparator());
        for(TransactionOutput to : outputs) {
            tx.addOutput(to);
        }

        return tx;
    }

}
