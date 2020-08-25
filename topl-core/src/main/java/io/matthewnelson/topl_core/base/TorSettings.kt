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

import io.matthewnelson.topl_core.base.BaseConsts

/**
 * This class is for defining default values for your torrc file. Extend this class and define
 * your own settings.
 *
 * Keep in mind that Orbot and TorBrowser are the 2 most widely used applications
 * using Tor, and to use settings that won't conflict (those settings are documented
 * as such, and contain further details).
 *
 * [TorSettings.Companion] contains pretty standard default values which'll get you a Socks5 proxy
 * running, nothing more.
 *
 * Would **highly recommend** reading up on what's what in the manual:
 *  - https://2019.www.torproject.org/docs/tor-manual.html.en
 * */
abstract class TorSettings: BaseConsts() {

    /**
     * Handy constants for declaring pre-defined values when you extend this class to set
     * things up if you're simply looking to use a Socks5 Proxy to connect to.
     * */
    companion object {
        const val DEFAULT__DISABLE_NETWORK = true
        const val DEFAULT__DNS_PORT = "0"
        const val DEFAULT__ENTRY_NODES = ""
        const val DEFAULT__EXCLUDED_NODES = ""
        const val DEFAULT__EXIT_NODES = ""
        const val DEFAULT__HTTP_TUNNEL_PORT = "0"
        const val DEFAULT__PROXY_HOST = ""
        const val DEFAULT__PROXY_PASSWORD = ""
        const val DEFAULT__PROXY_SOCKS5_HOST = "" // "127.0.0.1"
        const val DEFAULT__PROXY_TYPE = ""
        const val DEFAULT__PROXY_USER = ""
        const val DEFAULT__REACHABLE_ADDRESS_PORTS = "" // "*:80,*:443"
        const val DEFAULT__RELAY_NICKNAME = ""
        const val DEFAULT__HAS_BRIDGES = false
        const val DEFAULT__HAS_CONNECTION_PADDING = ConnectionPadding.OFF
        const val DEFAULT__HAS_COOKIE_AUTHENTICATION = true
        const val DEFAULT__HAS_DEBUG_LOGS = false
        const val DEFAULT__HAS_DORMANT_CANCELED_BY_STARTUP = true
        const val DEFAULT__HAS_ISOLATION_ADDRESS_FLAG_FOR_TUNNEL = false
        const val DEFAULT__HAS_OPEN_PROXY_ON_ALL_INTERFACES = false
        const val DEFAULT__HAS_REACHABLE_ADDRESS = false
        const val DEFAULT__HAS_REDUCED_CONNECTION_PADDING = true
        const val DEFAULT__HAS_SAFE_SOCKS = false
        const val DEFAULT__HAS_STRICT_NODES = false
        const val DEFAULT__HAS_TEST_SOCKS = false
        const val DEFAULT__IS_AUTO_MAP_HOSTS_ON_RESOLVE = true
        const val DEFAULT__IS_RELAY = false
        const val DEFAULT__RUN_AS_DAEMON = true
        const val DEFAULT__TRANS_PORT = "0"
        const val DEFAULT__USE_SOCKS5 = true
    }

    /**
     * See [DEFAULT__DISABLE_NETWORK]
     * */
    abstract val disableNetwork: Boolean

    /**
     * TorBrowser and Orbot use "5400" by default. It may be wise to pick something
     * that won't conflict.
     *
     * See [DEFAULT__DNS_PORT]
     * */
    abstract val dnsPort: String

    /**
     * Default [java.null]
     * */
    abstract val customTorrc: String?

    /**
     * See [DEFAULT__ENTRY_NODES]
     * */
    abstract val entryNodes: String?

    /**
     * See [DEFAULT__EXCLUDED_NODES]
     * */
    abstract val excludeNodes: String?

    /**
     * See [DEFAULT__EXIT_NODES]
     * */
    abstract val exitNodes: String?

    /**
     * Could be "auto" or a specific port, such as "8288".
     *
     * TorBrowser and Orbot use "8218" by default. It may be wise to pick something
     * that won't conflict if you're using this setting.
     *
     * Docs: https://2019.www.torproject.org/docs/tor-manual.html.en#HTTPTunnelPort
     *
     * See [DEFAULT__HTTP_TUNNEL_PORT] ("0", to disable it)
     *
     * TODO: Change to List<String> and update TorSettingsBuilder method for
     *  multi-port support.
     * */
    abstract val httpTunnelPort: String

    /**
     * Must have the transport binaries for obfs4 and/or snowflake, depending
     * on if you wish to include them in your bridges file to use.
     *
     * See [BaseConsts.SupportedBridges] for options.
     */
    abstract val listOfSupportedBridges: List<@SupportedBridges String>

    /**
     * See [DEFAULT__PROXY_HOST]
     * */
    abstract val proxyHost: String?

    /**
     * See [DEFAULT__PROXY_PASSWORD]
     * */
    abstract val proxyPassword: String?

    /**
     * Default = [java.null]
     * */
    abstract val proxyPort: Int?

    /**
     * See [DEFAULT__PROXY_SOCKS5_HOST]
     * */
    abstract val proxySocks5Host: String?

    /**
     * Default = [java.null]
     *
     * Try ((Math.random() * 1000) + 10000).toInt()
     * */
    abstract val proxySocks5ServerPort: Int?

    /**
     * See [DEFAULT__PROXY_TYPE]
     * */
    abstract val proxyType: String?

    /**
     * See [DEFAULT__PROXY_USER]
     * */
    abstract val proxyUser: String?

    /**
     * See [DEFAULT__REACHABLE_ADDRESS_PORTS]
     * */
    abstract val reachableAddressPorts: String

    /**
     * See [DEFAULT__RELAY_NICKNAME]
     *
     * If setting this value to something other than null or an empty String, see
     * [relayPort] documentation.
     * */
    abstract val relayNickname: String?

    /**
     * TorBrowser and Orbot use 9001 by default. It may be wise to pick something
     * that won't conflict.
     *
     * This only gets used if you declare the following settings set as:
     *   [hasReachableAddress] false
     *   [hasBridges] false
     *   [isRelay] true
     *   [relayNickname] "your nickname"
     *   [relayPort] some Int value
     *
     * Default = [java.null]
     * */
    abstract val relayPort: Int?

    /**
     * Could be "auto" or a specific port, such as "9051".
     *
     * TorBrowser uses "9150", and Orbot uses "9050" by default. It may be wise
     * to pick something that won't conflict.
     * */
    abstract val socksPort: String

    /**
     * TorBrowser and Orbot use "10.192.0.1/10", it may be wise to pick something
     * that won't conflict if you are using this setting.
     *
     * Docs: https://2019.www.torproject.org/docs/tor-manual.html.en#VirtualAddrNetworkIPv6
     * */
    abstract val virtualAddressNetwork: String?

    /**
     * See [DEFAULT__HAS_BRIDGES]
     * */
    abstract val hasBridges: Boolean

    /**
     * See [DEFAULT__HAS_CONNECTION_PADDING]
     * */
    abstract val connectionPadding: @ConnectionPadding String

    /**
     * See [DEFAULT__HAS_COOKIE_AUTHENTICATION]
     * */
    abstract val hasCookieAuthentication: Boolean

    /**
     * See [DEFAULT__HAS_DEBUG_LOGS]
     *
     * 
     * */
    abstract val hasDebugLogs: Boolean

    /**
     * See [DEFAULT__HAS_DORMANT_CANCELED_BY_STARTUP]
     * */
    abstract val hasDormantCanceledByStartup: Boolean

    /**
     * See [DEFAULT__HAS_ISOLATION_ADDRESS_FLAG_FOR_TUNNEL]
     * */
    abstract val hasIsolationAddressFlagForTunnel: Boolean

    /**
     * See [DEFAULT__HAS_OPEN_PROXY_ON_ALL_INTERFACES]
     * */
    abstract val hasOpenProxyOnAllInterfaces: Boolean

    /**
     * See [DEFAULT__HAS_REACHABLE_ADDRESS]
     * */
    abstract val hasReachableAddress: Boolean

    /**
     * See [DEFAULT__HAS_REDUCED_CONNECTION_PADDING]
     * */
    abstract val hasReducedConnectionPadding: Boolean

    /**
     * See [DEFAULT__HAS_SAFE_SOCKS]
     * */
    abstract val hasSafeSocks: Boolean

    /**
     * See [DEFAULT__HAS_STRICT_NODES]
     * */
    abstract val hasStrictNodes: Boolean

    /**
     * See [DEFAULT__HAS_TEST_SOCKS]
     * */
    abstract val hasTestSocks: Boolean

    /**
     * See [DEFAULT__IS_AUTO_MAP_HOSTS_ON_RESOLVE]
     *
     * Docs: https://2019.www.torproject.org/docs/tor-manual.html.en#AutomapHostsOnResolve
     * */
    abstract val isAutoMapHostsOnResolve: Boolean

    /**
     * See [DEFAULT__IS_RELAY]
     *
     * If setting this to true, see [relayPort] documentation.
     * */
    abstract val isRelay: Boolean

    /**
     * See [DEFAULT__RUN_AS_DAEMON]
     * */
    abstract val runAsDaemon: Boolean

    /**
     * Can be "auto", or a specified port such as "9141"
     *
     * See [listOfSupportedBridges] documentation.
     *
     * Orbot and TorBrowser default to "9140". It may be wise to pick something
     * that won't conflict.
     *
     * See [DEFAULT__TRANS_PORT]
     *
     * TODO: Change to a List<String>? to support multiple ports
     * */
    abstract val transPort: String

    /**
     * See [DEFAULT__USE_SOCKS5]
     * */
    abstract val useSocks5: Boolean

//    override fun toString(): String {
//        return "TorSettings{ " +
//                "disableNetwork=${if (disableNetwork) TRUE else FALSE}, " +
//                "dnsPort=${dnsPort.toString()}, " +
//                "customTorrc=${customTorrc ?: NULL}, " +
//                "entryNodes=${entryNodes ?: NULL}, " +
//                "excludeNodes=${excludeNodes ?: NULL}, " +
//                "exitNodes=${exitNodes ?: NULL}, " +
//                "httpTunnelPort=${httpTunnelPort.toString()}, " +
//                "listOfSupportedBridges=${listOfSupportedBridges.joinToString(", ", "[", "]")}, " +
//                "proxyHost=${proxyHost ?: NULL}, " +
//                "proxyPassword=${proxyPassword ?: NULL}, " +
//                "proxyPort=${proxyPort?.toString() ?: NULL}, " +
//                "proxySocks5Host=${proxySocks5Host ?: NULL}, " +
//                "proxySocks5ServerPort=${proxySocks5ServerPort?.toString() ?: NULL}, " +
//                "proxyType=${proxyType ?: NULL}, " +
//                "proxyUser=${proxyUser ?: NULL}, " +
//                "reachableAddressPorts=$reachableAddressPorts, " +
//                "relayNickname=${relayNickname ?: NULL}, " +
//                "relayPort=${relayPort.toString()}, " +
//                "socksPort=$socksPort, " +
//                "virtualAddressNetwork=${virtualAddressNetwork ?: NULL}, " +
//                "hasBridges=${if (hasBridges) TRUE else FALSE}, " +
//                "hasConnectionPadding=${if (hasConnectionPadding) TRUE else FALSE}, " +
//                "hasCookieAuthentication=${if (hasCookieAuthentication) TRUE else FALSE}, " +
//                "hasDebugLogs=${if (hasDebugLogs) TRUE else FALSE}, " +
//                "hasDormantCanceledByStartup=${if (hasDormantCanceledByStartup) TRUE else FALSE}, " +
//                "hasIsolationAddressFlagForTunnel=${if (hasIsolationAddressFlagForTunnel) TRUE else FALSE}, " +
//                "hasOpenProxyOnAllInterfaces=${if (hasOpenProxyOnAllInterfaces) TRUE else FALSE}, " +
//                "hasReachableAddress=${if (hasReachableAddress) TRUE else FALSE}, " +
//                "hasReducedConnectionPadding=${if (hasReducedConnectionPadding) TRUE else FALSE}, " +
//                "hasSafeSocks=${if (hasSafeSocks) TRUE else FALSE}, " +
//                "hasStrictNodes=${if (hasStrictNodes) TRUE else FALSE}, " +
//                "hasTestSocks=${if (hasTestSocks) TRUE else FALSE}, " +
//                "isAutoMapHostsOnResolve=${if (isAutoMapHostsOnResolve) TRUE else FALSE}, " +
//                "isRelay=${if (isRelay) TRUE else FALSE}, " +
//                "runAsDaemon=${if (runAsDaemon) TRUE else FALSE}, " +
//                "transPort=${transPort?.toString() ?: NULL}, " +
//                "useSocks5=${if (useSocks5) TRUE else FALSE} }"
//    }
}