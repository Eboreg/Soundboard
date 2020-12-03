package us.huseli.soundboard.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import us.huseli.soundboard.GlobalApplication
import us.huseli.soundboard.data.SoundRepository
import us.huseli.soundboard.data.SoundboardDatabase

abstract class BaseSoundEditViewModel : ViewModel() {
    internal val repository = SoundRepository(SoundboardDatabase.getInstance(GlobalApplication.application).soundDao())

    abstract fun setName(value: String)
    abstract fun setVolume(value: Int)
    abstract fun setCategoryId(value: Int): Any?
    abstract fun save(): Any?

    abstract val name: LiveData<String>
    abstract val volume: LiveData<Int>

    var categoryIndex: Int? = null
}