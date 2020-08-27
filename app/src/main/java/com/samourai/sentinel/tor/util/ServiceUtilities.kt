package com.samourai.sentinel.tor.util

import android.annotation.SuppressLint
import java.text.NumberFormat
import java.util.*

object ServiceUtilities {

    /**
     * Formats the supplied values to look like: `20kbps ↓ / 85kbps ↑`
     *
     * @param [download] Long value associated with download (bytesRead)
     * @param [upload] Long value associated with upload (bytesWritten)
     * */
    fun getFormattedBandwidthString(download: Long, upload: Long): String =
        "${formatBandwidth(download)} ↓ / ${formatBandwidth(upload)} ↑"

    @SuppressLint("ConstantLocale")
    private val numberFormat = NumberFormat.getInstance(Locale.getDefault())
    /**
     * Obtained from: https://gitweb.torproject.org/tor-android-service.git/tree/service/
     *                src/main/java/org/torproject/android/service/TorEventHandler.java
     *
     * Original method name: formatCount()
     * */
    private fun formatBandwidth(value: Long): String {
        return if (value < 1e6) {
            numberFormat.format(
                Math.round((((value * 10 / 1024).toInt()) / 10).toFloat())
            ) + "kbps"
        } else {
            numberFormat.format(
                Math.round((((value * 100 / 1024 / 1024).toInt()) / 100).toFloat())
            ) + "mbps"
        }
    }
}