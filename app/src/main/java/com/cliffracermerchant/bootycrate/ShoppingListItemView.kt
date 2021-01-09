/* Copyright 2020 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracermerchant.bootycrate

import android.app.Activity
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.integer_edit_layout.view.*
import kotlinx.android.synthetic.main.shopping_list_item_details_layout.view.*
import kotlinx.android.synthetic.main.shopping_list_item_layout.view.*

/** A layout to display the contents of a shopping list item.
 *
 *  ShoppingListItemView is a ExpandableSelectableItemView subclass that
 *  inflates a layout resource to display the data of a ShoppingListItem
 *  instance. Its update override updates the contained views with the informa-
 *  tion of the provided ShoppingListItem. It also overrides the setExpanded
 *  function with an implementation that shows or hides the shopping list
 *  item's extra details. */
class ShoppingListItemView(context: Context, attrs: AttributeSet? = null) :
    ExpandableSelectableItemView<ShoppingListItem>(context, attrs)
{
    var color get() = checkBox.color
        set(value) { checkBox.color = value }
    var colorIndex get() = ViewModelItem.Colors.indexOf(color)
                   set(value) { val index = value.coerceIn(ViewModelItem.Colors.indices)
                                checkBox.color = ViewModelItem.Colors[index] }

    // This companion object stores resources common to all ShoppingListItemViews.
    private companion object SharedResources {
        private var isInitialized = false
        private lateinit var imm: InputMethodManager
        private lateinit var linkedItemDescriptionString: String
        private lateinit var unlinkedItemDescriptionString: String
    }

    init {
        LayoutInflater.from(context).inflate(R.layout.shopping_list_item_layout, this, true)
        layoutParams = LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.WRAP_CONTENT)
        if (!SharedResources.isInitialized) initSharedResources(context)

        editButton.setOnClickListener {
            if (isExpanded) //TODO: Implement more options menu
            else             expand()
        }
        collapseButton.setOnClickListener { collapse() }

        layoutTransition = delaylessLayoutTransition()
        layoutParams = RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                                                 ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    override fun update(item: ShoppingListItem) {
        nameEdit.setText(item.name)
        extraInfoEdit.setText(item.extraInfo)
        val colorIndex = item.color.coerceIn(ViewModelItem.Colors.indices)
        checkBox.setColor(ViewModelItem.Colors[colorIndex], animate = false)

        shoppingListAmountEdit.initValue(item.amount)
        linkedToIndicator.text = if (item.linkedItemId != null) linkedItemDescriptionString
                                 else                           unlinkedItemDescriptionString
        checkBox.onCheckedChangedListener = null
        checkBox.isChecked = item.isChecked
        checkBox.onCheckedChangedListener = { checked -> setStrikeThroughEnabled(checked) }
        setStrikeThroughEnabled(checked = item.isChecked, animate = false)
        super.update(item)
    }

    override fun setExpanded(expanded: Boolean) {
        super.setExpanded(expanded)
        if (!expanded && nameEdit.isFocused || extraInfoEdit.isFocused ||
            shoppingListAmountEdit.valueEdit.isFocused)
                imm.hideSoftInputFromWindow(windowToken, 0)

        nameEdit.isEditable = expanded
        shoppingListAmountEdit.valueIsDirectlyEditable = expanded
        extraInfoEdit.isEditable = expanded
        checkBox.inColorEditMode = expanded
        shoppingListAmountEdit.valueIsDirectlyEditable = expanded
        editButton.isActivated = expanded
        val newVisibility = if (expanded) View.VISIBLE
                            else          View.GONE
        shoppingListItemDetailsGroup.visibility = newVisibility
        if (extraInfoEdit.text.isNullOrBlank())
            extraInfoEdit.visibility = newVisibility

        // For some reason, expanding an shopping list item whose extra info is not
        // blank causes a flicker. It appears that changing the layout params of a view
        // and requesting a layout for it stops this flicker from occurring. Given that
        // the bug seems to originate from somewhere deep in Android code, having this
        // useless layout parameter changing and requested layout is an easy and per-
        // formance negligible way to prevent the bug.
        spacer.layoutParams.width = if (expanded) 1 else 0
        spacer.requestLayout()
    }

    fun setStrikeThroughEnabled(checked: Boolean, animate: Boolean = true) {
        nameEdit.setStrikeThroughEnabled(checked, animate)
        extraInfoEdit.setStrikeThroughEnabled(checked, animate)
    }

    private fun initSharedResources(context: Context) {
        imm = context.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        linkedItemDescriptionString = context.getString(R.string.linked_item_description)
        unlinkedItemDescriptionString = context.getString(R.string.unlinked_item_description)
        isInitialized = true
    }
}