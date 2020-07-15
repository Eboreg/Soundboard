package us.huseli.soundboard_kotlin.data

import android.graphics.Color
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class SoundCategory(
        @PrimaryKey(autoGenerate = true) val id: Int? = null,
        var name: String,
        var backgroundColor: Int,
        var textColor: Int,
        var order: Int
) {
    constructor(name: String, backgroundColor: Int, textColor: Int, order: Int): this(null, name, backgroundColor, textColor, order)
    constructor(): this(null, "", Color.DKGRAY, Color.WHITE, 0)

    override fun toString() = name
}