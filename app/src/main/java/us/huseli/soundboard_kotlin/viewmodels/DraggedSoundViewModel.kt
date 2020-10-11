package us.huseli.soundboard_kotlin.viewmodels

import androidx.lifecycle.ViewModel
import us.huseli.soundboard_kotlin.data.Sound

class DraggedSoundViewModel(val sound: Sound, var recyclerViewHashCode: Int, val viewModel: SoundViewModel) : ViewModel() {
    fun start() = viewModel.startDrag()

    fun stop() = viewModel.stopDrag()
}