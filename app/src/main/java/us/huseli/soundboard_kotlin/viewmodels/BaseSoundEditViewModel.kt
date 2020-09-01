package us.huseli.soundboard_kotlin.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import us.huseli.soundboard_kotlin.GlobalApplication
import us.huseli.soundboard_kotlin.data.SoundRepository
import us.huseli.soundboard_kotlin.data.SoundboardDatabase

abstract class BaseSoundEditViewModel : ViewModel() {
    internal val repository = SoundRepository(SoundboardDatabase.getInstance(GlobalApplication.application, viewModelScope).soundDao())

    abstract fun setName(value: String)
    abstract fun setVolume(value: Int)
    abstract fun setCategoryId(value: Int)
    abstract fun save(): Job

    abstract val name: LiveData<String>
    abstract val volume: LiveData<Int>
    abstract val categoryId: LiveData<Int?>

    var categoryIndex: Int? = null
}