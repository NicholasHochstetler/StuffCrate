/* Copyright 2021 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracertech.bootycrate.fragment

import android.os.Bundle
import android.view.*
import android.widget.TextView
import androidx.annotation.Keep
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import com.cliffracertech.bootycrate.*
import com.cliffracertech.bootycrate.database.InventoryViewModel
import com.cliffracertech.bootycrate.database.ShoppingListItem
import com.cliffracertech.bootycrate.database.ShoppingListViewModel
import com.cliffracertech.bootycrate.databinding.MainActivityBinding
import com.cliffracertech.bootycrate.databinding.ShoppingListFragmentBinding
import com.cliffracertech.bootycrate.recyclerview.ShoppingListView
import com.cliffracertech.bootycrate.utils.NewShoppingListItemDialog
import com.cliffracertech.bootycrate.utils.repeatWhenStarted
import com.cliffracertech.bootycrate.view.CheckoutButton
import com.cliffracertech.bootycrate.view.ListActionBar
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

/**
 * A fragment to display and modify the user's shopping list.
 *
 * ShoppingListFragment is a ListViewFragment subclass to view and modify a
 * list of ShoppingListItems using a ShoppingListView. ShoppingListFragment
 * overrides ListViewFragment's abstract listView property with an instance of
 * ShoppingListView, and overrides its ActionModeCallback with its own version.
 *
 * ShoppingListFragment also manages the state and function of the checkout
 * button. The checkout button is enabled when the user has checked at least
 * one shopping list item, and disabled when no items are checked through its
 * observation of ShoppingListView's checkedItems member. If the checkout
 * button is clicked while it is enabled, it switches to its confirmatory state
 * to safeguard the user from checking out accidentally. If the user does not
 * press the button again within two seconds, it will revert to its normal state.
 */
@Keep class ShoppingListFragment : ListViewFragment<ShoppingListItem>() {
    override val viewModel: ShoppingListViewModel by activityViewModels()
    private val inventoryItemViewModel: InventoryViewModel by activityViewModels()
    override var listView: ShoppingListView? = null
    override lateinit var collectionName: String
    override val actionModeCallback = ShoppingListActionModeCallback()

    private var checkoutButton: CheckoutButton? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = ShoppingListFragmentBinding.inflate(inflater, container, false).apply {
        listView = shoppingListView
        shoppingListView.onItemCheckBoxClick = viewModel::toggleIsChecked
        collectionName = inflater.context.getString(
            R.string.shopping_list_item_collection_name)
    }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewLifecycleOwner.repeatWhenStarted {
            launch { viewModel.items.collect(::updateBadge) }
            launch { viewModel.checkedItemsSize.collect { newSize ->
                checkoutButton?.isEnabled = newSize != 0
            }}
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        listView = null
    }

    override fun onDetach() {
        super.onDetach()
        checkoutButton = null
        newItemsBadge = null
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.add_to_inventory_button -> {
            inventoryItemViewModel.addFromSelectedShoppingListItems()
            viewModel.clearSelection()
            true
        } R.id.check_all_menu_item -> { viewModel.checkAll(); true }
        R.id.uncheck_all_menu_item -> { viewModel.uncheckAll(); true }
        else -> super.onOptionsItemSelected(item)
    }

    override fun showsCheckoutButton() = true
    override fun onActiveStateChanged(isActive: Boolean, activityUi: MainActivityBinding) {
        super.onActiveStateChanged(isActive, activityUi)
        checkoutButton = activityUi.checkoutButton
        newItemsBadge = activityUi.shoppingListBadge
        activityUi.actionBar.optionsMenu.setGroupVisible(
            R.id.shopping_list_view_menu_group, isActive)
        if (!isActive) return

        activityUi.addButton.setOnClickListener {
            val activity = this.activity ?: return@setOnClickListener
            NewShoppingListItemDialog(activity)
                .show(activity.supportFragmentManager, null)
        }
        activityUi.checkoutButton.checkoutCallback = viewModel::checkout
    }

    private var shoppingListSize = -1
    private var shoppingListNumNewItems = 0
    private var newItemsBadge: TextView? = null
    private fun updateBadge(newShoppingList: List<ShoppingListItem>) {
        if (shoppingListSize == -1) {
            if (newShoppingList.isNotEmpty())
                shoppingListSize = newShoppingList.size
        } else {
            val badge = newItemsBadge ?: return
            val badgeParent = badge.parent as? ViewGroup
            val sizeChange = newShoppingList.size - shoppingListSize
            // The fragment view visibility check is to prevent the shopping list badge
            // from appearing when the shopping list fragment is active (since they can
            // see the items appear on screen anyways, the badge is not necessary in
            // this case). The badge parent visibility check is to prevent the badge
            // from appearing in the middle of its fade out animation if the parent is
            // made visible during the animation.
            if (view?.isVisible == false && badgeParent?.isVisible == true && sizeChange > 0) {
                shoppingListNumNewItems += sizeChange
                badge.text = getString(R.string.shopping_list_badge_text, shoppingListNumNewItems)
                badge.alpha = 1f
                badge.animate().alpha(0f).setDuration(1000).setStartDelay(1500).
                    withLayer().withEndAction { shoppingListNumNewItems = 0 }.start()
            }
            shoppingListSize = newShoppingList.size
        }
    }

    /** An override of SelectionActionModeCallback that alters the visibility of menu items specific to shopping list items. */
    inner class ShoppingListActionModeCallback : SelectionActionModeCallback() {
        override fun onStart(actionMode: ListActionBar.ActionMode, actionBar: ListActionBar) {
            super.onStart(actionMode, actionBar)
            actionBar.optionsMenu.setGroupVisible(
                R.id.shopping_list_view_action_mode_menu_group, true)
        }

        override fun onFinish(actionMode: ListActionBar.ActionMode, actionBar: ListActionBar) =
            actionBar.optionsMenu.setGroupVisible(R.id.shopping_list_view_action_mode_menu_group, false)
    }
}