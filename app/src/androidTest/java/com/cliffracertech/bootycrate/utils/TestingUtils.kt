/* Copyright 2020 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracertech.bootycrate.utils

import android.view.View
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.NoMatchingViewException
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.ViewAssertion
import androidx.test.espresso.matcher.RootMatchers.isPlatformPopup
import androidx.test.espresso.matcher.ViewMatchers.isEnabled
import com.cliffracertech.bootycrate.ExpandableSelectableItemView
import com.cliffracertech.bootycrate.InventoryItemView
import com.cliffracertech.bootycrate.database.ExpandableSelectableItem
import com.cliffracertech.bootycrate.database.InventoryItem
import com.cliffracertech.bootycrate.database.ShoppingListItem
import com.cliffracertech.bootycrate.recyclerview.ExpandableSelectableRecyclerView
import com.google.common.truth.Truth.assertThat
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.TypeSafeMatcher

/** Thanks to the author of this blog post for the idea.
 * https://medium.com/android-news/call-view-methods-when-testing-by-espresso-and-kotlin-in-android-781262f7348e */
fun <T>doStuff(method: (view: T) -> Unit): ViewAction {
    return object: ViewAction {
        override fun getDescription() = method.toString()
        override fun getConstraints() = isEnabled()
        @Suppress("UNCHECKED_CAST")
        override fun perform(uiController: UiController?, view: View) =
            method(view as? T ?: throw IllegalStateException("The matched view is null or not of type T"))
    }
}

fun actionOnChildWithId(viewId: Int, action: ViewAction) = object : ViewAction {
    override fun getConstraints() = null
    override fun getDescription() = "Click on a child view with specified id."
    override fun perform(uiController: UiController, view: View) =
        action.perform(uiController, view.findViewById(viewId))
}

class isEnabled : TypeSafeMatcher<View>() {
    override fun describeTo(description: Description) { description.appendText("is enabled: ") }
    override fun matchesSafely(item: View) = item.isEnabled
}

fun onPopupView(viewMatcher: Matcher<View>) = onView(viewMatcher).inRoot(isPlatformPopup())

/** Assert that the view is an ExpandableSelectableRecyclerView with only one expanded item at
 * index expandedIndex. The height of collapsed items must also be provided. */
fun onlyExpandedIndexIs(expandedIndex: Int?, collapsedHeight: Int) = ViewAssertion { view, e ->
    if (view == null) throw e!!
    assertThat(view).isInstanceOf(ExpandableSelectableRecyclerView::class.java)
    val it = view as ExpandableSelectableRecyclerView<*>
    for (i in 0 until it.adapter.itemCount) {
        val vh = it.findViewHolderForAdapterPosition(i)
        if (i != expandedIndex) assertThat(vh!!.itemView.height).isEqualTo(collapsedHeight)
        else                    assertThat(vh!!.itemView.height).isGreaterThan(collapsedHeight)
    }
}

/** Asserts that the view is an ExpandableSelectableRecyclerView, with the items
 * at the specified indices all selected, and with no other selected items. */
fun onlySelectedIndicesAre(vararg indices: Int) = ViewAssertion { view, e ->
    if (view == null) throw e!!
    assertThat(view).isInstanceOf(ExpandableSelectableRecyclerView::class.java)
    val it = view as ExpandableSelectableRecyclerView<*>
    for (i in 0 until it.adapter.itemCount) {
        val vh = it.findViewHolderForAdapterPosition(i)!! as ExpandableSelectableRecyclerView<*>.ViewHolder
        val shouldBeSelected = i in indices
        assertThat(vh.item.isSelected).isEqualTo(shouldBeSelected)
        val itemView = vh.itemView as ExpandableSelectableItemView<*>
        assertThat(itemView.isInSelectedState).isEqualTo(shouldBeSelected)
    }
}

/** Asserts that the view is an ExpandableSelectableRecyclerView that
    contains only the specified items of type T, in the order given. */
abstract class onlyShownItemsAre<T: ExpandableSelectableItem>(vararg items: T) : ViewAssertion
{
    private val items = items.asList()

    abstract fun itemFromView(view: ExpandableSelectableItemView<*>) : T

    override fun check(view: View?, noViewFoundException: NoMatchingViewException?) {
        if (view == null) throw noViewFoundException!!
        assertThat(view).isInstanceOf(ExpandableSelectableRecyclerView::class.java)
        val it = view as ExpandableSelectableRecyclerView<*>
        assertThat(items.size).isEqualTo(it.adapter.itemCount)
        for (i in 0 until it.adapter.itemCount) {
            val vh = it.findViewHolderForAdapterPosition(i)
            assertThat(vh).isNotNull()
            val itemView = vh!!.itemView as? ExpandableSelectableItemView<*>
            assertThat(itemView).isNotNull()
            assertThat(itemFromView(itemView!!)).isEqualTo(items[i])
        }
    }
}

class onlyShownShoppingListItemsAre(vararg items: ShoppingListItem) :
    onlyShownItemsAre<ShoppingListItem>(*items)
{
    override fun itemFromView(view: ExpandableSelectableItemView<*>) = ShoppingListItem(
        name = view.ui.nameEdit.text.toString(),
        extraInfo = view.ui.extraInfoEdit.text.toString(),
        color = view.ui.checkBox.colorIndex,
        amount = view.ui.amountEdit.value)
}

class onlyShownInventoryItemsAre(vararg items: InventoryItem) :
    onlyShownItemsAre<InventoryItem>(*items)
{
    override fun itemFromView(view: ExpandableSelectableItemView<*>) = InventoryItem(
        name = view.ui.nameEdit.text.toString(),
        extraInfo = view.ui.extraInfoEdit.text.toString(),
        color = view.ui.checkBox.colorIndex,
        amount = view.ui.amountEdit.value,
        addToShoppingList = (view as InventoryItemView).detailsUi.addToShoppingListCheckBox.isChecked,
        addToShoppingListTrigger = view.detailsUi.addToShoppingListTriggerEdit.value)
}