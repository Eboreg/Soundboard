package us.huseli.soundboard_kotlin.data

import android.net.Uri
import androidx.room.*

@Entity(
        foreignKeys = [ForeignKey(entity = Category::class, parentColumns = ["id"], childColumns = ["categoryId"])],
        indices = [Index("categoryId")]
)
data class Sound(
        @PrimaryKey(autoGenerate = true) var id: Int? = null,
        var categoryId: Int?,
        var name: String,
        val uri: Uri,
        var order: Int,
        var volume: Int
) {
    @Ignore constructor(name: String, uri: Uri): this(null, null, name, uri, 0, 100)
}
