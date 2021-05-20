/* Copyright 2020 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracertech.bootycrate.database

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction

private const val shoppingListItemFields = "(id, name, extraInfo, color, checked, shoppingListAmount, " +
                                           "expandedInShoppingList, selectedInShoppingList, inventoryAmount)"
private const val inventoryItemFields = "id, name, extraInfo, color, inventoryAmount, " +
                                        "expandedInInventory, selectedInInventory, autoAddToShoppingList, " +
                                        "autoAddToShoppingListAmount, shoppingListAmount"
private const val likeSearchFilter = "(name LIKE :filter OR extraInfo LIKE :filter)"
private const val onShoppingList = "shoppingListAmount != -1 AND NOT inShoppingListTrash"
private const val inInventory = "inventoryAmount != -1 AND NOT inInventoryTrash"

/** A Room DAO that provides methods to manipulate a database of BootyCrateItems. */
@Dao abstract class BootyCrateItemDao {

    @Insert abstract suspend fun add(item: BootyCrateItem): Long
    @Insert abstract suspend fun add(items: List<BootyCrateItem>)

    @Query("UPDATE item SET name = :name WHERE id = :id")
    abstract suspend fun updateName(id: Long, name: String)

    @Query("UPDATE item SET extraInfo = :extraInfo WHERE id = :id")
    abstract suspend fun updateExtraInfo(id: Long, extraInfo: String)

    @Query("""UPDATE item SET color = :color WHERE id = :id""")
    abstract suspend fun updateColor(id: Long, color: Int)



    fun getShoppingList(
        sort: BootyCrateItem.Sort,
        sortByChecked: Boolean,
        searchFilter: String?
    ) = run {
        val filter = "%${searchFilter ?: ""}%"
        if (!sortByChecked) when (sort) {
            BootyCrateItem.Sort.Color -> getShoppingListSortedByColor(filter)
            BootyCrateItem.Sort.NameAsc -> getShoppingListSortedByNameAsc(filter)
            BootyCrateItem.Sort.NameDesc -> getShoppingListSortedByNameDesc(filter)
            BootyCrateItem.Sort.AmountAsc -> getShoppingListSortedByAmountAsc(filter)
            BootyCrateItem.Sort.AmountDesc -> getShoppingListSortedByAmountDesc(filter)
        } else when (sort) {
            BootyCrateItem.Sort.Color -> getShoppingListSortedByColorAndChecked(filter)
            BootyCrateItem.Sort.NameAsc -> getShoppingListSortedByNameAscAndChecked(filter)
            BootyCrateItem.Sort.NameDesc -> getShoppingListSortedByNameDescAndChecked(filter)
            BootyCrateItem.Sort.AmountAsc -> getShoppingListSortedByAmountAscAndChecked(filter)
            BootyCrateItem.Sort.AmountDesc -> getShoppingListSortedByAmountDescAndChecked(filter)
        }
    }

    @Query("SELECT $shoppingListItemFields FROM item " +
           "WHERE $likeSearchFilter AND $onShoppingList " +
           "ORDER BY color")
    abstract fun getShoppingListSortedByColor(filter: String): LiveData<List<ShoppingListItem>>

    @Query("SELECT $shoppingListItemFields FROM item " +
           "WHERE $likeSearchFilter AND $onShoppingList " +
           "ORDER BY name COLLATE NOCASE ASC")
    abstract fun getShoppingListSortedByNameAsc(filter: String): LiveData<List<ShoppingListItem>>

    @Query("SELECT $shoppingListItemFields FROM item " +
           "WHERE $likeSearchFilter AND $onShoppingList " +
           "ORDER BY name COLLATE NOCASE DESC")
    abstract fun getShoppingListSortedByNameDesc(filter: String): LiveData<List<ShoppingListItem>>

    @Query("SELECT $shoppingListItemFields FROM item " +
           "WHERE $likeSearchFilter AND $onShoppingList " +
           "ORDER BY shoppingListAmount ASC")
    abstract fun getShoppingListSortedByAmountAsc(filter: String): LiveData<List<ShoppingListItem>>

    @Query("SELECT $shoppingListItemFields FROM item " +
           "WHERE $likeSearchFilter AND $onShoppingList " +
           "ORDER BY shoppingListAmount DESC")
    abstract fun getShoppingListSortedByAmountDesc(filter: String): LiveData<List<ShoppingListItem>>

    @Query("SELECT $shoppingListItemFields FROM item " +
           "WHERE $likeSearchFilter AND $onShoppingList " +
           "ORDER BY isChecked, color")
    abstract fun getShoppingListSortedByColorAndChecked(filter: String): LiveData<List<ShoppingListItem>>

    @Query("SELECT $shoppingListItemFields FROM item " +
           "WHERE $likeSearchFilter AND $onShoppingList " +
           "ORDER BY isChecked, name COLLATE NOCASE ASC")
    abstract fun getShoppingListSortedByNameAscAndChecked(filter: String): LiveData<List<ShoppingListItem>>

    @Query("SELECT $shoppingListItemFields FROM item " +
           "WHERE $likeSearchFilter AND $onShoppingList " +
           "ORDER BY isChecked, name COLLATE NOCASE DESC")
    abstract fun getShoppingListSortedByNameDescAndChecked(filter: String): LiveData<List<ShoppingListItem>>

    @Query("SELECT $shoppingListItemFields FROM item " +
           "WHERE $likeSearchFilter AND $onShoppingList " +
           "ORDER BY isChecked, shoppingListAmount ASC")
    abstract fun getShoppingListSortedByAmountAscAndChecked(filter: String): LiveData<List<ShoppingListItem>>

    @Query("SELECT $shoppingListItemFields FROM item " +
           "WHERE $likeSearchFilter AND $onShoppingList " +
           "ORDER BY isChecked, shoppingListAmount DESC")
    abstract fun getShoppingListSortedByAmountDescAndChecked(filter: String): LiveData<List<ShoppingListItem>>

    @Query("SELECT EXISTS(SELECT id FROM item WHERE $onShoppingList " +
                         "AND name = :name AND extraInfo = :extraInfo")
    abstract fun shoppingListItemWithNameAlreadyExists(name: String, extraInfo: String): LiveData<Boolean>

    @Query("UPDATE item SET shoppingListAmount = :amount WHERE id = :id")
    abstract suspend fun updateShoppingListAmount(id: Long, amount: Int)

    @Query("UPDATE item SET expandedInShoppingList = 1 WHERE id = :id")
    protected abstract suspend fun expandShoppingListItem(id: Long)

    @Query("UPDATE item SET expandedInShoppingList = 0")
    abstract suspend fun clearExpandedShoppingListItem()

    @Transaction open suspend fun setExpandedShoppingListItem(id: Long?) {
        clearExpandedShoppingListItem()
        if (id != null) expandShoppingListItem(id)
    }

    @Query("SELECT $shoppingListItemFields FROM item " +
           "WHERE selectedInShoppingList AND $onShoppingList")
    abstract fun getSelectedShoppingListItems(): LiveData<List<ShoppingListItem>>

    @Query("UPDATE item SET selectedInShoppingList = :selected WHERE id = :id")
    abstract suspend fun updateSelectedInShoppingList(id: Long, selected: Boolean)

    @Query("""UPDATE item SET selectedInShoppingList =
                CASE WHEN selectedInShoppingList THEN 0 ELSE 1 END
              WHERE id = :id""")
    abstract suspend fun toggleSelectedInShoppingList(id: Long)

    @Query("UPDATE item set selectedInShoppingList = 1 " +
           "WHERE $onShoppingList AND id in (:ids)")
    abstract suspend fun selectShoppingListItems(ids: List<Long>)

    @Query("UPDATE item SET selectedInShoppingList = 0")
    abstract suspend fun clearShoppingListSelection()

    @Query("""UPDATE item 
              SET shoppingListAmount = 1,
                  inShoppingListTrash = 0
              WHERE selectedInInventory 
              AND shoppingListAmount = -1
              AND $inInventory""")
    abstract suspend fun addToShoppingListFromSelectedInventoryItems()

    @Query("""UPDATE item
              SET inShoppingListTrash = 1,
                  expandedInShoppingList = 0,
                  selectedInShoppingList = 0
              WHERE selectedInShoppingList AND $onShoppingList""")
    abstract suspend fun deleteSelectedShoppingListItems()

    @Query("""UPDATE item SET inShoppingListTrash = 1,
                              expandedInShoppingList = 0,
                              selectedInShoppingList = 0
              WHERE id IN (:ids) AND $onShoppingList""")
    abstract suspend fun deleteShoppingListItems(ids: LongArray)

    @Query("""UPDATE item SET shoppingListAmount = -1,
                              checked = 0,
                              expandedInShoppingList = 0,
                              selectedInShoppingList = 0
              WHERE shoppingListAmount != -1""")
    abstract suspend fun deleteAllShoppingListItems()

    @Query("UPDATE item SET inShoppingListTrash = 0")
    abstract suspend fun undoDeleteShoppingListItems()

    @Query("UPDATE item SET shoppingListAmount = -1 WHERE inShoppingListTrash")
    abstract suspend fun emptyShoppingListTrash()

    @Query("UPDATE item SET checked = :checked WHERE id = :id")
    abstract suspend fun updateChecked(id: Long, checked: Boolean)

    @Query("UPDATE item SET checked = 1 WHERE $onShoppingList")
    abstract suspend fun checkAllShoppingListItems()

    @Query("UPDATE item SET checked = 0 WHERE $onShoppingList")
    abstract suspend fun uncheckAllShoppingListItems()

    @Query("SELECT COUNT(*) FROM item WHERE checked AND $onShoppingList")
    abstract fun getCheckedShoppingListItemsSize() : LiveData<Int>

    @Query("""UPDATE item
              SET inventoryAmount = inventoryAmount + shoppingListAmount,
                  checked = 0,
                  shoppingListAmount = -1,
                  expandedInShoppingList = 0,
                  selectedInShoppingList = 0
              WHERE $onShoppingList AND checked""")
    abstract suspend fun checkout()



    fun getInventory(sort: BootyCrateItem.Sort, searchFilter: String?) = run {
        val filter = "%${searchFilter ?: ""}%"
        when (sort) {
            BootyCrateItem.Sort.Color -> getInventorySortedByColor(filter)
            BootyCrateItem.Sort.NameAsc -> getInventorySortedByNameAsc(filter)
            BootyCrateItem.Sort.NameDesc -> getInventorySortedByNameDesc(filter)
            BootyCrateItem.Sort.AmountAsc -> getInventorySortedByAmountAsc(filter)
            BootyCrateItem.Sort.AmountDesc -> getInventorySortedByAmountDesc(filter)
        }
    }

    @Query("SELECT $inventoryItemFields FROM item " +
           "WHERE $likeSearchFilter AND $inInventory " +
           "ORDER BY color")
    abstract fun getInventorySortedByColor(filter: String): LiveData<List<InventoryItem>>

    @Query("SELECT $inventoryItemFields FROM item " +
           "WHERE $likeSearchFilter AND $inInventory " +
           "ORDER BY name COLLATE NOCASE ASC")
    abstract fun getInventorySortedByNameAsc(filter: String): LiveData<List<InventoryItem>>

    @Query("SELECT $inventoryItemFields FROM item " +
           "WHERE $likeSearchFilter AND $inInventory " +
           "ORDER BY name COLLATE NOCASE DESC")
    abstract fun getInventorySortedByNameDesc(filter: String): LiveData<List<InventoryItem>>

    @Query("SELECT $inventoryItemFields FROM item " +
           "WHERE $likeSearchFilter AND $inInventory " +
           "ORDER BY inventoryAmount ASC")
    abstract fun getInventorySortedByAmountAsc(filter: String): LiveData<List<InventoryItem>>

    @Query("SELECT $inventoryItemFields FROM item " +
           "WHERE $likeSearchFilter AND $inInventory " +
           "ORDER BY inventoryAmount DESC")
    abstract fun getInventorySortedByAmountDesc(filter: String): LiveData<List<InventoryItem>>

    @Query("SELECT EXISTS(SELECT id FROM item WHERE $inInventory " +
                         "AND name = :name AND extraInfo = :extraInfo")
    abstract fun inventoryItemWithNameAlreadyExists(name: String, extraInfo: String): LiveData<Boolean>

    @Query("UPDATE item SET inventoryAmount = :amount WHERE id = :id")
    abstract suspend fun updateInventoryAmount(id: Long, amount: Int)

    @Query("UPDATE item SET expandedInInventory = 1 WHERE id = :id")
    abstract suspend fun expandInventoryItem(id: Long)

    @Query("UPDATE item SET expandedInInventory = 0")
    abstract suspend fun clearExpandedInventoryItem()

    @Transaction open suspend fun setExpandedInventoryItem(id: Long?) {
        clearExpandedInventoryItem()
        if (id != null) expandInventoryItem(id)
    }

    @Query("SELECT $inventoryItemFields FROM item " +
           "WHERE selectedInInventory AND $inInventory")
    abstract fun getSelectedInventoryItems(): LiveData<List<InventoryItem>>

    @Query("UPDATE item SET selectedInInventory = :selected WHERE id = :id")
    abstract suspend fun updateSelectedInInventory(id: Long, selected: Boolean)

    @Query("""UPDATE item SET selectedInInventory =
                CASE WHEN selectedInInventory THEN 0 ELSE 1 END
              WHERE id = :id""")
    abstract suspend fun toggleSelectedInInventory(id: Long)

    @Query("UPDATE item set selectedInInventory = 1 " +
           "WHERE $inInventory AND id in (:ids)")
    abstract suspend fun selectInventoryItems(ids: List<Long>)

    @Query("UPDATE item SET selectedInInventory = 0")
    abstract suspend fun clearInventorySelection()

    @Query("""UPDATE item 
              SET inventoryAmount = 0,
                  inInventoryTrash = 0
              WHERE selectedInShoppingList 
              AND inventoryAmount = -1
              AND $onShoppingList""")
    abstract suspend fun addToInventoryFromSelectedShoppingListItems()

    @Query("""UPDATE item
              SET inInventoryTrash = 1,
                  expandedInInventory = 0,
                  selectedInInventory = 0
              WHERE selectedInInventory AND $inInventory""")
    abstract suspend fun deleteSelectedInventoryItems()

    @Query("""UPDATE item SET inInventoryTrash = 1,
                              expandedInInventory = 0,
                              selectedInInventory = 0,
                              autoAddToShoppingList = 0,
                              autoAddToShoppingListAmount = 0
              WHERE id IN (:ids) AND $inInventory""")
    abstract suspend fun deleteInventoryItems(ids: LongArray)

    @Query("""UPDATE item SET inventoryAmount = -1,
                              expandedInInventory = 0,
                              selectedInInventory = 0
              WHERE inventoryAmount != -1""")
    abstract suspend fun deleteAllInventoryItems()

    @Query("UPDATE item SET inInventoryTrash = 0")
    abstract suspend fun undoDeleteInventoryItems()

    @Query("UPDATE item SET inventoryAmount = -1 WHERE inInventoryTrash")
    abstract suspend fun emptyInventoryTrash()

    @Query("""UPDATE item SET autoAddToShoppingList = :autoAddToShoppingList
              WHERE id = :id AND $inInventory""")
    abstract suspend fun updateAutoAddToShoppingList(id: Long, autoAddToShoppingList: Boolean)

    @Query("""UPDATE item SET autoAddToShoppingListAmount = :autoAddToShoppingListAmount
              WHERE id = :id AND $inInventory""")
    abstract suspend fun updateAutoAddToShoppingListAmount(id: Long, autoAddToShoppingListAmount: Int)
}