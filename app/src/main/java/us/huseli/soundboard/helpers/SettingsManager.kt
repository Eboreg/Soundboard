package us.huseli.soundboard.helpers

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.annotations.Expose
import dagger.hilt.android.qualifiers.ApplicationContext
import us.huseli.soundboard.audio.SoundPlayer
import us.huseli.soundboard.data.Constants
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsManager @Inject constructor(@ApplicationContext context: Context) :
    SharedPreferences.OnSharedPreferenceChangeListener {
    private val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        .also { it.registerOnSharedPreferenceChangeListener(this) }
    private val listeners = mutableListOf<Listener>()

    @Expose
    private var language = prefs.getString(Constants.PREF_LANGUAGE, null) ?: Constants.DEFAULT_LANGUAGE
    fun getLanguage() = language
    fun setLanguage(value: String) {
        if (value != language) {
            language = value
            prefs.edit().putString(Constants.PREF_LANGUAGE, value).apply()
            listeners.forEach { it.onSettingChanged(Constants.PREF_LANGUAGE, value) }
        }
    }

    @Expose
    private var nightMode = prefs.getString(Constants.PREF_NIGHT_MODE, null) ?: "default"
    fun getNightMode() = nightMode
    private fun setNightMode(value: String) {
        if (value != nightMode) {
            nightMode = value
            prefs.edit().putString(Constants.PREF_NIGHT_MODE, value).apply()
            listeners.forEach { listener -> listener.onSettingChanged(Constants.PREF_NIGHT_MODE, value) }
        }
    }

    @Expose
    private var bufferSize =
        prefs.getInt(Constants.PREF_BUFFER_SIZE, Functions.bufferSizeToSeekbarValue(Constants.DEFAULT_BUFFER_SIZE))

    fun getBufferSize() = bufferSize
    fun setBufferSize(value: Int) {
        if (value != bufferSize && value > -1) {
            bufferSize = value
            prefs.edit().putInt(Constants.PREF_BUFFER_SIZE, value).apply()
            listeners.forEach { it.onSettingChanged(Constants.PREF_BUFFER_SIZE, value) }
        }
    }

    @Expose
    private var landscapeSpanCount =
        prefs.getInt(Constants.PREF_LANDSCAPE_SPAN_COUNT, Constants.DEFAULT_SPANCOUNT_LANDSCAPE)

    fun getLandscapeSpanCount() = landscapeSpanCount
    fun setLandscapeSpanCount(value: Int) {
        if (value != landscapeSpanCount && value > -1) {
            landscapeSpanCount = value
            prefs.edit().putInt(Constants.PREF_LANDSCAPE_SPAN_COUNT, value).apply()
            listeners.forEach { it.onSettingChanged(Constants.PREF_LANDSCAPE_SPAN_COUNT, value) }
        }
    }

    @Expose
    private var lastVersion = prefs.getLong(Constants.PREF_LAST_VERSION, 0)
    fun getLastVersion() = lastVersion
    fun setLastVersion(value: Long) {
        if (value != lastVersion) {
            lastVersion = value
            prefs.edit().putLong(Constants.PREF_LAST_VERSION, value).apply()
            listeners.forEach { it.onSettingChanged(Constants.PREF_LAST_VERSION, value) }
        }
    }

    @Expose
    private var repressMode = prefs.getString(Constants.PREF_REPRESS_MODE, null).let {
        if (it != null) SoundPlayer.RepressMode.valueOf(it) else SoundPlayer.RepressMode.STOP
    }

    fun getRepressMode() = repressMode
    fun setRepressMode(value: SoundPlayer.RepressMode) {
        if (value != repressMode) {
            repressMode = value
            prefs.edit().putString(Constants.PREF_REPRESS_MODE, value.name).apply()
            listeners.forEach { it.onSettingChanged(Constants.PREF_REPRESS_MODE, value) }
        }
    }

    fun dumpJson(): String =
        GsonBuilder()
            .excludeFieldsWithoutExposeAnnotation()
            .create()
            .toJson(this)

    fun loadJson(json: String) {
        Gson().fromJson(json, SettingsManager::class.java)?.also {
            setLanguage(it.language)
            setNightMode(it.nightMode)
            setBufferSize(it.bufferSize)
            setLandscapeSpanCount(it.landscapeSpanCount)
            setLastVersion(it.lastVersion)
            setRepressMode(it.repressMode)
        }
    }

    fun registerListener(listener: Listener) = listeners.add(listener)

    fun unregisterListener(listener: Listener) = listeners.remove(listener)

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        when (key) {
            Constants.PREF_LANGUAGE -> sharedPreferences?.getString(key, Constants.DEFAULT_LANGUAGE)?.also {
                if (language != it) {
                    language = it
                    listeners.forEach { listener -> listener.onSettingChanged(key, it) }
                }
            }
            Constants.PREF_NIGHT_MODE -> sharedPreferences?.getString(key, "default")?.also {
                if (nightMode != it) {
                    nightMode = it
                    listeners.forEach { listener -> listener.onSettingChanged(key, it) }
                }
            }
            Constants.PREF_BUFFER_SIZE -> sharedPreferences?.getInt(key,
                Functions.bufferSizeToSeekbarValue(Constants.DEFAULT_BUFFER_SIZE))?.also {
                if (bufferSize != it) {
                    bufferSize = it
                    listeners.forEach { listener -> listener.onSettingChanged(key, it) }
                }
            }
            Constants.PREF_LANDSCAPE_SPAN_COUNT -> sharedPreferences?.getInt(key, Constants.DEFAULT_SPANCOUNT_LANDSCAPE)
                ?.also {
                    if (landscapeSpanCount != it) {
                        landscapeSpanCount = it
                        listeners.forEach { listener -> listener.onSettingChanged(key, it) }
                    }
                }
            Constants.PREF_LAST_VERSION -> sharedPreferences?.getLong(key, 0)?.also {
                if (lastVersion != it) {
                    lastVersion = it
                    listeners.forEach { listener -> listener.onSettingChanged(key, it) }
                }
            }
            Constants.PREF_REPRESS_MODE -> sharedPreferences?.getString(key, null).also {
                val value = it?.let { SoundPlayer.RepressMode.valueOf(it) } ?: SoundPlayer.RepressMode.STOP
                if (repressMode != value) {
                    repressMode = value
                    listeners.forEach { listener -> listener.onSettingChanged(key, value) }
                }
            }
        }
    }

    fun destroy() {
        prefs.unregisterOnSharedPreferenceChangeListener(this)
    }


    companion object {
        fun createForContext(context: Context) = SettingsManager(context)
    }


    interface Listener {
        fun onSettingChanged(key: String, value: Any)
    }
}