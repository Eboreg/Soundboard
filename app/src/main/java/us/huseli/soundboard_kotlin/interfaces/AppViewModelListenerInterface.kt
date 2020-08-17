package us.huseli.soundboard_kotlin.interfaces

interface AppViewModelListenerInterface {
    fun onReorderEnabledChange(value: Boolean)
    fun onZoomLevelChange(value: Int)
}