/* Copyright 2021 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracertech.bootycrate.bottomdrawer

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.cliffracertech.bootycrate.R
import com.cliffracertech.bootycrate.model.NavigationState
import com.cliffracertech.bootycrate.model.database.ItemGroup
import com.cliffracertech.bootycrate.model.database.ItemGroupDao
import com.cliffracertech.bootycrate.model.database.ItemGroupNameValidator
import com.cliffracertech.bootycrate.model.database.SettingsDao
import com.cliffracertech.bootycrate.ui.ConfirmatoryDialogState
import com.cliffracertech.bootycrate.ui.NameDialogState
import com.cliffracertech.bootycrate.utils.StringResource
import com.cliffracertech.bootycrate.utils.collectAsState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

interface RenameItemGroupButtonHandler {
    val renameItemGroupDialogState: NameDialogState
    fun onItemGroupRenameClick(itemGroup: ItemGroup)
}

class RenameItemGroupButtonHandlerImpl(
    private val dao: ItemGroupDao,
    private val coroutineScope: CoroutineScope,
) : RenameItemGroupButtonHandler {
    private val validator = ItemGroupNameValidator(dao)
    private val validatorMessage by validator.message.collectAsState(null, coroutineScope)
    private var renameDialogTargetId by mutableStateOf<Long?>(null)

    private fun hideRenameDialog() {
        renameItemGroupDialogState = NameDialogState.NotShowing
        validator.clear()
    }

    private fun dialogShowingState(
        originalName: String,
    ) = NameDialogState.Showing(
        currentNameProvider = validator::value::get,
        messageProvider = ::validatorMessage,
        onNameChange = validator::value::set,
        onCancel = ::hideRenameDialog,
        onConfirm = {
            coroutineScope.launch {
                val validatedName = validator.validate()
                val id = renameDialogTargetId
                if (validatedName == null || id == null)
                    return@launch
                dao.updateName(id, validatedName)
                hideRenameDialog()
            }
        }, title = StringResource(
            R.string.rename_item_group_dialog_title, originalName))

    override var renameItemGroupDialogState by
            mutableStateOf<NameDialogState>(NameDialogState.NotShowing)
        private set

    override fun onItemGroupRenameClick(itemGroup: ItemGroup) {
        renameItemGroupDialogState = dialogShowingState(originalName = itemGroup.name)
        renameDialogTargetId = itemGroup.id
    }
}

interface DeleteItemGroupButtonHandler {
    val deleteItemGroupDialogState: ConfirmatoryDialogState
    fun onItemGroupDeleteClick(itemGroup: ItemGroup)
}

class DeleteItemGroupButtonHandlerImpl(
    private val dao: ItemGroupDao,
    coroutineScope: CoroutineScope,
) : DeleteItemGroupButtonHandler {
    private var targetId by mutableStateOf<Long?>(null)

    private fun hideDialog() {
        deleteItemGroupDialogState = ConfirmatoryDialogState.NotShowing
        targetId = null
    }

    private val dialogShowingState = ConfirmatoryDialogState.Showing(
        message = StringResource(R.string.confirm_delete_item_group_message),
        onCancel = ::hideDialog,
        onConfirm = {
            targetId?.let { id ->
                coroutineScope.launch { dao.delete(id) }
                hideDialog()
            }
        })

    override var deleteItemGroupDialogState by
            mutableStateOf<ConfirmatoryDialogState>(ConfirmatoryDialogState.NotShowing)
        private set

    override fun onItemGroupDeleteClick(itemGroup: ItemGroup) {
        targetId = itemGroup.id
        deleteItemGroupDialogState = dialogShowingState
    }
}

interface AddItemGroupButtonHandler {
    val newItemGroupDialogState: NameDialogState
    fun onAddButtonClick()
}

class AddItemGroupButtonHandlerImpl(
    private val dao: ItemGroupDao,
    coroutineScope: CoroutineScope,
) : AddItemGroupButtonHandler {
    private val validator = ItemGroupNameValidator(dao)
    private val validatorMessage by validator.message.collectAsState(null, coroutineScope)

    private fun hideNewItemGroupDialog() {
        newItemGroupDialogState = NameDialogState.NotShowing
        validator.clear()
    }
    private val newItemGroupDialogShowingState = NameDialogState.Showing(
        currentNameProvider = validator::value::get,
        messageProvider = ::validatorMessage,
        onNameChange = validator::value::set,
        onCancel = ::hideNewItemGroupDialog,
        onConfirm = {
            coroutineScope.launch {
                val validatedName = validator.validate()
                    ?: return@launch
                dao.add(validatedName)
                hideNewItemGroupDialog()
            }
        }, title = StringResource(R.string.add_item_group_dialog_title))

    override var newItemGroupDialogState by
            mutableStateOf<NameDialogState>(NameDialogState.NotShowing)
        private set

    override fun onAddButtonClick() {
        newItemGroupDialogState = newItemGroupDialogShowingState
    }
}

@HiltViewModel
class BottomAppDrawerViewModel @Inject constructor(
    private val navState: NavigationState,
    private val itemGroupDao: ItemGroupDao,
    private val settingsDao: SettingsDao,
    private val coroutineScope: CoroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
) : ViewModel(),
    RenameItemGroupButtonHandler by RenameItemGroupButtonHandlerImpl(itemGroupDao, coroutineScope),
    DeleteItemGroupButtonHandler by DeleteItemGroupButtonHandlerImpl(itemGroupDao, coroutineScope),
    AddItemGroupButtonHandler by AddItemGroupButtonHandlerImpl(itemGroupDao, coroutineScope)
{
    override fun onCleared() {
        coroutineScope.cancel()
    }

    fun onSettingsButtonClick() {
        navState.addToStack(NavigationState.AdditionalScreen.AppSettings)
    }

    val multiSelectItemGroups by settingsDao
        .getMultiSelectGroups()
        .collectAsState(false, coroutineScope)

    fun onSelectAllClick() {
        coroutineScope.launch {
            settingsDao.updateMultiSelectGroups(true)
            itemGroupDao.selectAll()
        }
    }

    fun onMultiSelectItemGroupsCheckboxClick() {
        coroutineScope.launch {
            settingsDao.toggleMultiSelectGroups()
        }
    }

    val itemGroups by itemGroupDao.getAll()
        .map(List<ItemGroup>::toImmutableList)
        .collectAsState(emptyList<ItemGroup>().toImmutableList(), coroutineScope)

    fun onItemGroupClick(itemGroup: ItemGroup) {
        coroutineScope.launch {
            itemGroupDao.updateIsSelected(itemGroup.id)
        }
    }
}