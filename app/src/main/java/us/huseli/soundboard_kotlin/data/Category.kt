package us.huseli.soundboard_kotlin.data

import android.graphics.Color
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "SoundCategory")
data class Category(
        @PrimaryKey(autoGenerate = true) var id: Int? = null,
        var name: String,
        var backgroundColor: Int,
        var textColor: Int,
        var order: Int
) {
    constructor(name: String, backgroundColor: Int, textColor: Int, order: Int): this(null, name, backgroundColor, textColor, order)
    constructor(): this("", Color.DKGRAY, Color.WHITE, 0)

    override fun toString() = name
}

data class CategoryExtended(
        @Embedded val category: Category,
        val soundCount: Int
) {
    constructor(name: String, backgroundColor: Int, textColor: Int, order: Int): this(Category(name, backgroundColor, textColor, order), 0)
}