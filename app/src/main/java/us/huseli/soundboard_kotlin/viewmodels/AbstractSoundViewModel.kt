package us.huseli.soundboard_kotlin.viewmodels

import android.graphics.Color
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import us.huseli.soundboard_kotlin.data.AbstractSound

abstract class AbstractSoundViewModel : ViewModel() {
    abstract val sound: AbstractSound

    open val backgroundColor: LiveData<Int> = liveData { emit(Color.TRANSPARENT) }
    open val textColor: LiveData<Int> = liveData { emit(Color.WHITE) }
    open var errorMessage: String = ""
    open val duration: LiveData<String> = liveData { emit("") }
    open val name: LiveData<String> = liveData { emit("") }
    open val volume: LiveData<Int> = liveData { emit(100) }
}