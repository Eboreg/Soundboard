package us.huseli.soundboard.data

import android.graphics.Color
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey

@Entity(tableName = "SoundCategory")
data class Category(
    @PrimaryKey(autoGenerate = true) val id: Int? = null,
    val name: String,
    val backgroundColor: Int,
    val order: Int,
    val collapsed: Boolean = false,
    val autoImportCategory: Boolean = false,
    @Ignore var textColor: Int?
) {
    constructor(id: Int?, name: String, backgroundColor: Int, order: Int, collapsed: Boolean, autoImportCategory: Boolean) :
            this(id, name, backgroundColor, order, collapsed, autoImportCategory, null)

    @Ignore
    constructor(name: String, backgroundColor: Int, autoImportCategory: Boolean) :
            this(null, name, backgroundColor, -1, false, autoImportCategory)

    @Ignore
    constructor(name: String, backgroundColor: Int) : this(name, backgroundColor, false)

    @Ignore
    constructor(name: String) : this(name, Color.DKGRAY)

    val collapseIconRotation: Float
        get() = if (collapsed) -90f else 0f

    override fun toString() = name
    override fun equals(other: Any?) = other is Category && other.id == id
    override fun hashCode() = id ?: 0
}
