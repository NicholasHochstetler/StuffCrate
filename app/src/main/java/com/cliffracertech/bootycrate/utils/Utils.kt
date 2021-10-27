/* Copyright 2021 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracertech.bootycrate.utils

import android.app.Activity
import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.content.res.Resources
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.LinearLayout
import com.cliffracertech.bootycrate.R
import com.google.android.material.bottomsheet.BottomSheetBehavior

/** Return a NotificationManager system service from the context. */
fun notificationManager(context: Context) =
    context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
/** Return an AlarmManager system service from the context. */
fun alarmManager(context: Context) =
        context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager

/** An object that, once initialized by calling init with an instance of Context,
 * can be used to either hide or show the soft input given a view instance using
 * the functions hide and show, and showWithDelay. */
object SoftKeyboard {
    private lateinit var imm: InputMethodManager
    fun init(context: Context) {
        imm = context.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
    }
    fun hide(view: View) = imm.hideSoftInputFromWindow(view.windowToken, 0)
    fun show(view: View) = imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
    /** Show the soft input after a given delay. which is useful when the soft input
     * should appear alongside a popup alert dialog (for some reason, requesting the
     * soft input to show at the same time as the dialog does not work). */
    fun showWithDelay(view: View, delay: Long = 50L) {
        view.handler.postDelayed({
            view.requestFocus()
            imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
        }, delay)
    }
}

fun View.setHeight(height: Int) { bottom = top + height }

/** Return the provided dp amount in terms of pixels. */
fun Resources.dpToPixels(dp: Float) =
    TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, displayMetrics)

/** Return the provided sp amount in terms of pixels. */
fun Resources.spToPixels(sp: Float) =
    TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, displayMetrics)

/** Return the provided dp amount in terms of pixels. */
fun Context.dpToPixels(dp: Float) = resources.dpToPixels(dp)
/** Return the provided sp amount in terms of pixels. */
fun Context.spToPixels(sp: Float) = resources.spToPixels(sp)

private val typedValue = TypedValue()
/** Resolve the current theme's value for the provided int attribute. */
fun Resources.Theme.resolveIntAttribute(attr: Int): Int {
    resolveAttribute(attr, typedValue, true)
    return typedValue.data
}

/** Return the IntArray pointed to by @param arrayResId, resolving theme attributes if necessary. */
fun Context.getIntArray(arrayResId: Int): IntArray {
    val ta = resources.obtainTypedArray(arrayResId)
    val array = IntArray(ta.length()) {
        if (ta.peekValue(it).type == TypedValue.TYPE_ATTRIBUTE)
            theme.resolveIntAttribute(ta.peekValue(it).data)
        else ta.getColor(it, 0)
    }
    ta.recycle()
    return array
}

/** Add the nullable element to the list if it is not null, or do nothing otherwise. */
fun <T> MutableList<T>.add(element: T?) { if (element != null) add(element) }

val <T: View>BottomSheetBehavior<T>.isExpanded get() = state == BottomSheetBehavior.STATE_EXPANDED
val <T: View>BottomSheetBehavior<T>.isCollapsed get() = state == BottomSheetBehavior.STATE_COLLAPSED
val <T: View>BottomSheetBehavior<T>.isDragging get() = state == BottomSheetBehavior.STATE_DRAGGING
val <T: View>BottomSheetBehavior<T>.isSettling get() = state == BottomSheetBehavior.STATE_SETTLING
val <T: View>BottomSheetBehavior<T>.isHidden get() = state == BottomSheetBehavior.STATE_HIDDEN

/** Perform the given block without the caller's LayoutTransition instance.
 * This is useful when changes need to be made instantaneously. */
fun ViewGroup.withoutLayoutTransition(block: () -> Unit) {
    val layoutTransitionBackup = layoutTransition
    layoutTransition = null
    block()
    layoutTransition = layoutTransitionBackup
}

/** A LinearLayout that allows settings a max height with the XML attribute maxHeight. */
class MaxHeightLinearLayout(context: Context, attrs: AttributeSet) : LinearLayout(context, attrs) {
    var maxHeight = -1
        set(value) { field = value; invalidate() }

    init {
        val a = context.obtainStyledAttributes(attrs, R.styleable.MaxHeightLinearLayout)
        maxHeight = a.getDimensionPixelSize(R.styleable.MaxHeightLinearLayout_maxHeight, -1)
        a.recycle()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val maxHeightSpec = MeasureSpec.makeMeasureSpec(maxHeight, MeasureSpec.AT_MOST)
        super.onMeasure(widthMeasureSpec, maxHeightSpec)
    }
}