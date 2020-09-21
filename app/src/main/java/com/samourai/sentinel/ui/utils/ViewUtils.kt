package com.samourai.sentinel.ui.utils

import android.accessibilityservice.AccessibilityServiceInfo
import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.view.View
import android.view.accessibility.AccessibilityManager
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import androidx.annotation.IdRes
import androidx.annotation.IntDef
import androidx.annotation.IntRange
import androidx.cardview.widget.CardView
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat.getSystemService
import androidx.dynamicanimation.animation.DynamicAnimation
import androidx.dynamicanimation.animation.SpringAnimation
import androidx.dynamicanimation.animation.SpringForce
import com.google.android.material.behavior.SwipeDismissBehavior
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.snackbar.Snackbar.SnackbarLayout
import com.samourai.sentinel.R
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy
import java.util.*


/**
 * sentinel-android
 *
 * @author Sarath
 */

@IntDef(BaseTransientBottomBar.LENGTH_INDEFINITE, BaseTransientBottomBar.LENGTH_SHORT, BaseTransientBottomBar.LENGTH_LONG)
@IntRange(from = 1)
@Retention(RetentionPolicy.SOURCE)
annotation class Duration

class MultiSpringEndListener(
        onEnd: (Boolean) -> Unit,
        vararg springs: SpringAnimation
) {
    private val listeners = ArrayList<DynamicAnimation.OnAnimationEndListener>(springs.size)

    private var wasCancelled = false

    init {
        springs.forEach {
            val listener = object : DynamicAnimation.OnAnimationEndListener {
                override fun onAnimationEnd(
                        animation: DynamicAnimation<out DynamicAnimation<*>>?,
                        canceled: Boolean,
                        value: Float,
                        velocity: Float
                ) {
                    animation?.removeEndListener(this)
                    wasCancelled = wasCancelled or canceled
                    listeners.remove(this)
                    if (listeners.isEmpty()) {
                        onEnd(wasCancelled)
                    }
                }
            }
            it.addEndListener(listener)
            listeners.add(listener)
        }
    }
}

fun listenForAllSpringsEnd(
        onEnd: (Boolean) -> Unit,
        vararg springs: SpringAnimation
) = MultiSpringEndListener(onEnd, *springs)


fun View.hideKeyboard() {
    val imm: InputMethodManager = context.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
    imm.hideSoftInputFromWindow(windowToken, 0)
}


fun View.spring(
        property: DynamicAnimation.ViewProperty,
        stiffness: Float = 500f,
        damping: Float = SpringForce.DAMPING_RATIO_NO_BOUNCY,
        startVelocity: Float? = null
): SpringAnimation {
    val key = getKey(property)
    var springAnim = getTag(key) as? SpringAnimation?
    if (springAnim == null) {
        springAnim = SpringAnimation(this, property)
        setTag(key, springAnim)
    }
    springAnim.spring = (springAnim.spring ?: SpringForce()).apply {
        this.dampingRatio = damping
        this.stiffness = stiffness
    }
    startVelocity?.let { springAnim.setStartVelocity(it) }
    return springAnim
}


fun Activity.showFloatingSnackBar(parent_view: View, text: String = "", actionClick: (() -> Unit)? = null,
                                  actionText: String = "Undo",
                                  @IdRes anchorView: Int? = null,
                                  @Duration duration: Int = Snackbar.LENGTH_LONG) {

    val snackBar = Snackbar.make(parent_view, "", duration)
            .apply {
                applySnackBarAnimationFix()
            }
    val view: View = this.layoutInflater.inflate(R.layout.snackbar_floating, null)
    snackBar.view.setBackgroundColor(Color.TRANSPARENT)
    val snackBarView = snackBar.view as SnackbarLayout
    snackBarView.setPadding(0, 0, 0, 0)
    snackBarView.alpha = 0.96f
    val mainTextTv = view.findViewById<TextView>(R.id.mainText)
    val separator = view.findViewById<View>(R.id.separator)
    val undoTv = view.findViewById<TextView>(R.id.actionButton)
    undoTv.text = actionText
    val swipe: SwipeDismissBehavior<CoordinatorLayout> = SwipeDismissBehavior()
    swipe.setSwipeDirection(SwipeDismissBehavior.SWIPE_DIRECTION_ANY)
    swipe.listener = object : SwipeDismissBehavior.OnDismissListener {
        override fun onDismiss(view: View) {
            snackBar.dismiss()
        }

        override fun onDragStateChanged(state: Int) {
        }
    }
    val coordinatorParams: CoordinatorLayout.LayoutParams? = view.findViewById<CardView>(R.id.snackBarCard).layoutParams as CoordinatorLayout.LayoutParams?
    coordinatorParams?.behavior = swipe

    if (text.isNotBlank()) {
        mainTextTv.text = text
    } else {
        mainTextTv.visibility = View.GONE
    }
    if (actionClick != null) {
        snackBar.duration = Snackbar.LENGTH_INDEFINITE
        view.findViewById<View>(R.id.actionButton).setOnClickListener {
            snackBar.dismiss()
            actionClick.invoke()
        }
    } else {
        undoTv.visibility = View.GONE
        separator.visibility = View.GONE
    }

    snackBarView.addView(view, 0)
    if(anchorView!=null){
        snackBar.setAnchorView(anchorView)
    }
    snackBar.show()

}


@IdRes
private fun getKey(property: DynamicAnimation.ViewProperty): Int {
    return when (property) {
        SpringAnimation.TRANSLATION_X -> R.id.translation_x
        SpringAnimation.TRANSLATION_Y -> R.id.translation_y
        SpringAnimation.TRANSLATION_Z -> R.id.translation_z
        SpringAnimation.SCALE_X -> R.id.scale_x
        SpringAnimation.SCALE_Y -> R.id.scale_y
        SpringAnimation.ROTATION -> R.id.rotation
        SpringAnimation.ROTATION_X -> R.id.rotation_x
        SpringAnimation.ROTATION_Y -> R.id.rotation_y
        SpringAnimation.X -> R.id.x
        SpringAnimation.Y -> R.id.y
        SpringAnimation.Z -> R.id.z
        SpringAnimation.ALPHA -> R.id.alpha
        SpringAnimation.SCROLL_X -> R.id.scroll_x
        SpringAnimation.SCROLL_Y -> R.id.scroll_y
        else -> throw IllegalAccessException("Unknown ViewProperty: $property")
    }
}


fun Snackbar.applySnackBarAnimationFix(){

    val manager: AccessibilityManager = this.context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
    val shouldForceAnimate = !manager.isEnabled && manager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_SPOKEN).size == 0

    if (shouldForceAnimate) {
        try {
            val accManagerField = BaseTransientBottomBar::class.java.getDeclaredField("mAccessibilityManager")
            accManagerField.isAccessible = true
            val accManager = accManagerField.get(this)
            AccessibilityManager::class.java.getDeclaredField("mIsEnabled").apply {
                isAccessible = true
                setBoolean(accManager, false)
            }
            accManagerField.set(this, accManager)
        } catch (e: Exception) {
        }
    }
}