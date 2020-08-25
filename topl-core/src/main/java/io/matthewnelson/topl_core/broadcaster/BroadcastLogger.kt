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
package io.matthewnelson.topl_core.broadcaster

import android.util.Log
import io.matthewnelson.topl_core.util.CoreConsts
import io.matthewnelson.topl_core.base.EventBroadcaster
import io.matthewnelson.topl_core.base.TorSettings

/**
 * This class is for standardizing broadcast messages across all classes for this Library.
 * Debugging is important while hacking on TOPL-Android, but those Logcat messages
 * should **never** make it to a release build.
 *
 * To enable Logcat messages, [buildConfigDebug] must be `true` (w/e you have sent
 * [io.matthewnelson.topl_core.OnionProxyManager] upon instantiation), and
 * [TorSettings.hasDebugLogs] must also be `true`. This way if your implementation of the
 * Library is causing problems in your App you need only set [TorSettings.hasDebugLogs] to
 * `true` for a Debug build of your App.
 *
 * See helper method [io.matthewnelson.topl_core.OnionProxyManager.getBroadcastLogger] to
 * instantiate.
 *
 * @param [TAG] Typically, the class name, but able to be set to whatever you wish
 * @param [eventBroadcaster] For broadcasting the info
 * @param [buildConfigDebug] To enable/disable Logcat messages
 * @param [hasDebugLogs] To switch debug logs on/off, as well as Logcat messages on Debug builds.
 * */
class BroadcastLogger internal constructor(
        val TAG: String,
        val eventBroadcaster: EventBroadcaster,
        private val buildConfigDebug: Boolean,
        hasDebugLogs: Boolean
): CoreConsts() {

    @Volatile
    internal var hasDebugLogs: Boolean = hasDebugLogs
        private set

    /**
     * See [io.matthewnelson.topl_core.broadcaster.BroadcastLoggerHelper.refreshBroadcastLoggersHasDebugLogsVar]
     * */
    internal fun updateHasDebugLogs(hasDebugLogs: Boolean) {
        this.hasDebugLogs = hasDebugLogs
    }

    private fun toLogcat(): Boolean =
        hasDebugLogs && buildConfigDebug

    /**
     * Will only broadcast if [hasDebugLogs] is on.
     * */
    fun debug(msg: String) {
        if (!hasDebugLogs) return
        eventBroadcaster.broadcastDebug("${BroadcastType.DEBUG}|$TAG|$msg")
        if (!buildConfigDebug) return
        Log.d(TAG, msg)
    }

    fun exception(e: Exception) {
        eventBroadcaster.broadcastException("${BroadcastType.EXCEPTION}|$TAG|${e.message}", e)
        if (!toLogcat()) return
        Log.e(TAG, e.message, e)

    }

    fun notice(msg: String) {
        eventBroadcaster.broadcastNotice("${BroadcastType.NOTICE}|$TAG|$msg")
        if (!toLogcat()) return
        Log.i(TAG, msg)
    }

    fun warn(msg: String) {
        eventBroadcaster.broadcastNotice("${BroadcastType.WARN}|$TAG|$msg")
        if (!toLogcat()) return
        Log.w(TAG, msg)
    }

    fun error(msg: String) {
        eventBroadcaster.broadcastNotice("${BroadcastType.ERROR}|$TAG|$msg")
        if (!toLogcat()) return
        Log.e(TAG, msg)
    }

    fun torState(@TorState state: String, @TorNetworkState networkState: String) {
        eventBroadcaster.broadcastTorState(state, networkState)
    }

}