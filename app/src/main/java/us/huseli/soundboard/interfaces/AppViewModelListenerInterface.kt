package us.huseli.soundboard.interfaces

interface AppViewModelListenerInterface {
    fun onReorderEnabledChange(value: Boolean): Any?
    fun onSelectEnabledChange(value: Boolean): Any?
}