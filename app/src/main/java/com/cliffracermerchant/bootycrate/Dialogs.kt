/* Copyright 2020 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracermerchant.bootycrate

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.TypedValue
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.setPadding
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.preference.PreferenceManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import dev.sasikanth.colorsheet.ColorSheet
import kotlinx.android.synthetic.main.inventory_item_details_layout.view.*
import kotlinx.android.synthetic.main.inventory_item_layout.view.*
import kotlinx.android.synthetic.main.share_dialog.*

/** An object that contains functions to open dialogs.
 *
 *  Dialog contains functions to open various dialogs. An instance of an activity must
 *  be supplied to Dialog via the init function. If any of the dialogs are called
 *  before init, an exception will occur. */
object Dialog {
    private lateinit var context: Context
    private lateinit var fragmentManager: FragmentManager
    fun init(activity: AppCompatActivity) {
        context = activity
        fragmentManager = activity.supportFragmentManager
    }

    /** Display a color picker dialog to choose from one of ViewModelItem's twelve colors.
     *
     *  Note that the initial color parameter and the return value are the
     *  indices of the chosen color, not the Android color value for the color. */
    fun colorPicker(initialColorIndex: Int = 0, callback: (Int) -> Unit) {
        val index = initialColorIndex.coerceIn(ViewModelItem.Colors.indices)
        val initialColor = ViewModelItem.Colors[index]
        val colorPicker = ColorSheet().colorPicker(ViewModelItem.Colors, initialColor,
                                                   noColorOption = false) { color ->
            val colorIndex = ViewModelItem.Colors.indexOf(color)
            callback(if (colorIndex != -1) colorIndex else 0)
        }
        colorPicker.show(fragmentManager)
    }

    private enum class ShareOption { TextMessage, Email }
    /** Display a dialog to provide options to share the list of items */
    fun <Entity: ViewModelItem>shareList(items: List<Entity>, snackBarAnchor: View) {
        themedAlertBuilder().setView(R.layout.share_dialog).create().apply {
            setOnShowListener {
                shareTextMessageOption.setOnClickListener {
                    shareList(items, snackBarAnchor, ShareOption.TextMessage)
                    dismiss()
                }
                shareEmailOption.setOnClickListener {
                    shareList(items, snackBarAnchor, ShareOption.Email)
                    dismiss()
                }
            }
        }.show()
    }

    /** Export the list of items via the selected share option. */
    private fun <Entity: ViewModelItem>shareList(
        items: List<Entity>,
        snackBarAnchor: View,
        shareOption: ShareOption
    ) {
        val intent = Intent(Intent.ACTION_SENDTO)
        intent.type = "HTTP.PLAIN_TEXT_TYPE"

        var message = ""
        for (item in items)
            message += item.toString() + "\n"
        message.removeSuffix("\n")

        when (shareOption) {
            ShareOption.TextMessage -> {
                intent.putExtra("sms_body", message)
                intent.data = Uri.parse("smsto:")
            } ShareOption.Email -> {
                intent.data = Uri.parse("mailto:")
                val subject = context.getString(R.string.shopping_list_navigation_item_name)
                intent.putExtra(Intent.EXTRA_SUBJECT, subject)
                intent.putExtra(Intent.EXTRA_TEXT, message)
            }
        }
        if (intent.resolveActivity(context.packageManager) != null)
            context.startActivity(intent)
        else Snackbar.make(snackBarAnchor, R.string.share_error_message, Snackbar.LENGTH_SHORT).
                      setAnchorView(snackBarAnchor).show()
    }

    fun aboutApp() { themedAlertBuilder().setView(R.layout.about_app_dialog).show() }

    /** Open a dialog to create a new shopping list item, and invoke @param callback on the new item. */
    fun newShoppingListItem(callback: (ShoppingListItem) -> Unit) =
        newItem<ShoppingListItem>(callback, isShoppingListItem = true)

    /** Open a dialog to create a new inventory item, and invoke @param callback on the new item. */
    fun newInventoryItem(callback: (InventoryItem) -> Unit) =
        newItem<InventoryItem>(callback, isShoppingListItem = false)

    /** Open a dialog to ask the user to the type of database import they want (merge existing or overwrite, and recreate the given activity if the import requires it. */
    fun importDatabaseFromUri(maybeUri: Uri?, activity: FragmentActivity?)  {
        val uri = maybeUri ?: return
        themedAlertBuilder().
            setMessage(R.string.import_database_question_message).
            setNeutralButton(android.R.string.cancel) { _, _ -> }.
            setNegativeButton(R.string.import_database_question_merge_option) { _, _ ->
                BootyCrateDatabase.mergeWithBackup(context, uri)
            }.setPositiveButton(R.string.import_database_question_overwrite_option) { _, _ ->
                themedAlertBuilder().
                    setMessage(R.string.import_database_overwrite_confirmation_message).
                    setNegativeButton(android.R.string.no) { _, _ -> }.
                    setPositiveButton(android.R.string.yes) { _, _ ->
                        BootyCrateDatabase.replaceWithBackup(context, uri)
                        // The pref pref_viewmodels_need_cleared needs to be set to true so that
                        // when the MainActivity is recreated, it will clear its ViewModelStore
                        // and use the DAOs of the new database instead of the old one.
                        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
                        val editor = prefs.edit()
                        editor.putBoolean(context.getString(R.string.pref_viewmodels_need_cleared), true)
                        editor.apply()
                        activity?.recreate()
                    }.show()
            }.show()
    }

    /** Return an AlertDialog.Builder that uses the current theme's alertDialogTheme. */
    fun themedAlertBuilder(): MaterialAlertDialogBuilder {
        // AlertDialog seems to ignore the theme's alertDialogTheme value, making it
        // necessary to pass it's value in manually to the AlertDialog.builder constructor.
        val typedValue = TypedValue()
        context.theme.resolveAttribute(R.attr.materialAlertDialogTheme, typedValue, true)
        return MaterialAlertDialogBuilder(context, typedValue.data)
    }

    /** Return a themed TextView for use as a custom title in an AlertDialog.
     *
     *  Changing the style of AlertDialog's default title seems to not work when
     *  done through styles (e.g. with the android:windowTitleStyle attribute).
     *  themedAlertDialogTitle returns a TextView with a padding, text size, text
     *  color, and background color suited for use as a custom title for an Alert-
     *  Dialog (using AlertDialog.setCustomTitle() or AlertDialog.Builder.setCus-
     *  tomTitle(). */
    private fun themedAlertTitle(title: String): TextView {
        val titleView = TextView(context)
        val dm = context.resources.displayMetrics
        titleView.textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 7f, dm)
        titleView.setPadding(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 12f, dm).toInt())

        val typedValue = TypedValue()
        context.theme.resolveAttribute(android.R.attr.textColorPrimary, typedValue, true)
        titleView.setTextColor(typedValue.data)

        titleView.text = title
        return titleView
    }
    private fun themedAlertTitle(titleResId: Int) = themedAlertTitle(context.getString(titleResId))

    private fun <Entity: ExpandableSelectableItem>newItem(
        callback: (Entity) -> Unit,
        isShoppingListItem: Boolean
    ) {
        val imm = context.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager?
        val view = InventoryItemView(context)
        if (isShoppingListItem) view.prepForNewShoppingListItem()
        else                    view.prepForNewInventoryItem()
        val dialog = themedAlertBuilder().
            setCustomTitle(themedAlertTitle(R.string.add_item_button_name)).
            setNeutralButton(android.R.string.cancel) { _, _ -> }.
            setNegativeButton(context.getString(R.string.add_another_item_button_description)) { _, _ ->}.
            setPositiveButton(android.R.string.ok) { _, _ ->
                callback((if (isShoppingListItem) shoppingListItemFromItemView(view)
                          else                    inventoryItemFromItemView(view)) as Entity)
            }.setView(view).create()
        dialog.apply {
            setOnShowListener {
                // Override the add another button's default on click listener
                // to prevent it from closing the dialog when clicked
                getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener {
                    callback((if (isShoppingListItem) shoppingListItemFromItemView(view)
                              else                    inventoryItemFromItemView(view)) as Entity)
                    view.apply {
                        nameEdit.text?.clear()
                        extraInfoEdit.text?.clear()
                        inventoryAmountEdit.apply { value = minValue }
                        // We'll leave the color edit set to whichever color it was on previously,
                        // in case the user wants to add items with like colors consecutively.
                        nameEdit.requestFocus()
                        imm?.showSoftInput(nameEdit, InputMethodManager.SHOW_IMPLICIT)
                    }
                }
                view.nameEdit.requestFocus()
                imm?.showSoftInput(view.nameEdit, InputMethodManager.SHOW_IMPLICIT)
            }
            dialog.show()
        }
    }

    private fun InventoryItemView.prepForNewShoppingListItem() {
        background = null
        editButton.visibility = View.GONE
        inventoryItemDetailsGroup.visibility = View.GONE
        setColorIndex(0, animate = false)
        colorEdit.setOnClickListener {
            colorPicker(ViewModelItem.Colors[0]) { chosenColorIndex ->
                colorIndex = chosenColorIndex
            }
        }
        nameEdit.isEditable = true
        extraInfoEdit.isEditable = true
        inventoryAmountEdit.valueIsDirectlyEditable = true
        inventoryAmountEdit.minValue = 1
    }

    private fun InventoryItemView.prepForNewInventoryItem() {
        background = null
        expand()
        editButton.visibility = View.GONE
        // Setting collapseButton's visibility to GONE or INVISIBLE doesn't seem to work for some reason
        collapseButton.alpha = 0f
        collapseButton.setOnClickListener { }
        addToShoppingListTriggerEdit.value = addToShoppingListTriggerEdit.minValue
        setColorIndex(0, animate = false)
        colorEdit.setOnClickListener {
            colorPicker(ViewModelItem.Colors[0]) { chosenColorIndex ->
                colorIndex = chosenColorIndex
            }
        }
    }
}