package com.samourai.sentinel.tor.service.components.onionproxy

import com.samourai.sentinel.R
import io.matthewnelson.topl_core.base.TorConfigFiles
import io.matthewnelson.topl_core.util.TorInstaller
import com.samourai.sentinel.tor.TorServiceController
import com.samourai.sentinel.tor.util.ServiceConsts.PrefKeyList
import io.matthewnelson.topl_service.prefs.TorServicePrefs
import com.samourai.sentinel.tor.service.BaseService
import java.io.*
import java.util.concurrent.TimeoutException

/**
 * Installs things needed for Tor.
 *
 * @param [torService] [BaseService] for context
 * */
internal class ServiceTorInstaller(private val torService: BaseService): TorInstaller() {

    private companion object {
        const val APP_VERSION_CODE = "APP_VERSION_CODE"
    }

    private val torConfigFiles: TorConfigFiles
        get() = TorServiceController.getTorConfigFiles()

    private val torServicePrefs = TorServicePrefs(torService.context)
    private val localPrefs = BaseService.getLocalPrefs(torService.context)
    private var geoIpFileCopied = false
    private var geoIpv6FileCopied = false

    // broadcastLogger is available from TorInstaller and is instantiated as soon as
    // OnionProxyManager gets initialized.
//    var broadcastLogger: BroadcastLogger? = null

    @Throws(IOException::class, SecurityException::class)
    override fun setup() {
        if (!torConfigFiles.geoIpFile.exists()) {
            copyGeoIpAsset()
            geoIpFileCopied = true
        }
        if (!torConfigFiles.geoIpv6File.exists()) {
            copyGeoIpv6Asset()
            geoIpv6FileCopied = true
        }

        // If the app version has been increased, or if this is a debug build, copy over
        // geoip assets then update SharedPreferences with the new version code. This
        // mitigates copying to be done only if a version upgrade is had.
        if (BaseService.buildConfigDebug ||
            BaseService.buildConfigVersionCode > localPrefs.getInt(APP_VERSION_CODE, -1)
        ) {
            if (!geoIpFileCopied)
                copyGeoIpAsset()
            if (!geoIpv6FileCopied)
                copyGeoIpv6Asset()
            localPrefs.edit()
                .putInt(APP_VERSION_CODE, BaseService.buildConfigVersionCode)
                .apply()
        }
    }

    private fun copyGeoIpAsset() =
        synchronized(torConfigFiles.geoIpFileLock) {
            torService.copyAsset(BaseService.geoipAssetPath, torConfigFiles.geoIpFile)
            broadcastLogger?.debug(
                "Asset copied from ${BaseService.geoipAssetPath} -> ${torConfigFiles.geoIpFile}"
            )
        }

    private fun copyGeoIpv6Asset() =
        synchronized(torConfigFiles.geoIpv6FileLock) {
            torService.copyAsset(BaseService.geoip6AssetPath, torConfigFiles.geoIpv6File)
            broadcastLogger?.debug(
                "Asset copied from ${BaseService.geoip6AssetPath} -> ${torConfigFiles.geoIpv6File}"
            )
        }

    @Throws(IOException::class, TimeoutException::class)
    override fun updateTorConfigCustom(content: String?) {

    }

    @Throws(IOException::class)
    override fun openBridgesStream(): InputStream? {
        /*
            BridgesList is an overloaded field, which can cause some confusion.

            The list can be:
              1) a filter like obfs4, meek, or snowflake OR
              2) it can be a custom bridge

            For (1), we just pass back all bridges, the filter will occur
              elsewhere in the library.
            For (2) we return the bridge list as a raw stream.

            If length is greater than 9, then we know this is a custom bridge
        * */
        // TODO: Completely refactor how bridges work.
        val userDefinedBridgeList: String =
            torServicePrefs.getList(PrefKeyList.LIST_OF_SUPPORTED_BRIDGES, arrayListOf()).joinToString()
        var bridgeType = (if (userDefinedBridgeList.length > 9) 1 else 0).toByte()
        // Terrible hack. Must keep in sync with topl::addBridgesFromResources.
        if (bridgeType.toInt() == 0) {
            when (userDefinedBridgeList) {
                SupportedBridges.OBFS4 -> bridgeType = 2
                SupportedBridges.MEEK -> bridgeType = 3
                SupportedBridges.SNOWFLAKE -> bridgeType = 4
            }
        }

        val bridgeTypeStream = ByteArrayInputStream(byteArrayOf(bridgeType))
        val bridgeStream =
            if (bridgeType.toInt() == 1)
                ByteArrayInputStream(userDefinedBridgeList.toByteArray())
            else
                torService.context.resources.openRawResource(R.raw.bridges)
        return SequenceInputStream(bridgeTypeStream, bridgeStream)
    }

}