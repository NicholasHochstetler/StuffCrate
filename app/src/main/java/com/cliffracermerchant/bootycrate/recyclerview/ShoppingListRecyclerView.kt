/* Copyright 2020 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracermerchant.bootycrate.recyclerview

import android.content.Context
import android.util.AttributeSet
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.recyclerview.widget.DiffUtil
import com.cliffracermerchant.bootycrate.R
import com.cliffracermerchant.bootycrate.ShoppingListItemView
import com.cliffracermerchant.bootycrate.database.ShoppingListItem
import com.cliffracermerchant.bootycrate.shoppingListViewModel
import com.cliffracermerchant.bootycrate.utils.AnimatorConfig
import java.util.*
import kotlin.collections.set

/**
 * A RecyclerView to display the data provided by a ShoppingListViewModel.
 *
 * ShoppingListRecyclerView is a ExpandableSelectableRecyclerView subclass
 * specialized for displaying the contents of a shopping list. ShoppingList-
 * RecyclerView adds a sortByChecked property, which mirrors the ShoppingList-
 * ViewModel property, for convenience. sortByChecked should not be changed
 * the property viewModel is initialized, or an exception will be thrown.
 */
class ShoppingListRecyclerView(context: Context, attrs: AttributeSet) :
    ExpandableSelectableRecyclerView<ShoppingListItem>(context, attrs)
{
    override val diffUtilCallback = ShoppingListDiffUtilCallback()
    override val adapter = ShoppingListAdapter()
    override val viewModel = shoppingListViewModel(context)

    var sortByChecked get() = viewModel.sortByChecked
        set(value) { viewModel.sortByChecked = value }

    init {
        itemAnimator.animatorConfig = AnimatorConfig(
            context.resources.getInteger(R.integer.shoppingListItemAnimationDuration).toLong(),
            AnimationUtils.loadInterpolator(context, R.anim.default_interpolator))
        itemAnimator.registerAdapterDataObserver(adapter)
    }

    /**
     * A RecyclerView.Adapter to display the contents of a list of shopping list items.
     *
     * ShoppingListAdapter is a subclass of ExpandableSelectableItemAdapter using
     * ShoppingListItemViewHolder instances to represent shopping list items. Its
     * overrides of onBindViewHolder make use of the ShoppingListItem.Field values
     * passed by ShoppingListItemDiffUtilCallback to support partial binding. Note
     * that ShoppingListAdapter assumes that any payloads passed to it are of the
     * type EnumSet<ShoppingListItem.Field>. If a payload of another type is
     * passed to it, an exception will be thrown.
     */
    @Suppress("UNCHECKED_CAST")
    inner class ShoppingListAdapter : ExpandableSelectableItemAdapter<ShoppingListItemViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            ShoppingListItemViewHolder(ShoppingListItemView(context, itemAnimator.animatorConfig))

        override fun onBindViewHolder(holder: ShoppingListItemViewHolder, position: Int) {
            holder.view.update(holder.item)
            super.onBindViewHolder(holder, position)
        }

        override fun onBindViewHolder(
            holder: ShoppingListItemViewHolder,
            position: Int,
            payloads: MutableList<Any>
        ) {
            if (payloads.size == 0)
                return onBindViewHolder(holder, position)
            val unhandledChanges = mutableListOf<Any>()

            for (payload in payloads) {
                if (payload is EnumSet<*>) {
                    val item = getItem(position)
                    val changes = payload as EnumSet<ShoppingListItem.Field>
                    val ui = holder.view.ui

                    if (changes.contains(ShoppingListItem.Field.Name) &&
                        ui.nameEdit.text.toString() != item.name)
                            ui.nameEdit.setText(item.name)
                    if (changes.contains(ShoppingListItem.Field.ExtraInfo) &&
                        ui.extraInfoEdit.text.toString() != item.extraInfo)
                            ui.extraInfoEdit.setText(item.extraInfo)
                    if (changes.contains(ShoppingListItem.Field.Color) &&
                        ui.checkBox.colorIndex != item.color)
                            ui.checkBox.colorIndex = item.color
                    if (changes.contains(ShoppingListItem.Field.Amount) &&
                        ui.amountEdit.value != item.amount)
                            ui.amountEdit.value = item.amount
                    if (changes.contains(ShoppingListItem.Field.IsExpanded) &&
                        holder.view.isExpanded != item.isExpanded)
                            holder.view.setExpanded(item.isExpanded)
                    if (changes.contains(ShoppingListItem.Field.IsSelected) &&
                        holder.view.isInSelectedState != item.isSelected)
                            holder.view.setSelectedState(item.isSelected)
                    if (changes.contains(ShoppingListItem.Field.IsChecked) &&
                        ui.checkBox.isChecked != item.isChecked)
                            ui.checkBox.isChecked = item.isChecked
                }
                else unhandledChanges.add(payload)
            }
            if (unhandledChanges.isNotEmpty())
                super.onBindViewHolder(holder, position, unhandledChanges)
        }
    }

    /**
     * A ExpandableSelectableItemViewHolder that wraps an instance of ShoppingListItemView.
     *
     * ShoppingListItemViewHolder is a subclass of ExpandableSelectableItemView-
     * Holder that holds an instance of ShoppingListItemView to display the data
     * for a ShoppingListItem. It also connects changes in the checkbox state to
     * view model updateIsChecked calls.
     */
    inner class ShoppingListItemViewHolder(val view: ShoppingListItemView) :
            ExpandableSelectableItemViewHolder(view) {

        init {
            view.ui.checkBox.onCheckedChangedListener = { checked ->
                viewModel.updateIsChecked(item.id, checked)
                view.setStrikeThroughEnabled(checked)
            }
        }
    }

    /**
     * Computes a diff between two shopping list item lists.
     *
     * ShoppingListDiffUtilCallback uses the ids of shopping list items to deter-
     * mine if they are the same or not. If they are the same, the change payload
     * will be an instance of EnumSet<ShoppingListItem.Field> that contains the
     * ShoppingListItem.Field values for all of the fields that were changed.
     */
    class ShoppingListDiffUtilCallback : DiffUtil.ItemCallback<ShoppingListItem>() {
        private val listChanges = mutableMapOf<Long, EnumSet<ShoppingListItem.Field>>()
        private val itemChanges = EnumSet.noneOf(ShoppingListItem.Field::class.java)

        override fun areItemsTheSame(oldItem: ShoppingListItem, newItem: ShoppingListItem) =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: ShoppingListItem, newItem: ShoppingListItem) =
            itemChanges.apply {
                clear()
                if (newItem.name != oldItem.name)             add(ShoppingListItem.Field.Name)
                if (newItem.extraInfo != oldItem.extraInfo)   add(ShoppingListItem.Field.ExtraInfo)
                if (newItem.color != oldItem.color)           add(ShoppingListItem.Field.Color)
                if (newItem.amount != oldItem.amount)         add(ShoppingListItem.Field.Amount)
                if (newItem.isExpanded != oldItem.isExpanded) add(ShoppingListItem.Field.IsExpanded)
                if (newItem.isSelected != oldItem.isSelected) add(ShoppingListItem.Field.IsSelected)
                if (newItem.isChecked != oldItem.isChecked)   add(ShoppingListItem.Field.IsChecked)

                if (!isEmpty())
                    listChanges[newItem.id] = EnumSet.copyOf(this)
            }.isEmpty()

        override fun getChangePayload(oldItem: ShoppingListItem, newItem: ShoppingListItem) =
            listChanges.remove(newItem.id)
    }
}