package us.huseli.soundboard.data

import android.graphics.Color
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey

@Entity(tableName = "SoundCategory")
data class Category(
    @PrimaryKey(autoGenerate = true) val id: Int? = null,
    var name: String,
    var backgroundColor: Int,
    var order: Int,
    var collapsed: Boolean = false,
    @Ignore var textColor: Int?,
    @Ignore var secondaryTextColor: Int?
) {
    constructor(id: Int?, name: String, backgroundColor: Int, order: Int, collapsed: Boolean) :
            this(id, name, backgroundColor, order, collapsed, null, null)

    @Ignore
    constructor(name: String, backgroundColor: Int, order: Int) : this(null, name, backgroundColor, order, false)

    @Ignore
    constructor(name: String, backgroundColor: Int) : this(name, backgroundColor, -1)

    @Ignore
    constructor(name: String) : this(name, Color.DKGRAY)

    val collapseIconRotation: Float
        get() = if (collapsed) -90f else 0f

    override fun toString() = name
    override fun equals(other: Any?) = other is Category && other.id == id
    override fun hashCode() = id ?: 0
}