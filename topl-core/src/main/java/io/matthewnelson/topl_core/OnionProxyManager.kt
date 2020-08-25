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
package io.matthewnelson.topl_core

import android.content.Context
import android.content.IntentFilter
import android.net.ConnectivityManager
import io.matthewnelson.topl_core.base.EventBroadcaster
import io.matthewnelson.topl_core.listener.BaseEventListener
import io.matthewnelson.topl_core.receiver.NetworkStateReceiver
import io.matthewnelson.topl_core.settings.TorSettingsBuilder
import io.matthewnelson.topl_core.broadcaster.BroadcastLogger
import io.matthewnelson.topl_core.broadcaster.BroadcastLoggerHelper
import io.matthewnelson.topl_core.broadcaster.TorStateMachine
import io.matthewnelson.topl_core.util.FileUtilities
import io.matthewnelson.topl_core.util.CoreConsts
import io.matthewnelson.topl_core.util.TorInstaller
import io.matthewnelson.topl_core.util.WriteObserver
import io.matthewnelson.topl_core.base.TorConfigFiles
import io.matthewnelson.topl_core.base.TorSettings
import net.freehaven.tor.control.TorControlCommands
import net.freehaven.tor.control.TorControlConnection
import java.io.*
import java.net.Socket
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * This is where all the fun is, this is the class which acts as a gateway into the `topl-core`
 * module, and ensures synchronicity is had.
 *
 * This class is thread safe but that's mostly because we hit everything over the head
 * with 'synchronized'. Given the way this class is used there shouldn't be any performance
 * implications of this.
 *
 * This class began life as TorPlugin from the Briar Project
 *
 * @param [context] Context.
 * @param [torConfigFiles] [TorConfigFiles] For setting up [OnionProxyContext]
 * @param [torInstaller] [TorInstaller] For setting up [OnionProxyContext]
 * @param [torSettings] [TorSettings] For setting up [OnionProxyContext]
 * @param [eventListener] [BaseEventListener] For processing Tor OP messages.
 * @param [eventBroadcaster] Your own broadcaster which extends [EventBroadcaster]
 * @param [buildConfigDebug] Send [BuildConfig.DEBUG] which will show Logcat messages for this
 *   module on Debug builds of your Application. If `null`, all the messages will still be
 *   broadcast to the provided [EventBroadcaster] and you can handle them there how you'd like.
 * @sample [io.matthewnelson.topl_service.service.TorService.initTOPLCore]
 * */
class OnionProxyManager(
        context: Context,
        torConfigFiles: TorConfigFiles,
        torInstaller: TorInstaller,
        torSettings: TorSettings,
        internal val eventListener: BaseEventListener,
        eventBroadcaster: EventBroadcaster,
        buildConfigDebug: Boolean? = null
): CoreConsts() {

    private val appContext = context.applicationContext
    internal val onionProxyContext = OnionProxyContext(torConfigFiles, torInstaller, torSettings)

    // Ensures that these live only in OnionProxyContext, but are accessible from here.
    val torConfigFiles: TorConfigFiles
        get() = onionProxyContext.torConfigFiles
    val torInstaller: TorInstaller
        get() = onionProxyContext.torInstaller
    val torSettings: TorSettings
        get() = onionProxyContext.torSettings

    private val logHelper =
        BroadcastLoggerHelper(this, eventBroadcaster, buildConfigDebug ?: BuildConfig.DEBUG)
    private val broadcastLogger =
        getBroadcastLogger(OnionProxyManager::class.java)
    val torStateMachine =
        TorStateMachine(getBroadcastLogger(TorStateMachine::class.java))

    /**
     * See [BroadcastLoggerHelper.refreshBroadcastLoggersHasDebugLogsVar]
     * */
    fun refreshBroadcastLoggersHasDebugLogsVar() =
        logHelper.refreshBroadcastLoggersHasDebugLogsVar()

    /**
     * See [BroadcastLoggerHelper.getBroadcastLogger]
     *
     * @param [clazz] Class<*> - For initializing [BroadcastLogger.TAG] with your class' name.
     * */
    fun getBroadcastLogger(clazz: Class<*>): BroadcastLogger =
        logHelper.getBroadcastLogger(clazz)

    /**
     * See [BroadcastLoggerHelper.getBroadcastLogger]
     *
     * @param [tagName] String - For initializing [BroadcastLogger.TAG].
     * */
    fun getBroadcastLogger(tagName: String): BroadcastLogger =
        logHelper.getBroadcastLogger(tagName)

    init {
        try {
            onionProxyContext.createDataDir()
        } catch (e: SecurityException) {
            broadcastLogger.exception(e)
        }
    }

    companion object {
        private const val OWNER = "__OwningControllerProcess"
        private const val HOSTNAME_TIMEOUT = 30
        const val NEWNYM_SUCCESS_MESSAGE = "You've changed Tor identities!"
        const val NEWNYM_NO_NETWORK = "No network, cannot change Tor identities"
        const val NEWNYM_RATE_LIMIT_PARTIAL_MSG = "Rate limiting NEWNYM request: "
    }

    /**
     * Sets up and installs any files needed to run tor. If the tor files are already on
     * the system this does not need to be invoked.
     */
    @Throws(IOException::class)
    fun setup() = torInstaller.setup()

    fun getNewSettingsBuilder(): TorSettingsBuilder {
        broadcastLogger.debug("Generating a new SettingsBuilder")
        return TorSettingsBuilder(
            onionProxyContext,
            getBroadcastLogger(TorSettingsBuilder::class.java)
        )
    }

    private fun warnControlConnectionNotResponding(methodCall: String) =
        broadcastLogger.warn("TorControlConnection is not responding properly to $methodCall")

    @Volatile
    private var networkStateReceiver: NetworkStateReceiver? = null
    @Volatile
    private var controlSocket: Socket? = null
    // If controlConnection is not null then this means that a connection exists and the Tor OP
    // will die when the connection fails.
    @Volatile
    private var controlConnection: TorControlConnection? = null
    @Volatile
    private var controlPort = 0

    /**
     * Returns the socks port on the IPv4 localhost address that the Tor OP is listening on
     *
     * @return Discovered socks port
     * @throws [IOException] TorControlConnection or File errors.
     * @throws [RuntimeException] If Tor is not running or there's no localhost binding for Socks.
     * @throws [NullPointerException] If [controlConnection] is null even after checking.
     */
    @get:Throws(IOException::class, RuntimeException::class, NullPointerException::class)
    @get:Synchronized
    val iPv4LocalHostSocksPort: Int
        get() {
            if (!isRunning) throw RuntimeException("Tor is not running!")

            val socksIpPorts = try {
                // This returns a set of space delimited quoted strings which could be Ipv4,
                // Ipv6 or unix sockets.
                controlConnection!!.getInfo("net/listeners/socks").split(" ".toRegex()).toTypedArray()
            } catch (e: KotlinNullPointerException) {
                throw NullPointerException(e.message)
            } catch (ee: IOException) {
                warnControlConnectionNotResponding("getInfo")
                throw IOException(ee)
            }

            for (address in socksIpPorts) {
                if (address.contains("\"127.0.0.1:")) {
                    // Remember, the last character will be a " so we have to remove that.
                    return address.substring(address.lastIndexOf(":") + 1, address.length - 1).toInt()
                }
            }

            throw RuntimeException("We don't have an Ipv4 localhost binding for socks!")
        }

    /**
     * Publishes a hidden service
     *
     * @param [hiddenServicePort] The port that the hidden service will accept connections on
     * @param [localPort] The local port that the hidden service will relay connections to
     * @return The hidden service's onion address in the form X.onion.
     * @throws [IOException] File errors
     * @throws [RuntimeException] See [io.matthewnelson.topl_core.util.WriteObserver.poll]
     * @throws [IllegalStateException] If [controlConnection] is null (service isn't running)
     * @throws [NullPointerException] If [controlConnection] is null even after checking
     * @throws [SecurityException] Unauthorized access to file/directory.
     * @throws [IllegalArgumentException]
     */
    @Synchronized
    @Throws(
        IOException::class,
        RuntimeException::class,
        IllegalStateException::class,
        NullPointerException::class,
        SecurityException::class,
        IllegalArgumentException::class)
    fun publishHiddenService(hiddenServicePort: Int, localPort: Int): String {
        checkNotNull(controlConnection) { "Service is not running." }
        val hostnameFile = torConfigFiles.hostnameFile

        broadcastLogger.notice("Creating hidden service")
        if (!onionProxyContext.createNewFileIfDoesNotExist(ConfigFile.HOSTNAME_FILE))
            throw IOException("Could not create hostnameFile")

        // Watch for the hostname file being created/updated
        val hostNameFileObserver = onionProxyContext.createFileObserver(ConfigFile.HOSTNAME_FILE)
        if (!onionProxyContext.setHostnameDirPermissionsToReadOnly())
            throw RuntimeException("Unable to set permissions on hostName dir")

        // Use the control connection to update the Tor config
        val config = listOf(
            "HiddenServiceDir ${hostnameFile.parentFile?.absolutePath}",
            "HiddenServicePort $hiddenServicePort 127.0.0.1:$localPort"
        )

        try {
            controlConnection!!.setConf(config)
            controlConnection!!.saveConf()
        } catch (e: KotlinNullPointerException) {
            throw NullPointerException(e.message)
        }
        // TODO: catch IOException from not responding to signals.

        // Wait for the hostname file to be created/updated
        if (!hostNameFileObserver.poll(HOSTNAME_TIMEOUT.toLong(), TimeUnit.SECONDS)) {
            hostnameFile.parentFile?.let { FileUtilities.listFilesToLog(it) }
            throw RuntimeException("Wait for hidden service hostname file to be created expired.")
        }

        // Publish the hidden service's onion hostname in transport properties
        val hostname = String(onionProxyContext.readFile(ConfigFile.HOSTNAME_FILE), Charsets.UTF_8)
            .trim { it <= ' ' }
        broadcastLogger.notice("Hidden service configuration completed.")

        return hostname
    }

    /**
     * Kills the Tor OP Process. Once you have called this method nothing is going
     * to work until you either call startWithRepeat or start
     *
     * @throws [NullPointerException] If [controlConnection] magically changes to null.
     * @throws [IOException] If [controlConnection] is not responding to `shutdownTor`.
     */
    @Synchronized
    @Throws(NullPointerException::class, IOException::class)
    fun stop() {
        if (!hasControlConnection) {
            broadcastLogger.notice("Stop command called but no TorControlConnection exists.")

            // Re-sync state if it's out of whack
            torStateMachine.setTorState(TorState.OFF)
            return
        }

        torStateMachine.setTorState(TorState.STOPPING)
        broadcastLogger.debug("Using the Control Port to shutdown Tor")
        try {
            disableNetwork(true)
            controlConnection!!.signal(TorControlCommands.SIGNAL_SHUTDOWN)
        } catch (e: KotlinNullPointerException) {
            torStateMachine.setTorState(TorState.ON)
            throw NullPointerException(e.message)
        } catch (ee: IOException) {
            warnControlConnectionNotResponding("signal")

            try {
                controlConnection!!.shutdownTor(TorControlCommands.SIGNAL_HALT)
            } catch (eee: KotlinNullPointerException) {
                throw NullPointerException(eee.message)
            } catch (eeee: IOException) {
                warnControlConnectionNotResponding("shutdownTor")
                throw IOException(eeee.message)
            } finally {
                torStateMachine.setTorState(TorState.ON)
            }

        } finally {

            try {
                controlConnection!!.removeRawEventListener(eventListener)
            } catch (e: KotlinNullPointerException) {}

            controlConnection = null
            if (controlSocket != null) {
                try {
                    controlSocket!!.close()
                } finally {
                    controlSocket = null
                }
            }

            torStateMachine.setTorState(TorState.OFF)

            if (networkStateReceiver == null) return

            try {
                appContext.unregisterReceiver(networkStateReceiver)
            } catch (e: IllegalArgumentException) {
                // There is a race condition where if someone calls stop before
                // installAndStartTorOp is done then we could get an exception because
                // the network state receiver might not be properly registered.
                broadcastLogger.exception(
                    IllegalArgumentException(
                        "Someone tried calling stop before registering of NetworkStateReceiver", e
                    )
                )
            }
        }
    }

    /**
     * Checks to see if the Tor OP is running (e.g. fully bootstrapped) and open to
     * network connections.
     *
     * @return True if running
     */
    @get:Synchronized
    val isRunning: Boolean
        get() = try {
            isBootstrapped && !isNetworkDisabled
        } catch (e: Exception) {
            false
        }

    private val disableNetworkLock = Object()
    /**
     * Tells the Tor OP if it should accept network connections.
     *
     * Whenever setting Tor's Conf to `DisableNetwork X`, ONLY use this method to do it
     * such that [torStateMachine] will reflect the proper
     * [io.matthewnelson.topl_core_base.BaseConsts.TorNetworkState].
     *
     * @param [disable] If true then the Tor OP will **not** accept SOCKS connections, otherwise yes.
     * @throws [IOException] if having issues with TorControlConnection#setConf
     * @throws [KotlinNullPointerException] if [controlConnection] is null even after checking.
     */
    @Throws(IOException::class, KotlinNullPointerException::class)
    fun disableNetwork(disable: Boolean) {
        synchronized(disableNetworkLock) {
            if (!hasControlConnection) return

            val networkIsSetToDisable = try {
                isNetworkDisabled
            } catch (e: Exception) {
                !disable
            }

            if (networkIsSetToDisable == disable) return

            broadcastLogger.debug("Setting Tor conf DisableNetwork: $disable")

            try {
                controlConnection!!.setConf("DisableNetwork", if (disable) "1" else "0")
                torStateMachine.setTorNetworkState(
                    if (disable) TorNetworkState.DISABLED else TorNetworkState.ENABLED
                )
            } catch (e: IOException) {
                warnControlConnectionNotResponding("setConf")
                throw IOException(e.message)
            } catch (ee: KotlinNullPointerException) {
                throw NullPointerException(ee.message)
            }
        }
    }

    /**
     * Specifies if Tor OP is accepting network connections.
     *
     * @return True = "DisableNetwork 1" (network disabled), false = "DisableNetwork 0" (network enabled)
     * @throws [IOException] if [TorControlConnection] is not responding to getConf.
     * @throws [NullPointerException] if [controlConnection] is null.
     */
    @get:Throws(IOException::class, NullPointerException::class)
    @get:Synchronized
    private val isNetworkDisabled: Boolean
        get() {
            if (!hasControlConnection) return true

            val disableNetworkSettingValues = try {
                controlConnection!!.getConf("DisableNetwork")
            } catch (e: KotlinNullPointerException) {
                throw NullPointerException(e.message)
            } catch (ee: IOException) {
                warnControlConnectionNotResponding("getConf")
                throw IOException(ee.message)
            }

            var result = true

            // It's theoretically possible for us to get multiple values back, if even one is
            // "DisableNetwork 1" then we will assume all are "DisableNetwork 1"
            for (configEntry in disableNetworkSettingValues) {
                result = if (configEntry.value == "1") {
                    return true
                } else {
                    false
                }
            }
            return result
        }

    /**
     * Determines if the boot strap process has completed.
     *
     * @return True if complete
     */
    @get:Synchronized
    private val isBootstrapped: Boolean
        get() {
            if (!hasControlConnection) return false

            try {
                val phase = controlConnection?.getInfo("status/bootstrap-phase")
                if (phase != null && phase.contains("PROGRESS=100")) {
                    broadcastLogger.debug("isBootstrapped: true")
                    return true
                }
            } catch (e: IOException) {
                warnControlConnectionNotResponding("getInfo")
            }
            return false
        }

    /**
     * Starts tor control service if it isn't already running.
     *
     * @throws [IOException] File errors
     * @throws [SecurityException] Unauthorized access to file/directory.
     * @throws [IllegalArgumentException] if [onionProxyContext] methods are passed incorrect
     *   [CoreConsts.ConfigFile] string values
     */
    @Synchronized
    @Throws(IOException::class, SecurityException::class, IllegalArgumentException::class)
    fun start() {
        if (hasControlConnection) {
            broadcastLogger.notice("Start command called but TorControlConnection already exists")

            // Re-sync state if it's out of whack
            torStateMachine.setTorState(TorState.ON)
            return
        }
        refreshBroadcastLoggersHasDebugLogsVar()

        torStateMachine.setTorState(TorState.STARTING)

        var torProcess: Process? = null
        var controlConnection = findExistingTorConnection()
        val hasExistingTorConnection = controlConnection != null

        if (!hasExistingTorConnection) {

            try {
                onionProxyContext.deleteFile(ConfigFile.CONTROL_PORT_FILE)
                onionProxyContext.deleteFile(ConfigFile.COOKIE_AUTH_FILE)
            } catch (e: Exception) {
                torStateMachine.setTorState(TorState.OFF)
                throw IOException(e)
            }

            torProcess = try {
                spawnTorProcess()
            } catch (e: Exception) {
                torStateMachine.setTorState(TorState.OFF)
                throw IOException(e)
            }

            controlConnection = try {
                waitForFileCreation(ConfigFile.CONTROL_PORT_FILE)
                connectToTorControlSocket()
            } catch (e: Exception) {
                torProcess?.destroy()
                torStateMachine.setTorState(TorState.OFF)
                throw IOException(e)
            }
        } else {
            broadcastLogger.debug("Using already existing TorProcess")
        }
        try {
            this.controlConnection = controlConnection

            waitForFileCreation(ConfigFile.COOKIE_AUTH_FILE)
            controlConnection!!.authenticate(onionProxyContext.readFile(ConfigFile.COOKIE_AUTH_FILE))
            broadcastLogger.debug("Authentication to TorControlConnection port successful")
            if (hasExistingTorConnection) {
                controlConnection.signal(TorControlCommands.SIGNAL_RELOAD)
                broadcastLogger.debug("Reloaded configuration file")
            }
            controlConnection.takeOwnership()
            controlConnection.resetConf(setOf(OWNER))
            broadcastLogger.debug("Took ownership of Control Port")

            if (eventListener.CONTROL_COMMAND_EVENTS.isNotEmpty()) {
                broadcastLogger.debug("Adding listener to the TorProcess and setting Events")
                controlConnection.addRawEventListener(eventListener)
                controlConnection.setEvents(listOf(*eventListener.CONTROL_COMMAND_EVENTS))
            }

            disableNetwork(false)
        } catch (e: Exception) {
            torProcess?.destroy()
            this.controlConnection = null
            torStateMachine.setTorState(TorState.OFF)
            broadcastLogger.warn("Failed to start Tor")
            throw IOException(e)
        }

        torStateMachine.setTorState(TorState.ON)

        networkStateReceiver = NetworkStateReceiver(this)

        @Suppress("DEPRECATION")
        val filter = IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
        appContext.registerReceiver(networkStateReceiver, filter)
        broadcastLogger.notice("Completed starting of Tor")
    }

    /**
     * Finds existing tor control connection by trying to connect. Returns null if
     *
     * @throws [SecurityException] Unauthorized access to file/directory.
     */
    @Throws(SecurityException::class)
    private fun findExistingTorConnection(): TorControlConnection? {
        return if (torConfigFiles.controlPortFile.exists())
            try {
                connectToTorControlSocket()
            } catch (e: IOException) {
                null
            }
        else
            null
    }

    /**
     * Looks in the specified `controlPortFile` for the port and attempts to open a
     * control connection.
     *
     * @throws [IOException] File errors
     * @throws [SecurityException] Unauthorized access to file/directory.
     * @throws [NullPointerException] If controlSocket was null even after setting it.
     */
    @Throws(IOException::class, SecurityException::class, NullPointerException::class)
    private fun connectToTorControlSocket(): TorControlConnection {
        val controlConnection: TorControlConnection
        try {
            val controlPortTokens = String(onionProxyContext.readFile(ConfigFile.CONTROL_PORT_FILE))
                .trim { it <= ' ' }.split(":".toRegex()).toTypedArray()
            controlPort = controlPortTokens[1].toInt()
            broadcastLogger.debug("Connecting to Control Port")
            controlSocket = Socket(
                controlPortTokens[0].split("=".toRegex()).toTypedArray()[1],
                controlPort
            )
            controlConnection = TorControlConnection(controlSocket!!)
            broadcastLogger.notice("Successfully connected to Control Port: $controlPort")
        } catch (e: IOException) {
            broadcastLogger.warn("Failed to connect to Control Port.")
            throw IOException(e.message)
        } catch (ee: ArrayIndexOutOfBoundsException) {
            throw IOException(
                "Failed to read control port: " +
                        String(onionProxyContext.readFile(ConfigFile.CONTROL_PORT_FILE))
            )
        } catch (eee: KotlinNullPointerException) {
            throw NullPointerException(eee.message)
        }
//        if (torSettings.hasDebugLogs) {
//            // TODO: think about changing this to something other than System.out. Maybe
//            //  try to pipe it to the BroadcastLogger to keep it out of Logcat on release
//            //  builds?
//            controlConnection.setDebugging(System.out)
//        }
        return controlConnection
    }

    val processId: String
        get() = onionProxyContext.processId

    /**
     * Spawns the tor native process from the existing Java process.
     *
     * @throws [IOException] File errors.
     * @throws [SecurityException] Unauthorized access to file/directory.
     */
    @Throws(SecurityException::class, IOException::class)
    private fun spawnTorProcess(): Process {

        // We want this to throw exceptions if files do not exist so we can propagate
        // the exceptions to where start was called.
        val cmd = arrayOf(
            torExecutable().absolutePath,
            "-f",
            torrc().absolutePath,
            OWNER,
            processId
        )
        val processBuilder = ProcessBuilder(*cmd)
        setEnvironmentArgsAndWorkingDirectoryForStart(processBuilder)
        broadcastLogger.debug("Spawning Tor Process")

        val torProcess: Process = try {
            processBuilder.start()
        } catch (e: Exception) {
            broadcastLogger.warn("ProcessBuilder failed to start.")
            throw IOException(e)
        }

        eatStream(torProcess.errorStream, true)
        if (torSettings.hasDebugLogs)
            eatStream(torProcess.inputStream, false)
        return torProcess
    }

    /**
     * Waits for the controlPort or cookieAuth file to be created by the Tor process depending on
     * which you send it. If there is any problem creating the file OR if the timeout for the file
     * to be created is exceeded, then an IOException is thrown.
     *
     * @throws [IOException] File problems or timeout
     * @throws [SecurityException] Unauthorized access to file/directory.
     * @throws [IllegalArgumentException] Method only accepts
     *   [CoreConsts.ConfigFile.CONTROL_PORT_FILE] or [CoreConsts.ConfigFile.COOKIE_AUTH_FILE]
     * @throws [RuntimeException] See [io.matthewnelson.topl_core.util.WriteObserver.poll]
     */
    @Throws(
        IOException::class,
        SecurityException::class,
        IllegalArgumentException::class,
        RuntimeException::class
    )
    private fun waitForFileCreation(@ConfigFile configFileReference: String) {
        val file = when (configFileReference) {
            ConfigFile.CONTROL_PORT_FILE -> {
                torConfigFiles.controlPortFile
            }
            ConfigFile.COOKIE_AUTH_FILE -> {
                torConfigFiles.cookieAuthFile
            }
            else -> {
                throw IllegalArgumentException("$configFileReference is not a valid argument")
            }
        }

        val startTime = System.currentTimeMillis()
        broadcastLogger.debug("Waiting for $configFileReference")

        val isCreated: Boolean = onionProxyContext.createNewFileIfDoesNotExist(configFileReference)
        val fileObserver: WriteObserver? = onionProxyContext.createFileObserver(configFileReference)
        val fileCreationTimeout = torConfigFiles.fileCreationTimeout
        if (!isCreated || file.length() == 0L &&
            fileObserver?.poll(fileCreationTimeout.toLong(), TimeUnit.SECONDS) != true
        ) {
            torStateMachine.setTorState(TorState.STOPPING)
            throw IOException(
                "$configFileReference not created: ${file.absolutePath}, len = ${file.length()}"
            )
        }
        broadcastLogger.debug(
            "Created $configFileReference: time = ${System.currentTimeMillis()-startTime}ms"
        )
    }

    // TODO: Re-work using coroutines
    private fun eatStream(inputStream: InputStream, isError: Boolean) {
        object : Thread() {
            override fun run() {
                val scanner = Scanner(inputStream)
                try {
                    while (scanner.hasNextLine()) {
                        val line = scanner.nextLine()
                        if (isError) {
                            broadcastLogger.exception(IOException("Error with $line"))
                        } else {
                            broadcastLogger.notice(line)
                        }
                    }
                } finally {
                    try {
                        inputStream.close()
                    } catch (e: IOException) {
                        broadcastLogger.exception(IOException("Could not close input stream", e))
                    }
                }
            }
        }.start()
    }

    @Throws(IOException::class, SecurityException::class)
    private fun torExecutable(): File {
        var torExe = torConfigFiles.torExecutableFile
        //Try removing platform specific extension
        if (!torExe.exists())
            // Named to match GuardianProject's binaries, just in case someone
            //  forgets to create/set a custom TorConfigFile if using their dependency.
            torExe = File(torExe.parent, "libtor.so")

        if (!torExe.exists()) {
            torStateMachine.setTorState(TorState.STOPPING)
            throw IOException("Tor executable file not found")
        }
        return torExe
    }

    @Throws(IOException::class, SecurityException::class)
    private fun torrc(): File {
        val torrc = torConfigFiles.torrcFile
        if (!torrc.exists()) {
            torStateMachine.setTorState(TorState.STOPPING)
            throw IOException("Torrc file not found")
        }
        return torrc
    }

    /**
     * Sets environment variables and working directory needed for Tor
     *
     * @param [processBuilder] we will call start on this to run Tor.
     * @throws [SecurityException] Unauthorized access to the ProcessBuilder's environment.
     */
    @Throws(SecurityException::class)
    private fun setEnvironmentArgsAndWorkingDirectoryForStart(processBuilder: ProcessBuilder) {
        processBuilder.directory(torConfigFiles.configDir)
        val environment = processBuilder.environment()
        environment["HOME"] = torConfigFiles.configDir.absolutePath
    }

    private val environmentArgsForExec: Array<String>
        get() {
            val envArgs: MutableList<String> = ArrayList()
            envArgs.add("HOME=" + torConfigFiles.configDir.absolutePath)
            return envArgs.toTypedArray()
        }

    val isIPv4LocalHostSocksPortOpen: Boolean
        get() = try {
            iPv4LocalHostSocksPort
            true
        } catch (e: Exception) {
            false
        }

    /**
     * Sets the exit nodes through the tor control connection
     *
     * @param [exitNodes]
     *
     * @return true if successfully set, otherwise false
     */
    fun setExitNode(exitNodes: String?): Boolean {
        //Based on config params from Orbot project
        if (!hasControlConnection) return false

        if (exitNodes.isNullOrEmpty()) {
            try {
                val resetBuffer = ArrayList<String>()
                resetBuffer.add("ExitNodes")
                resetBuffer.add("StrictNodes")
                controlConnection!!.resetConf(resetBuffer)
                disableNetwork(true)
                disableNetwork(false)
            } catch (e: Exception) {
                broadcastLogger.exception(
                    IOException("Connection exception occurred while resetting exits", e)
                )
                return false
            }
        } else {
            try {
                controlConnection!!.setConf("GeoIPFile", torConfigFiles.geoIpFile.canonicalPath)
                controlConnection!!.setConf("GeoIPv6File", torConfigFiles.geoIpv6File.canonicalPath)
                controlConnection!!.setConf("ExitNodes", exitNodes)
                controlConnection!!.setConf("StrictNodes", "1")
                disableNetwork(true)
                disableNetwork(false)
            } catch (e: Exception) {
                broadcastLogger.exception(
                    IOException("Connection exception occurred while resetting exits", e)
                )
                return false
            }
        }
        return true
    }

    /**
     * Will signal for a NewNym, then broadcast [NEWNYM_SUCCESS_MESSAGE] if successful.
     *
     * Because there is no way to easily ascertain success, we need to check
     * see if we've been rate limited. Being rate limited means we were **not** successful
     * when signaling NEWNYM, so we don't want to broadcast the success message.
     *
     * See [BaseEventListener] for more information on how this is done via calling the
     * [BaseEventListener.beginWatchingNoticeMsgs] & [BaseEventListener.doesNoticeMsgBufferContain]
     * methods.
     *
     * If the [eventListener] you're instantiating [OnionProxyManager] with has it's
     * [BaseEventListener.noticeMsg] being piped to the [EventBroadcaster.broadcastNotice],
     * you will receive the message of being rate limited.
     * */
    @Synchronized
    suspend fun signalNewNym() {
        if (!hasControlConnection || !isBootstrapped) return
        if (networkStateReceiver?.networkConnectivity != true) {
            broadcastLogger.notice("NEWNYM: $NEWNYM_NO_NETWORK")
            return
        }

        broadcastLogger.debug("Attempting to acquire a new nym")
        eventListener.beginWatchingNoticeMsgs()

        val signalSuccess = signalControlConnection(TorControlCommands.SIGNAL_NEWNYM)
        val rateLimited = eventListener.doesNoticeMsgBufferContain(NEWNYM_RATE_LIMIT_PARTIAL_MSG, 50L)

        if (signalSuccess) {
            if (!rateLimited) {
                broadcastLogger.notice(
                    "${TorControlCommands.SIGNAL_NEWNYM}: $NEWNYM_SUCCESS_MESSAGE"
                )
            }
        } else {
            broadcastLogger.notice("Failed to acquire a ${TorControlCommands.SIGNAL_NEWNYM}")
        }
    }

    /**
     * Sends a signal to the  [TorControlConnection]
     *
     * @param [torControlSignalCommand] See [TorControlCommands] for acceptable `SIGNAL_` values.
     * @return `true` if the signal was received by [TorControlConnection], `false` if not.
     * */
    fun signalControlConnection(torControlSignalCommand: String): Boolean {
        return if (!hasControlConnection) {
            false
        } else {
            return try {
                controlConnection!!.signal(torControlSignalCommand)
                true
            } catch (e: IOException) {
                warnControlConnectionNotResponding("signal")
                false
            } catch (ee: KotlinNullPointerException) {
                false
            }
        }
    }

    val hasControlConnection: Boolean
        get() = controlConnection != null

    val torPid: Int
        get() {
            val pidS = getInfo("process/pid")
            return if (pidS.isNullOrEmpty()) -1 else Integer.valueOf(pidS)
        }

    /**
     * See the torspec for accepted queries:
     *  - https://torproject.gitlab.io/torspec/control-spec/#getinfo
     *
     * @param [queryCommand] What data you are querying the [TorControlConnection] for
     * */
    fun getInfo(queryCommand: String): String? {
        return if (!hasControlConnection) {
            null
        } else try {
            controlConnection!!.getInfo(queryCommand)
        } catch (e: IOException) {
            warnControlConnectionNotResponding("getInfo")
            null
        } catch (ee: KotlinNullPointerException) {
            null
        }
    }

    fun reloadTorConfig(): Boolean {
        if (!hasControlConnection) return false

        try {
            controlConnection!!.signal(TorControlCommands.SIGNAL_RELOAD)
            return true
        } catch (e: IOException) {
            warnControlConnectionNotResponding("signal")
        } catch (ee: KotlinNullPointerException) {}

        try {
            restartTorProcess()
            return true
        } catch (e: Exception) {
            broadcastLogger.exception(e)
        }

        return false
    }

    @Throws(Exception::class)
    fun restartTorProcess() = killTorProcess(-1)

    @Throws(Exception::class)
    fun killTorProcess() = killTorProcess(-9)

    @Throws(Exception::class)
    private fun killTorProcess(signal: Int) {
        //Based on logic from Orbot project
        val torFileName = torConfigFiles.torExecutableFile.name
        var procId: Int
        var killAttempts = 0
        while (torPid.also { procId = it } != -1) {

            val pidString = procId.toString()
            execIgnoreException(String.format("busybox killall %d %s", signal, torFileName))
            execIgnoreException(String.format("toolbox kill %d %s", signal, pidString))
            execIgnoreException(String.format("busybox kill %d %s", signal, pidString))
            execIgnoreException(String.format("kill %d %s", signal, pidString))

            try {
                Thread.sleep(1000)
            } catch (e: InterruptedException) {}

            killAttempts++

            if (killAttempts > 4)
                throw Exception("Cannot kill: ${torConfigFiles.torExecutableFile.absolutePath}")
        }
    }

    private fun execIgnoreException(command: String): Process? =
        try {
            Runtime.getRuntime().exec(command)
        } catch (e: Exception) {
            null
        }

}