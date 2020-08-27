package com.samourai.sentinel.tor.lifecycle

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.ProcessLifecycleOwner
import com.samourai.sentinel.tor.service.BaseService
import com.samourai.sentinel.tor.service.TorService
import com.samourai.sentinel.tor.service.components.actions.ServiceActionProcessor
import com.samourai.sentinel.tor.service.components.binding.BaseServiceConnection
import com.samourai.sentinel.tor.util.ServiceConsts

/**
 * When your application is sent to the background (the Recent App's tray or lock screen), the
 * chosen [BackgroundManager.Builder.Policy] will be executed after the number of seconds you've
 * declared.
 *
 * If brought back into the foreground by the user:
 *
 *   - **Before Policy execution**: Execution is canceled. If [BaseService.lastAcceptedServiceAction]
 *   was **not** [ServiceConsts.ServiceActionName.STOP], a startService call is made to ensure it's
 *   started.
 *
 *   - **After Policy execution**: If [BaseService.lastAcceptedServiceAction]
 *   was **not** [ServiceConsts.ServiceActionName.STOP], a startService call is made to ensure it's
 *   started.
 *
 *   - See [BaseService.updateLastAcceptedServiceAction] and [TorService.onTaskRemoved] for
 *   more information.
 *
 * While your application is in the foreground the only way to stop the service is by
 * calling [io.matthewnelson.topl_service.TorServiceController.stopTor], or via the
 * [io.matthewnelson.topl_service.notification.ServiceNotification] Action (if enabled);
 * The OS will not kill a service started using `Context.startService` &
 * `Context.bindService` (how [TorService] is started) while in the foreground.
 *
 * When the user sends your application to the Recent App's tray though, to recoup resources
 * the OS will kill your app after being idle for a period of time (random AF... typically
 * 0.75m to 1.25m). This is not an issue if the user removes the task before the OS
 * kills the app, as Tor will be able to shutdown properly and the service will stop.
 *
 * This is where Services get sketchy (especially when trying to implement an always
 * running service for networking), and is the purpose of the [BackgroundManager] class.
 *
 * This class starts your chosen [BackgroundManager.Builder.Policy] as soon as your
 * application is sent to the background, waits for the time you declared, and then executes.
 *
 * See the [BackgroundManager.Builder] for more detail.
 *
 * @param [policy] The chosen [ServiceConsts.BackgroundPolicy] to be executed.
 * @param [executionDelay] Length of time before the policy gets executed *after* the application
 *   is sent to the background.
 * @param [serviceClass] The Service class being managed
 * @param [serviceConnection] The ServiceConnection being used to bind with
 * @see [io.matthewnelson.topl_service.service.components.binding.TorServiceBinder.executeBackgroundPolicyJob]
 * @see [io.matthewnelson.topl_service.service.components.binding.TorServiceBinder.cancelExecuteBackgroundPolicyJob]
 * */
class BackgroundManager internal constructor(
    @BackgroundPolicy private val policy: String,
    private val executionDelay: Long,
    private val serviceClass: Class<*>,
    private val serviceConnection: BaseServiceConnection
): ServiceConsts(), LifecycleObserver {


    /**
     * This [BackgroundManager.Builder] sets how you want the service to operate while your
     * app is in the background (the Recent App's tray or lock screen), such that things run
     * reliably based off of your application's needs.
     *
     * When your application is brought back into the foreground your [Policy] is canceled
     * and, if [BaseService.lastAcceptedServiceAction] was **not** to Stop the service, a
     * startup command is issued to bring it back to the started state no matter if it is still
     * running or not.
     *
     * @sample [io.matthewnelson.sampleapp.App.generateBackgroundManagerPolicy]
     * */
    class Builder {

        @BackgroundPolicy
        private lateinit var chosenPolicy: String
        private var executionDelay: Long = 30_000L

        // TODO: Needs more work... running in the foreground is inhibiting the Application from
        //  performing it's normal lifecycle after user swipes it away such that it's not going
        //  through Application.onCreate, but is holding onto references. (same problem when
        //  starting the service using Context.startForegroundService), which is bullshit.
//        /**
//         * While your application is in the background (the Recent App's tray or lock screen),
//         * this [Policy] periodically switches [TorService] to the foreground then immediately
//         * back the background. Doing do prevents your application from going idle and being
//         * killed by the OS. It is much more resource intensive than choosing
//         * [respectResourcesWhileInBackground].
//         *
//         * @param [secondsFrom20To40]? Seconds between the events of cycling from background to
//         * foreground to background. Sending null will use the default (30s)
//         * @return [BackgroundManager.Builder.Policy] To use when initializing
//         *   [com.samourai.sentinel.tor.TorServiceController.Builder]
//         * */
//        fun keepAliveWhileInBackground(secondsFrom20To40: Int? = null): Policy {
//            chosenPolicy = BackgroundPolicy.KEEP_ALIVE
//            if (secondsFrom20To40 != null && secondsFrom20To40 in 20..40)
//                executionDelay = (secondsFrom20To40 * 1000).toLong()
//            return Policy(this)
//        }

        /**
         * Stops [TorService] after being in the background for the declared [secondsFrom5To45].
         *
         * Your application won't go through it's normal `Application.onCreate` process unless
         * it was killed, but [TorService] may have been stopped when the policy gets executed.
         *
         * Electing this option ensures [TorService] gets restarted in a more reliable manner then
         * returning `Context.START_STICKY` in [TorService.onStartCommand]. It also allows for
         * a proper shutdown of Tor prior to the service being stopped instead of it being
         * killed along with your application (which causes problems sometimes).
         *
         * If killed by the OS then this gets garbage collected such that in the event
         * the user brings your application back into the foreground (after it had been killed),
         * this will be re-instantiated when going through `Application.onCreate` again, and
         * [TorService] started by however you have it implemented.
         *
         * @param [secondsFrom5To45]? Seconds before the [Policy] is executed after the
         *   Application goes to the background. Sending null will use the default (30s)
         * @return [BackgroundManager.Builder.Policy] To use when initializing
         *   [io.matthewnelson.topl_service.TorServiceController.Builder]
         * */
        fun respectResourcesWhileInBackground(secondsFrom5To45: Int? = null): Policy {
            chosenPolicy = BackgroundPolicy.RESPECT_RESOURCES
            if (secondsFrom5To45 != null && secondsFrom5To45 in 5..45)
                executionDelay = (secondsFrom5To45 * 1000).toLong()
            return Policy(this)
        }

        /**
         * Holds the chosen policy to be built in
         * [io.matthewnelson.topl_service.TorServiceController.Builder.build].
         *
         * @param [policyBuilder] The [BackgroundManager.Builder] to be built during initialization
         * */
        class Policy(private val policyBuilder: Builder) {

            /**
             * Only available internally, so this is where we intercept for integration testing.
             * Calling [Policy.build] before
             * [io.matthewnelson.topl_service.TorServiceController.Builder.build] ensures our
             * test classes get initialized so they aren't overwritten by production classes.
             * */
            internal fun build(
                serviceClass: Class<*>,
                serviceConnection: BaseServiceConnection
            ) {

                // Only initialize it once. Reflection has issues here
                // as it's in a Companion object.
                try {
                    backgroundManager.hashCode()
                } catch (e: UninitializedPropertyAccessException) {
                    backgroundManager =
                        BackgroundManager(
                            policyBuilder.chosenPolicy,
                            policyBuilder.executionDelay,
                            serviceClass,
                            serviceConnection
                        )
                }
            }
        }
    }

    internal companion object {
        private lateinit var backgroundManager: BackgroundManager

        // TODO: re-implement in BaseService as a monitor for Tor's state so it can automatically
        //  handle hiccups (such as network getting stuck b/c Android is sometimes unreliable,
        //  or Bootstrapping stalling).
//        var heartbeatTime = 30_000L
//            private set
//
//        fun initialize(milliseconds: Long) {
//            heartbeatTime = milliseconds
//        }
    }

    init {
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    private fun applicationMovedToForeground() {
        // if the last _accepted_ ServiceAction to be issued by the Application was not to STOP
        // the service, then we want to put it back in the state it was in
        if (!ServiceActionProcessor.wasLastAcceptedServiceActionStop()) {
            BaseServiceConnection.serviceBinder?.cancelExecuteBackgroundPolicyJob()
            BaseService.startService(
                BaseService.getAppContext(),
                serviceClass,
                serviceConnection,
                includeIntentActionStart = false
            )
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    private fun applicationMovedToBackground() {
        if (!ServiceActionProcessor.wasLastAcceptedServiceActionStop())
            BaseServiceConnection.serviceBinder?.executeBackgroundPolicyJob(policy, executionDelay)
    }
}