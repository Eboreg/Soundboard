package us.huseli.soundboard.data

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface CategoryDao {
    @Query("SELECT MAX(`order`) FROM SoundCategory")
    fun getMaxOrder(): Int?

    @Insert
    fun privateInsert(category: Category)

    @Query("DELETE FROM SoundCategory WHERE id = :id")
    fun delete(id: Int)

    @Query("DELETE FROM SoundCategory WHERE id NOT IN (:categoryIds)")
    fun deleteExcluding(categoryIds: List<Int>)

    @Query("SELECT * FROM SoundCategory WHERE id = :id")
    fun get(id: Int): LiveData<Category?>

    @Query("SELECT backgroundColor FROM SoundCategory")
    fun getUsedColors(): List<Int>

    @Query("SELECT backgroundColor FROM SoundCategory")
    fun getUsedColorsLive(): LiveData<List<Int>>

    @Query("INSERT INTO SoundCategory (id, name, backgroundColor, `order`, collapsed) VALUES(:categoryId, :name, :backgroundColor, :order, :collapsed)")
    fun insert(categoryId: Int, name: String, backgroundColor: Int, order: Int, collapsed: Boolean)

    @Query("INSERT INTO SoundCategory (name, backgroundColor, `order`, collapsed) VALUES(:name, :backgroundColor, :order, :collapsed)")
    fun insert(name: String, backgroundColor: Int, order: Int, collapsed: Boolean)

    @Transaction
    fun insert(category: Category) {
        val order = if (category.order > -1) category.order else (getMaxOrder() ?: -1) + 1
        insert(category.name, category.backgroundColor, order, category.collapsed)
    }

    @Query("SELECT * FROM SoundCategory ORDER BY `order`, id")
    fun list(): List<Category>

    @Query("SELECT * FROM SoundCategory ORDER BY `order`, id")
    fun listLive(): LiveData<List<Category>>

    @Transaction
    fun reset(categories: List<Category>) {
        val dbCategories = list()
        categories.forEach { category ->
            if (dbCategories.contains(category)) {
                update(category)
                // Dont reset collapsed status
                dbCategories.findLast { it == category }?.let {
                    if (it.collapsed != category.collapsed && category.id != null) updateCollapsed(category.id,
                        if (it.collapsed) 1 else 0)
                }
            } else insert(category)
        }
        deleteExcluding(categories.mapNotNull { it.id })
    }

    @Query("UPDATE SoundCategory SET collapsed = :value WHERE id = :id")
    fun updateCollapsed(id: Int, value: Int)

    @Transaction
    fun sort(categories: List<Category>) {
        /** Update .order according to current order in list and save */
        categories.mapNotNull { it.id }.forEachIndexed { index, categoryId ->
            updateOrder(categoryId, index)
        }
    }

    @Query("UPDATE SoundCategory SET `order` = :order WHERE id = :categoryId")
    fun updateOrder(categoryId: Int, order: Int)

    @Update
    fun update(category: Category)

    @Transaction
    fun update(categoryId: Int, name: String?, backgroundColor: Int?) {
        if (name != null) updateName(categoryId, name)
        if (backgroundColor != null) updateBackgroundColor(categoryId, backgroundColor)
    }

    @Query("UPDATE SoundCategory SET backgroundColor=:backgroundColor WHERE id=:categoryId")
    fun updateBackgroundColor(categoryId: Int, backgroundColor: Int)

    @Query("UPDATE SoundCategory SET name=:name WHERE id=:categoryId")
    fun updateName(categoryId: Int, name: String)
}