package com.samourai.sentinel.ui.webview

import com.samourai.sentinel.core.SentinelState
import com.samourai.sentinel.ui.utils.PrefsUtil
import com.samourai.sentinel.util.MonetaryUtil
import org.koin.java.KoinJavaComponent
import org.koin.java.KoinJavaComponent.inject

/**
 * sentinel-android
 *
 * @author Sarath
 */

object ExplorerRepository {

    private const val TX_KEY = ":TXID:"

    data class Explorer(val url: String, val name: String, val testnet: Boolean = false)

    private val prefsUtil: PrefsUtil by inject(PrefsUtil::class.java);

    private val Explorers: ArrayList<Explorer> = arrayListOf(
            Explorer("https://oxt.me/transaction/${TX_KEY}", name = "oxt.me"),
            Explorer("https://blockstream.info/tx/${TX_KEY}", name = "blockstream.info"),
            Explorer("https://blockchair.com/bitcoin/testnet/transaction/${TX_KEY}", name = "blockstream.info-testnet",testnet = true),
            Explorer("https://blockstream.info/testnet/tx/${TX_KEY}", name = "blockstream.info-testnet",testnet = true),
    )

    fun getExplorer(txId: String): String {
        return if (SentinelState.isTestNet()) {
            makeUrl(Explorers.first { it.testnet }, txId)
        } else {
            val selection = prefsUtil.selectedExplorer
            val explorer = Explorers.first { it.name == selection }
            makeUrl(explorer, txId)
        }
    }

    private fun makeUrl(explorer: Explorer, txId: String): String {
        return explorer.url.replace(TX_KEY, txId)
    }

    fun getExplorers(): ArrayList<Explorer> {
        return Explorers
    }
}