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
package com.samourai.sentinel.tor.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.NotificationVisibility
import androidx.core.content.ContextCompat
import com.samourai.sentinel.R
import com.samourai.sentinel.tor.service.TorService
import com.samourai.sentinel.tor.service.components.receiver.TorServiceReceiver
import com.samourai.sentinel.tor.service.BaseService
import com.samourai.sentinel.tor.util.ServiceConsts

/**
 * Everything to do with [TorService]'s notification.
 *
 * @see [Builder]
 * */
class ServiceNotification internal constructor(
    private val channelName: String,
    private val channelID: String,
    private val channelDescription: String,
    private val notificationID: Int,

    var activityWhenTapped: Class<*>? = null,
    var activityIntentKey: String? = null,
    var activityIntentExtras: String? = null,
    var activityIntentRequestCode: Int = 0,

    @DrawableRes var imageNetworkEnabled: Int = R.drawable.ic_tor_onion,
    @DrawableRes var imageNetworkDisabled: Int = R.drawable.ic_tor_onion,
    @DrawableRes var imageDataTransfer: Int =  R.drawable.ic_tor_onion,
    @DrawableRes var imageError: Int =  R.drawable.ic_baseline_error_outline,

    @ColorRes var colorWhenConnected: Int = R.color.green_ui_2,

    @NotificationVisibility var visibility: Int = NotificationCompat.VISIBILITY_SECRET,

    var enableRestartButton: Boolean = false,
    var enableStopButton: Boolean = false,

    var showNotification: Boolean = true
): ServiceConsts() {


    ///////////////
    /// Builder ///
    ///////////////
    /**
     * Where you get to customize how your notification will look and function.
     *
     * A notification is required to be displayed while [TorService] is running in the
     * Foreground. Even if you set [Builder.showNotification] to false, [TorService]
     * is brought to the Foreground when the user removes your task from the recent apps tray
     * in order to properly shut down Tor and clean up w/o being killed by the OS.
     *
     * @param [channelName] Your notification channel's name (Cannot be Empty).
     * @param [channelID] Your notification channel's ID (Cannot be Empty).
     * @param [channelDescription] Your notification channel's description (Cannot be Empty).
     * @param [notificationID] Your foreground notification's ID.
     * @sample [io.matthewnelson.sampleapp.App.generateTorServiceNotificationBuilder]
     * @throws [IllegalArgumentException] If String fields are empty.
     * */
    class Builder(
        channelName: String,
        channelID: String,
        channelDescription: String,
        notificationID: Int
    ) {

        init {
            require(
                channelName.isNotEmpty() && channelID.isNotEmpty() && channelDescription.isNotEmpty()
            ) { "channelName, channelID, & channelDescription must not be empty." }
        }

        private val serviceNotification =
            ServiceNotification(
                channelName,
                channelID,
                channelDescription,
                notificationID
            )

        /**
         * Define the Activity to be opened when your user taps TorService's notification.
         *
         * See [Builder] for code samples.
         *
         * @param [clazz] The Activity to be opened when tapped.
         * @param [intentExtrasKey]? The key for if you with to add extras in the PendingIntent.
         * @param [intentExtras]? The extras that will be sent in the PendingIntent.
         * @param [intentRequestCode]? The request code - Defaults to 0 if not set.
         *
         * */
        fun setActivityToBeOpenedOnTap(
            clazz: Class<*>,
            intentExtrasKey: String?,
            intentExtras: String?,
            intentRequestCode: Int?
        ): Builder {
            serviceNotification.activityWhenTapped = clazz
            serviceNotification.activityIntentKey = intentExtrasKey
            serviceNotification.activityIntentExtras = intentExtras
            intentRequestCode?.let { serviceNotification.activityIntentRequestCode = it }
            return this
        }

        /**
         * Defaults to Orbot/TorBrowser's icon [R.drawable.tor_stat_network_enabled].
         *
         * The small icon you wish to display when Tor's network state is
         * [io.matthewnelson.topl_core_base.BaseConsts.TorNetworkState.ENABLED].
         *
         * See [Builder] for code samples.
         *
         * @param [drawableRes] Drawable resource id
         * @return [Builder] To continue customizing
         * */
        fun setImageTorNetworkingEnabled(@DrawableRes drawableRes: Int): Builder {
            serviceNotification.imageNetworkEnabled = drawableRes
            return this
        }

        /**
         * Defaults to Orbot/TorBrowser's icon [R.drawable.tor_stat_network_disabled].
         *
         * The small icon you wish to display when Tor's network state is
         * [io.matthewnelson.topl_core_base.BaseConsts.TorNetworkState.DISABLED].
         *
         * See [Builder] for code samples.
         *
         * @param [drawableRes] Drawable resource id
         * @return [Builder] To continue customizing
         * */
        fun setImageTorNetworkingDisabled(@DrawableRes drawableRes: Int): Builder {
            serviceNotification.imageNetworkDisabled = drawableRes
            return this
        }

        /**
         * Defaults to Orbot/TorBrowser's icon [R.drawable.tor_stat_network_dataxfer].
         *
         * The small icon you wish to display when bandwidth is being used.
         *
         * See [Builder] for code samples.
         *
         * @param [drawableRes] Drawable resource id
         * @return [Builder] To continue customizing
         * */
        fun setImageTorDataTransfer(@DrawableRes drawableRes: Int): Builder {
            serviceNotification.imageDataTransfer = drawableRes
            return this
        }

        /**
         * Defaults to Orbot/TorBrowser's icon [R.drawable.tor_stat_notifyerr].
         *
         * The small icon you wish to display when Tor is having problems.
         *
         * See [Builder] for code samples.
         *
         * @param [drawableRes] Drawable resource id
         * @return [Builder] To continue customizing
         * */
        fun setImageTorErrors(@DrawableRes drawableRes: Int): Builder {
            serviceNotification.imageError = drawableRes
            return this
        }

        /**
         * Defaults to [R.color.tor_service_white]
         *
         * The color you wish to display when Tor's network state is
         * [io.matthewnelson.topl_core_base.BaseConsts.TorNetworkState.ENABLED].
         *
         * See [Builder] for code samples.
         *
         * @param [colorRes] Color resource id
         * @return [Builder] To continue customizing
         * */
        fun setCustomColor(@ColorRes colorRes: Int): Builder {
            serviceNotification.colorWhenConnected = colorRes
            return this
        }

        /**
         * Defaults to NotificationVisibility.VISIBILITY_SECRET
         *
         * The visibility of your notification on the user's lock screen.
         *
         * See [Builder] for code samples.
         *
         * @param [visibility] The [NotificationVisibility] you desire your notification to have
         * @return [Builder] To continue customizing
         * */
        fun setVisibility(@NotificationVisibility visibility: Int): Builder {
            if (visibility in -1..1)
                serviceNotification.visibility = visibility
            return this
        }

        /**
         * Disabled by Default
         *
         * Enable on the notification the ability to **restart** Tor.
         *
         * See [Builder] for code samples.
         *
         * @param [enable] Boolean, automatically set to true but provides cleaner option
         *   for implementor to query SharedPreferences for user's settings (if desired)
         * @return [Builder] To continue customizing
         * */
        fun enableTorRestartButton(enable: Boolean = true): Builder {
            serviceNotification.enableRestartButton = enable
            return this
        }

        /**
         * Disabled by Default
         *
         * Enable on the notification the ability to **stop** Tor.
         *
         * See [Builder] for code samples.
         *
         * @param [enable] Boolean, automatically set to true but provides cleaner option
         *   for implementor to query SharedPreferences for user's settings (if desired)
         * @return [Builder] To continue customizing
         * */
        fun enableTorStopButton(enable: Boolean = true): Builder {
            serviceNotification.enableStopButton = enable
            return this
        }

        /**
         * Shown by Default.
         *
         * Setting it to false will only show a notification when the end user removes your
         * Application from the Recent App's tray. In that event, [TorService.onTaskRemoved]
         * moves the Service to the Foreground in order to properly shutdown Tor w/o the OS
         * killing it beforehand.
         *
         * See [Builder] for code samples.
         *
         * @param [show] Boolean, automatically set to false but provides cleaner option for
         *   implementor to query SharedPreferences for user's settings (if desired)
         * @return [Builder] To continue customizing
         * */
        fun showNotification(show: Boolean = false): Builder {
            serviceNotification.showNotification = show
            return this
        }

        /**
         * Initializes your notification customizations and sets up the notification
         * channel. This is called by
         * [io.matthewnelson.topl_service.TorServiceController.Builder.build]
         * */
        internal fun build(context: Context) {
            Companion.serviceNotification = this.serviceNotification
            Companion.serviceNotification.setupNotificationChannel(context)
        }

    }

    internal companion object {
        lateinit var serviceNotification: ServiceNotification
            private set
    }


    /////////////
    /// Setup ///
    /////////////
    private var notificationBuilder: NotificationCompat.Builder? = null
    private var notificationManager: NotificationManager? = null

    internal fun buildNotification(torService: BaseService): NotificationCompat.Builder {
        val builder = NotificationCompat.Builder(torService.context, channelID)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setContentText(currentContentText)
            .setContentTitle(currentContentTitle)
            .setGroup("TorService")
            .setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_SUMMARY)
            .setGroupSummary(false)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setProgress(0, 100, true)
            .setSmallIcon(currentIcon)
            .setSound(null)
            .setVisibility(visibility)

        if (activityWhenTapped != null)
            builder.setContentIntent(getContentPendingIntent(torService))

        notificationBuilder = builder
        return builder
    }

    private fun getContentPendingIntent(torService: BaseService): PendingIntent {
        val contentIntent = Intent(torService.context, activityWhenTapped)

        if (!activityIntentKey.isNullOrEmpty() && !activityIntentExtras.isNullOrEmpty())
            contentIntent.putExtra(activityIntentKey, activityIntentExtras)

        return PendingIntent.getActivity(
            torService.context,
            activityIntentRequestCode,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private fun notify(builder: NotificationCompat.Builder) {
        notificationBuilder = builder
        if (showNotification || inForeground)
            notificationManager?.notify(notificationID, builder.build())
    }

    @Synchronized
    internal fun remove() {
        notificationManager?.cancel(notificationID)
        notificationShowing = false
    }

    /**
     * Called once per application start in
     * [io.matthewnelson.topl_service.TorServiceController.Builder.build]
     * */
    internal fun setupNotificationChannel(context: Context): ServiceNotification {
        val nm: NotificationManager? = context.applicationContext
            .getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager?
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelID,
                channelName,
                NotificationManager.IMPORTANCE_DEFAULT
            )
            channel.description = channelDescription
            channel.setSound(null, null)
            nm?.let {
                notificationManager = it
                it.createNotificationChannel(channel)
            }
        }
        return serviceNotification
    }


    //////////////////////////
    /// Foreground Service ///
    //////////////////////////
    @Volatile
    internal var inForeground = false
        private set
    @Volatile
    internal var notificationShowing = false
        private set

    @Synchronized
    internal fun startForeground(torService: BaseService): ServiceNotification {
        if (!inForeground) {
            notificationBuilder?.let {
                torService.startForeground(notificationID, it.build())
                inForeground = true
                notificationShowing = true
            }
        }
        return serviceNotification
    }

    @Synchronized
    internal fun stopForeground(torService: BaseService): ServiceNotification {
        if (inForeground) {
            torService.stopForeground(!showNotification)
            inForeground = false
            notificationShowing = showNotification
        }
        return serviceNotification
    }


    ///////////////
    /// Actions ///
    ///////////////
    @Volatile
    internal var actionsPresent = false
        private set

    @Synchronized
    internal fun addActions(torService: BaseService) {
        val builder = notificationBuilder ?: return
        builder.addAction(
            imageNetworkEnabled,
            "New Identity",
            getActionPendingIntent(torService, ServiceActionName.NEW_ID, 1)
        )

        if (enableRestartButton)
            builder.addAction(
                imageNetworkEnabled,
                "Restart Tor",
                getActionPendingIntent(torService, ServiceActionName.RESTART_TOR, 2)
            )

        if (enableStopButton)
            builder.addAction(
                imageNetworkEnabled,
                "Stop Tor",
                getActionPendingIntent(torService, ServiceActionName.STOP, 3)
            )
        actionsPresent = true
        notify(builder)
    }

    private fun getActionPendingIntent(
            torService: BaseService,
            @ServiceActionName action: String,
            requestCode: Int
    ): PendingIntent {
        val intent = Intent(TorServiceReceiver.SERVICE_INTENT_FILTER)
        intent.putExtra(TorServiceReceiver.SERVICE_INTENT_FILTER, action)
        intent.setPackage(torService.context.packageName)

        return PendingIntent.getBroadcast(
            torService.context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    @Synchronized
    internal fun removeActions(torService: BaseService) {
        actionsPresent = false
        notify(buildNotification(torService))
    }


    ////////////////////
    /// Content Text ///
    ////////////////////
    @Volatile
    internal var currentContentText = "Waiting..."
        private set

    @Synchronized
    internal fun updateContentText(string: String) {
        if (currentContentText == string) return
        currentContentText = string
        val builder = notificationBuilder ?: return
        builder.setContentText(string)
        notify(builder)
    }


    /////////////////////
    /// Content Title ///
    /////////////////////
    @Volatile
    internal var currentContentTitle = TorState.OFF
        private set

    @Synchronized
    internal fun updateContentTitle(title: String) {
        if (currentContentTitle == title) return
        currentContentTitle = title
        val builder = notificationBuilder ?: return
        builder.setContentTitle(title)
        notify(builder)
    }


    ////////////
    /// Icon ///
    ////////////
    @Volatile
    internal var currentIcon = imageNetworkDisabled
        private set

    @Synchronized
    internal fun updateIcon(torService: BaseService, @NotificationImage notificationImage: Int) {
        val builder = notificationBuilder ?: return
        when (notificationImage) {
            NotificationImage.ENABLED -> {
                if (currentIcon == imageNetworkEnabled) return
                currentIcon = imageNetworkEnabled
                builder.setSmallIcon(imageNetworkEnabled)
                builder.color = ContextCompat.getColor(torService.context, colorWhenConnected)
            }
            NotificationImage.DISABLED -> {
                if (currentIcon == imageNetworkDisabled) return
                currentIcon = imageNetworkDisabled
                builder.setSmallIcon(imageNetworkDisabled)
                builder.color = ContextCompat.getColor(torService.context, R.color.white)
            }
            NotificationImage.DATA -> {
                if (currentIcon == imageDataTransfer) return
                currentIcon = imageDataTransfer
                builder.setSmallIcon(imageDataTransfer)
            }
            NotificationImage.ERROR -> {
                if (currentIcon == imageError) return
                currentIcon = imageError
                builder.setSmallIcon(imageError)
            }
            else -> {}
        }
        notify(builder)
    }


    ////////////////////
    /// Progress Bar ///
    ////////////////////
    @Volatile
    internal var progressBarShown = false
        private set

    @Synchronized
    internal fun updateProgress(show: Boolean, progress: Int? = null) {
        val builder = notificationBuilder ?: return
        progressBarShown = when {
            progress != null -> {
                builder.setProgress(100, progress, false)
                true
            }
            show -> {
                builder.setProgress(100, 0, true)
                true
            }
            else -> {
                builder.setProgress(0, 0, false)
                false
            }
        }
        notify(builder)
    }
}