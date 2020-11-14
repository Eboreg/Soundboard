package us.huseli.soundboard_kotlin.data

import android.net.Uri

data class EmptySound(
        override var id: Int?,
        override var categoryId: Int?,
        override var name: String,
        override val uri: Uri,
        override val volume: Int,
        var adapterPosition: Int?  // TODO: Maybe not necessary?
) : AbstractSound() {
    constructor(categoryId: Int) : this(-1, categoryId, "", Uri.EMPTY, 100, null)
}