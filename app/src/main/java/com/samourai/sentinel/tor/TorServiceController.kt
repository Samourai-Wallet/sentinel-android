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

import android.app.Application
import android.content.Context
import com.samourai.sentinel.tor.notification.ServiceNotification
import com.samourai.sentinel.tor.service.TorService
import com.samourai.sentinel.tor.service.BaseService
import com.samourai.sentinel.tor.lifecycle.BackgroundManager
import com.samourai.sentinel.tor.service.components.binding.BaseServiceConnection
import com.samourai.sentinel.tor.service.components.actions.ServiceActionProcessor
import com.samourai.sentinel.tor.service.components.actions.ServiceActions
import com.samourai.sentinel.tor.service.components.binding.TorServiceConnection
import com.samourai.sentinel.tor.service.components.onionproxy.TorServiceEventBroadcaster
import com.samourai.sentinel.tor.util.ServiceConsts
import io.matthewnelson.topl_core.base.TorConfigFiles
import io.matthewnelson.topl_core.base.TorSettings

class TorServiceController private constructor(): ServiceConsts() {

    class Builder(
            private val application: Application,
            private val torServiceNotificationBuilder: ServiceNotification.Builder,
            private val backgroundManagerPolicy: BackgroundManager.Builder.Policy,
            private val buildConfigVersionCode: Int,
            private val torSettings: TorSettings,
            private val geoipAssetPath: String,
            private val geoip6AssetPath: String
    ) {

        private var appEventBroadcaster: TorServiceEventBroadcaster? = Companion.appEventBroadcaster
//        private var heartbeatTime = BackgroundManager.heartbeatTime
        private var restartTorDelayTime = ServiceActionProcessor.restartTorDelayTime
        private var stopServiceDelayTime = ServiceActionProcessor.stopServiceDelayTime
        private var torConfigFiles: TorConfigFiles? = null

        // On published releases of this Library, this value will **always** be `false`.
        private var buildConfigDebug = BaseService.buildConfigDebug

        /**
         * Default is set to 500ms, (what this method adds time to).
         *
         * A slight delay is required when starting and stopping Tor to allow the [Process]
         * for which it is running in to settle. This method adds time to the cautionary
         * delay between execution of stopTor and startTor, which are the individual calls
         * executed when using the [restartTor] method.
         *
         * The call to [restartTor] executes individual commands to:
         *
         *   - stop tor + delay (300ms)
         *   - delay (500ms) <---------------------- what this method will add to
         *   - start tor + delay (300ms)
         *
         * @param [milliseconds] A value greater than 0
         * @see [io.matthewnelson.topl_service.service.components.actions.ServiceActions.RestartTor]
         * @see [io.matthewnelson.topl_service.service.components.actions.ServiceActionProcessor.processServiceAction]
         * */
        fun addTimeToRestartTorDelay(milliseconds: Long): Builder {
            if (milliseconds > 0L)
                this.restartTorDelayTime += milliseconds
            return this
        }

        /**
         * Default is set to 100ms (what this method adds time to).
         *
         * A slight delay is required when starting and stopping Tor to allow the [Process]
         * for which it is running in to settle. This method adds time to the cautionary
         * delay between execution of stopping Tor and stopping [TorService].
         *
         * The call to [stopTor] executes individual commands to:
         *
         *   - stop tor + delay (300ms)
         *   - delay (100ms) <---------------------- what this method will add to
         *   - stop service
         *
         * @param [milliseconds] A value greater than 0
         * @see [io.matthewnelson.topl_service.service.components.actions.ServiceActions.Stop]
         * @see [io.matthewnelson.topl_service.service.components.actions.ServiceActionProcessor.processServiceAction]
         * */
        fun addTimeToStopServiceDelay(milliseconds: Long): Builder {
            if (milliseconds > 0L)
                this.stopServiceDelayTime += milliseconds
            return this
        }

//        /**
//         * Default is set to 30_000ms
//         *
//         * When the user sends your application to the background (recent app's tray),
//         * [com.samourai.sentinel.tor.lifecycle.BackgroundManager] begins
//         * a heartbeat for Tor, as well as cycling [TorService] between foreground and
//         * background as to keep the OS from killing things due to being idle for too long.
//         *
//         * If the user returns the application to the foreground, the heartbeat and
//         * foreground/background cycling stops.
//         *
//         * This method sets the time between each heartbeat.
//         *
//         * @param [milliseconds] A Long between 15_000 and 45_000. Will fallback to default
//         *   value if not between that range
//         * */
//        fun setBackgroundHeartbeatTime(milliseconds: Long): Builder {
//            if (milliseconds in 15_000L..45_000L)
//                heartbeatTime = milliseconds
//            return this
//        }

        /**
         * This makes it such that on your Application's **Debug** builds, the `topl-core` and
         * `topl-service` modules will provide you with Logcat messages (when
         * [TorSettings.hasDebugLogs] is enabled).
         *
         * For your **Release** builds no Logcat messaging will be provided, but you
         * will still get the same messages sent to your [EventBroadcaster] if you set it
         * via [Builder.setEventBroadcaster].
         *
         * @param [buildConfigDebug] Send [BuildConfig.DEBUG]
         * @see [io.matthewnelson.topl_core.broadcaster.BroadcastLogger]
         *
         * TODO: Provide a link to gh-pages that discusses logging and how it work, it's pretty
         *  complex with everything that is going on.
         * */
        fun setBuildConfigDebug(buildConfigDebug: Boolean): Builder {
            this.buildConfigDebug = buildConfigDebug
            return this
        }

        /**
         * Get broadcasts piped to your Application to do with them what you desire. What
         * you send this will live at [Companion.appEventBroadcaster] for the remainder of
         * your application's lifecycle to refer to elsewhere in your App.
         *
         * NOTE: You will, ofc, have to cast [Companion.appEventBroadcaster] as whatever your
         * class actually is.
         * */
        fun setEventBroadcaster(eventBroadcaster: TorServiceEventBroadcaster): Builder {
            this.appEventBroadcaster = eventBroadcaster
            return this
        }

        /**
         * If you wish to customize the file structure of how Tor is installed in your app,
         * you can do so by instantiating your own [TorConfigFiles] and customizing it via
         * the [TorConfigFiles.Builder], or overridden method [TorConfigFiles.createConfig].
         *
         * By default, [TorService] will call [TorConfigFiles.createConfig] using your
         * [Context.getApplicationContext] to set up a standard directory hierarchy for Tor
         * to operate with.
         *
         * @return [Builder]
         * @sample [io.matthewnelson.sampleapp.App.customTorConfigFilesSetup]
         * @see [Builder.build]
         * */
        fun useCustomTorConfigFiles(torConfigFiles: TorConfigFiles): Builder {
            this.torConfigFiles = torConfigFiles
            return this
        }

        /**
         * Initializes [TorService] setup and enables the ability to call methods from the
         * [Companion] object w/o throwing exceptions.
         *
         * See [Builder] for code samples.
         * */
        fun build() {
            appEventBroadcaster
            // If `BaseService.application` has been initialized
            // already, return as to not overwrite things.
            try {
                BaseService.getAppContext()
                return
            } catch (e: RuntimeException) {}

            BaseService.initialize(
                application,
                buildConfigVersionCode,
                buildConfigDebug,
                geoipAssetPath,
                geoip6AssetPath,
                torConfigFiles ?: TorConfigFiles.createConfig(application.applicationContext),
                torSettings
            )

//            BackgroundManager.initialize(heartbeatTime)
            ServiceActionProcessor.initialize(restartTorDelayTime, stopServiceDelayTime)

            Companion.appEventBroadcaster = this.appEventBroadcaster

            torServiceNotificationBuilder.build(application.applicationContext)

            backgroundManagerPolicy.build(
                TorService::class.java,
                TorServiceConnection.torServiceConnection
            )
        }
    }

    /**
     * Where everything needed to interact with [TorService] resides.
     * */
    companion object {

        var appEventBroadcaster: TorServiceEventBroadcaster? = null
            private set

        /**
         * Get the [TorConfigFiles] that have been set after calling [Builder.build]
         *
         * @return Instance of [TorConfigFiles] that are being used throughout TOPL-Android
         * @throws [RuntimeException] if called before [Builder.build]
         * */
        @Throws(RuntimeException::class)
        fun getTorConfigFiles(): TorConfigFiles {
            BaseService.getAppContext()
            return BaseService.torConfigFiles
        }

        /**
         * Get the [TorSettings] that have been set after calling [Builder.build]. These are
         * the [TorSettings] you initialized [TorServiceController.Builder] with.
         *
         * @return Instance of [TorSettings] that are being used throughout TOPL-Android
         * @throws [RuntimeException] if called before [Builder.build]
         * */
        @Throws(RuntimeException::class)
        fun getTorSettings(): TorSettings {
            BaseService.getAppContext()
            return BaseService.torSettings
        }

        /**
         * Starts [TorService] and then Tor. You can call this as much as you want. If
         * the Tor [Process] is already running, it will do nothing.
         *
         * @throws [RuntimeException] if called before [Builder.build]
         * */
        @Throws(RuntimeException::class)
        fun startTor() =
            BaseService.startService(
                BaseService.getAppContext(),
                TorService::class.java,
                TorServiceConnection.torServiceConnection
            )

        /**
         * Stops [TorService].
         * */
        fun stopTor() =
            BaseServiceConnection.serviceBinder?.submitServiceAction(ServiceActions.Stop())

        /**
         * Restarts Tor.
         * */
        fun restartTor() =
            BaseServiceConnection.serviceBinder?.submitServiceAction(ServiceActions.RestartTor())

        /**
         * Changes identities.
         * */
        fun newIdentity() =
            BaseServiceConnection.serviceBinder?.submitServiceAction(ServiceActions.NewId())
    }
}