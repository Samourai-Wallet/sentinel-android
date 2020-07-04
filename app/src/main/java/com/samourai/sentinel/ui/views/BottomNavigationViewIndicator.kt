package com.samourai.sentinel.ui.views

import android.animation.AnimatorSet
import android.animation.ObjectAnimator.ofInt
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.LOLLIPOP
import android.util.AttributeSet
import android.util.Log
import android.view.MenuItem
import android.view.View
import androidx.annotation.Keep
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import com.google.android.material.bottomnavigation.BottomNavigationMenuView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.samourai.sentinel.R
import com.samourai.sentinel.R.styleable.*

/**
 * sentinel-android
 *
 * @author Sarath on 20/06/20
 */
class BottomNavigationViewIndicator @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val targetId: Int
    private var target: BottomNavigationMenuView? = null

    private var rect = Rect()
    private val backgroundDrawable: Drawable

    private var index = 0
    private var animator: AnimatorSet? = null

    init {
        if (attrs == null) {
            targetId = NO_ID
            backgroundDrawable = ColorDrawable(Color.TRANSPARENT)
        } else {
            with(context.obtainStyledAttributes(attrs, BottomNavigationViewIndicator)) {
                targetId = getResourceId(BottomNavigationViewIndicator_targetBottomNavigation, NO_ID)
                backgroundDrawable = getDrawable(BottomNavigationViewIndicator_clipableBackground)!!
                recycle()
            }
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (targetId == NO_ID) return attachedError("invalid target id $targetId, did you set the app:targetBottomNavigation attribute?")
        val parentView = parent as? View
                ?: return attachedError("Impossible to find the view using $parent")
        val child = parentView.findViewById<View?>(targetId)
        if (child !is ListenableBottomNavigationView) return attachedError("Invalid view $child, the app:targetBottomNavigation has to be n ListenableBottomNavigationView")
        for (i in 0 until child.childCount) {
            val subView = child.getChildAt(i)
            if (subView is BottomNavigationMenuView) target = subView
        }
        if (SDK_INT >= LOLLIPOP) elevation = child.elevation
        child.addOnNavigationItemSelectedListener { updateRectByIndex(it, true) }
        post { updateRectByIndex(index, false) }
    }

    private fun attachedError(message: String) {
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        target = null
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.clipRect(rect)
        backgroundDrawable.draw(canvas)
    }

    private fun updateRectByIndex(index: Int, animated: Boolean) {
        this.index = index
        target?.apply {
            if (childCount < 1 || index >= childCount) return
            val reference = getChildAt(index)

            val start = reference.left + left
            val end = reference.right + left

            backgroundDrawable.setBounds(left, top, right, bottom)
            val newRect = Rect(start, 0, end, height)
            if (animated) startUpdateRectAnimation(newRect) else updateRect(newRect)
        }
    }

    private fun startUpdateRectAnimation(rect: Rect) {
        animator?.cancel()
        animator = AnimatorSet().also {
            it.playTogether(
                    ofInt(this, "rectLeft", this.rect.left, rect.left),
                    ofInt(this, "rectRight", this.rect.right, rect.right),
                    ofInt(this, "rectTop", this.rect.top, rect.top),
                    ofInt(this, "rectBottom", this.rect.bottom, rect.bottom)
            )
            it.interpolator = FastOutSlowInInterpolator()
            it.duration = resources.getInteger(android.R.integer.config_shortAnimTime).toLong()
            it.start()
        }
    }

    private fun updateRect(rect: Rect) {
        this.rect = rect
        postInvalidate()
    }

    @Keep
    fun setRectLeft(left: Int) = updateRect(rect.apply { this.left = left })
    @Keep
    fun setRectRight(right: Int) = updateRect(rect.apply { this.right = right })
    @Keep
    fun setRectTop(top: Int) = updateRect(rect.apply { this.top = top })
    @Keep
    fun setRectBottom(bottom: Int) = updateRect(rect.apply { this.bottom = bottom })

}

/**
 * Custom bottom nav
 */
class ListenableBottomNavigationView @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : BottomNavigationView(context, attrs, defStyleAttr), BottomNavigationView.OnNavigationItemSelectedListener {

    private val onNavigationItemSelectedListeners = mutableListOf<OnNavigationItemSelectedListener>()

    init {
        super.setOnNavigationItemSelectedListener(this)
    }

    override fun setOnNavigationItemSelectedListener(listener: OnNavigationItemSelectedListener?) {
        if (listener != null) addOnNavigationItemSelectedListener(listener)
    }

    private fun addOnNavigationItemSelectedListener(listener: OnNavigationItemSelectedListener) {
        onNavigationItemSelectedListeners.add(listener)
    }

    fun addOnNavigationItemSelectedListener(listener: (Int) -> Unit) {
        addOnNavigationItemSelectedListener(OnNavigationItemSelectedListener {
            for (i in 0 until menu.size()) if (menu.getItem(i) == it) listener(i)
            false
        })
    }

    override fun onNavigationItemSelected(item: MenuItem) = onNavigationItemSelectedListeners
            .map { it.onNavigationItemSelected(item) }
            .fold(false) { acc, it -> acc || it }

}