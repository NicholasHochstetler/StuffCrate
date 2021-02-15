/* Copyright 2020 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracermerchant.bootycrate

import android.animation.Animator
import android.animation.LayoutTransition
import android.animation.ObjectAnimator
import android.content.res.Configuration
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import android.graphics.Rect
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.animation.doOnEnd
import androidx.core.view.doOnNextLayout
import androidx.core.view.isVisible
import androidx.preference.PreferenceManager.getDefaultSharedPreferences
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.android.synthetic.main.activity_main.*

//@HiltAndroidApp
//class BootyCrateApplication: Application()

/** The primary activity for BootyCrate
 *
 *  Instead of switching between activities, nearly everything in BootyCrate is
 *  accomplished in the ShoppingListFragment, InventoryFragment, or the Preferences-
 *  Fragment. Instances of ShoppingListFragment and InventoryFragment are created
 *  on app startup, and hidden/shown by the fragment manager as appropriate. The
 *  currently shown fragment can be determined via the boolean members showing-
 *  Inventory and showingPreferences as follows:
 *  Shown fragment = if (showingPreferences)    PreferencesFragment
 *                   else if (showingInventory) InventoryFragment
 *                   else                       ShoppingListFragment
 *  If showingPreferences is true, the value of showingInventory determines the
 *  fragment "under" the preferences (i.e. the one that will be returned to on a
 *  back button press or a navigate up). */
//@AndroidEntryPoint
open class MainActivity : AppCompatActivity() {
    private lateinit var shoppingListFragment: ShoppingListFragment
    private lateinit var inventoryFragment: InventoryFragment
    private var showingInventory = false
    private var showingPreferences = false
    val activeFragment get() = if (showingInventory) inventoryFragment
                               else                  shoppingListFragment

    private var checkoutButtonIsVisible = true
    private var shoppingListSize = -1
    private var shoppingListNumNewItems = 0
    private var pendingCradleAnim: Animator? = null

    val shoppingListViewModel: ShoppingListViewModel by viewModels()
    val inventoryViewModel: InventoryViewModel by viewModels()
    lateinit var addButton: OutlinedGradientButton
    lateinit var checkoutButton: CheckoutButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getDefaultSharedPreferences(this)
     /* The activity's ViewModelStore will by default retain instances of the
        app's view models across activity restarts. In case this is not desired
        (e.g. when the database was replaced with an external one, and the view-
        models therefore need to be reset), setting the shared preference whose
        key is equal to the value of R.string.pref_viewmodels_need_cleared to
        true will cause MainActivity to call viewModelStore.clear() */
        var prefKey = getString(R.string.pref_viewmodels_need_cleared)
        if (prefs.getBoolean(prefKey, false)) {
            viewModelStore.clear()
            val editor = prefs.edit()
            editor.putBoolean(prefKey, false)
            editor.apply()
        }

        prefKey = getString(R.string.pref_app_theme)
        val themeDefault = getString(R.string.sys_default_theme_description)
        setTheme(when (prefs.getString(prefKey, themeDefault) ?: "") {
            getString(R.string.light_theme_description) -> R.style.LightTheme
            getString(R.string.dark_theme_description) ->  R.style.DarkTheme
            else -> if (sysDarkThemeIsActive) R.style.DarkTheme
                    else                      R.style.LightTheme
        })
        setContentView(R.layout.activity_main)

        addButton = add_button
        checkoutButton = checkout_button

        cradleLayout.layoutTransition = defaultLayoutTransition()
        cradleLayout.layoutTransition.doOnStart { _, _, _, _ ->
            pendingCradleAnim?.start()
            pendingCradleAnim = null
        }

        bottomAppBar.indicatorWidth = 3 * bottomNavigationBar.itemIconSize
        bottomNavigationBar.setOnNavigationItemSelectedListener(onNavigationItemSelected)

        supportFragmentManager.addOnBackStackChangedListener {
            if (supportFragmentManager.backStackEntryCount == 0) {
                showBottomAppBar()
                showingPreferences = false
                topActionBar.ui.backButton.isVisible = false
                activeFragment.isActive = true
            }
        }
        initFragments(savedInstanceState)
        val navButton = findViewById<View>(if (showingInventory) R.id.inventory_button
                                           else                  R.id.shopping_list_button)
        navButton.doOnNextLayout {
            bottomAppBar.indicatorXPos = (it.width - bottomAppBar.indicatorWidth) / 2 + it.left
        }
        if (showingInventory)
            showCheckoutButton(showing = false, animate = false)
        bottomAppBar.prepareCradleLayout(cradleLayout)

        shoppingListViewModel.items.observe(this) { newList ->
            updateShoppingListBadge(newList)
        }

        topActionBar.ui.backButton.setOnClickListener { onSupportNavigateUp() }
        onCreateOptionsMenu(topActionBar.optionsMenu)
        topActionBar.onDeleteButtonClickedListener = {
            onOptionsItemSelected(topActionBar.optionsMenu.findItem(R.id.delete_selected_menu_item))
        }
        topActionBar.setOnSortOptionClickedListener { item ->
            onOptionsItemSelected(item)
        }
        topActionBar.setOnOptionsItemClickedListener { item ->
            onOptionsItemSelected(item)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("showingInventory", showingInventory)
        supportFragmentManager.putFragment(outState, "shoppingListFragment", shoppingListFragment)
        supportFragmentManager.putFragment(outState, "inventoryFragment",    inventoryFragment)
        outState.putBoolean("showingPreferences", showingPreferences)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.settings_menu_item) {
            showPreferencesFragment()
            return true
        }
        return activeFragment.onOptionsItemSelected(item)
    }

    override fun onSupportNavigateUp() = when {
        showingPreferences -> {
            supportFragmentManager.popBackStack()
            true
        } activeFragment.searchIsActive -> {
            topActionBar.ui.searchView.findViewById<ImageView>(
                androidx.appcompat.R.id.search_close_btn)
                .apply { performClick(); performClick() }
            true
        } activeFragment.actionMode.isStarted -> {
            activeFragment.actionMode.finishAndClearSelection()
            true
        } else -> false
    }

    override fun onBackPressed() {
        if (showingPreferences) supportFragmentManager.popBackStack()
        else                    super.onBackPressed()
    }

    private fun showPreferencesFragment(animate: Boolean = true) {
        showingPreferences = true
        inputMethodManager(this)?.hideSoftInputFromWindow(bottomAppBar.windowToken, 0)
        showBottomAppBar(false)
        activeFragment.isActive = false
        topActionBar.ui.backButton.isVisible = true

        val enterAnimResId = if (animate) R.animator.fragment_close_enter else 0
        supportFragmentManager.beginTransaction().
            setCustomAnimations(enterAnimResId, R.animator.fragment_close_exit,
                                enterAnimResId, R.animator.fragment_close_exit).
            hide(activeFragment).
            add(R.id.fragmentContainer, PreferencesFragment()).
            addToBackStack(null).commit()
    }

    private fun switchToInventory() = toggleMainFragments(switchingToInventory = true)
    private fun switchToShoppingList() = toggleMainFragments(switchingToInventory = false)
    private fun toggleMainFragments(switchingToInventory: Boolean) {
        if (showingPreferences) return

        val oldFragment = activeFragment
        showingInventory = switchingToInventory
        showCheckoutButton(showing = !showingInventory)
        inputMethodManager(this)?.hideSoftInputFromWindow(bottomAppBar.windowToken, 0)

        val newFragmentTranslationStart = fragmentContainer.width * if (showingInventory) 1f else -1f
        val fragmentTranslationAmount = fragmentContainer.width * if (showingInventory) -1f else 1f

        oldFragment.isActive = false
        val oldFragmentView = oldFragment.view
        oldFragmentView?.animate()?.translationXBy(fragmentTranslationAmount)?.
                                    setDuration(300)?.//withLayer()?.
                                    withEndAction { oldFragmentView.visibility = View.INVISIBLE }?.
                                    start()

        activeFragment.isActive = true
        val newFragmentView = activeFragment.view
        newFragmentView?.translationX = newFragmentTranslationStart
        newFragmentView?.visibility = View.VISIBLE
        newFragmentView?.animate()?.translationX(0f)?.setDuration(300)?.start()//withLayer()
    }

    private fun showBottomAppBar(show: Boolean = true) {
        val screenHeight = resources.displayMetrics.heightPixels.toFloat()
        val views = arrayOf<View>(bottomAppBar, addButton, checkoutButton)

        if (!show && bottomAppBar.height == 0) {
            bottomAppBar.doOnNextLayout {
                val translationAmount = screenHeight - cradleLayout.top
                for (view in views) view.translationY = translationAmount
            }
            return
        }
        val translationAmount = screenHeight - cradleLayout.top
        val translationStart = if (show) translationAmount else 0f
        val translationEnd =   if (show) 0f else translationAmount
        for (view in views) {
            view.translationY = translationStart
            view.animate().withLayer().translationY(translationEnd).start()
        }
    }

    private fun showCheckoutButton(showing: Boolean, animate: Boolean = true) {
        if (checkoutButtonIsVisible == showing) return

        checkoutButtonIsVisible = showing
        checkoutButton.isVisible = showing

        val wrapContentSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        cradleLayout.measure(wrapContentSpec, wrapContentSpec)
        val cradleEndWidth = if (showing) cradleLayout.measuredWidth
                             else         addButton.layoutParams.width

        // These z values seem not to stick when set in XML, so we have to
        // set them here every time to ensure that the addButton remains on
        // top of the others.
        addButton.elevation = 5f
        checkoutButton.elevation = -10f
        if (!animate) {
            bottomAppBar.cradleWidth = cradleEndWidth
            return
        }
        // Settings the checkout button's clip bounds prevents the
        // right corners of the checkout button from sticking out
        // underneath the FAB during the show / hide animation.
        val checkoutBtnClipBounds = Rect(0, 0, 0, checkoutButton.height)
        ObjectAnimator.ofInt(bottomAppBar, "cradleWidth", cradleEndWidth).apply {
            interpolator = cradleLayout.layoutTransition.getInterpolator(LayoutTransition.CHANGE_APPEARING)
            duration = cradleLayout.layoutTransition.getDuration(LayoutTransition.CHANGE_APPEARING)
            addUpdateListener {
                checkoutBtnClipBounds.right = bottomAppBar.cradleWidth - addButton.measuredWidth / 2
                checkoutButton.clipBounds = checkoutBtnClipBounds
            }
            doOnEnd { checkoutButton.clipBounds = null }
            // The anim is stored here and started in the cradle layout's
            // layoutTransition's transition listener's transitionStart override
            // so that the animation is synced with the layout transition.
            pendingCradleAnim = this
        }
    }

    private fun updateShoppingListBadge(newShoppingList: List<ShoppingListItem>) {
        if (shoppingListSize == -1) {
            if (newShoppingList.isNotEmpty())
                shoppingListSize = newShoppingList.size
        } else {
            val sizeChange = newShoppingList.size - shoppingListSize
            if (activeFragment == inventoryFragment && sizeChange > 0) {
                shoppingListNumNewItems += sizeChange
                shoppingListBadge.text = getString(R.string.shopping_list_badge_text,
                                                   shoppingListNumNewItems)
                shoppingListBadge.clearAnimation()
                shoppingListBadge.alpha = 1f
                shoppingListBadge.animate().alpha(0f).setDuration(1000).setStartDelay(1500).
                    withLayer().withEndAction { shoppingListNumNewItems = 0 }.start()
            }
            shoppingListSize = newShoppingList.size
        }
    }

    private fun initFragments(savedInstanceState: Bundle?) {
        showingInventory = savedInstanceState?.getBoolean("showingInventory") ?: false
        showingPreferences = savedInstanceState?.getBoolean("showingPreferences") ?: false

        if (savedInstanceState != null) {
            shoppingListFragment = supportFragmentManager.getFragment(
                savedInstanceState, "shoppingListFragment") as ShoppingListFragment
            inventoryFragment = supportFragmentManager.getFragment(
                savedInstanceState, "inventoryFragment") as InventoryFragment

            if (showingPreferences) {
                showBottomAppBar(false)
                supportActionBar?.setDisplayHomeAsUpEnabled(true)
            }

        } else {
            shoppingListFragment = ShoppingListFragment(isActive = !showingInventory)
            inventoryFragment = InventoryFragment(isActive = showingInventory)
            supportFragmentManager.beginTransaction().
                add(R.id.fragmentContainer, shoppingListFragment, "shoppingList").
                add(R.id.fragmentContainer, inventoryFragment, "inventory").
                commit()
        }
    }

    private val onNavigationItemSelected = BottomNavigationView.OnNavigationItemSelectedListener { item ->
        if (item.isChecked) false // Selected item was already selected
        else {
            item.isChecked = true
            toggleMainFragments(switchingToInventory = item.itemId == R.id.inventory_button)

            val newIcon = findViewById<View>(
                if (item.itemId == R.id.inventory_button) R.id.inventory_button
                else                                      R.id.shopping_list_button)
            val indicatorNewXPos = (newIcon.width - bottomAppBar.indicatorWidth) / 2 + newIcon.left
            ObjectAnimator.ofInt(bottomAppBar, "indicatorXPos", indicatorNewXPos).apply {
                duration = cradleLayout.layoutTransition.getDuration(LayoutTransition.CHANGE_APPEARING)
            }.start()
            true
        }
    }

    val sysDarkThemeIsActive get() =
        (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == UI_MODE_NIGHT_YES
}
