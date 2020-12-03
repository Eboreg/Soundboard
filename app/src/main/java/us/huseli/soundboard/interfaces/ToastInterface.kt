package us.huseli.soundboard.interfaces

interface ToastInterface {
    fun showToast(text: CharSequence): Any?
    fun showToast(textResource: Int): Any?
}