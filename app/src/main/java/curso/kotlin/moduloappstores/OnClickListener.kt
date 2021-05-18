package curso.kotlin.moduloappstores

interface OnClickListener {
    fun onClick(storeId: Long)

    fun onFavoriteStore(storeEntity: StoreEntity)

    fun onDeleteStore(storeEntity: StoreEntity)
}