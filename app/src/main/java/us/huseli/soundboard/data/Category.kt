package us.huseli.soundboard.data

import android.graphics.Color
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey

@Entity(tableName = "SoundCategory")
data class Category(
        @PrimaryKey(autoGenerate = true) var id: Int? = null,
        var name: String,
        var backgroundColor: Int,
        var order: Int,
        var collapsed: Boolean = false
) {
    constructor(name: String, backgroundColor: Int, order: Int): this(null, name, backgroundColor, order, false)
    @Ignore constructor(name: String, backgroundColor: Int): this(name, backgroundColor, 0)
    @Ignore constructor(name: String): this(name, Color.DKGRAY)

    override fun toString() = name

}