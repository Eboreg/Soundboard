package us.huseli.soundboard_kotlin.data

import android.net.Uri
import androidx.room.*
import us.huseli.soundboard_kotlin.interfaces.OrderableItem

@Entity(
        tableName = "Sound",
        foreignKeys = [ForeignKey(
                entity = Category::class, parentColumns = ["id"], childColumns = ["categoryId"],
                onDelete = ForeignKey.CASCADE, onUpdate = ForeignKey.CASCADE)],
        indices = [Index("categoryId")]
)
data class Sound(
        @PrimaryKey(autoGenerate = true) var id: Int? = null,
        var categoryId: Int?,
        var name: String,
        val uri: Uri,
        override val order: Int,
        var volume: Int
) : OrderableItem {
    @Ignore constructor(name: String, uri: Uri): this(null, null, name, uri, 0, 100)
    @Ignore constructor(sound: Sound, order: Int): this(sound.id, sound.categoryId, sound.name, sound.uri, order, sound.volume)
}

/*
data class SoundWithBackgroundColor(
        var id: Int?,
        var categoryId: Int?,
        var name: String,
        val uri: Uri,
        var order: Int,
        var volume: Int,
        @Relation(parentColumn = "categoryId", entityColumn = "id", entity = Category::class, projection = ["backgroundColor"])
        val backgroundColor: Int
) {
    fun toEntity() = SoundEntity(id, categoryId, name, uri, order, volume)
}
*/

/*
data class Sound(
        var id: Int?,
        var categoryId: Int?,
        var name: String,
        val uri: Uri,
        var order: Int,
        var volume: Int,
        var backgroundColor: Int
) {
    constructor(name: String, uri: Uri): this(null, null, name, uri, 0, 100, Color.DKGRAY)
    constructor(s: SoundEntity, backgroundColor: Int?): this(s.id, s.categoryId, s.name, s.uri, s.order, s.volume, backgroundColor ?: Color.DKGRAY)
    constructor(s: SoundEntity): this(s, null)

    fun toEntity() = SoundEntity(id, categoryId, name, uri, order, volume)
}
*/
