/* Copyright 2020 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracermerchant.bootycrate

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.*
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar

/** A RecyclerView for displaying the contents of a BootyCrate ViewModel<Entity>.
 *
 *  ViewModelRecyclerView is an abstract RecyclerView subclass that tailors the
 *  RecyclerView interface toward displaying the contents of a ViewModel<
 *  Entity> and updating itself asynchronously using its custom ListAdapter-
 *  derived adapter type. To achieve this, its abstract properties diffUtilCall-
 *  back and adapter must be overridden in subclasses. diffUtilCallback must be
 *  overridden with an appropriate DiffUtil.ItemCallback<Entity> for the adap-
 *  ter. adapter must be overridden with a ViewModelAdapter subclass that imple-
 *  ments onCreateViewHolder. While it is not abstract, the viewModel property
 *  should probably be overridden with a concrete ViewModel<Entity> subclass
 *  that implements its abstract methods. collectionName, used in user facing
 *  strings regarding the item collection, should be overridden with a string
 *  that describes the collection of items (e.g. inventory for a collection of
 *  inventory items).
 *
 *  Because AndroidViewModels are created after views inflated from xml are
 *  created, it is necessary to finish initialization with these required mem-
 *  bers. To do this, the function finishInit must be called with an Lifecycle-
 *  Owner and a ViewModel<Entity> instance during runtime, but before any sort
 *  of data access is attempted.
 *
 *  ViewModelRecyclerView has two public properties, sort and searchFilter,
 *  that mirror these properties from the view model from which it obtains its
 *  data. Changing these properties will therefore change the sorting or text
 *  filter of the RecyclerView items.
 *
 *  To utilize ViewModel<Entity>'s support for treating new items differently,
 *  ViewModelRecyclerView has an open function onNewItemInsertion. onNewItem-
 *  Insertion smooth scrolls to the new item by default.
 *
 *  ViewModelRecyclerView provides public functions to execute most of View-
 *  Model<Entity>'s public functions such as add, delete, deleteAll, and undo-
 *  Delete. It also utilizes a ItemTouchHelper with a SwipeToDeleteCallback to
 *  allow the user to call deleteItem on the swiped item. It is worth noting
 *  that due to being Entity agnostic, the addItem function requires an already
 *  created instance of Entity to add to the ViewModel data. Subclasses may
 *  wish to provide an alternative that inserts a default constructed Entity
 *  instance.
 *
 *  When items are deleted, a snackbar will appear informing the user of the
 *  amount of items that were deleted, as well as providing an undo option. The
 *  snackbar will be anchored to the view set as the public property snackBar-
 *  Anchor, in case this needs to be customized, or to the RecyclerView itself
 *  otherwise. */
abstract class ViewModelRecyclerView<Entity: ViewModelItem>(
    context: Context,
    attrs: AttributeSet
) : RecyclerView(context, attrs) {

    abstract val diffUtilCallback: DiffUtil.ItemCallback<Entity>
    abstract val adapter: ViewModelAdapter<out ViewModelItemViewHolder>
    abstract val collectionName: String
    private lateinit var viewModel: ViewModel<Entity>
    var snackBarAnchor: View? = null

    var sort: ViewModelItem.Sort? get() = viewModel.sort
                                  set(value) { viewModel.sort = value }
    var searchFilter: String? get() = viewModel.searchFilter
                              set(value) { viewModel.searchFilter = value }

    init {
        ItemTouchHelper(SwipeToDeleteCallback(::deleteItem, context)).attachToRecyclerView(this)
    }

    fun finishInit(owner: LifecycleOwner, viewModel: ViewModel<Entity> ) {
        setAdapter(adapter)
        this.viewModel = viewModel
        viewModel.items.observe(owner) { items -> adapter.submitList(items) }
    }

    open fun addItem(item: Entity) = viewModel.add(item)

    open fun deleteItem(pos: Int) = deleteItems(LongArray(1) { adapter.getItemId(pos) })

    open fun deleteItems(ids: LongArray) {
        viewModel.delete(ids)
        val text = context.getString(R.string.delete_snackbar_text, ids.size)
        Snackbar.make(this, text, Snackbar.LENGTH_LONG).
            setAnchorView(snackBarAnchor ?: this).
            setAction(R.string.delete_snackbar_undo_text) { undoDelete() }.
            addCallback(object: BaseTransientBottomBar.BaseCallback<Snackbar>() {
                override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
                    viewModel.emptyTrash()
            }}).show()
    }

    open fun undoDelete() { viewModel.undoDelete() }

    fun deleteAll() {
        if (adapter.currentList.isEmpty()) {
            val message = context.getString(R.string.empty_recycler_view_message, collectionName)
            Snackbar.make(this, message, Snackbar.LENGTH_SHORT).
                setAnchorView(snackBarAnchor ?: this).show()
            return
        }
        Dialog.deleteAllFromViewModel(viewModel, collectionName)
    }

    open fun onNewItemInsertion(item: Entity, vh: ViewModelItemViewHolder) =
        smoothScrollToPosition(vh.adapterPosition)

    /** A ListAdapter derived RecyclerView.Adapter for ViewModelRecyclerView.
     *
     *  ViewModelAdapter is an abstract (because it does not implement onCreate-
     *  ViewHolder) ListAdapter subclass that enforces the use of stable ids
     *  and calls onNewItemInsertion for newly inserted items. */
    abstract inner class ViewModelAdapter<VHType: ViewModelItemViewHolder> :
            ListAdapter<Entity, VHType>(diffUtilCallback) {
        init { setHasStableIds(true) }

        final override fun setHasStableIds(hasStableIds: Boolean) =
            super.setHasStableIds(true)

        override fun onBindViewHolder(holder: VHType, position: Int) {
            if (getItemId(holder.adapterPosition) == viewModel.newlyAddedItemId) {
                onNewItemInsertion(currentList[holder.adapterPosition], holder)
                viewModel.resetNewlyAddedItemId()
            }
        }

        override fun getItemId(position: Int) = currentList[position].id
    }

   /** A ViewHolder subclass that provides a simplified way of obtaining the
    *  instance of the item that it represents through the property item. */
    open inner class ViewModelItemViewHolder(view: View) : ViewHolder(view) {
        val item: Entity get() = adapter.currentList[adapterPosition]
    }
}