package us.huseli.soundboard.data

import androidx.room.Embedded
import androidx.room.Relation

data class SoundWithCategory(
    @Embedded val sound: Sound,
    @Relation(
        parentColumn = "categoryId",
        entityColumn = "id",
        entity = Category::class
    ) val category: Category
)
