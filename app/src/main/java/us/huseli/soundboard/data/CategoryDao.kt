package us.huseli.soundboard.data

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface CategoryDao {
    /********* INSERT ************************************************************************************************/
    @Insert
    fun insert(categories: List<Category>)

    @Query("INSERT INTO SoundCategory (name, backgroundColor, `order`, collapsed) VALUES(:name, :backgroundColor, :order, :collapsed)")
    fun insert(name: String, backgroundColor: Int, order: Int, collapsed: Boolean)

    @Transaction
    fun insert(category: Category) {
        val order = if (category.order > -1) category.order else (getMaxOrder() ?: -1) + 1
        insert(category.name, category.backgroundColor, order, category.collapsed)
    }

    @Insert
    fun reInsert(category: Category)

    /********* LIST **************************************************************************************************/
    @Query("SELECT * FROM SoundCategory ORDER BY `order`, id")
    fun list(): List<Category>

    @Query("SELECT id FROM SoundCategory ORDER BY `order`, id")
    fun listIds(): List<Int>

    @Query("SELECT * FROM SoundCategory ORDER BY `order`, id")
    fun listLive(): LiveData<List<Category>>


    /********* UPDATE ************************************************************************************************/
    @Update
    fun update(category: Category)

    @Transaction
    fun update(categoryId: Int, name: String?, backgroundColor: Int?) {
        if (name != null) updateName(categoryId, name)
        if (backgroundColor != null) updateBackgroundColor(categoryId, backgroundColor)
    }

    @Query("UPDATE SoundCategory SET backgroundColor=:backgroundColor WHERE id=:categoryId")
    fun updateBackgroundColor(categoryId: Int, backgroundColor: Int)

    @Query("UPDATE SoundCategory SET collapsed = :value WHERE id = :id")
    fun updateCollapsed(id: Int, value: Int)

    @Query("UPDATE SoundCategory SET name=:name WHERE id=:categoryId")
    fun updateName(categoryId: Int, name: String)

    @Query("UPDATE SoundCategory SET `order` = :order WHERE id = :categoryId")
    fun updateOrder(categoryId: Int, order: Int)


    /********* DELETE ************************************************************************************************/
    @Query("DELETE FROM SoundCategory WHERE id = :id")
    fun delete(id: Int)

    @Query("DELETE FROM SoundCategory")
    fun deleteAll()

    @Query("DELETE FROM SoundCategory WHERE id NOT IN (:categoryIds)")
    fun deleteExcluding(categoryIds: List<Int>)


    /********* VARIOUS ***********************************************************************************************/
    @Query("SELECT MAX(`order`) FROM SoundCategory")
    fun getMaxOrder(): Int?

    @Query("SELECT backgroundColor FROM SoundCategory")
    fun getUsedColors(): LiveData<List<Int>>

    @Transaction
    fun applyState(categories: List<Category>) {
        val dbCategories = list()
        categories.forEach { category ->
            if (dbCategories.contains(category)) {
                update(category)
                // Don't reset collapsed status (why?)
                dbCategories.findLast { it == category }?.let {
                    if (it.collapsed != category.collapsed && category.id != null)
                        updateCollapsed(category.id, if (it.collapsed) 1 else 0)
                }
            } else reInsert(category)
        }
        deleteExcluding(categories.mapNotNull { it.id })
    }

    @Transaction
    fun sort(categoryIds: List<Int>) {
        /** Update .order according to current order in list and save */
        categoryIds.forEachIndexed { index, categoryId -> updateOrder(categoryId, index) }
    }

    @Transaction
    fun totalReset(categories: List<Category>) {
        deleteAll()
        insert(categories)
    }
}