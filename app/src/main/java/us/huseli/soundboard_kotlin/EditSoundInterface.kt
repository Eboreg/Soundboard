package us.huseli.soundboard_kotlin

import android.os.Bundle
import us.huseli.soundboard_kotlin.data.Sound

interface EditSoundInterface {
    fun onSoundDialogSave(data: Bundle)
    fun showEditDialog(sound: Sound)
}