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
package com.samourai.sentinel.tor.service.components.onionproxy

import io.matthewnelson.topl_core.listener.BaseEventListener
import net.freehaven.tor.control.TorControlCommands


internal class ServiceEventListener: BaseEventListener() {

    // broadcastLogger is available from BaseEventListener and is instantiated as soon as
    // OnionProxyManager gets initialized.
//    var broadcastLogger: BroadcastLogger? = null

    override val CONTROL_COMMAND_EVENTS: Array<String>
        get() = arrayOf(
                TorControlCommands.EVENT_CIRCUIT_STATUS,
                TorControlCommands.EVENT_CIRCUIT_STATUS_MINOR,
                TorControlCommands.EVENT_STREAM_STATUS,
                TorControlCommands.EVENT_OR_CONN_STATUS,
                TorControlCommands.EVENT_BANDWIDTH_USED,
                TorControlCommands.EVENT_NOTICE_MSG,
                TorControlCommands.EVENT_WARN_MSG,
                TorControlCommands.EVENT_ERR_MSG,
                TorControlCommands.EVENT_NEW_DESC,
                TorControlCommands.EVENT_STATUS_GENERAL,
                TorControlCommands.EVENT_STATUS_CLIENT,
                TorControlCommands.EVENT_TRANSPORT_LAUNCHED
        )

    private fun debug(data: String) {
        broadcastLogger?.debug(data)
    }

    override fun noticeMsg(data: String?) {
        if (!data.isNullOrEmpty()) {
            broadcastLogger?.notice(data)
        }
        super.noticeMsg(data)
    }

    override fun unrecognized(data: String?) {
        if (data.isNullOrEmpty()) return
        debug("UNRECOGNIZED $data")
    }

    override fun newConsensus(data: String?) {
        if (data.isNullOrEmpty()) return
        debug("${TorControlCommands.EVENT_NEWCONSENSUS} $data")
    }

    override fun connBw(data: String?) {
        if (data.isNullOrEmpty()) return
        debug("${TorControlCommands.EVENT_CONN_BW} $data")
    }

    override fun circBandwidthUsed(data: String?) {
        if (data.isNullOrEmpty()) return
        debug("${TorControlCommands.EVENT_CIRC_BANDWIDTH_USED} $data")
    }

    override fun networkLiveness(data: String?) {
        if (data.isNullOrEmpty()) return
        debug("${TorControlCommands.EVENT_NETWORK_LIVENESS} $data")
    }

    override fun onEvent(keyword: String?, data: String?) {
        super.onEvent(keyword, data)
    }

    override fun newDesc(data: String?) {
        if (data.isNullOrEmpty()) return
        debug("${TorControlCommands.EVENT_NEW_DESC} $data")
    }

    override fun ns(data: String?) {
        if (data.isNullOrEmpty()) return
        debug("${TorControlCommands.EVENT_NS} $data")
    }

    override fun guard(data: String?) {
        if (data.isNullOrEmpty()) return
        debug("${TorControlCommands.EVENT_GUARD} $data")
    }

    override fun clientsSeen(data: String?) {
        if (data.isNullOrEmpty()) return
        debug("${TorControlCommands.EVENT_CLIENTS_SEEN} $data")
    }

    override fun gotSignal(data: String?) {
        if (data.isNullOrEmpty()) return
        debug("${TorControlCommands.EVENT_GOT_SIGNAL} $data")
    }

    override fun hsDescContent(data: String?) {
        if (data.isNullOrEmpty()) return
        debug("${TorControlCommands.EVENT_HS_DESC_CONTENT} $data")
    }

    override fun transportLaunched(data: String?) {
        if (data.isNullOrEmpty()) return
        debug("${TorControlCommands.EVENT_TRANSPORT_LAUNCHED} $data")
    }

    // https://torproject.gitlab.io/torspec/control-spec/#bandwidth-used-in-the-last-second
    override fun bandwidthUsed(data: String?) {
        if (data.isNullOrEmpty()) return

        val dataList = data.split(" ")
        if (dataList.size != 2) return

        broadcastLogger?.eventBroadcaster?.broadcastBandwidth(dataList[0], dataList[1])

    }

    override fun addrMap(data: String?) {
        if (data.isNullOrEmpty()) return
        debug("${TorControlCommands.EVENT_ADDRMAP} $data")
    }

    override fun warnMsg(data: String?) {
        if (data.isNullOrEmpty()) return
        broadcastLogger?.warn(data)
    }

    override fun statusGeneral(data: String?) {
        if (data.isNullOrEmpty()) return
        debug("${TorControlCommands.EVENT_STATUS_GENERAL} $data")
    }

    override fun circuitStatusMinor(data: String?) {
        if (data.isNullOrEmpty()) return
        debug("${TorControlCommands.EVENT_CIRCUIT_STATUS_MINOR} $data")
    }

    override fun errMsg(data: String?) {
        if (data.isNullOrEmpty()) return
        broadcastLogger?.error(data)
    }

    override fun streamStatus(data: String?) {
        if (data.isNullOrEmpty()) return
        debug("${TorControlCommands.EVENT_STREAM_STATUS} $data")
    }

    override fun descChanged(data: String?) {
        if (data.isNullOrEmpty()) return
        debug("${TorControlCommands.EVENT_DESCCHANGED} $data")
    }

    override fun orConnStatus(data: String?) {
        if (data.isNullOrEmpty()) return
        debug("${TorControlCommands.EVENT_OR_CONN_STATUS} $data")
    }

    override fun infoMsg(data: String?) {
        if (data.isNullOrEmpty()) return
        debug("${TorControlCommands.EVENT_INFO_MSG} $data")
    }

    override fun hsDesc(data: String?) {
        if (data.isNullOrEmpty()) return
        debug("${TorControlCommands.EVENT_HS_DESC} $data")
    }

    override fun statusClient(data: String?) {
        if (data.isNullOrEmpty()) return
        debug("${TorControlCommands.EVENT_STATUS_CLIENT} $data")
    }

    override fun debugMsg(data: String?) {
        if (data.isNullOrEmpty()) return
        debug("${TorControlCommands.EVENT_DEBUG_MSG} $data")
    }

    override fun streamBandwidthUsed(data: String?) {
        if (data.isNullOrEmpty()) return
        debug("${TorControlCommands.EVENT_STREAM_BANDWIDTH_USED} $data")
    }

    override fun confChanged(data: String?) {
        if (data.isNullOrEmpty()) return
        debug("${TorControlCommands.EVENT_CONF_CHANGED} $data")
    }

    override fun cellStats(data: String?) {
        if (data.isNullOrEmpty()) return
        debug("${TorControlCommands.EVENT_CELL_STATS} $data")
    }

    override fun circuitStatus(data: String?) {
        if (data.isNullOrEmpty()) return
        debug("${TorControlCommands.EVENT_CIRCUIT_STATUS} $data")
    }

    override fun buildTimeoutSet(data: String?) {
        if (data.isNullOrEmpty()) return
        debug("${TorControlCommands.EVENT_BUILDTIMEOUT_SET} $data")
    }

    override fun statusServer(data: String?) {
        if (data.isNullOrEmpty()) return
        debug("${TorControlCommands.EVENT_STATUS_SERVER} $data")
    }
}