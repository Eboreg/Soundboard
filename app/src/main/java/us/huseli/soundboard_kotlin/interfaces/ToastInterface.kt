package us.huseli.soundboard_kotlin.interfaces

interface ToastInterface {
    fun showToast(text: CharSequence): Any?
    fun showToast(textResource: Int): Any?
}