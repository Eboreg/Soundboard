package us.huseli.soundboard_kotlin.data

import android.net.Uri
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
        foreignKeys = [ForeignKey(entity = SoundCategory::class, parentColumns = ["id"], childColumns = ["categoryId"])],
        indices = [Index("categoryId")]
)
data class Sound(
        @PrimaryKey(autoGenerate = true) val id: Int? = null,
        var categoryId: Int,
        var name: String,
        val uri: Uri,
        var order: Int,
        var volume: Int
) {
    // TODO: Constructors with categoryId
    constructor(name: String, uri: Uri): this(null, 0, name.trim(), uri, 0, 100)
    constructor(name: String, uri: Uri, volume: Int): this(null, 0, name.trim(), uri, 0, volume)
}
