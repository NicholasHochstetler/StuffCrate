/* Copyright 2020 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracermerchant.bootycrate

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.Toolbar
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.core.content.res.getResourceIdOrThrow
import androidx.core.graphics.withClip
import androidx.core.view.doOnNextLayout
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.shape.EdgeTreatment
import com.google.android.material.shape.MaterialShapeDrawable
import com.google.android.material.shape.ShapeAppearanceModel
import com.google.android.material.shape.ShapePath
import kotlin.math.atan

/**
 * A custom toolbar that has a cutout in its top edge to hold the contents of a layout.
 *
 * BottomAppBar functions mostly as a regular Toolbar, except that its cus-
 * tom CradleTopEdgeTreatment used on its top edge gives it a cutout in
 * its shape that can be used to hold the contents of a layout. The layout
 * in question must be referenced by the XML attribute cradleLayoutResId,
 * must be a sibling of the BottomAppBar, and both must have a Coordinator-
 * Layout parent.
 *
 * Like the Material library BottomAppBar, BottomAppBar manages its own
 * background. In order to tint the background a solid color, the property
 * backgroundTint can be set in XML or programmatically to an int color
 * code.
 */
open class BottomAppBar(context: Context, attrs: AttributeSet) : Toolbar(context, attrs) {
    enum class CradleAlignmentMode { Start, Center, End }
    val cradleAlignmentMode: CradleAlignmentMode
    var cradleWidth = 0
        set(value) { field = value
                     materialShapeDrawable.invalidateSelf() }
    var cradleDepth: Int
    var cradleTopCornerRadius: Int
    var cradleBottomCornerRadius: Int
    var cradleStartEndMargin: Int
    var cradleContentsMargin: Int

    private val materialShapeDrawable = MaterialShapeDrawable()
    protected val outlinePath = Path()
    protected val topEdgePath = Path()

    val backgroundPaint = Paint().apply { style = Paint.Style.FILL }
    var backgroundTint: Int get() = backgroundPaint.color
                            set(value) { backgroundPaint.color = value }

    private val arcQuarter = 90f
    private val angleDown = 90f
    private val angleUp = 270f

    init {
        val a = context.obtainStyledAttributes(attrs, R.styleable.BottomAppBar)
        cradleAlignmentMode = CradleAlignmentMode.values()[
            a.getInt(R.styleable.BottomAppBar_cradleAlignmentMode, CradleAlignmentMode.Center.ordinal)]
        cradleDepth = a.getDimensionPixelOffset(R.styleable.BottomAppBar_cradleDepth, 0)
        cradleTopCornerRadius = a.getDimensionPixelOffset(R.styleable.BottomAppBar_cradleTopCornerRadius, 0)
        cradleBottomCornerRadius = a.getDimensionPixelOffset(R.styleable.BottomAppBar_cradleBottomCornerRadius, 0)
        cradleStartEndMargin = a.getDimensionPixelOffset(R.styleable.BottomAppBar_cradleStartEndMargin, 90)
        cradleContentsMargin = a.getDimensionPixelOffset(R.styleable.BottomAppBar_cradleContentsMargin, 0)
        val cradleLayoutResId = a.getResourceIdOrThrow(R.styleable.BottomAppBar_cradleLayoutResId)
        backgroundTint = a.getColor(R.styleable.BottomAppBar_backgroundTint,
                                    ContextCompat.getColor(context, android.R.color.white))
        a.recycle()

        materialShapeDrawable.shapeAppearanceModel =
            ShapeAppearanceModel.builder().setTopEdge(CradleTopEdgeTreatment()).build()
        background = materialShapeDrawable
        @Suppress("LeakingThis") setWillNotDraw(false)

        doOnNextLayout {
            val cradleLayout = (parent as ViewGroup).findViewById<ViewGroup>(cradleLayoutResId)
            prepareCradleLayout(cradleLayout)
        }
    }

    override fun onDraw(canvas: Canvas?) {
        if (canvas == null) return
        canvas.drawPath(outlinePath, backgroundPaint)
    }

    private fun prepareCradleLayout(cradleLayout: ViewGroup) {
        if (cradleLayout.parent !is CoordinatorLayout)
            throw IllegalStateException("The cradle layout should have a CoordinatorLayout as a parent.")

        val wrapContentSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
        (cradleLayout.layoutParams as CoordinatorLayout.LayoutParams).apply {
            when (cradleAlignmentMode) {
                CradleAlignmentMode.Start -> {
                    gravity = Gravity.BOTTOM or Gravity.START
                    marginStart = cradleStartEndMargin + cradleContentsMargin
                } CradleAlignmentMode.Center -> {
                    gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
                    measure(wrapContentSpec, wrapContentSpec)
                    bottomMargin = measuredHeight + cradleContentsMargin - cradleDepth
                } CradleAlignmentMode.End -> {
                    gravity = Gravity.BOTTOM or Gravity.END
                    marginEnd = cradleStartEndMargin + cradleContentsMargin
                }
            }
        }
        cradleLayout.clipChildren = false
        cradleLayout.measure(wrapContentSpec, wrapContentSpec)
        cradleWidth = cradleLayout.measuredWidth
        materialShapeDrawable.invalidateSelf()
    }

    /** An EdgeTreatment used to draw a cradle cutout for the BottomAppBar.
     *
     *  CradleTopEdgeTreatment's getEdgePath draws a cutout shape for the
     *  BottomAppBar to hold the contents of the BottomAppBar's cradle lay-
     *  out. CradleTopEdgeTreatment does not support the interpolation fea-
     *  ture of EdgeTreatment. */
    inner class CradleTopEdgeTreatment : EdgeTreatment() {
        override fun getEdgePath(
            length: Float,
            center: Float,
            interpolation: Float,
            shapePath: ShapePath
        ) {
            val cradleFullWidth = cradleWidth + 2 * cradleContentsMargin
            // start will be the x coordinate of the start of the cradle if cradleTopCornerRadius is zero
            val start = when (cradleAlignmentMode) {
                CradleAlignmentMode.Start ->  left + cradleStartEndMargin
                CradleAlignmentMode.Center -> left + (width - cradleFullWidth) / 2
                CradleAlignmentMode.End ->    right - cradleStartEndMargin - cradleFullWidth
            }.toFloat()
            val end = start + cradleFullWidth

            // If the top and bottom corner radii combined are less than the cradle depth,
            // there will be vertical sides to the cradle. Conversely, if the top and bottom
            // corner radii add up to more than the cradle depth, then the sweep angles of
            // the top and bottom corners will be less than the full ninety degrees.
            val cradleVerticalSideLength = cradleDepth - cradleTopCornerRadius -
                                           cradleBottomCornerRadius.toFloat()
            val topCornerArc = if (cradleVerticalSideLength < 0) {
                val distanceX = cradleTopCornerRadius
                val distanceY = -cradleVerticalSideLength.toDouble()
                val arcOffset = Math.toDegrees(atan(distanceY / distanceX))
                (arcQuarter - arcOffset.toFloat()).coerceAtMost(90f)
            } else arcQuarter
            val bottomCornerArc = if (cradleVerticalSideLength < 0) {
                val distanceX = cradleBottomCornerRadius
                val distanceY = -cradleVerticalSideLength.toDouble()
                val arcOffset = Math.toDegrees(atan(distanceY / distanceX))
                (arcOffset.toFloat() - arcQuarter).coerceAtLeast(-90f)
            } else -arcQuarter

            shapePath.apply {
                lineTo(start - cradleTopCornerRadius, 0f)
                addArc(/*left*/       start - 2 * cradleTopCornerRadius,
                       /*top*/        0f,
                       /*right*/      start,
                       /*bottom*/     2f * cradleTopCornerRadius,
                       /*startAngle*/ angleUp,
                       /*sweepAngle*/ topCornerArc)
                if (cradleVerticalSideLength > 0)
                    lineTo(start, cradleDepth - cradleBottomCornerRadius.toFloat())
                addArc(/*left*/       start,
                       /*top*/        cradleDepth - 2f * cradleBottomCornerRadius,
                       /*right*/      start + 2 * cradleBottomCornerRadius,
                       /*bottom*/     cradleDepth.toFloat(),
                       /*startAngle*/ angleDown + topCornerArc,
                       /*sweepAngle*/ bottomCornerArc)
                lineTo(end - cradleBottomCornerRadius, cradleDepth.toFloat())
                addArc(/*left*/       end - 2 * cradleBottomCornerRadius,
                       /*top*/        cradleDepth - 2f * cradleBottomCornerRadius,
                       /*right*/      end,
                       /*bottom*/     cradleDepth.toFloat(),
                       /*startAngle*/ angleDown,
                       /*sweepAngle*/ bottomCornerArc)
                if (cradleVerticalSideLength > 0)
                    lineTo(end, cradleDepth - cradleBottomCornerRadius.toFloat())
                addArc(/*left*/       end,
                       /*top*/        0f,
                       /*right*/      end + 2 * cradleTopCornerRadius,
                       /*bottom*/     2f * cradleTopCornerRadius,
                       /*startAngle*/ angleUp + bottomCornerArc,
                       /*sweepAngle*/ topCornerArc)
                lineTo(width.toFloat(), 0f)
            }
            // Copy the shapePath to topEdgePath and outlinePath, and additionally
            // finish the outlinePath so that it goes all the way around the view.
            topEdgePath.rewind()
            shapePath.applyToPath(Matrix(), topEdgePath)
            outlinePath.set(topEdgePath)
            outlinePath.lineTo(length, height.toFloat())
            outlinePath.lineTo(0f, height.toFloat())
            outlinePath.close()
        }
    }
}

/**
 * A BottomAppBar that also draws an indicator above the selected navigation bar item.
 *
 * BottomAppBarWithIndicator extends BottomAppBar by also drawing an indi-
 * cator, either with the solid color described by the @property indicator-
 * Color, or with the shader object described by the @property indicator-
 * Gradient if set. The indicator can be moved to be above a given nav bar
 * menu item by calling the function moveIndicatorToNavBarItem with the id
 * of the menu item. The BottomNavigationView must be referenced through
 * the XML attribute navigationBarResId, and must be a descendant of the
 * BottomAppBarWithIndicator. The XML attributes indicatorThickness and
 * indicatorWidth are used to define the dimensions of the indicator.
 */
open class BottomAppBarWithIndicator(context: Context, attrs: AttributeSet) :
    BottomAppBar(context, attrs)
{
    protected lateinit var navBar: BottomNavigationView
    val indicatorPaint = Paint().apply { style = Paint.Style.STROKE }
    var indicatorWidth = 0
    var indicatorThickness get() = indicatorPaint.strokeWidth
                           set(value) { indicatorPaint.strokeWidth = value }
    var indicatorColor: Int get() = indicatorPaint.color
                            set(value) { indicatorPaint.color = value }
    private var indicatorXPos = 0
    var indicatorAnimatorConfig: AnimatorConfig? = null

    init {
        val a = context.obtainStyledAttributes(attrs, R.styleable.BottomAppBarWithIndicator)
        indicatorThickness = a.getDimension(R.styleable.BottomAppBarWithIndicator_indicatorThickness, 0f)
        indicatorWidth = a.getDimensionPixelOffset(R.styleable.BottomAppBarWithIndicator_indicatorWidth, 0)
        indicatorPaint.color = a.getColor(R.styleable.BottomAppBarWithIndicator_indicatorColor,
                                          ContextCompat.getColor(context, android.R.color.black))
        val navViewResId = a.getResourceIdOrThrow(R.styleable.BottomAppBarWithIndicator_navigationBarResId)
        a.recycle()

        setWillNotDraw(false)
        doOnNextLayout {
            navBar = findViewById(navViewResId)
            moveIndicatorToNavBarItem(navBar.selectedItemId, animate = false)
        }
    }

    private val rect = Rect()
    /** Move the indicator to be above the item with id equal to @param menuItemId,
     * animating the change if @param animate is equal to true. */
    fun moveIndicatorToNavBarItem(menuItemId: Int, animate: Boolean = true) {
        if (!::navBar.isInitialized) return
        navBar.findViewById<View>(menuItemId)?.let {
            // Using it.centerX() instead of getting the global visible rect's centerX
            // seems to result in a slightly incorrect value when in landscape mode.
            it.getGlobalVisibleRect(rect)
            val newIndicatorXPos = rect.centerX() - indicatorWidth / 2
            if (!animate) indicatorXPos = newIndicatorXPos
            else ValueAnimator.ofInt(indicatorXPos, newIndicatorXPos).apply {
                addUpdateListener { indicatorXPos = animatedValue as Int; invalidate() }
                applyConfig(indicatorAnimatorConfig)
            }.start()
        }
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        canvas?.withClip(indicatorXPos, 0, indicatorXPos + indicatorWidth, bottom) {
            drawPath(topEdgePath, indicatorPaint) }
    }
}