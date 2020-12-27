package us.huseli.soundboard.interfaces

interface SnackbarInterface {
    fun showSnackbar(text: CharSequence): Any?
    fun showSnackbar(textResource: Int): Any?
}