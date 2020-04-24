package com.samourai.sentinel.utxos.models;


import com.samourai.sentinel.sweep.MyTransactionOutPoint;
import com.samourai.sentinel.sweep.UTXO;


/**
 * Sections for UTXO lists
 */
public class UTXOCoinSegment extends UTXOCoin {

    //for UTXOActivity
    public boolean isActive = false;

    //for whirlpool utxo list
    public boolean unCycled = false;

    public UTXOCoinSegment(MyTransactionOutPoint outPoint, UTXO utxo) {
        super(outPoint, utxo);
    }
}