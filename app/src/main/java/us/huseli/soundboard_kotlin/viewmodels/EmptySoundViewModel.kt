package us.huseli.soundboard_kotlin.viewmodels

import us.huseli.soundboard_kotlin.data.EmptySound

class EmptySoundViewModel(override val sound: EmptySound, var adapterPosition: Int) : AbstractSoundViewModel()