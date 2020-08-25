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
package io.matthewnelson.topl_core.util

import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketAddress

object Utilities {

    private const val READ_TIMEOUT_MILLISECONDS = 60000
    private const val CONNECT_TIMEOUT_MILLISECONDS = 60000

    /**
     * When making a request via the Tor Proxy one needs to establish the socket using SOCKS4a. However Android
     * only supports SOCKS4 so this class provides a wrapper when getting a socket to handle things.
     * @param networkHost DNS or IP address of destination host
     * @param networkPort Port of destination host
     * @param socksHost DNS or IP address of local SOCKS4A Proxy (the Tor Onion Proxy)
     * @param socksPort Port of SOCKS4A Proxy (the Tor Onion Proxy)
     * @return A socket set up to relay via socks to the local Tor Onion Proxy and via the Tor Network to the
     * destination host.
     * @throws IOException Networking issues
     */
    @Throws(IOException::class)
    fun socks4aSocketConnection(
        networkHost: String,
        networkPort: Int,
        socksHost: String,
        socksPort: Int
    ): Socket {
        // Perform explicit SOCKS4a connection request. SOCKS4a supports remote host name resolution
        // (i.e., Tor resolves the hostname, which may be an onion address).
        // The Android (Apache Harmony) Socket class appears to support only SOCKS4 and throws an
        // exception on an address created using INetAddress.createUnresolved() -- so the typical
        // technique for using Java SOCKS4a/5 doesn't appear to work on Android:
        // https://android.googlesource.com/platform/libcore/+/master/luni/src/main/java/java/net/PlainSocketImpl.java
        // See also: http://www.mit.edu/~foley/TinFoil/src/tinfoil/TorLib.java, for a similar implementation

        // From http://en.wikipedia.org/wiki/SOCKS#SOCKS4a:
        //
        // field 1: SOCKS version number, 1 byte, must be 0x04 for this version
        // field 2: command code, 1 byte:
        //     0x01 = establish a TCP/IP stream connection
        //     0x02 = establish a TCP/IP port binding
        // field 3: network byte order port number, 2 bytes
        // field 4: deliberate invalid IP address, 4 bytes, first three must be 0x00 and the last one must not be 0x00
        // field 5: the user ID string, variable length, terminated with a null (0x00)
        // field 6: the domain name of the host we want to contact, variable length, terminated with a null (0x00)
        val socket = Socket()
        socket.soTimeout = READ_TIMEOUT_MILLISECONDS

        val socksAddress: SocketAddress = InetSocketAddress(socksHost, socksPort)
        socket.connect(socksAddress, CONNECT_TIMEOUT_MILLISECONDS)

        val outputStream = DataOutputStream(socket.getOutputStream())
        outputStream.write(byteArrayOf(0x04.toByte()))
        outputStream.write(byteArrayOf(0x01.toByte()))
        outputStream.writeShort(networkPort)
        outputStream.writeInt(0x01)
        outputStream.write(byteArrayOf(0x00.toByte()))
        outputStream.write(networkHost.toByteArray())
        outputStream.write(byteArrayOf(0x00.toByte()))

        val inputStream = DataInputStream(socket.getInputStream())
        val firstByte = inputStream.readByte()
        val secondByte = inputStream.readByte()
        if (firstByte != 0x00.toByte() || secondByte != 0x5a.toByte()) {
            socket.close()
            throw IOException(
                "SOCKS4a connect failed, got $firstByte - $secondByte, but expected 0x00 - 0x5a: "
                        + "networkHost = $networkHost, networkPort = $networkPort, "
                        + "socksHost = $socksHost, socksPort = $socksPort"
            )
        }
        inputStream.readShort()
        inputStream.readInt()
        return socket
    }
}