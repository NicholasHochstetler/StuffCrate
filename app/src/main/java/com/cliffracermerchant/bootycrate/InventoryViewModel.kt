package com.cliffracermerchant.bootycrate

import android.app.Application
import androidx.lifecycle.*
import kotlinx.coroutines.launch

class InventoryViewModel(app: Application) : AndroidViewModel(app) {

    private val dao: InventoryItemDao = BootyCrateDatabase.get(app).inventoryItemDao()
    private val sortLiveData = MutableLiveData(Sort.OriginalInsertionOrder)
    private val searchFilterLiveData = MutableLiveData("")
    private val sortAndFilterLiveData = MediatorLiveData<Pair<Sort?, String?>>().apply {
        addSource(sortLiveData) { value = Pair(it, searchFilterLiveData.value) }
        addSource(searchFilterLiveData) { value = Pair(sortLiveData.value, it) }
    }
    private val items = Transformations.switchMap(sortAndFilterLiveData) { sortAndFilter ->
        val filter = '%' + (sortAndFilter.second ?: "") + '%'
        when (sortAndFilter.first) {
            null -> dao.getAll(filter)
            Sort.OriginalInsertionOrder -> dao.getAll(filter)
            Sort.NameAsc -> dao.getAllSortedByNameAsc(filter)
            Sort.NameDesc -> dao.getAllSortedByNameDesc(filter)
            Sort.AmountAsc -> dao.getAllSortedByAmountAsc(filter)
            Sort.AmountDesc -> dao.getAllSortedByAmountDesc(filter)
        }
    }
    var sort get() = sortLiveData.value
        set(value) { sortLiveData.value = value }
    var searchFilter get() = searchFilterLiveData.value
        set(value) { searchFilterLiveData.value = value }

    init { viewModelScope.launch{ dao.emptyTrash() } }

    fun getAll() = items

    fun insert(vararg items: InventoryItem) = viewModelScope.launch {
        dao.insert(*items)
    }
    fun insertFromShoppingListItems(vararg shoppingListItemIds: Long) = viewModelScope.launch {
        dao.insertFromShoppingListItems(*shoppingListItemIds)
    }
    fun updateName(id: Long, name: String) = viewModelScope.launch {
        dao.updateName(id, name)
    }
    fun updateAmount(id: Long, amount: Int) = viewModelScope.launch {
        dao.updateAmount(id, amount)
    }
    fun updateExtraInfo(id: Long, extraInfo: String) = viewModelScope.launch {
        dao.updateExtraInfo(id, extraInfo)
    }
    fun updateAutoAddToShoppingList(id: Long, autoAddToShoppingList: Boolean) = viewModelScope.launch {
        dao.updateAutoAddToShoppingList(id, autoAddToShoppingList)
    }
    fun updateAutoAddToShoppingListTrigger(id: Long, autoAddToShoppingListTrigger: Int) = viewModelScope.launch {
        dao.updateAutoAddToShoppingListTrigger(id, autoAddToShoppingListTrigger)
    }
    fun deleteAll() = viewModelScope.launch {
        dao.deleteAll()
    }
    fun emptyTrash() = viewModelScope.launch {
        dao.emptyTrash()
    }
    fun delete(vararg ids: Long) = viewModelScope.launch {
        dao.delete(*ids)
    }
    fun undoDelete() = viewModelScope.launch {
        dao.undoDelete()
    }
}
