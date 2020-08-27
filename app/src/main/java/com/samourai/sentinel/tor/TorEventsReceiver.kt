/*
* TorOnionProxyLibrary-Android (a.k.a. topl-android) is a derivation of
* work from the Tor_Onion_Proxy_Library project that started at commit
* hash `74407114cbfa8ea6f2ac51417dda8be98d8aba86`. Contributions made after
* said commit hash are:
*
*     Copyright (C) 2020 Matthew Nelson
*
*     This program is free software: you can redistribute it and/or modify it
*     under the terms of the GNU General Public License as published by the
*     Free Software Foundation, either version 3 of the License, or (at your
*     option) any later version.
*
*     This program is distributed in the hope that it will be useful, but
*     WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
*     or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
*     for more details.
*
*     You should have received a copy of the GNU General Public License
*     along with this program. If not, see <https://www.gnu.org/licenses/>.
*
* `===========================================================================`
* `+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++`
* `===========================================================================`
*
* The following exception is an additional permission under section 7 of the
* GNU General Public License, version 3 (“GPLv3”).
*
*     "The Interfaces" is henceforth defined as Application Programming Interfaces
*     that are publicly available classes/functions/etc (ie: do not contain the
*     visibility modifiers `internal`, `private`, `protected`, or are within
*     classes/functions/etc that contain the aforementioned visibility modifiers)
*     to TorOnionProxyLibrary-Android users that are needed to implement
*     TorOnionProxyLibrary-Android and reside in ONLY the following modules:
*
*      - topl-core-base
*      - topl-service
*
*     The following are excluded from "The Interfaces":
*
*       - All other code
*
*     Linking TorOnionProxyLibrary-Android statically or dynamically with other
*     modules is making a combined work based on TorOnionProxyLibrary-Android.
*     Thus, the terms and conditions of the GNU General Public License cover the
*     whole combination.
*
*     As a special exception, the copyright holder of TorOnionProxyLibrary-Android
*     gives you permission to combine TorOnionProxyLibrary-Android program with free
*     software programs or libraries that are released under the GNU LGPL and with
*     independent modules that communicate with TorOnionProxyLibrary-Android solely
*     through "The Interfaces". You may copy and distribute such a system following
*     the terms of the GNU GPL for TorOnionProxyLibrary-Android and the licenses of
*     the other code concerned, provided that you include the source code of that
*     other code when and as the GNU GPL requires distribution of source code and
*     provided that you do not modify "The Interfaces".
*
*     Note that people who make modified versions of TorOnionProxyLibrary-Android
*     are not obligated to grant this special exception for their modified versions;
*     it is their choice whether to do so. The GNU General Public License gives
*     permission to release a modified version without this exception; this exception
*     also makes it possible to release a modified version which carries forward this
*     exception. If you modify "The Interfaces", this exception does not apply to your
*     modified version of TorOnionProxyLibrary-Android, and you must remove this
*     exception when you distribute your modified version.
* */
package com.samourai.sentinel.tor

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.samourai.sentinel.tor.service.components.onionproxy.TorServiceEventBroadcaster
import com.samourai.sentinel.tor.util.ServiceUtilities
import io.matthewnelson.topl_core.base.BaseConsts

/**
 * @suppress
 * @see [TorServiceEventBroadcaster]
 * @see [io.matthewnelson.topl_core_base.EventBroadcaster]
 * */
class TorEventsReceiver : TorServiceEventBroadcaster() {


    //////////////////////////
    /// ControlPortAddress ///
    //////////////////////////

    // Make Volatile so that if referencing on a different thread, we get
    // the update immediately
    @Volatile
    var controlPortAddress: String? = null
        private set

    override fun broadcastControlPortAddress(controlPortAddress: String?) {
        this.controlPortAddress = controlPortAddress
    }


    ////////////////////////
    /// SocksPortAddress ///
    ////////////////////////

    // Make Volatile so that if referencing on a different thread, we get
    // the update immediately
    @Volatile
    var socksPortAddress: String? = null
        private set

    override fun broadcastSocksPortAddress(socksPortAddress: String?) {
        this.socksPortAddress = socksPortAddress
    }


    ///////////////////////
    /// HttpPortAddress ///
    ///////////////////////

    // Make Volatile so that if referencing on a different thread, we get
    // the update immediately
    @Volatile
    var httpPortAddress: String? = null
        private set

    override fun broadcastHttpPortAddress(httpPortAddress: String?) {
        this.httpPortAddress = httpPortAddress
    }


    /////////////////
    /// Bandwidth ///
    /////////////////
    private var lastDownload = "0"
    private var lastUpload = "0"

    private val _liveBandwidth = MutableLiveData<String>(
            ServiceUtilities.getFormattedBandwidthString(0L, 0L)
    )
    val liveBandwidth: LiveData<String> = _liveBandwidth

    override fun broadcastBandwidth(bytesRead: String, bytesWritten: String) {
        if (bytesRead == lastDownload && bytesWritten == lastUpload) return

        lastDownload = bytesRead
        lastUpload = bytesWritten
        if (!liveBandwidth.hasActiveObservers()) return

        _liveBandwidth.value = ServiceUtilities.getFormattedBandwidthString(
                bytesRead.toLong(), bytesWritten.toLong()
        )
    }

    override fun broadcastDebug(msg: String) {
        broadcastLogMessage(msg)
    }

    override fun broadcastException(msg: String?, e: Exception) {
        if (msg.isNullOrEmpty()) return

        broadcastLogMessage(msg)
        e.printStackTrace()
    }

    private val liveTorLogs: MutableLiveData<String> = MutableLiveData()

    ////////////////////
    /// Log Messages ///
    ////////////////////
    override fun broadcastLogMessage(logMessage: String?) {
        if (logMessage.isNullOrEmpty()) return

        val splitMsg = logMessage.split("|")
        if (splitMsg.size < 3) return

        liveTorLogs.postValue("${splitMsg[0]} | ${splitMsg[1]} | ${splitMsg[2]}")
    }

    override fun broadcastNotice(msg: String) {
        broadcastLogMessage(msg)
    }


    val torLogs: LiveData<String>
        get() = liveTorLogs

    ///////////////////
    /// Tor's State ///
    ///////////////////
    inner class TorStateData(val state: String, val networkState: String)

    private var lastState = BaseConsts.TorState.OFF
    private var lastNetworkState = BaseConsts.TorNetworkState.DISABLED

    private val _liveTorState = MutableLiveData<TorStateData>(
            TorStateData(lastState, lastNetworkState)
    )
    val liveTorState: LiveData<TorStateData> = _liveTorState

    override fun broadcastTorState(@BaseConsts.TorState state: String, @BaseConsts.TorNetworkState networkState: String) {
        if (state == lastState && networkState == lastNetworkState) return

        lastState = state
        lastNetworkState = networkState
        _liveTorState.value = TorStateData(state, networkState)
    }
}