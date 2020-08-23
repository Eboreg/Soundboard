package us.huseli.soundboard_kotlin.data

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "SoundCategory")
data class Category(
        @PrimaryKey(autoGenerate = true) var id: Int? = null,
        var name: String,
        var backgroundColor: Int,
        var order: Int
) {
    constructor(name: String, backgroundColor: Int, order: Int): this(null, name, backgroundColor, order)
    constructor(name: String, backgroundColor: Int): this(name, backgroundColor, 0)

    override fun toString() = name
}

data class CategoryExtended(
        @Embedded val category: Category,
        val soundCount: Int
)