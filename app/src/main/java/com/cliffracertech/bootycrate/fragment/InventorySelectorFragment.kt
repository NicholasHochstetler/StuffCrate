/*
 * Copyright 2021 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory.
 */

package com.cliffracertech.bootycrate.fragment

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.os.Bundle
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewPropertyAnimator
import android.widget.LinearLayout
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.cliffracertech.bootycrate.R
import com.cliffracertech.bootycrate.database.BootyCrateInventory
import com.cliffracertech.bootycrate.database.inventoryViewModel
import com.cliffracertech.bootycrate.databinding.InventorySelectorBinding
import com.cliffracertech.bootycrate.databinding.InventoryViewBinding
import com.cliffracertech.bootycrate.recyclerview.ExpandableItemAnimator
import com.cliffracertech.bootycrate.recyclerview.ItemSpacingDecoration
import com.cliffracertech.bootycrate.utils.AnimatorConfig
import com.cliffracertech.bootycrate.utils.applyConfig
import com.cliffracertech.bootycrate.utils.dpToPixels
import com.cliffracertech.bootycrate.utils.intValueAnimator
import java.util.*

class InventorySelectorFragment : Fragment() {
    var ui: InventorySelectorBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = InventorySelectorBinding.inflate(inflater, container, false)
        .apply { ui = this }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        ui?.recyclerView?.observeViewModel(viewLifecycleOwner)
    }

    override fun onDestroyView() {
        ui = null
        super.onDestroyView()
    }
}

class InventorySelectorRecyclerView(context: Context, attrs: AttributeSet) :
    RecyclerView(context, attrs)
{
    private val viewModel = inventoryViewModel(context)
    private val adapter = Adapter()
    private val animator = ExpandableItemAnimator(AnimatorConfig.appDefault(context))

    init {
        setAdapter(adapter)
        setHasFixedSize(true)
        layoutManager = LinearLayoutManager(context)
        val spacing = resources.getDimensionPixelSize(R.dimen.recycler_view_item_spacing)
        addItemDecoration(ItemSpacingDecoration(spacing))
        itemAnimator = animator
    }

    fun observeViewModel(owner: LifecycleOwner) =
        viewModel.inventories.observe(owner) { adapter.submitList(it) }

    private inner class Adapter : ListAdapter<BootyCrateInventory, ViewHolder>(DiffUtilCallback()) {

        init { setHasStableIds(true) }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            ViewHolder(InventoryView(context))

        override fun onBindViewHolder(holder: ViewHolder, position: Int) =
            (holder.itemView as InventoryView).update(getItem(position))

        override fun onBindViewHolder(holder: ViewHolder, position: Int, payloads: MutableList<Any>) {
            if (payloads.size == 0)
                return onBindViewHolder(holder, position)

            for (payload in payloads) {
                val item = getItem(position)
                val ui = (holder.itemView as InventoryView).ui
                @Suppress("UNCHECKED_CAST")
                val changes = payload as EnumSet<Field>

                if (changes.contains(Field.Name) && item.name != ui.nameView.text.toString())
                    ui.nameView.setText(item.name)
                if (changes.contains(Field.ShoppingListItemCount))
                    ui.shoppingListItemCountView.text = item.shoppingListItemCount.toString()
                if (changes.contains(Field.InventoryItemCount))
                    ui.inventoryItemCountView.text = item.inventoryItemCount.toString()
                if (changes.contains(Field.IsSelected))
                    holder.view.setSelectedState(item.isSelected)
            }
        }

        override fun getItemId(position: Int) = currentList[position].id
    }

    private inner class ViewHolder(view: InventoryView) : RecyclerView.ViewHolder(view) {
        val view get() = itemView as InventoryView
        val item: BootyCrateInventory get() = adapter.currentList[adapterPosition]

        init {
            view.ui.optionsButton.setOnClickListener {

            }
        }
    }

    private enum class Field { Name, ShoppingListItemCount, InventoryItemCount, IsSelected }

    private class DiffUtilCallback : DiffUtil.ItemCallback<BootyCrateInventory>() {
        private val listChanges = mutableMapOf<Long, EnumSet<Field>>()
        private val itemChanges = EnumSet.noneOf(Field::class.java)

        override fun areItemsTheSame(oldItem: BootyCrateInventory, newItem: BootyCrateInventory) =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: BootyCrateInventory, newItem: BootyCrateInventory) =
            itemChanges.apply {
                clear()
                if (newItem.name != oldItem.name)
                    add(Field.Name)
                if (newItem.shoppingListItemCount != oldItem.shoppingListItemCount)
                    add(Field.ShoppingListItemCount)
                if (newItem.inventoryItemCount != oldItem.inventoryItemCount)
                    add(Field.InventoryItemCount)
                if (newItem.isSelected != oldItem.isSelected)
                    add(Field.IsSelected)

                if (!isEmpty())
                    listChanges[newItem.id] = EnumSet.copyOf(this)
            }.isEmpty()

        override fun getChangePayload(oldItem: BootyCrateInventory, newItem: BootyCrateInventory) =
            listChanges.remove(newItem.id)
    }
}

class InventoryView(context: Context) : LinearLayout(context) {
    val ui = InventoryViewBinding.inflate(LayoutInflater.from(context), this)
    private val gradientOutline: GradientDrawable

    init {
        val inventoryIcon = ContextCompat.getDrawable(context, R.drawable.inventory_icon)
        val shoppingListIcon = ContextCompat.getDrawable(context, R.drawable.shopping_cart_icon)
        val iconPadding = resources.dpToPixels(3f).toInt()
        ui.shoppingListItemCountView.setCompoundDrawablesRelativeWithIntrinsicBounds(shoppingListIcon, null, null, null)
        ui.inventoryItemCountView.setCompoundDrawablesRelativeWithIntrinsicBounds(inventoryIcon, null, null, null)
        inventoryIcon?.bounds?.inset(iconPadding, iconPadding)
        shoppingListIcon?.bounds?.inset(iconPadding, iconPadding)

        background = ContextCompat.getDrawable(context, R.drawable.recycler_view_item).also {
            gradientOutline = ((it as LayerDrawable).getDrawable(1) as LayerDrawable).getDrawable(0) as GradientDrawable
        }
        layoutParams = RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                                                 ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    fun update(inventory: BootyCrateInventory) {
        ui.nameView.setText(inventory.name)
        ui.shoppingListItemCountView.text = inventory.shoppingListItemCount.toString()
        ui.inventoryItemCountView.text = inventory.inventoryItemCount.toString()
    }

    private var _isInSelectedState = false
    val isInSelectedState get() = _isInSelectedState
    fun select() = setSelectedState(true)
    fun deselect() = setSelectedState(false)
    fun setSelectedState(selected: Boolean, animate: Boolean = true) {
        _isInSelectedState = selected
        val endAlpha = if (selected) 255 else 0
        if (animate) intValueAnimator(setter = gradientOutline::setAlpha,
                                      from = gradientOutline.alpha,
                                      to = endAlpha).start()
        else gradientOutline.alpha = endAlpha
    }
}