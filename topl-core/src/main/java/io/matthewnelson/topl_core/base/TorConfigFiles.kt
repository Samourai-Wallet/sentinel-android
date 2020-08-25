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
package io.matthewnelson.topl_core.base

import android.content.Context
import java.io.File
import java.io.IOException

/**
 * Holds Tor configuration information for files and directories that Tor will use.
 *
 * See [Companion.createConfig] or [Builder] to instantiate.
 *
 * When modifying/querying Files, ensure you are using `synchronized` and acquiring
 * the appropriate `FileLock` object pertaining to that File. This inhibits errors
 * across the library.
 *
 * See extension function [readTorConfigFile].
 * */
class TorConfigFiles private constructor(
    val geoIpFile: File,
    val geoIpv6File: File,
    torrcFile: File,
    val torExecutableFile: File,
    val hiddenServiceDir: File,
    val dataDir: File,
    val configDir: File,

    /**
     * The <base32-encoded-fingerprint>.onion domain name for this hidden service.
     * If the hidden service is restricted to authorized clients only, this file
     * also contains authorization data for all clients.
     *
     * @return [hostnameFile] </base32-encoded-fingerprint>
     * */
    val hostnameFile: File,

    /**
     * Used for cookie authentication with the controller. Location can be
     * overridden by the CookieAuthFile config option. Regenerated on startup.
     * See control-spec.txt in torspec for details.
     *
     * Only used when cookie authentication is enabled.
     *
     * @return [cookieAuthFile]
     * */
    val cookieAuthFile: File,
    val libraryPath: File?,
    val resolveConf: File,
    val controlPortFile: File,
    val installDir: File,

    /**
     * When tor starts it waits for the control port and cookie auth files to be created
     * before it proceeds to the next step in startup. If these files are not created
     * after a certain amount of time, then the startup has failed.
     *
     * This method returns how much time to wait in seconds until failing the startup.
     * */
    val fileCreationTimeout: Int
): BaseConsts() {

    companion object {

        /**
         * Convenience method for if you're including in your App's jniLibs directory
         * the `libTor.so` binary, or utilizing those maintained by this project.
         *
         * @param [context] Context
         * @param [configDir] context.getDir("dir_name_here", Context.MODE_PRIVATE)
         * @param [dataDir] if you wish it in a different location than lib/tor
         * */
        fun createConfig(context: Context, configDir: File, dataDir: File? = null): TorConfigFiles {
            val installDir = File(context.applicationInfo.nativeLibraryDir)
            val builder = Builder(installDir, configDir)
            if (dataDir != null)
                builder.dataDir(dataDir)
            return builder.build()
        }

        /**
         * Convenience method for setting up all of your files and directories in their
         * default locations.
         *
         * @param context Context
         * */
        fun createConfig(context: Context): TorConfigFiles =
            createConfig(context, context.getDir("torservice", Context.MODE_PRIVATE))
    }

    var torrcFile = torrcFile
        private set


    //////////////////
    /// File Locks ///
    //////////////////
    val torrcFileLock = Object()
    val controlPortFileLock = Object()
    val cookieAuthFileLock = Object()
    val dataDirLock = Object()
    val geoIpFileLock = Object()
    val geoIpv6FileLock = Object()
    val resolvConfFileLock = Object()
    val hostnameFileLock = Object()

    /**
     * Resolves the tor configuration file. If the torrc file hasn't been set, then
     * this method will attempt to resolve the config file by looking in the root of
     * the $configDir and then in $user.home directory
     *
     * @return [torrcFile]
     * @throws [IOException] If torrc file is not resolved.
     * @throws [SecurityException] Unauthorized access to file/directory.
     * */
    @Throws(IOException::class, SecurityException::class)
    fun resolveTorrcFile(): File {
        synchronized(torrcFileLock) {
            if (torrcFile.exists()) {
                return torrcFile
            }

            val tmpTorrcFile = File(configDir, ConfigFileName.TORRC)
            if (tmpTorrcFile.exists()) {
                torrcFile = tmpTorrcFile
                return torrcFile
            }

            torrcFile = File(configDir, ConfigFileName.TORRC)
            if (torrcFile.createNewFile()) {
                return torrcFile
            }

            throw IOException("Failed to create torrc file")
        }
    }

    override fun toString(): String {
        return "TorConfigFiles{ " +
                "geoIpFile=$geoIpFile, " +
                "geoIpv6File=$geoIpv6File, " +
                "torrcFile=$torrcFile, " +
                "torExecutableFile=$torExecutableFile, " +
                "hiddenServiceDir=$hiddenServiceDir, " +
                "dataDir=$dataDir, " +
                "configDir=$configDir, " +
                "installDir=$installDir, " +
                "hostnameFile=$hostnameFile, " +
                "cookieAuthFile=$cookieAuthFile, " +
                "libraryPath=$libraryPath }"
    }

    /**
     * Builder for TorConfig.
     *
     * See also [Companion.createConfig] for convenience methods.
     *
     * @param [installDir] directory where the tor binaries are installed.
     * @param [configDir] directory where the filesystem will be setup for tor.
     * @sample [io.matthewnelson.sampleapp.App.customTorConfigFilesSetup]
     */
    class Builder(private val installDir: File, private val configDir: File) {

        private lateinit var mTorExecutableFile: File
        private lateinit var mGeoIpFile: File
        private lateinit var mGeoIpv6File: File
        private lateinit var mTorrcFile: File
        private lateinit var mHiddenServiceDir: File
        private lateinit var mDataDir: File
        private var mLibraryPath: File? = null
        private lateinit var mCookieAuthFile: File
        private lateinit var mHostnameFile: File
        private lateinit var mResolveConf: File
        private lateinit var mControlPortFile: File
        private var mFileCreationTimeout = 0

        fun torExecutable(file: File): Builder {
            mTorExecutableFile = file
            return this
        }

        /**
         * Store data files for a hidden service in DIRECTORY. Every hidden service must
         * have a separate directory. You may use this option multiple times to specify
         * multiple services. If DIRECTORY does not exist, Tor will create it. (Note: in
         * current versions of Tor, if DIRECTORY is a relative path, it will be relative
         * to the current working directory of Tor instance, not to its DataDirectory. Do
         * not rely on this behavior; it is not guaranteed to remain the same in future
         * versions.)
         *
         * Default value: $configDir/hiddenservices
         *
         * @param [directory] hidden services directory
         *
         * @return [Builder]
         */
        fun hiddenServiceDir(directory: File): Builder {
            mHiddenServiceDir = directory
            return this
        }

        /**
         * A filename containing IPv6 GeoIP data, for use with by-country statistics.
         *
         * Default value: $configDir/geoip6
         *
         * @param [file] geoip6 file
         *
         * @return [Builder]
         */
        fun geoipv6(file: File): Builder {
            mGeoIpv6File = file
            return this
        }

        /**
         * A filename containing IPv4 GeoIP data, for use with by-country statistics.
         *
         * Default value: $configDir/geoip
         *
         * @param [file] geoip file
         *
         * @return [Builder]
         */
        fun geoip(file: File): Builder {
            mGeoIpFile = file
            return this
        }

        /**
         * Store working data in DIR. Can not be changed while tor is running.
         *
         * Default value: $configDir/lib/tor
         *
         * @param [directory] directory where tor runtime data is stored
         *
         * @return [Builder]
         */
        fun dataDir(directory: File): Builder {
            mDataDir = directory
            return this
        }

        /**
         * The configuration file, which contains "option value" pairs.
         *
         * Default value: $configDir/torrc
         *
         * @param [file] your torrc file
         *
         * @return [Builder]
         */
        fun torrc(file: File): Builder {
            mTorrcFile = file
            return this
        }

        fun libraryPath(directory: File): Builder {
            mLibraryPath = directory
            return this
        }

        fun cookieAuthFile(file: File): Builder {
            mCookieAuthFile = file
            return this
        }

        fun hostnameFile(file: File): Builder {
            mHostnameFile = file
            return this
        }

        fun resolveConf(resolveConf: File): Builder {
            this.mResolveConf = resolveConf
            return this
        }

        /**
         * When tor starts it waits for the control port and cookie auth files to be
         * created before it proceeds to the next step in startup. If these files are
         * not created after a certain amount of time, then the startup has failed.
         *
         * This method specifies how much time to wait until failing the startup.
         *
         * Default value is 15 seconds
         *
         * @param [timeoutSeconds] Int
         *
         * @return [Builder]
         */
        fun fileCreationTimeout(timeoutSeconds: Int): Builder {
            mFileCreationTimeout = timeoutSeconds
            return this
        }

        /**
         * Builds torConfig and sets default values if not explicitly configured through builder.
         *
         * @return [TorConfigFiles]
         */
        fun build(): TorConfigFiles {
            if (!::mTorExecutableFile.isInitialized)
                mTorExecutableFile = File(installDir, ConfigFileName.TOR_EXECUTABLE)

            if (!::mGeoIpFile.isInitialized)
                mGeoIpFile = File(configDir, ConfigFileName.GEO_IP)

            if (!::mGeoIpv6File.isInitialized)
                mGeoIpv6File = File(configDir, ConfigFileName.GEO_IPV_6)

            if (!::mTorrcFile.isInitialized)
                mTorrcFile = File(configDir, ConfigFileName.TORRC)

            if (!::mHiddenServiceDir.isInitialized)
                mHiddenServiceDir = File(configDir, ConfigFileName.HIDDEN_SERVICE)

            if (!::mDataDir.isInitialized)
                mDataDir = File(configDir, ConfigFileName.DATA_DIR)

            if (mLibraryPath == null)
                mLibraryPath = mTorExecutableFile.parentFile

            if (!::mHostnameFile.isInitialized)
                mHostnameFile = File(mDataDir, ConfigFileName.HOST)

            if (!::mCookieAuthFile.isInitialized)
                mCookieAuthFile = File(mDataDir, ConfigFileName.COOKIE_AUTH)

            if (!::mResolveConf.isInitialized)
                mResolveConf = File(configDir, ConfigFileName.RESOLVE_CONF)

            if (!::mControlPortFile.isInitialized)
                mControlPortFile = File(mDataDir, ConfigFileName.CONTROL_PORT)

            if (mFileCreationTimeout <= 0)
                mFileCreationTimeout = 15

            return TorConfigFiles(
                mGeoIpFile,
                mGeoIpv6File,
                mTorrcFile,
                mTorExecutableFile,
                mHiddenServiceDir,
                mDataDir,
                configDir,
                mHostnameFile,
                mCookieAuthFile,
                mLibraryPath,
                mResolveConf,
                mControlPortFile,
                installDir,
                mFileCreationTimeout
            )
        }
    }
}