package com.samourai.sentinel.core

import com.samourai.sentinel.data.LatestBlock
import org.bitcoinj.core.NetworkParameters

/**
 * Utility class for handling basic app states
 */
class SentinelState {

    private var networkParams: NetworkParameters? = NetworkParameters.fromID(NetworkParameters.ID_MAINNET)
    var blockHeight: LatestBlock? = null

    fun setNetworkParam(param: NetworkParameters) {
        this.networkParams = param;
    }


}