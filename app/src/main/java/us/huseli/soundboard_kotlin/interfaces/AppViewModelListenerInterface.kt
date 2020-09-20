package us.huseli.soundboard_kotlin.interfaces

interface AppViewModelListenerInterface {
    fun onReorderEnabledChange(value: Boolean): Any?
    fun onZoomLevelChange(value: Int): Any?
    fun onSelectEnabledChange(value: Boolean): Any?
}