package com.cliffracermerchant.bootycrate

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class InventoryViewModel(app: Application) : AndroidViewModel(app) {

    private val dao: InventoryItemDao = BootyCrateDatabase.get(app).inventoryItemDao()
    private val items: LiveData<List<InventoryItem>> = dao.getAll()

    init { viewModelScope.launch{ dao.emptyTrash() } }

    fun getAll() : LiveData<List<InventoryItem>> = items

    fun insert(vararg items: InventoryItem) = viewModelScope.launch { dao.insert(*items) }

    fun updateName(item: InventoryItem, name: String) = viewModelScope.launch {
        dao.updateName(item.id, name)
    }
    fun updateAmount(item: InventoryItem, amount: Int) = viewModelScope.launch {
        dao.updateAmount(item.id, amount)
    }
    fun updateExtraInfo(item: InventoryItem, extraInfo: String) = viewModelScope.launch {
        dao.updateExtraInfo(item.id, extraInfo)
    }
    fun updateAutoAddToShoppingList(item: InventoryItem, autoAddToShoppingList: Boolean) =
            viewModelScope.launch {
        dao.updateAutoAddToShoppingList(item.id, autoAddToShoppingList)
    }
    fun updateAutoAddToShoppingListTrigger(item: InventoryItem, autoAddToShoppingListTrigger: Int) =
            viewModelScope.launch {
        dao.updateAutoAddToShoppingListTrigger(item.id, autoAddToShoppingListTrigger)
    }
    fun delete(vararg items: InventoryItem) = viewModelScope.launch {
        dao.delete(*items)
    }
    fun delete(vararg ids: Long) = viewModelScope.launch {
        dao.delete(*ids)
    }
    fun deleteAll() = viewModelScope.launch {
        dao.deleteAll()
    }
    fun undoDelete() = viewModelScope.launch {
        dao.undoDelete()
    }
}