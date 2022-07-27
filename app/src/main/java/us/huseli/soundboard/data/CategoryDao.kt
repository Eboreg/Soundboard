package us.huseli.soundboard.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

@Dao
interface CategoryDao {
    /********* INSERT ************************************************************************************************/
    @Insert
    fun insert(category: Category)

    @Insert
    fun insert(categories: List<Category>)

    /********* LIST **************************************************************************************************/
    @Query("SELECT * FROM SoundCategory ORDER BY `order`, id")
    fun list(): Flow<List<Category>>

    @Query("SELECT id FROM SoundCategory ORDER BY `order`, id")
    fun listIds(): List<Int>


    /********* UPDATE ************************************************************************************************/
    @Update
    fun update(category: Category)

    @Query("UPDATE SoundCategory SET backgroundColor=:backgroundColor WHERE id=:id")
    fun updateBackgroundColor(id: Int, backgroundColor: Int)

    @Query("UPDATE SoundCategory SET collapsed = :value WHERE id = :id")
    fun updateCollapsed(id: Int, value: Boolean)

    @Query("UPDATE SoundCategory SET name=:name WHERE id=:id")
    fun updateName(id: Int, name: String)

    @Query("UPDATE SoundCategory SET `order` = :order WHERE id = :id")
    fun updateOrder(id: Int, order: Int)


    /********* DELETE ************************************************************************************************/
    @Query("DELETE FROM SoundCategory WHERE id = :id")
    fun delete(id: Int)

    @Query("DELETE FROM SoundCategory")
    fun deleteAll()

    @Query("DELETE FROM SoundCategory WHERE id NOT IN (:categoryIds)")
    fun deleteExcluding(categoryIds: List<Int>)


    /********* VARIOUS ***********************************************************************************************/
    @Query("SELECT IFNULL(MAX(`order`), -1) FROM SoundCategory")
    fun getMaxOrder(): Int

    @Query("SELECT DISTINCT backgroundColor FROM SoundCategory")
    fun getUsedColors(): Flow<List<Int>>

    @Transaction
    suspend fun applyState(categories: List<Category>) {
        val dbCategories = list().first()

        categories.forEach { category ->
            if (dbCategories.contains(category)) {
                update(category)
                // Don't reset collapsed status (why?)
                dbCategories.findLast { it == category }?.let {
                    if (it.collapsed != category.collapsed && category.id != null)
                        updateCollapsed(category.id, it.collapsed)
                }
            } else insert(category)
        }

        deleteExcluding(categories.mapNotNull { it.id })
    }

/*
    @Transaction
    fun updateOrder(ids: List<Int>) {
        */
/** Update .order according to current order in list and save *//*

        ids.forEachIndexed { index, id -> updateOrder(id, index) }
    }
*/

/*
    @Transaction
    fun totalReset(categories: List<Category>) {
        deleteAll()
        insert(categories)
    }
*/
}