package us.huseli.soundboard.data

import android.net.Uri

abstract class AbstractSound {
    abstract val volume: Int
    abstract var id: Int?
    abstract var categoryId: Int?
    abstract var name: String
    abstract val uri: Uri
    abstract var order: Int
}