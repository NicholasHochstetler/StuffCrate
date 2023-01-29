/* Copyright 2021 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracertech.bootycrate.itemlist

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.ui.unit.Dp
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import com.cliffracertech.bootycrate.R
import com.cliffracertech.bootycrate.ui.theme.BootyCrateTheme
import com.cliffracertech.bootycrate.model.database.ListItem
import kotlin.math.ceil

/** Compose the contents of [content] while being provided with the
* unconstrained [Dp] dimensions of the content provided in [measurable]. */
@Composable fun MeasureUnconstrainedViewSize(
    measurable: @Composable () -> Unit,
    content: @Composable (Dp, Dp) -> Unit,
) = SubcomposeLayout { constraints ->
    val (measuredWidth, measuredHeight) = run {
        val size = subcompose("measurable", measurable)[0]
            .measure(Constraints())
        size.width.toDp() to size.height.toDp()
    }
    val contentPlaceable = subcompose("content") {
        content(measuredWidth, measuredHeight)
    }[0].measure(constraints)
    layout(contentPlaceable.width, contentPlaceable.height) {
        contentPlaceable.place(0, 0)
    }
}

/**
* A text field that toggles between an unconstrained size when [readOnly] is
* true, and a minimum touch target size when [readOnly] is false.
*
* @param text The text that will be displayed
* @param onTextChange The callback that will be invoked when the user attempts
*     to change the [text] value through input
* @param modifier The [Modifier] that will be used for the text field
* @param tint The tint that will be used for the text cursor
* @param readOnly Whether or not the text field will prevent editing of the
*     text and allow its size to fall below minimum touch target sizes.
* @param textStyle The [TextStyle] that will be used for the text.
*/
@Composable fun TextFieldEdit(
    text: String,
    onTextChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current,
    readOnly: Boolean = true,
    textStyle: TextStyle = LocalTextStyle.current,
) = MeasureUnconstrainedViewSize(measurable = {
    BasicTextField(
        value = text,
        onValueChange = {},
        modifier = modifier,
        readOnly = readOnly,
        textStyle = textStyle,
        singleLine = true)
}) { _, minHeight ->
    val height = maxOf(minHeight, (if (readOnly) 0 else 48).dp)
    Box(modifier.animateContentSize().height(height)) {
        BasicTextField(
            value = text,
            onValueChange = onTextChange,
            modifier = Modifier.align(Alignment.CenterStart),
            readOnly = readOnly,
            textStyle = textStyle,//.copy(textAlign = TextAlign.Center),
            singleLine = true,
            cursorBrush = remember(tint) { SolidColor(tint) },
        ) { innerTextField ->
            Box(contentAlignment = Alignment.CenterStart) {
                innerTextField()
                AnimatedVisibility(
                    visible = !readOnly,
                    modifier = Modifier.align(Alignment.BottomStart),
                    enter = fadeIn(), exit = fadeOut()
                ) {
                    Divider(Modifier, LocalContentColor.current, 1.dp)
                }
            }
        }
    }
}

/**
* An editor for an Int [amount] that displays decrease and increase buttons on
* either side of the [amount], and allows direct keyboard editing of the value
* when [isEditableByKeyboard] is true. [amountDecreaseDescription] and
* [amountIncreaseDescription] will be used as the content descriptions for the
* decrease and increase buttons, respectively, while [tint] will be used to
* tint the text cursor when the [amount] is being edited via they keyboard.
* An attempt to change the amount either by keyboard or the buttons will cause
* [onAmountChangeRequest] to be invoked.
*/
@Composable fun AmountEdit(
    amount: Int,
    isEditableByKeyboard: Boolean,
    tint: Color,
    amountDecreaseDescription: String,
    amountIncreaseDescription: String,
    onAmountChangeRequest: (Int) -> Unit,
    modifier: Modifier = Modifier,
) = MeasureUnconstrainedViewSize(measurable = {
    BasicTextField(
        value = amount.toString(),
        onValueChange = {},
        modifier = Modifier.width(IntrinsicSize.Max).height(IntrinsicSize.Max),
        textStyle = MaterialTheme.typography.h5.copy(textAlign = TextAlign.Center),
        singleLine = true)
}) { valueWidth, valueHeight ->
    val valueEditMinWidth = (if (isEditableByKeyboard) 48 else 0).dp
    val width = maxOf(valueWidth, valueEditMinWidth) + 96.dp
    val height = maxOf(valueHeight, 48.dp)

    Box(modifier.animateContentSize().size(width, height)) {
        IconButton(
            onClick = { onAmountChangeRequest(amount - 1) },
            modifier = Modifier.align(Alignment.CenterStart),
        ) {
            Icon(painterResource(R.drawable.minus_icon), amountDecreaseDescription)
        }

        IconButton(
            onClick = { onAmountChangeRequest(amount + 1) },
            modifier = Modifier.align(Alignment.CenterEnd),
        ) {
            Icon(painterResource(R.drawable.plus_icon), amountIncreaseDescription)
        }

        BasicTextField(
            value = amount.toString(),
            onValueChange = { onAmountChangeRequest(it.toInt()) },
            modifier = Modifier.align(Alignment.Center),
            readOnly = !isEditableByKeyboard,
            textStyle = MaterialTheme.typography.h5.copy(textAlign = TextAlign.Center),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            cursorBrush = remember(tint) { SolidColor(tint) },
            decorationBox = { valueDisplay ->
                Box(Modifier.animateContentSize(), Alignment.Center) {
                    valueDisplay()
                    AnimatedVisibility(
                        visible = isEditableByKeyboard,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .width(valueEditMinWidth),
                        enter = fadeIn(), exit = fadeOut(),
                    ) {
                        Divider(Modifier, LocalContentColor.current, 1.dp)
                    }
                }
            })
    }
}

@Preview @Composable
fun AmountEditPreview() = BootyCrateTheme {
    Row {
        var isEditable by remember { mutableStateOf(false) }
        var amount by remember { mutableStateOf(2) }
        AmountEdit(
            amount = amount,
            tint = Color.Red,
            isEditableByKeyboard = isEditable,
            amountDecreaseDescription = "",
            amountIncreaseDescription = "",
            onAmountChangeRequest = { amount = it },
            modifier = Modifier.background(MaterialTheme.colors.surface,
                                           MaterialTheme.shapes.small))
        IconButton({ isEditable = !isEditable }) {
            Icon(Icons.Default.Edit, "")
        }
    }
}

/**
* A grid arrangement of color options to choose from.
* @param modifier The [Modifier] that will be used for the picker
* @param currentColor The [Color] in [colors] that will be
*     identified as the currently picked color by a checkmark
* @param colors A [List] containing all of the [Color] options to display
* @param colorDescriptions A [List] the same size as [colors]
*     containing [String] descriptions for each of the color options.
* @param onColorClick The callback that will be invoked when a color
*     option is chosen. Both the index of and the [Color] value of
*     the clicked option are provided.
*/
@Composable fun ColorPicker(
    modifier: Modifier = Modifier,
    currentColor: Color? = null,
    colors: List<Color>,
    colorDescriptions: List<String>,
    onColorClick: (Int, Color) -> Unit,
) = BoxWithConstraints {
    val maxColorsPerRow = (maxWidth / 48.dp).toInt()
    val rows = ceil(colors.size.toFloat() / maxColorsPerRow).toInt()
    val columns = colors.size / rows

    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        modifier = modifier,
        contentPadding = PaddingValues(vertical = 8.dp, horizontal = 2.dp),
        verticalArrangement = Arrangement.SpaceEvenly,
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        require(colors.size == colorDescriptions.size)
        itemsIndexed(items = colors, contentType = { _, _ -> true }) { index, color ->
            val label = stringResource(R.string.edit_item_color_description,
                                       colorDescriptions[index])
            Box(Modifier
                .requiredSize(48.dp)
                .padding(10.dp)
                .background(color, CircleShape)
                .clickable(
                    role = Role.Button,
                    onClick = { onColorClick(index, color) },
                    onClickLabel = label)
            ) {
                if (color == currentColor)
                    // The check mark's offset makes it appear more centered
                    Icon(Icons.Default.Check, null, Modifier.offset(1.dp, 2.dp))
            }
        }
    }
}

@Preview @Composable
fun ColorPickerPreview() = BootyCrateTheme {
    val colors = ListItem.Color.asComposeColors()
    var currentColor by remember { mutableStateOf(colors.first()) }
    val descriptions = ListItem.Color.descriptions()
    Surface(shape = MaterialTheme.shapes.large) {
        ColorPicker(
            currentColor = currentColor,
            colors = colors,
            colorDescriptions = descriptions,
        ) { _, color -> currentColor = color }
    }

}