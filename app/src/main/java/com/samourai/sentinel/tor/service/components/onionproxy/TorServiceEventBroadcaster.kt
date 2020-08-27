package com.samourai.sentinel.tor.service.components.onionproxy

import io.matthewnelson.topl_core.base.EventBroadcaster

abstract class TorServiceEventBroadcaster: EventBroadcaster() {

    /**
     * Override this method to implement receiving of the control port address that Tor
     * is operating on.
     *
     * Example of what will be broadcast:
     *
     *   - "127.0.0.1:33432"
     * */
    abstract fun broadcastControlPortAddress(controlPortAddress: String?)

    /**
     * Override this method to implement receiving of the Socks port address that Tor
     * is operating on (if you've specified a
     * [io.matthewnelson.topl_core_base.TorSettings.socksPort]).
     *
     * Example of what will be broadcast:
     *
     *   - "127.0.0.1:9051"
     * */
    abstract fun broadcastSocksPortAddress(socksPortAddress: String?)

    /**
     * Override this method to implement receiving of the http port address that Tor
     * is operating on (if you've specified a
     * [io.matthewnelson.topl_core_base.TorSettings.httpTunnelPort]).
     *
     * Example of what will be broadcast:
     *
     *   - "127.0.0.1:33432"
     * */
    abstract fun broadcastHttpPortAddress(httpPortAddress: String?)
}