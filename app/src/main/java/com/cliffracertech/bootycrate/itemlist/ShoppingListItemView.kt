/* Copyright 2021 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracertech.bootycrate.itemlist

import androidx.compose.animation.graphics.res.animatedVectorResource
import androidx.compose.animation.graphics.res.rememberAnimatedVectorPainter
import androidx.compose.animation.graphics.vector.AnimatedImageVector
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.cliffracertech.bootycrate.R
import com.cliffracertech.bootycrate.model.database.ListItem
import com.cliffracertech.bootycrate.model.database.ShoppingListItem
import com.cliffracertech.bootycrate.ui.theme.BootyCrateTheme

/** A checkmark that animates between its unchecked and checked states. */
@Composable fun AnimatedCheckmark(checked: Boolean) {
    val uncheckedToChecked = AnimatedImageVector.animatedVectorResource(
        R.drawable.animated_checkbox_unchecked_to_checked_checkmark)
    val uncheckedToCheckedPainter = rememberAnimatedVectorPainter(uncheckedToChecked, checked)
    val checkedToUnchecked = AnimatedImageVector.animatedVectorResource(
        R.drawable.animated_checkbox_checked_to_unchecked_checkmark)
    val checkedToUncheckedPainter = rememberAnimatedVectorPainter(checkedToUnchecked, !checked)
    Icon(painter = if (checked) uncheckedToCheckedPainter
                   else         checkedToUncheckedPainter,
         contentDescription = null,
         modifier = Modifier.offset(2.dp, 2.dp))
}

/**
 * A combination tinted checkbox and color indicator.
 *
 * CheckboxAndColorIndicator will appear as a checkbox tinted to match [tint]
 * when [showCheckbox] is true, or as a tinted circle otherwise. [checked]
 * indicates the current checked state of the checkbox when [showCheckbox] is
 * true.The parameters [checkboxClickLabel] and [colorIndicatorClickLabel]
 * define the click labels that will be used for each state. The parameters
 * [onCheckboxClick] and [onColorIndicatorClick] define the onclick callbacks
 * that will be invoked when [showCheckbox] is true or false, respectively.
 * The parameter [wasShowingCheckbox] only affects the state transition
 * animations. It should be false when [showCheckbox] is changed from false
 * to true, or true when showCheckbox has remained false but [checked] has
 * changed in value.
 */
@Composable fun CheckboxAndColorIndicator(
    showCheckbox: Boolean,
    wasShowingCheckbox: Boolean,
    tint: Color,
    checked: Boolean,
    checkboxClickLabel: String,
    onCheckboxClick: () -> Unit,
    colorIndicatorClickLabel: String,
    onColorIndicatorClick: () -> Unit,
    modifier: Modifier = Modifier,
) = Box(modifier
    .minTouchTargetSize()
    .clip(CircleShape)
    .then(if (showCheckbox) Modifier.clickable(
              role = Role.Checkbox,
              onClickLabel = checkboxClickLabel,
              onClick = onCheckboxClick)
          else Modifier.clickable(
              role = Role.Button,
              onClickLabel = colorIndicatorClickLabel,
              onClick = onColorIndicatorClick))
    .padding(11.dp)
) {
    val uncheckedToCheckedBg = AnimatedImageVector.animatedVectorResource(
        R.drawable.animated_checkbox_unchecked_to_checked_background)
    val uncheckedToCircle = AnimatedImageVector.animatedVectorResource(
        R.drawable.animated_checkbox_unchecked_background_to_circle)
    val checkedToCircle = AnimatedImageVector.animatedVectorResource(
        R.drawable.animated_checkbox_checked_background_to_circle)
    val uncheckedToCheckedPainter = rememberAnimatedVectorPainter(
        uncheckedToCheckedBg, checked)
    val uncheckedToCirclePainter = rememberAnimatedVectorPainter(
        uncheckedToCircle, !showCheckbox)
    val checkedToCirclePainter = rememberAnimatedVectorPainter(
        checkedToCircle, !showCheckbox)
    Icon(contentDescription = null,
         tint = tint,
         painter = when {
             showCheckbox && wasShowingCheckbox ->
                 uncheckedToCheckedPainter
             checked -> checkedToCirclePainter
             else -> uncheckedToCirclePainter
         })
    AnimatedCheckmark(checked && showCheckbox)
}

/** An interface containing callbacks for ShoppingListItem related interactions. */
interface ShoppingListItemCallback : ListItemCallback {
    /** The callback that will be invoked when the item view's checkbox is clicked */
    fun onCheckboxClick(id: Long)
}

/** Return a [ShoppingListItemCallback] implementation using the provided
 * lambdas as the implementations for the [ShoppingListItemCallback] methods
 * of the same name. */
fun shoppingListItemCallback(
    onClick: (Long) -> Unit = {},
    onLongClick: (Long) -> Unit = {},
    onSwipe: (Long) -> Unit = {},
    onColorGroupClick: (Long, ListItem.ColorGroup) -> Unit = { _, _ -> },
    onRenameRequest: (Long, String) -> Unit = { _, _ -> },
    onExtraInfoChangeRequest: (Long, String) -> Unit = { _, _ -> },
    onAmountChangeRequest: (Long, Int) -> Unit = { _, _ -> },
    onEditButtonClick: (Long) -> Unit = {},
    showEditButton: Boolean = true,
    onCheckboxClick: (Long) -> Unit = {}
) = object: ShoppingListItemCallback {
    override fun onClick(id: Long) = onClick(id)
    override fun onLongClick(id: Long) = onLongClick(id)
    override fun onSwipe(id: Long) = onSwipe(id)
    override fun onColorGroupClick(id: Long, colorGroup: ListItem.ColorGroup) =
        onColorGroupClick(id, colorGroup)
    override fun onRenameRequest(id: Long, newName: String) =
        onRenameRequest(id, newName)
    override fun onExtraInfoChangeRequest(id: Long, newExtraInfo: String) =
        onExtraInfoChangeRequest(id, newExtraInfo)
    override fun onAmountChangeRequest(id: Long, newAmount: Int) =
        onAmountChangeRequest(id, newAmount)
    override fun onEditButtonClick(id: Long) = onEditButtonClick(id)
    override val showEditButton = showEditButton
    override fun onCheckboxClick(id: Long) = onCheckboxClick(id)
}

/**
 * A visual display of a [ShoppingListItem] that also allows user
 * interactions to e.g. change the [ShoppingListItem]'s state.
 *
 * @param colorGroup The [ListItem.ColorGroup] that the item belongs to
 * @param name The name of the displayed item
 * @param extraInfo The extra info of the displayed item
 * @param amount The amount of the displayed item
 * @param isChecked Whether or not the item is currently checked
 * @param isSelected Whether or not the item is selected
 * @param selectionBrush The [Brush] that will be shown at half
 *     opacity over the normal background when isSelected is true
 * @param isEditable Whether or not the item will present itself in its editable state
 * @param callback The ShoppingListItemCallback whose method implementations
 *     will be used as the callbacks for user interactions
 * @param modifier The [Modifier] that will be used for the root layout
 */
@Composable fun ShoppingListItemView(
    sizes: ListItemViewSizes,
    id: Long,
    colorGroup: ListItem.ColorGroup,
    name: String,
    extraInfo: String,
    amount: Int,
    isChecked: Boolean,
    isLinked: Boolean,
    isEditable: Boolean,
    isSelected: Boolean,
    selectionBrush: Brush,
    callback: ShoppingListItemCallback,
    modifier: Modifier = Modifier
) = ListItemView(
    sizes, id,
    colorGroup, name, extraInfo,
    amount, isLinked, isEditable,
    isSelected, selectionBrush,
    callback, modifier,
    colorIndicator = { colorIndicatorModifier, isCollapsed, showColorPicker ->
        val colors = ListItem.ColorGroup.colors()
        val color = colors.getOrElse(colorGroup.ordinal) { colors.first() }
        CheckboxAndColorIndicator(
            showCheckbox = !isEditable,
            wasShowingCheckbox = !isCollapsed,
            tint = color,
            checked = isChecked,
            checkboxClickLabel = stringResource(
                R.string.item_checkbox_description, name),
            onCheckboxClick = { callback.onCheckboxClick(id) },
            colorIndicatorClickLabel = stringResource(
                R.string.edit_item_color_description, name),
            onColorIndicatorClick = showColorPicker,
            modifier = colorIndicatorModifier.requiredSize(48.dp))
    })

/**
 * A visual display of a [ShoppingListItem] that also allows user
 * interactions to e.g. change the [ShoppingListItem]'s state.
 *
 * @param item The [ShoppingListItem] instance whose data is being displayed
 * @param isSelected Whether or not the item is selected
 * @param selectionBrush The [Brush] that will be shown at half
 *     opacity over the normal background when isSelected is true
 * @param isEditable Whether or not the item will present itself in its editable state
 * @param callback The ShoppingListItemCallback whose method implementations
 *     will be used as the callbacks for user interactions
 * @param modifier The [Modifier] that will be used for the root layout
 */
@Composable fun ShoppingListItemView(
    sizes: ListItemViewSizes,
    item: ShoppingListItem,
    isSelected: Boolean,
    selectionBrush: Brush,
    isEditable: Boolean,
    callback: ShoppingListItemCallback,
    modifier: Modifier = Modifier
) = ShoppingListItemView(
    sizes, item.id,
    item.colorGroup, item.name, item.extraInfo,
    item.amount, item.isChecked, item.isLinked, isEditable,
    isSelected, selectionBrush, callback, modifier)

@Preview @Composable
fun ShoppingListItemViewPreview() = BootyCrateTheme {
    var isSelected by remember { mutableStateOf(false) }
    val color1 = MaterialTheme.colors.primary
    val color2 = MaterialTheme.colors.secondary
    val brush = remember { Brush.horizontalGradient(listOf(color1, color2)) }
    var isEditable by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("Test item") }
    var extraInfo by remember { mutableStateOf("Test extra info") }
    var colorGroup by remember { mutableStateOf(ListItem.ColorGroup.Orange) }
    var amount by remember { mutableStateOf(5) }
    var isChecked by remember { mutableStateOf(false) }

    val sizes = remember { ListItemViewSizes() }
    val callback = remember { shoppingListItemCallback(
        onLongClick = { isSelected = !isSelected },
        onColorGroupClick = { _, clickedColorGroup ->
            colorGroup = clickedColorGroup
        }, onRenameRequest = { _, newName -> name = newName },
        onExtraInfoChangeRequest = { _, newExtraInfo ->
            extraInfo = newExtraInfo
        }, onAmountChangeRequest = { _, newAmount ->
            amount = newAmount
        }, onEditButtonClick = { isEditable = !isEditable },
        onCheckboxClick = { isChecked = !isChecked })
    }
    Box(Modifier
        .background(MaterialTheme.colors.primary)
        .padding(8.dp)
    ) {
        ShoppingListItemView(
            sizes, id = 0, colorGroup, name, extraInfo,
            amount, isChecked, isLinked = true,
            isEditable, isSelected, brush, callback)
    }
}