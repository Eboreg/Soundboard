package us.huseli.soundboard_kotlin.data

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import androidx.room.Relation
import us.huseli.soundboard_kotlin.interfaces.OrderableItem

@Entity(tableName = "SoundCategory")
data class Category(
        @PrimaryKey(autoGenerate = true) var id: Int? = null,
        var name: String,
        var backgroundColor: Int,
        val order: Int
) {
    constructor(name: String, backgroundColor: Int, order: Int): this(null, name, backgroundColor, order)
    @Ignore constructor(name: String, backgroundColor: Int): this(name, backgroundColor, 0)

    override fun toString() = name
}

data class CategoryWithSounds(
        var id: Int?,
        var name: String,
        var backgroundColor: Int,
        override val order: Int,
        @Relation(parentColumn = "id", entityColumn = "categoryId") var sounds: List<Sound>
) : OrderableItem {
    override fun toString() = name
    fun toCategory() = Category(id, name, backgroundColor, order)
    fun soundCount(): Int = sounds.size

    constructor(name: String, backgroundColor: Int, order: Int): this(null, name, backgroundColor, order, emptyList())
}