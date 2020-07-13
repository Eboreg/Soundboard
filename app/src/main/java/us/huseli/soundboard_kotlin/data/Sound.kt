package us.huseli.soundboard_kotlin.data

import android.net.Uri
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Sound(
        @PrimaryKey(autoGenerate = true) val id: Int? = null,
        var name: String,
        val uri: Uri,
        var order: Int,
        var volume: Int
) {
    constructor(name: String, uri: Uri): this(null, name, uri, 0, 100)
    constructor(name: String, uri: Uri, volume: Int): this(null, name, uri, 0, volume)
}
