package com.cliffracertech.bootycrate.activity

import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Shader
import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import com.cliffracertech.bootycrate.R
import com.cliffracertech.bootycrate.utils.GradientBuilder
import com.cliffracertech.bootycrate.utils.resolveIntAttribute
import com.cliffracertech.bootycrate.view.ActionBarTitle
import com.cliffracertech.bootycrate.view.GradientVectorDrawable


/**
 * A styled subclass of MainActivity.
 *
 * Unfortunately many of the desired aspects of MainActivity's style (e.g. the
 * menu item icons being tinted to match the gradient background of the top and
 * bottom action bar) are impossible to accomplish in XML. GradientStyledMainActivity
 * performs additional operations to initialize its style. Its foreground
 * gradient is made by creating a linear gradient using the values of the XML
 * attributes foregroundGradientColorLeft, foregroundGradientColorMiddle, and
 * foregroundGradientColorRight. The background gradient is made from the
 * colors colorAccent, colorInBetweenPrimaryAccent, and colorPrimary. It is
 * assumed that the action bar will have had its background set correctly in
 * XML to match the background gradient.
 */
class GradientStyledMainActivity : MainActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setBackgroundDrawable(ContextCompat.getDrawable(this, R.drawable.background_gradient))

        val screenWidth = resources.displayMetrics.widthPixels
        val actionBarHeight = theme.resolveIntAttribute(R.attr.actionBarSize).toFloat()

        val fgColors = intArrayOf(theme.resolveIntAttribute(R.attr.foregroundGradientColorLeft),
                                  theme.resolveIntAttribute(R.attr.foregroundGradientColorMiddle),
                                  theme.resolveIntAttribute(R.attr.foregroundGradientColorRight))
        val bgColors = intArrayOf(theme.resolveIntAttribute(R.attr.backgroundGradientColorLeft),
                                  theme.resolveIntAttribute(R.attr.backgroundGradientColorMiddle),
                                  theme.resolveIntAttribute(R.attr.backgroundGradientColorRight))

        val fgGradientBitmap = Bitmap.createBitmap(screenWidth, actionBarHeight.toInt(), Bitmap.Config.ARGB_8888)
        val bgGradientBitmap = Bitmap.createBitmap(screenWidth, actionBarHeight.toInt(), Bitmap.Config.ARGB_8888)
        val paint = Paint()
        paint.style = Paint.Style.FILL

        val fgGradientBuilder = GradientBuilder(x2 = screenWidth.toFloat(), colors = fgColors)
        val fgGradientShader = fgGradientBuilder.buildLinearGradient()
        paint.shader = fgGradientShader
        val canvas = Canvas(fgGradientBitmap)
        canvas.drawRect(0f, 0f, screenWidth.toFloat(), actionBarHeight, paint)

        val bgGradientBuilder = fgGradientBuilder.copy(colors  = bgColors)
        val bgGradientShader = bgGradientBuilder.buildLinearGradient()
        paint.shader = bgGradientShader
        canvas.setBitmap(bgGradientBitmap)
        canvas.drawRect(0f, 0f, screenWidth.toFloat(), actionBarHeight, paint)

        styleActionBarContents(fgGradientShader, fgGradientBitmap)
        ui.bottomAppBar.backgroundGradient = bgGradientShader
        ui.bottomAppBar.indicatorGradient = fgGradientShader
        styleBottomAppBar(screenWidth, fgGradientBitmap, fgGradientBuilder, bgGradientBuilder)
    }

    private fun styleActionBarContents(fgGradientShader: Shader, fgGradientBitmap: Bitmap) {
        val buttonWidth = ui.actionBar.ui.backButton.drawable.intrinsicWidth
        var x = buttonWidth / 2
        ui.actionBar.ui.backButton.drawable.setTint(fgGradientBitmap.getPixel(x, 0))
        ui.actionBar.ui.titleSwitcher.setShader(fgGradientShader)

        x = resources.displayMetrics.widthPixels - buttonWidth / 2
        ui.actionBar.ui.menuButton.drawable.setTint(fgGradientBitmap.getPixel(x, 0))
        x -= buttonWidth
        ui.actionBar.ui.changeSortButton.drawable.setTint(fgGradientBitmap.getPixel(x, 0))
        x -= buttonWidth
        ui.actionBar.ui.searchButton.drawable?.setTint(fgGradientBitmap.getPixel(x, 0))
    }

    private fun styleBottomAppBar(screenWidth: Int, fgGradientBitmap: Bitmap,
                                  fgGradientBuilder: GradientBuilder,
                                  bgGradientBuilder: GradientBuilder
    ) {
        // Checkout button
        val wrapContent = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        ui.cradleLayout.measure(wrapContent, wrapContent)
        val cradleWidth = ui.cradleLayout.measuredWidth
        val cradleLeft = (screenWidth - cradleWidth) / 2f
        ui.checkoutButton.foregroundGradient = fgGradientBuilder
            .setX1(-cradleLeft).setX2(screenWidth - cradleLeft).buildLinearGradient()
        ui.checkoutButton.backgroundGradient = bgGradientBuilder
            .setX1(-cradleLeft).setX2(screenWidth - cradleLeft).buildLinearGradient()

        // Add button
        val addButtonWidth = ui.addButton.layoutParams.width
        val addButtonLeft = cradleLeft + cradleWidth - addButtonWidth * 1f
        ui.addButton.foregroundGradient = fgGradientBuilder
            .setX1(-addButtonLeft).setX2(screenWidth - addButtonLeft).buildLinearGradient()
        ui.addButton.backgroundGradient = bgGradientBuilder
            .setX1(-addButtonLeft).setX2(screenWidth - addButtonLeft).buildLinearGradient()

        // Bottom navigation view
        val menuSize = ui.bottomNavigationBar.menu.size()
        for (i in 0 until menuSize) {
            val center = ((i + 0.5f) / menuSize * screenWidth).toInt()
            val tint = ColorStateList.valueOf(fgGradientBitmap.getPixel(center, 0))
            ui.bottomNavigationBar.getIconAt(i).imageTintList = tint
            ui.bottomNavigationBar.setTextTintList(i, tint)
        }
        ui.bottomNavigationBar.invalidate()
    }

    private fun ActionBarTitle.setShader(shader: Shader?) {
        titleView.paint.shader = shader
        actionModeTitleView.paint.shader = shader
        searchQueryView.paint.shader = shader
        (searchQueryView.background as? GradientVectorDrawable)?.gradient = shader
    }
}