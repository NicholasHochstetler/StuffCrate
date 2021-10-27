/* Copyright 2021 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracertech.bootycrate.activity

import android.animation.Animator
import android.animation.ValueAnimator
import android.content.res.ColorStateList
import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.view.ViewGroup
import androidx.core.animation.doOnEnd
import androidx.core.content.ContextCompat
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import com.cliffracertech.bootycrate.R
import com.cliffracertech.bootycrate.databinding.MainActivityBinding
import com.cliffracertech.bootycrate.utils.*
import com.cliffracertech.bootycrate.view.ActionBarTitle
import com.cliffracertech.bootycrate.view.GradientVectorDrawable
import com.google.android.material.bottomsheet.BottomSheetBehavior
import kotlin.math.abs

/** Return the appropriate target BottomAppBar.cradle.width value for a checkout
 * button visibility matching the value of @param showingCheckoutButton. */
private fun MainActivityBinding.cradleWidth(showingCheckoutButton: Boolean) =
    if (!showingCheckoutButton)
        addButton.layoutParams.width.toFloat()
    else {
        val wrapContent = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        cradleLayout.measure(wrapContent, wrapContent)
        cradleLayout.measuredWidth.toFloat()
    }

private val checkoutButtonClipBounds = Rect()
/** Show or hide the checkout button according to the value of @param
 * showing, returning the animator used if @param animate == true or
 * null otherwise, while using the values of the @param animatorConfig
 * as the animation's duration and interpolator if not null.*/
fun MainActivityBinding.showCheckoutButton(
    showing: Boolean,
    animatorConfig: AnimatorConfig? = null,
    animate: Boolean = true
): Animator? {
    if (!animate || checkoutButton.isVisible == showing) {
        cradleLayout.withoutLayoutTransition { checkoutButton.isVisible = showing }
        bottomAppBar.cradle.width = cradleWidth(showing)
        return null
    }
    checkoutButton.isVisible = showing
    val cradleNewWidth = cradleWidth(showing)

    // Ideally we would only animate the cradle's width, and let the cradleLayout's
    // layoutTransition handle the rest. Unfortunately it will only animate its own
    // translationX if it has animateParentHierarchy set to true. Due to this
    // causing the BottomNavigationDrawer to jump to the top of the screen whenever
    // the layoutTransition animates (apparently a bug with BottomSheetBehavior),
    // we have to animate the cradleLayout's translationX ourselves.
    val cradleNewLeft = (bottomAppBar.width - cradleNewWidth) / 2
    val cradleStartTranslationX = cradleLayout.left - cradleNewLeft

    // The checkoutButton's clipBounds is set here to prevent it's right edge
    // from sticking out underneath the addButton during the animation
    checkoutButton.getDrawingRect(checkoutButtonClipBounds)
    val addButtonHalfWidth = addButton.width / 2

    return ValueAnimator.ofFloat(bottomAppBar.cradle.width, cradleNewWidth).apply {
        applyConfig(animatorConfig)
        addUpdateListener {
            cradleLayout.translationX = cradleStartTranslationX * (1f - it.animatedFraction)
            bottomAppBar.cradle.width = it.animatedValue as Float
            bottomAppBar.invalidate()
            checkoutButtonClipBounds.right = addButton.x.toInt() + addButtonHalfWidth
            checkoutButton.clipBounds = checkoutButtonClipBounds
        }
        doOnEnd { checkoutButton.clipBounds = null }
    }
}

/**
 * Style the contents of the actionBar and bottomAppBar members to match their background gradients.
 *
 * Unfortunately many of the desired aspects of the app's style (e.g. the menu
 * item icons being tinted to match the gradient background of the top and
 * bottom action bar) are impossible to accomplish in XML. initGradientStyle
 * performs additional operations to initialize the desired style. Its
 * foreground gradient (used to tint the text and icon drawables in the action
 * bar and bottom app bar) is made by creating a linear gradient using the
 * values of the XML attributes foregroundGradientLeftColor, foregroundGradientMiddleColor,
 * and foregroundGradientRightColor. The background gradient (smaller subsets
 * of which are used as the backgrounds for the checkout and add buttons) is
 * made from the colors backgroundGradientLeftColor, backgroundGradientMiddleColor,
 * and backgroundGradientRightColor. The gradient used for the indicator of the
 * bottom navigation bar is recommended to be the same as the foreground
 * gradient for thematic consistency, but can be changed through the properties
 * indicatorGradientLeftColor, indicatorGradientMiddleColor, and indicatorGradientRightColor
 * if needed for better visibility.
 */
fun MainActivity.initGradientStyle() {
    window.setBackgroundDrawable(ContextCompat.getDrawable(this, R.drawable.background_gradient))

    val screenWidth = resources.displayMetrics.widthPixels

    val fgColors = intArrayOf(theme.resolveIntAttribute(R.attr.foregroundGradientLeftColor),
                              theme.resolveIntAttribute(R.attr.foregroundGradientMiddleColor),
                              theme.resolveIntAttribute(R.attr.foregroundGradientRightColor))
    val bgColors = intArrayOf(theme.resolveIntAttribute(R.attr.backgroundGradientLeftColor),
                              theme.resolveIntAttribute(R.attr.backgroundGradientMiddleColor),
                              theme.resolveIntAttribute(R.attr.backgroundGradientRightColor))
    val indicatorColors = intArrayOf(theme.resolveIntAttribute(R.attr.indicatorGradientLeftColor),
                                     theme.resolveIntAttribute(R.attr.indicatorGradientMiddleColor),
                                     theme.resolveIntAttribute(R.attr.indicatorGradientRightColor))

    val fgGradientBuilder = GradientBuilder(x2 = screenWidth.toFloat(), colors = fgColors)
    val fgGradientShader = fgGradientBuilder.buildLinearGradient()

    val bgGradientBuilder = fgGradientBuilder.copy(colors  = bgColors)
    val bgGradientShader = bgGradientBuilder.buildLinearGradient()
    ui.bottomAppBar.backgroundGradient = bgGradientShader

    ui.bottomAppBar.navIndicator.gradient =
        fgGradientBuilder.copy(colors  = indicatorColors).buildLinearGradient()

    val paint = Paint()
    paint.style = Paint.Style.FILL
    paint.shader = fgGradientShader
    val fgGradientBitmap = Bitmap.createBitmap(screenWidth, 1, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(fgGradientBitmap)
    canvas.drawRect(0f, 0f, screenWidth.toFloat(), 1f, paint)

    val bgGradientBitmap = Bitmap.createBitmap(screenWidth, 1, Bitmap.Config.ARGB_8888)
    paint.shader = bgGradientShader
    canvas.setBitmap(bgGradientBitmap)
    canvas.drawRect(0f, 0f, screenWidth.toFloat(), 1f, paint)

    ui.styleActionBarContents(screenWidth, fgGradientShader, fgGradientBitmap)
    ui.styleBottomNavDrawerContents(screenWidth, fgGradientBitmap, fgGradientBuilder,
                                                 bgGradientBitmap, bgGradientBuilder)
}

private fun MainActivityBinding.styleActionBarContents(screenWidth: Int,
                                                       fgGradientShader: Shader,
                                                       fgGradientBitmap: Bitmap) {
    val buttonWidth = actionBar.ui.backButton.drawable.intrinsicWidth
    var x = buttonWidth / 2
    actionBar.ui.backButton.drawable.setTint(fgGradientBitmap.getPixel(x, 0))
    actionBar.ui.titleSwitcher.setShader(fgGradientShader)

    x = screenWidth - buttonWidth / 2
    actionBar.ui.menuButton.drawable.setTint(fgGradientBitmap.getPixel(x, 0))
    x -= buttonWidth
    actionBar.ui.changeSortButton.drawable.setTint(fgGradientBitmap.getPixel(x, 0))
    x -= buttonWidth
    actionBar.ui.searchButton.drawable?.setTint(fgGradientBitmap.getPixel(x, 0))
}

private fun MainActivityBinding.styleBottomNavDrawerContents(screenWidth: Int,
                                                             fgGradientBitmap: Bitmap,
                                                             fgGradientBuilder: GradientBuilder,
                                                             bgGradientBitmap: Bitmap,
                                                             bgGradientBuilder: GradientBuilder) {
    // Checkout button
    val wrapContent = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
    cradleLayout.measure(wrapContent, wrapContent)
    val cradleWidth = cradleLayout.measuredWidth
    val cradleLeft = (screenWidth - cradleWidth) / 2f
    checkoutButton.foregroundGradient = fgGradientBuilder
        .setX1(-cradleLeft).setX2(screenWidth - cradleLeft).buildLinearGradient()
    checkoutButton.backgroundGradient = bgGradientBuilder
        .setX1(-cradleLeft).setX2(screenWidth - cradleLeft).buildLinearGradient()

    // Add button
    var right = cradleLeft.toInt() + cradleWidth
    var left = right - addButton.layoutParams.width
    val addButtonCenter = right - addButton.layoutParams.width / 2f
    val color = fgGradientBitmap.getPixel(addButtonCenter.toInt(), 0)
    addButton.background.mutate()
    addButton.imageTintList = ColorStateList.valueOf(color)
    var leftColor = bgGradientBitmap.getPixel(left, 0)
    var rightColor = bgGradientBitmap.getPixel(right, 0)
    (addButton.background as GradientDrawable).colors = intArrayOf(leftColor, rightColor)

    // Bottom navigation view
    val menuSize = bottomNavigationView.menu.size()
    for (i in 0 until menuSize) {
        val center = ((i + 0.5f) / menuSize * screenWidth).toInt()
        val tint = ColorStateList.valueOf(fgGradientBitmap.getPixel(center, 0))
        bottomNavigationView.getIconAt(i).imageTintList = tint
        bottomNavigationView.setTextTintList(i, tint)
    }
    bottomNavigationView.invalidate()

    // App title
    left = (appTitle.layoutParams as ViewGroup.MarginLayoutParams).marginStart
    appTitle.paint.shader = fgGradientBuilder.setX1(-left.toFloat())
        .setX2(screenWidth - left.toFloat()).buildLinearGradient()

    // Settings and inventory selector more options button
    val layoutParams = inventorySelectorOptionsButton.layoutParams as ViewGroup.MarginLayoutParams
    left = screenWidth - layoutParams.marginEnd - inventorySelectorOptionsButton.width
    inventorySelectorOptionsButton.drawable.setTint(fgGradientBitmap.getPixel(left, 0))
    left -= settingsButton.layoutParams.width
    settingsButton.drawable.setTint(fgGradientBitmap.getPixel(left, 0))

    // Add inventory button
    val marginEnd = (addInventoryButton.layoutParams as ViewGroup.MarginLayoutParams).marginEnd
    right = screenWidth - inventorySelectorBackground.paddingEnd - marginEnd
    left = right - addInventoryButton.layoutParams.width
    leftColor = bgGradientBitmap.getPixel(left, 0)
    rightColor = bgGradientBitmap.getPixel(right, 0)
    (addInventoryButton.background as GradientDrawable).colors = intArrayOf(leftColor, rightColor)
}

private fun ActionBarTitle.setShader(shader: Shader?) {
    fragmentTitleView.paint.shader = shader
    actionModeTitleView.paint.shader = shader
    searchQueryView.paint.shader = shader
    (searchQueryView.background as? GradientVectorDrawable)?.gradient = shader
}

fun MainActivityBinding.bottomSheetCallback() = object: BottomSheetBehavior.BottomSheetCallback() {
    override fun onStateChanged(bottomSheet: View, newState: Int) { }

    override fun onSlide(bottomSheet: View, slideOffset: Float) {
        if (bottomNavigationDrawer.targetState == BottomSheetBehavior.STATE_HIDDEN)
            return
        val slide = abs(slideOffset)

        appTitle.isVisible = slideOffset != 0f
        settingsButton.isVisible = slideOffset != 0f
        inventorySelectorOptionsButton.isVisible = slideOffset != 0f
        inventorySelector.isInvisible = slideOffset == 0f
        bottomNavigationView.isVisible = slideOffset != 1f

        appTitle.alpha = slide
        settingsButton.alpha = slide
        inventorySelectorOptionsButton.alpha = slide
        inventorySelector.alpha = slide
        bottomNavigationView.alpha = 1f - slide

        bottomAppBar.apply {
            cradle.layout?.apply {
                isVisible = slideOffset != 1f
                alpha = 1f - slide
                scaleX = 1f - 0.1f * slide
                scaleY = 1f - 0.1f * slide
                translationY = height * -0.9f * slide
            }
            navIndicator.alpha = 1f - slide
            interpolation = 1f - slide
        }
    }
}