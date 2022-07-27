package us.huseli.soundboard.helpers

import android.content.Context
import android.net.Uri
import android.util.AttributeSet
import androidx.preference.Preference

class FolderSelectPreference(context: Context, attrs: AttributeSet?) : Preference(context, attrs) {
    private var value: Uri? = null

    override fun onSetInitialValue(defaultValue: Any?) {
        val value = getPersistedString(defaultValue?.toString() ?: "")
        setValue(if (value == "") null else Uri.parse(value))
    }

    fun getValue() = value

    fun setValue(newValue: Uri?) {
        summary = newValue?.path ?: "(Not set)"
        persistString(newValue?.toString() ?: "")
        notifyChanged()
    }
}