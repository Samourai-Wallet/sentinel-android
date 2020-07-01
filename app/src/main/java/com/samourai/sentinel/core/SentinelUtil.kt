package com.samourai.sentinel.core

import org.bitcoinj.core.NetworkParameters

/**
 * Utility class for handling basic app states
 */
class SentinelUtil {

    private var networkParams: NetworkParameters? = NetworkParameters.fromID(NetworkParameters.ID_MAINNET)


    fun setNetworkParam(param: NetworkParameters) {
        this.networkParams = param;
    }


}