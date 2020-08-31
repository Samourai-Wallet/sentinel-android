package com.samourai.sentinel.ui.views

import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
import android.transition.ChangeBounds
import android.transition.TransitionManager
import android.view.*
import android.view.animation.CycleInterpolator
import android.view.animation.TranslateAnimation
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import com.samourai.sentinel.R
import com.samourai.sentinel.core.access.AccessFactory
import com.samourai.sentinel.ui.utils.PrefsUtil
import kotlinx.android.synthetic.main.fragment_lock_screen.*
import org.koin.java.KoinJavaComponent
import org.koin.java.KoinJavaComponent.inject

/**
 * sentinel-android
 *
 * @author Sarath
 */
class LockScreenDialog(private val cancelable: Boolean = false, private val lockScreenMessage: String = "") : DialogFragment() {
    private val prefsUtil: PrefsUtil by inject(PrefsUtil::class.java);
    private var userInput = StringBuilder()
    private var strPassphrase = ""
    private var onConfirm: ((String) -> Unit)? = null

    override
    fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_lock_screen, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requireActivity().window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        pinEntryView.setConfirmClickListener {
            onConfirm.let { it?.invoke(userInput.toString()) }
        }
        lockScreenText.text = lockScreenMessage
        prefsUtil.scramblePin?.let { pinEntryView.setScramble(it) }
        prefsUtil.haptics?.let { pinEntryView.isHapticFeedbackEnabled = it }
        pinEntryView.setEntryListener { key, _ ->
            if (userInput.length <= AccessFactory.MAX_PIN_LENGTH - 1) {
                userInput = userInput.append(key)
                if (userInput.length >= AccessFactory.MIN_PIN_LENGTH) {
                    pinEntryView.showCheckButton()
                } else {
                    pinEntryView.hideCheckButton()
                }
                setPinMaskView()
            }
        }
        pinEntryView.setClearListener { clearType ->
            if (clearType === PinEntryView.KeyClearTypes.CLEAR) {
                if (userInput.isNotEmpty()) userInput = java.lang.StringBuilder(userInput.substring(0, userInput.length - 1))
                if (userInput.length >= AccessFactory.MIN_PIN_LENGTH) {
                    pinEntryView.showCheckButton()
                } else {
                    pinEntryView.hideCheckButton()
                }
            } else {
                strPassphrase = ""
                userInput = java.lang.StringBuilder()
                pinEntryMaskLayout.removeAllViews()
                pinEntryView.hideCheckButton()
            }
            setPinMaskView()
        }
    }

    public fun setOnPinEntered(callback: ((String) -> Unit)) {
        this.onConfirm = callback
    }

    override fun onStart() {
        super.onStart()

    }


    private fun setPinMaskView() {
        if (userInput.length > pinEntryMaskLayout.childCount && userInput.isNotEmpty()) {
            val image = ImageView(requireContext())
            image.setImageDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.circle_dot_white))
            image.drawable.setColorFilter(Color.WHITE, PorterDuff.Mode.ADD)
            val params = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.setMargins(8, 0, 8, 0)
            TransitionManager.beginDelayedTransition(pinEntryMaskLayout, ChangeBounds().setDuration(50))
            pinEntryMaskLayout.addView(image, params)
        } else {
            if (pinEntryMaskLayout.childCount != 0) {
                TransitionManager.beginDelayedTransition(pinEntryMaskLayout, ChangeBounds().setDuration(200))
                pinEntryMaskLayout.removeViewAt(pinEntryMaskLayout.childCount - 1)
            }
        }

    }

    fun showError() {
        val errorShake = TranslateAnimation(0F, 12F, 0F, 0F)
        errorShake.duration = 420
        errorShake.interpolator = CycleInterpolator(4F)
        pinEntryMaskLayout.startAnimation(errorShake)
        if (prefsUtil.haptics!!)
            pinEntryMaskLayout.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
    }
}