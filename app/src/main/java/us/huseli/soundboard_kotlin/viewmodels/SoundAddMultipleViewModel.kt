package us.huseli.soundboard_kotlin.viewmodels

import android.content.Context
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import us.huseli.soundboard_kotlin.R
import us.huseli.soundboard_kotlin.data.Sound

class SoundAddMultipleViewModel(private val sounds: List<Sound>, context: Context) : BaseSoundEditViewModel() {
    override val name = liveData { emit(context.getString(R.string.multiple_sounds_selected)) }
    override val volume = liveData { emit(100) }

    override fun setName(value: String) {}

    override fun setVolume(value: Int) = sounds.forEach { it.volume = value }

    override fun setCategoryId(value: Int) = sounds.forEach { it.categoryId = value }

    override fun save() = viewModelScope.launch(Dispatchers.IO) {
        sounds.forEach { repository.insert(it) }
    }
}