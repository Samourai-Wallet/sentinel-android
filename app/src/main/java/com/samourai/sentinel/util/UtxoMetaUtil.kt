package com.samourai.sentinel.util

import com.samourai.sentinel.data.Utxo
import com.samourai.sentinel.data.db.SentinelCollectionStore
import kotlinx.coroutines.*
import org.koin.java.KoinJavaComponent.inject
import timber.log.Timber

/**
 * sentinel-android
 *
 * @author Sarath
 */
object UtxoMetaUtil {

    data class UtxoState(val hash: String,
                         val index: Int,
                         val associatedPub: String,
                         val amount: Long)

    data class BlockPayload(val utxoBlockState: MutableMap<String, UtxoState>)

    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
    private val dbHandler: SentinelCollectionStore by inject(SentinelCollectionStore::class.java)

    private var utxoBlockState: MutableMap<String, UtxoState> = mutableMapOf()

    init {
        read()
    }

    fun put(utxo: Utxo) {
        val key = "${utxo.txHash}-${utxo.txOutputN}"
        utxoBlockState[key] = UtxoState(
                utxo.txHash!!,
                utxo.txOutputN!!,
                utxo.pubKey,
                if (utxo.value != null) utxo.value!! else 0
        )
        write()
    }

    fun has(hash: String, index: Int): Boolean {
        val key = "${hash}-${index}"
        return utxoBlockState.containsKey(key)
    }

    fun has(utxo: Utxo): Boolean {
        val key = "${utxo.txHash}-${utxo.txOutputN}"
        return utxoBlockState.containsKey(key)
    }

    fun getBlockedAssociatedWithPubKey(pubKey: String): Collection<UtxoState> {
        return utxoBlockState.values.filter { it.associatedPub == pubKey }
    }

    fun remove(utxo: Utxo) {
        val key = "${utxo.txHash}-${utxo.txOutputN}"
        if (utxoBlockState.containsKey(key)) {
            utxoBlockState.remove(key)
            write()
        }
    }


    @Synchronized
    fun read() {
        scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val items = dbHandler.getUtxoMetaData().read<BlockPayload>()
                    items?.let {
                        if (it.utxoBlockState != null)
                            utxoBlockState = it.utxoBlockState
                    }
                }
            } catch (e: Exception) {
                throw CancellationException(e.message)
            }
        }
    }

    @Synchronized
    private fun write() {
        scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val payload = BlockPayload(utxoBlockState)
                    dbHandler.getUtxoMetaData().write(payload, replace = true)
                }
            } catch (e: Exception) {
                Timber.e(e)
                throw CancellationException(e.message)
            }
        }
    }

    fun dispose() {
        if (scope.isActive)
            scope.cancel()
    }
}