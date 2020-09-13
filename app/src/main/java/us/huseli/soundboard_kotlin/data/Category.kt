package us.huseli.soundboard_kotlin.data

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import us.huseli.soundboard_kotlin.interfaces.OrderableItem

@Entity(tableName = "SoundCategory")
data class Category(
        @PrimaryKey(autoGenerate = true) var id: Int? = null,
        var name: String,
        var backgroundColor: Int,
        override var order: Int,
        var collapsed: Boolean = false
) : OrderableItem {
    constructor(name: String, backgroundColor: Int, order: Int): this(null, name, backgroundColor, order, false)
    @Ignore constructor(name: String, backgroundColor: Int): this(name, backgroundColor, 0)

    override fun toString() = name
}