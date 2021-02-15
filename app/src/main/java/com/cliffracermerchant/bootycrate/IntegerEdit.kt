/* Copyright 2020 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracermerchant.bootycrate

import android.content.Context
import android.graphics.Paint
import android.util.AttributeSet
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.inputmethod.EditorInfo
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.android.synthetic.main.integer_edit.view.*

/** A compound view to edit an integer quantity.
 *
 *  IntegerEdit is a compound view that combines an EditText displaying an
 *  integer with two image buttons on its left and right to act as decrease and
 *  increase buttons. IntegerEdit provides a publicly accessible LiveData mem-
 *  ber to allow external entities to react to a change in its data.
 *
 *  The EditText is by default not focusable in touch mode, but this can be
 *  changed by setting the member isEditable. If set to true, the user can edit
 *  the value directly (rather than through the use of the decrease / increase
 *  buttons). When in the editable state, the EditText will underline the cur-
 *  rent value to indicate this to the user, and will, if necessary, expand the
 *  width of the value to R.dimen.integer_edit_editable_value_min_width to make
 *  it a larger touch target. */
class IntegerEdit(context: Context, attrs: AttributeSet?) : ConstraintLayout(context, attrs) {

    // this.value = this.value is not pointless due
    // to value's custom getter and setter
    private var _minValue = 0
    var minValue get() = _minValue
                 set(value) { _minValue = value; this.value = this.value }
    private var _maxValue = 0
    var maxValue get() = _maxValue
                 set(value) { _maxValue = value; this.value = this.value }
    var stepSize: Int

    private var _value get() = try { valueEdit.text.toString().toInt() }
                               catch (e: Exception) { 0 }
                       set(value) { val newValue = value.coerceIn(minValue, maxValue)
                                    valueEdit.setText(newValue.toString()) }
    var value get() = _value
              set(newValue) { _value = newValue
                              _liveData.value = value }

    var valueIsDirectlyEditable get() = valueEdit.isFocusableInTouchMode
        set(editable) = setValueIsDirectlyEditable(editable, animate = true)

    private val _liveData = MutableLiveData(value)
    val liveData: LiveData<Int> get() = _liveData

    private val inputMethodManager = inputMethodManager(context)
    private val interpolator = AccelerateDecelerateInterpolator()

    init {
        LayoutInflater.from(context).inflate(R.layout.integer_edit, this, true)
        var a = context.obtainStyledAttributes(attrs, R.styleable.IntegerEdit)
        _value = a.getInt(R.styleable.IntegerEdit_initialValue, 0)
        _minValue = a.getInt(R.styleable.IntegerEdit_minValue, 0)
        _maxValue = a.getInt(R.styleable.IntegerEdit_maxValue, 99)
        stepSize = a.getInt(R.styleable.IntegerEdit_stepSize, 1)
        setValueIsDirectlyEditable(
            a.getBoolean(R.styleable.IntegerEdit_valueIsDirectlyEditable, false),
            animate = false)

        a = context.obtainStyledAttributes(attrs, intArrayOf(android.R.attr.textSize))
        valueEdit.setTextSize(TypedValue.COMPLEX_UNIT_PX, a.getDimension(0, 0f))
        a.recycle()

        decreaseButton.setOnClickListener { decrement() }
        increaseButton.setOnClickListener { increment() }
        valueEdit.setOnEditorActionListener{ _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                clearFocus()
                inputMethodManager?.hideSoftInputFromWindow(windowToken, 0)
                value = value // To enforce min/max value
            }
            actionId == EditorInfo.IME_ACTION_DONE
        }
        valueEdit.setOnFocusChangeListener { _, focused ->
            if (!focused) { value = value } // To enforce min/max value
        }
    }

    fun initValue(newValue: Int) { _value = newValue }
    fun increment() = modifyValue(stepSize)
    fun decrement() = modifyValue(-stepSize)
    private fun modifyValue(stepSize: Int) { value += stepSize }

    fun setValueIsDirectlyEditable(editable: Boolean, animate: Boolean = true) {
        valueEdit.isFocusableInTouchMode = editable
        if (!editable && valueEdit.isFocused)
            valueEdit.clearFocus()
        if (editable) valueEdit.paintFlags = valueEdit.paintFlags or Paint.UNDERLINE_TEXT_FLAG
        else          valueEdit.paintFlags = valueEdit.paintFlags and Paint.UNDERLINE_TEXT_FLAG.inv()

        val oldWidth = valueEdit.width
        valueEdit.minWidth = if (!editable) 0 else
            resources.getDimensionPixelSize(R.dimen.integer_edit_editable_value_min_width)
        if (!animate || oldWidth == 0) return

        /* The following animations will make the IntegerEdit smoothly expand
         * to its new total width after the change in the value edit's min-
         * Width (layout transitions apparently do not handle this). */
        val wrapContentSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
        valueEdit.measure(wrapContentSpec, wrapContentSpec)
        val newWidth = valueEdit.measuredWidth
        val widthChange = newWidth - oldWidth
        valueEdit.translationX = -widthChange / 2f
        increaseButton.translationX = -widthChange.toFloat()
        valueEdit.animate().translationX(0f).setDuration(300)
            .withLayer().setInterpolator(interpolator).start()
        increaseButton.animate().translationX(0f).setDuration(300)
            .withLayer().setInterpolator(interpolator).start()
    }
}
