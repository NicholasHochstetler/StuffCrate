/* Copyright 2021 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracertech.bootycrate.bottombar

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cliffracertech.bootycrate.model.NavigationState
import com.cliffracertech.bootycrate.model.NewItemDialogVisibilityState
import com.cliffracertech.bootycrate.model.database.ItemDao
import com.cliffracertech.bootycrate.utils.collectAsState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * A [ViewModel] that contains state and callbacks for a
 * bottom app bar with a checkout and add button in a cutout.
 *
 * Exposed state that should be displayed includes
 * [selectedRootScreen],
 * [checkoutButtonIsVisible],
 * [checkoutButtonIsEnabled],
 * [newShoppingListItemDialogIsVisible],
 * and [newInventoryItemDialogIsVisible].
 *
 * The [Flow] property [shoppingListSizeChanges] should be collected so that
 * the emitted amounts are displayed to the user.
 *
 * Callbacks that should be connected to UI events are
 * [onCheckoutButtonConfirm], [onAddButtonClick], and [onNavBarItemClick].
 */
@HiltViewModel class BottomAppBarViewModel(
    private val navState: NavigationState,
    private val dialogVisibilityState: NewItemDialogVisibilityState,
    private val itemDao: ItemDao,
    coroutineScope: CoroutineScope?
) : ViewModel() {

    @Inject constructor(
        navigationState: NavigationState,
        dialogVisibilityState: NewItemDialogVisibilityState,
        itemDao: ItemDao,
    ) : this(navigationState, dialogVisibilityState, itemDao, null)

    private val scope = coroutineScope ?: viewModelScope

    /** The navigation destination that the nav indicator should
     * hover above to indicate that it is the active one. */
    val selectedRootScreen by navState::rootScreen

    private val checkedItemsSize by itemDao
        .getCheckedShoppingListItemsSize()
        .collectAsState(0, scope)

    val checkoutButtonIsVisible by derivedStateOf {
        selectedRootScreen.isShoppingList
    }

    val checkoutButtonIsEnabled by derivedStateOf {
        checkedItemsSize > 0
    }

    private var oldShoppingListSize = 0

    /** Changes in the shopping list size that should be displayed
     * temporarily (e.g. with a badge over the shopping list nav
     * item that fades out) when new amounts are emitted. */
    val shoppingListSizeChanges =
        itemDao.getShoppingListItemCount().map { shoppingListSize ->
            val change = shoppingListSize - oldShoppingListSize
            oldShoppingListSize = shoppingListSize
            if (selectedRootScreen.isShoppingList) 0
            else change
        }.drop(1).shareIn(scope, SharingStarted.WhileSubscribed(3000), 0)

    fun onCheckoutButtonConfirm() {
        if (selectedRootScreen.isShoppingList)
            scope.launch { itemDao.checkout() }
    }

    val newShoppingListItemDialogIsVisible by
        dialogVisibilityState::showingNewShoppingListItemDialog
    val newInventoryItemDialogIsVisible by
        dialogVisibilityState::showingNewInventoryItemDialog

    fun onAddButtonClick() {
        if (navState.visibleScreen.isShoppingList)
            dialogVisibilityState.showingNewShoppingListItemDialog = true
        else if (navState.visibleScreen.isInventory)
            dialogVisibilityState.showingNewInventoryItemDialog = true
    }

    fun onNavBarItemClick(screen: NavigationState.RootScreen) =
        navState.navigateToRootScreen(screen)
}
