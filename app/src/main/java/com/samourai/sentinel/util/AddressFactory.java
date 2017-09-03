package com.samourai.sentinel.util;

import android.content.Context;
import android.widget.Toast;

import org.bitcoinj.core.ECKey;
import org.bitcoinj.crypto.MnemonicException;
import com.samourai.sentinel.access.AccessFactory;
import com.samourai.sentinel.hd.HD_Address;
import com.samourai.sentinel.hd.HD_WalletFactory;

import org.json.JSONException;

import java.io.IOException;
import java.util.HashMap;

public class AddressFactory {

    public static final int LOOKAHEAD_GAP = 20;

    public static final int RECEIVE_CHAIN = 0;
    public static final int CHANGE_CHAIN = 1;

    private static Context context = null;
    private static AddressFactory instance = null;

    private static HashMap<Integer,Integer> highestTxReceiveIdx = null;
    private static HashMap<Integer,Integer> highestTxChangeIdx = null;

    private static HashMap<String,Integer> xpub2account = null;
    private static HashMap<Integer,String> account2xpub = null;

    private AddressFactory() { ; }

    public static AddressFactory getInstance(Context ctx) {

        context = ctx;

        if(instance == null) {
            instance = new AddressFactory();

            highestTxReceiveIdx = new HashMap<Integer,Integer>();
            highestTxChangeIdx = new HashMap<Integer,Integer>();
            xpub2account = new HashMap<String,Integer>();
            account2xpub = new HashMap<Integer,String>();
        }

        return instance;
    }

    public static AddressFactory getInstance() {

        if(instance == null) {
            instance = new AddressFactory();

            highestTxReceiveIdx = new HashMap<Integer,Integer>();
            highestTxChangeIdx = new HashMap<Integer,Integer>();
            xpub2account = new HashMap<String,Integer>();
            account2xpub = new HashMap<Integer,String>();
        }

        return instance;
    }

    public String get(int chain, int accountIdx)	{

        int idx = 0;
        HD_Address addr = null;

        try	{
            idx = HD_WalletFactory.getInstance(context).get().getAccount(accountIdx).getChain(chain).getAddrIdx();
            addr = HD_WalletFactory.getInstance(context).get().getAccount(accountIdx).getChain(chain).getAddressAt(idx);
            if(chain == RECEIVE_CHAIN && canIncReceiveAddress(accountIdx))	{
                HD_WalletFactory.getInstance(context).get().getAccount(accountIdx).getChain(chain).incAddrIdx();
            }
        }
        catch(IOException ioe)	{
            ioe.printStackTrace();
            Toast.makeText(context, "HD wallet error", Toast.LENGTH_SHORT).show();
        }
        catch(MnemonicException.MnemonicLengthException mle)	{
            mle.printStackTrace();
            Toast.makeText(context, "HD wallet error", Toast.LENGTH_SHORT).show();
        }

        return addr.getAddressString();

    }

    public ECKey getECKey(int chain, int accountIdx)	{

        int idx = 0;
        HD_Address addr = null;

        try	{
            idx = HD_WalletFactory.getInstance(context).get().getAccount(accountIdx).getChain(chain).getAddrIdx();
            addr = HD_WalletFactory.getInstance(context).get().getAccount(accountIdx).getChain(chain).getAddressAt(idx);
            if(chain == RECEIVE_CHAIN && canIncReceiveAddress(accountIdx))	{
                HD_WalletFactory.getInstance(context).get().getAccount(accountIdx).getChain(chain).incAddrIdx();
            }
        }
        catch(IOException ioe)	{
            ioe.printStackTrace();
            Toast.makeText(context, "HD wallet error", Toast.LENGTH_SHORT).show();
        }
        catch(MnemonicException.MnemonicLengthException mle)	{
            mle.printStackTrace();
            Toast.makeText(context, "HD wallet error", Toast.LENGTH_SHORT).show();
        }

        return addr.getECKey();

    }

    public HD_Address get(int accountIdx, int chain, int idx)	{

        HD_Address addr = null;

        try	{
            addr = HD_WalletFactory.getInstance(context).get().getAccount(accountIdx).getChain(chain).getAddressAt(idx);
        }
        catch(IOException ioe)	{
            ioe.printStackTrace();
            Toast.makeText(context, "HD wallet error", Toast.LENGTH_SHORT).show();
        }
        catch(MnemonicException.MnemonicLengthException mle)	{
            mle.printStackTrace();
            Toast.makeText(context, "HD wallet error", Toast.LENGTH_SHORT).show();
        }

        return addr;
    }

    public int getHighestTxReceiveIdx(int account)  {
        if(highestTxReceiveIdx.get(account) != null)  {
            return highestTxReceiveIdx.get(account);
        }
        else  {
            return -1;
        }
    }

    public void setHighestTxReceiveIdx(int account, int idx) {
        //       Log.i("AddressFactory", "setting highestTxReceiveIdx to " + idx);
        highestTxReceiveIdx.put(account, idx);
    }

    public int getHighestTxChangeIdx(int account) {
        if(highestTxChangeIdx.get(account) != null)  {
            return highestTxChangeIdx.get(account);
        }
        else  {
            return -1;
        }
    }

    public void setHighestTxChangeIdx(int account, int idx) {
        //       Log.i("AddressFactory", "setting highestTxChangeIdx to " + idx);
        highestTxChangeIdx.put(account, idx);
    }

    public boolean canIncReceiveAddress(int account, int idx) {
        if(highestTxReceiveIdx.get(account) != null) {
            return ((idx - highestTxReceiveIdx.get(account)) < (LOOKAHEAD_GAP - 1));
        }
        else {
            return false;
        }
    }

    public boolean canIncReceiveAddress(int account) {
        try {
            return canIncReceiveAddress(account, HD_WalletFactory.getInstance(context).get().getAccount(account).getReceive().getAddrIdx());
        } catch (Exception e) {
            return false;
        }
    }

    public HashMap<String,Integer> xpub2account()   {
        return xpub2account;
    }

    public HashMap<Integer,String> account2xpub()   {
        return account2xpub;
    }

}
