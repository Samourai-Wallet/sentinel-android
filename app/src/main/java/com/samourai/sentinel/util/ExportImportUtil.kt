package com.samourai.sentinel.util

import com.google.gson.reflect.TypeToken
import com.samourai.sentinel.BuildConfig
import com.samourai.sentinel.core.access.AccessFactory
import com.samourai.sentinel.core.crypto.AESUtil
import com.samourai.sentinel.data.AddressTypes
import com.samourai.sentinel.data.PubKeyCollection
import com.samourai.sentinel.data.PubKeyModel
import com.samourai.sentinel.data.db.dao.TxDao
import com.samourai.sentinel.data.db.dao.UtxoDao
import com.samourai.sentinel.data.repository.CollectionRepository
import com.samourai.sentinel.helpers.fromJSON
import com.samourai.sentinel.helpers.toJSON
import com.samourai.sentinel.ui.dojo.DojoUtility
import com.samourai.sentinel.ui.utils.PrefsUtil
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import org.koin.java.KoinJavaComponent.inject

/**
 * sentinel-android
 *
 */

class ExportImportUtil {

    private val accessFactory: AccessFactory by inject(AccessFactory::class.java);
    private val txDao: TxDao by inject(TxDao::class.java);
    private val utxoDao: UtxoDao by inject(UtxoDao::class.java);
    private val dojoUtility: DojoUtility by inject(DojoUtility::class.java);
    private val prefsUtil: PrefsUtil by inject(PrefsUtil::class.java);
    private val collectionRepository: CollectionRepository by inject(CollectionRepository::class.java);


    fun makePayload(): JSONObject {
        return JSONObject().apply {
            put("collections", JSONArray(collectionRepository.pubKeyCollections.toJSON()))
            put("prefs", prefsUtil.export())
            if (dojoUtility.isDojoEnabled() && dojoUtility.exportDojoPayload() != null) {
                put("dojo", JSONObject(dojoUtility.exportDojoPayload()!!))
            }
        }
    }

    fun addVersionInfo(content: String): JSONObject {
        val payload = JSONObject()
        payload.put("version", BuildConfig.VERSION_CODE)
        payload.put("time", System.currentTimeMillis())
        payload.put("payload", content)
        return payload
    }

    fun decryptAndParseSamouraiPayload(backUp: String, password: String): PubKeyCollection {
        val backUpJson = JSONObject(backUp)
        if (backUpJson.has("payload")) {
            val decrypted = AESUtil.decrypt(backUpJson.getString("payload"), CharSequenceX(password), AESUtil.DefaultPBKDF2Iterations)
            val pubKeyCollection = PubKeyCollection(collectionLabel = "Samourai wallet")
            val json = JSONObject(decrypted)
            if (json.has("wallet")) {
                val wallet = json.getJSONObject("wallet")
                val accounts = if (wallet.has("accounts")) wallet.getJSONArray("accounts") else JSONArray()
                val biP49Accounts = if (wallet.has("bip49_accounts")) wallet.getJSONArray("bip49_accounts") else JSONArray()
                val bip84Accounts = if (wallet.has("bip84_accounts")) wallet.getJSONArray("bip84_accounts") else JSONArray()
                val whirlpoolAccount = if (wallet.has("whirlpool_account")) wallet.getJSONArray("whirlpool_account") else JSONArray()

                //Add default BIP44 xpub account
                repeat(accounts.length()) {
                    val jsonObject = accounts.getJSONObject(it)
                    if (jsonObject.has("xpub")) {
                        val xpub = jsonObject.getString("xpub")
                        if (FormatsUtil.isValidXpub(xpub)) {
                            val pubKeyModel = PubKeyModel(pubKey = xpub,
                                    label = "BIP44 account ${it}",
                                    AddressTypes.BIP44,
                                    change_index = if (jsonObject.has("changeIdx")) jsonObject.getInt("changeIdx") else 0,
                                    account_index = if (jsonObject.has("receiveIdx")) jsonObject.getInt("receiveIdx") else 0
                            )
                            pubKeyCollection.pubs.add(pubKeyModel)
                        }
                    }
                }

                //Add BIP49 xpubs
                repeat(biP49Accounts.length()) {
                    val jsonObject = biP49Accounts.getJSONObject(it)
                    if (jsonObject.has("ypub")) {
                        val xpub = jsonObject.getString("ypub")
                        if (FormatsUtil.isValidXpub(xpub)) {
                            val pubKeyModel = PubKeyModel(pubKey = xpub,
                                    label = "BIP49 account ${it}",
                                    AddressTypes.BIP49,
                                    change_index = if (jsonObject.has("changeIdx")) jsonObject.getInt("changeIdx") else 0,
                                    account_index = if (jsonObject.has("receiveIdx")) jsonObject.getInt("receiveIdx") else 0
                            )
                            pubKeyCollection.pubs.add(pubKeyModel)
                        }
                    }
                }

                //Add BIP84 xpub
                repeat(bip84Accounts.length()) {
                    val jsonObject = bip84Accounts.getJSONObject(it)
                    if (jsonObject.has("zpub")) {
                        val xpub = jsonObject.getString("zpub")
                        if (FormatsUtil.isValidXpub(xpub)) {
                            var label = "BIP84 account ${it}"
                            val pubKeyModel = PubKeyModel(pubKey = xpub,
                                    label = label,
                                    AddressTypes.BIP84,
                                    change_index = if (jsonObject.has("changeIdx")) jsonObject.getInt("changeIdx") else 0,
                                    account_index = if (jsonObject.has("receiveIdx")) jsonObject.getInt("receiveIdx") else 0
                            )
                            pubKeyCollection.pubs.add(pubKeyModel)
                        }
                    }
                }

                //Add Whirlpool accounts
                repeat(whirlpoolAccount.length()) {
                    val jsonObject = whirlpoolAccount.getJSONObject(it)
                    if (jsonObject.has("zpub")) {
                        val xpub = jsonObject.getString("zpub")
                        var label = "Whirlpool $it"
                        when (it) {
                            0 -> {
                                label = "Whirlpool Pre-mix"
                            }
                            1 -> {
                                label = "Whirlpool Post-mix"
                            }
                            2 -> {
                                label = "Whirlpool bad bank"
                            }
                        }
                        if (FormatsUtil.isValidXpub(xpub)) {
                            val pubKeyModel = PubKeyModel(pubKey = xpub,
                                    label = label,
                                    AddressTypes.BIP84,
                                    change_index = if (jsonObject.has("changeIdx")) jsonObject.getInt("changeIdx") else 0,
                                    account_index = if (jsonObject.has("receiveIdx")) jsonObject.getInt("receiveIdx") else 0
                            )
                            pubKeyCollection.pubs.add(pubKeyModel)
                        }
                    }
                }
                return pubKeyCollection
            } else {
                throw  Exception("Invalid payload")
            }
        } else {
            throw  Exception("Invalid payload")
        }
    }

    fun decryptSentinel(backUp: String, password: String): Triple<ArrayList<PubKeyCollection>?, JSONObject, JSONObject?> {
        val json = JSONObject(backUp)
        if (json.has("payload")) {
            val decrypted = AESUtil.decrypt(json.getString("payload"), CharSequenceX(password), AESUtil.DefaultPBKDF2Iterations)
            val payloadJSON = JSONObject(decrypted)
            val collectionArrayType = object : TypeToken<ArrayList<PubKeyCollection?>?>() {}.type
            val collections = fromJSON<ArrayList<PubKeyCollection>>(payloadJSON.getJSONArray("collections").toString(), collectionArrayType)
            val prefs = payloadJSON.getJSONObject("prefs")
            var dojo: JSONObject? = null
            if (payloadJSON.has("dojo")) {
                dojo = payloadJSON.getJSONObject("dojo")
            }
            return Triple(collections, prefs, dojo)
        } else {
            throw  Exception("Invalid payload")
        }
    }


    suspend fun startImportCollections(pubKeyCollection: ArrayList<PubKeyCollection>, replace: Boolean) = withContext(Dispatchers.IO) {
        try {
            if (replace) {
                utxoDao.delete()
                txDao.delete()
                collectionRepository.reset()
            }
            pubKeyCollection.forEach { collectionRepository.addNew(it) }
        } catch (ex: Exception) {
            throw  CancellationException(ex.message)
        }
    }

    suspend fun importDojo(dojo: JSONObject) = withContext(Dispatchers.IO) {
        dojoUtility.setDojo(dojo.toString())
    }

    fun importPrefs(it: JSONObject) {
        prefsUtil.import(it)
    }
}