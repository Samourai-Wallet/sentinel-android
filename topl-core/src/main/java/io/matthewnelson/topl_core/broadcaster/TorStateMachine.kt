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
*
* `===========================================================================`
* `+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++`
* `===========================================================================`
*
* The original code, prior to commit hash 74407114cbfa8ea6f2ac51417dda8be98d8aba86,
* was:
*
*     Copyright (c) Microsoft Open Technologies, Inc.
*     All Rights Reserved
*
*     Licensed under the Apache License, Version 2.0 (the "License");
*     you may not use this file except in compliance with the License.
*     You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
*
*
*     THIS CODE IS PROVIDED ON AN *AS IS* BASIS, WITHOUT WARRANTIES OR
*     CONDITIONS OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING
*     WITHOUT LIMITATION ANY IMPLIED WARRANTIES OR CONDITIONS OF TITLE,
*     FITNESS FOR A PARTICULAR PURPOSE, MERCHANTABLITY OR NON-INFRINGEMENT.
*
*     See the Apache 2 License for the specific language governing permissions and
*     limitations under the License.
* */
package io.matthewnelson.topl_core.broadcaster

import io.matthewnelson.topl_core.util.CoreConsts

/**
 * Current State of Tor
 */
class TorStateMachine(private val broadcastLogger: BroadcastLogger): CoreConsts() {

    private var currentTorState: @TorState String = TorState.OFF
    private var currentTorNetworkState: @TorNetworkState String = TorNetworkState.DISABLED

    val isOff: Boolean
        get() = TorState.OFF == currentTorState

    val isOn: Boolean
        get() = TorState.ON == currentTorState

    val isStarting: Boolean
        get() = TorState.STARTING == currentTorState

    val isStopping: Boolean
        get() = TorState.STOPPING == currentTorState

    val isNetworkDisabled: Boolean
        get() = TorNetworkState.DISABLED == currentTorNetworkState

    /**
     * Will set the state to that which is specified if it isn't already.
     *
     * @param [state] [io.matthewnelson.topl_core_base.BaseConsts.TorState]
     * @return Previous [io.matthewnelson.topl_core_base.BaseConsts.TorState]
     * */
    @Synchronized
    internal fun setTorState(@TorState state: String): @TorState String {
        val currentState = currentTorState
        if (currentTorState != state) {
            currentTorState = state
            broadcastLogger.torState(currentTorState, currentTorNetworkState)
            broadcastLogger.debug("$currentTorState & $currentTorNetworkState")
        } else {
            broadcastLogger.debug("TorState was already set to $currentState")
        }
        return currentState
    }

    /**
     * Will set the network state to that which is specified if it isn't already.
     *
     * @param [networkState] [io.matthewnelson.topl_core_base.BaseConsts.TorNetworkState]
     * @return Previous [io.matthewnelson.topl_core_base.BaseConsts.TorNetworkState]
     * */
    @Synchronized
    internal fun setTorNetworkState(@TorNetworkState networkState: String): @TorNetworkState String {
        val currentNetworkState = currentTorNetworkState
        if (currentTorNetworkState != networkState) {
            currentTorNetworkState = networkState
            broadcastLogger.torState(currentTorState, currentTorNetworkState)
            broadcastLogger.debug("$currentTorState & $currentTorNetworkState")
        } else {
            broadcastLogger.debug("TorNetworkState was already set to $currentNetworkState")
        }
        return currentNetworkState
    }
}