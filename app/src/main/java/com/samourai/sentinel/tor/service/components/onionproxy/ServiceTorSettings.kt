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

import io.matthewnelson.topl_core.base.TorSettings
import com.samourai.sentinel.tor.util.ServiceConsts.PrefKeyString
import com.samourai.sentinel.tor.util.ServiceConsts.PrefKeyList
import com.samourai.sentinel.tor.util.ServiceConsts.PrefKeyInt
import com.samourai.sentinel.tor.util.ServiceConsts.PrefKeyBoolean
import io.matthewnelson.topl_service.prefs.TorServicePrefs
import com.samourai.sentinel.tor.service.BaseService

/**
 * This class is for enabling the updating of settings in a standardized manner
 * such that library users can simply instantiate [TorServicePrefs], change things,
 * and then call [io.matthewnelson.topl_service.TorServiceController.restartTor] to have
 * them applied to the Tor Process.
 *
 * @param [torService] To instantiate [TorServicePrefs]
 * @param [defaults] Default values to fall back on if nothing is returned from [TorServicePrefs]
 * */
internal class ServiceTorSettings(
        torService: BaseService,
        private val defaults: TorSettings
): TorSettings() {

    private val prefs = TorServicePrefs(torService.context)

    override val disableNetwork: Boolean
        get() = prefs.getBoolean(PrefKeyBoolean.DISABLE_NETWORK, defaults.disableNetwork)

    override val dnsPort: String
        get() = prefs.getString(PrefKeyString.DNS_PORT, defaults.dnsPort)
            ?: defaults.dnsPort

    override val customTorrc: String?
        get() = prefs.getString(PrefKeyString.CUSTOM_TORRC, defaults.customTorrc)

    override val entryNodes: String?
        get() = prefs.getString(PrefKeyString.ENTRY_NODES, defaults.entryNodes)

    override val excludeNodes: String?
        get() = prefs.getString(PrefKeyString.EXCLUDED_NODES, defaults.excludeNodes)

    override val exitNodes: String?
        get() = prefs.getString(PrefKeyString.EXIT_NODES, defaults.exitNodes)

    override val httpTunnelPort: String
        get() = prefs.getString(PrefKeyString.HTTP_TUNNEL_PORT, defaults.httpTunnelPort)
            ?: defaults.httpTunnelPort

    override val listOfSupportedBridges: List<String>
        get() = prefs.getList(PrefKeyList.LIST_OF_SUPPORTED_BRIDGES, defaults.listOfSupportedBridges)

    override val proxyHost: String?
        get() = prefs.getString(PrefKeyString.PROXY_HOST, defaults.proxyHost)

    override val proxyPassword: String?
        get() = prefs.getString(PrefKeyString.PROXY_PASSWORD, defaults.proxyPassword)

    override val proxyPort: Int?
        get() = prefs.getInt(PrefKeyInt.PROXY_PORT, defaults.proxyPort)

    override val proxySocks5Host: String?
        get() = prefs.getString(PrefKeyString.PROXY_SOCKS5_HOST, defaults.proxySocks5Host)

    override val proxySocks5ServerPort: Int?
        get() = prefs.getInt(PrefKeyInt.PROXY_SOCKS5_SERVER_PORT, defaults.proxySocks5ServerPort)

    override val proxyType: String?
        get() = prefs.getString(PrefKeyString.PROXY_TYPE, defaults.proxyType)

    override val proxyUser: String?
        get() = prefs.getString(PrefKeyString.PROXY_USER, defaults.proxyUser)

    override val reachableAddressPorts: String
        get() = prefs.getString(PrefKeyString.REACHABLE_ADDRESS_PORTS, defaults.reachableAddressPorts)
            ?: defaults.reachableAddressPorts

    override val relayNickname: String?
        get() = prefs.getString(PrefKeyString.RELAY_NICKNAME, defaults.relayNickname)

    override val relayPort: Int?
        get() = prefs.getInt(PrefKeyInt.RELAY_PORT, defaults.relayPort)
            ?: defaults.relayPort

    override val socksPort: String
        get() = prefs.getString(PrefKeyString.SOCKS_PORT, defaults.socksPort)
            ?: defaults.socksPort

    override val virtualAddressNetwork: String?
        get() = prefs.getString(PrefKeyString.VIRTUAL_ADDRESS_NETWORK, defaults.virtualAddressNetwork)

    override val hasBridges: Boolean
        get() = prefs.getBoolean(PrefKeyBoolean.HAS_BRIDGES, defaults.hasBridges)

    override val connectionPadding: String
        get() = prefs.getString(PrefKeyString.HAS_CONNECTION_PADDING, defaults.connectionPadding)
            ?: defaults.connectionPadding

    override val hasCookieAuthentication: Boolean
        get() = prefs.getBoolean(PrefKeyBoolean.HAS_COOKIE_AUTHENTICATION, defaults.hasCookieAuthentication)

    override val hasDebugLogs: Boolean
        get() = prefs.getBoolean(PrefKeyBoolean.HAS_DEBUG_LOGS, defaults.hasDebugLogs)

    override val hasDormantCanceledByStartup: Boolean
        get() = prefs.getBoolean(PrefKeyBoolean.HAS_DORMANT_CANCELED_BY_STARTUP, defaults.hasDormantCanceledByStartup)

    override val hasIsolationAddressFlagForTunnel: Boolean
        get() = prefs.getBoolean(PrefKeyBoolean.HAS_ISOLATION_ADDRESS_FLAG_FOR_TUNNEL, defaults.hasIsolationAddressFlagForTunnel)

    override val hasOpenProxyOnAllInterfaces: Boolean
        get() = prefs.getBoolean(PrefKeyBoolean.HAS_OPEN_PROXY_ON_ALL_INTERFACES, defaults.hasOpenProxyOnAllInterfaces)

    override val hasReachableAddress: Boolean
        get() = prefs.getBoolean(PrefKeyBoolean.HAS_REACHABLE_ADDRESS, defaults.hasReachableAddress)

    override val hasReducedConnectionPadding: Boolean
        get() = prefs.getBoolean(PrefKeyBoolean.HAS_REDUCED_CONNECTION_PADDING, defaults.hasReducedConnectionPadding)

    override val hasSafeSocks: Boolean
        get() = prefs.getBoolean(PrefKeyBoolean.HAS_SAFE_SOCKS, defaults.hasSafeSocks)

    override val hasStrictNodes: Boolean
        get() = prefs.getBoolean(PrefKeyBoolean.HAS_STRICT_NODES, defaults.hasStrictNodes)

    override val hasTestSocks: Boolean
        get() = prefs.getBoolean(PrefKeyBoolean.HAS_TEST_SOCKS, defaults.hasTestSocks)

    override val isAutoMapHostsOnResolve: Boolean
        get() = prefs.getBoolean(PrefKeyBoolean.IS_AUTO_MAP_HOSTS_ON_RESOLVE, defaults.isAutoMapHostsOnResolve)

    override val isRelay: Boolean
        get() = prefs.getBoolean(PrefKeyBoolean.IS_RELAY, defaults.isRelay)

    override val runAsDaemon: Boolean
        get() = prefs.getBoolean(PrefKeyBoolean.RUN_AS_DAEMON, defaults.runAsDaemon)

    override val transPort: String
        get() = prefs.getString(PrefKeyString.TRANS_PORT, defaults.transPort) ?: defaults.transPort

    override val useSocks5: Boolean
        get() = prefs.getBoolean(PrefKeyBoolean.USE_SOCKS5, defaults.useSocks5)

}