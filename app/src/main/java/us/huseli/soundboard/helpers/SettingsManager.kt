package us.huseli.soundboard.helpers

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
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
    private var watchFolder = prefs.getString("watchFolder", null)?.let {
        if (it != "") Uri.parse(it) else null
    }
    fun getWatchFolder() = watchFolder
    private fun setWatchFolder(value: Uri?) {
        watchFolder = value
        prefs.edit().putString("watchFolder", value?.toString()).apply()
        listeners.forEach { listener -> listener.onSettingChanged("watchFolder", value) }
    }

    @Expose
    private var watchFolderTrashMissing = prefs.getBoolean("watchFolderTrashMissing", false)
    fun getWatchFolderTrashMissing() = watchFolderTrashMissing
    private fun setWatchFolderTrashMissing(value: Boolean) {
        if (value != watchFolderTrashMissing) {
            watchFolderTrashMissing = value
            prefs.edit().putBoolean("watchFolderTrashMissing", value).apply()
            listeners.forEach { it.onSettingChanged("watchFolderTrashMissing", value) }
        }
    }

    @Expose
    private var language = prefs.getString("language", null) ?: Constants.DEFAULT_LANGUAGE
    fun getLanguage() = language
    private fun setLanguage(value: String) {
        if (value != language) {
            language = value
            prefs.edit().putString("language", value).apply()
            listeners.forEach { it.onSettingChanged("language", value) }
        }
    }

    @Expose
    private var nightMode = prefs.getString("nightMode", null) ?: "default"
    fun getNightMode() = nightMode
    private fun setNightMode(value: String) {
        if (value != nightMode) {
            nightMode = value
            prefs.edit().putString("nightMode", value).apply()
            listeners.forEach { listener -> listener.onSettingChanged("nightMode", value) }
        }
    }

    @Expose
    private var bufferSize =
        prefs.getInt("bufferSize", Functions.bufferSizeToSeekbarValue(Constants.DEFAULT_BUFFER_SIZE))
    fun getBufferSize() = bufferSize
    private fun setBufferSize(value: Int) {
        if (value != bufferSize && value > -1) {
            bufferSize = value
            prefs.edit().putInt("bufferSize", value).apply()
            listeners.forEach { it.onSettingChanged("bufferSize", value) }
        }
    }

    @Expose
    private var landscapeSpanCount = prefs.getInt("landscapeSpanCount", Constants.DEFAULT_SPANCOUNT_LANDSCAPE)
    fun getLandscapeSpanCount() = landscapeSpanCount
    fun setLandscapeSpanCount(value: Int) {
        if (value != landscapeSpanCount && value > -1) {
            landscapeSpanCount = value
            prefs.edit().putInt("landscapeSpanCount", value).apply()
            listeners.forEach { it.onSettingChanged("landscapeSpanCount", value) }
        }
    }

    @Expose
    private var lastVersion = prefs.getLong("lastRunVersionCode", 0)
    fun getLastVersion() = lastVersion
    fun setLastVersion(value: Long) {
        if (value != lastVersion) {
            lastVersion = value
            prefs.edit().putLong("lastRunVersionCode", value).apply()
            listeners.forEach { it.onSettingChanged("lastRunVersionCode", value) }
        }
    }

    @Expose
    private var repressMode = prefs.getString("repressMode", null).let {
        if (it != null) SoundPlayer.RepressMode.valueOf(it) else SoundPlayer.RepressMode.STOP
    }

    fun getRepressMode() = repressMode
    fun setRepressMode(value: SoundPlayer.RepressMode) {
        if (value != repressMode) {
            repressMode = value
            prefs.edit().putString("repressMode", value.name).apply()
            listeners.forEach { it.onSettingChanged("repressMode", value) }
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
            setWatchFolder(it.watchFolder)
            setWatchFolderTrashMissing(it.watchFolderTrashMissing)
        }
    }

    fun registerListener(listener: Listener) = listeners.add(listener)

    fun unregisterListener(listener: Listener) = listeners.remove(listener)

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        when (key) {
            "language" -> sharedPreferences?.getString(key, Constants.DEFAULT_LANGUAGE)?.also {
                if (language != it) {
                    language = it
                    listeners.forEach { listener -> listener.onSettingChanged(key, it) }
                }
            }
            "nightMode" -> sharedPreferences?.getString(key, "default")?.also {
                if (nightMode != it) {
                    nightMode = it
                    listeners.forEach { listener -> listener.onSettingChanged(key, it) }
                }
            }
            "bufferSize" -> sharedPreferences?.getInt(key,
                Functions.bufferSizeToSeekbarValue(Constants.DEFAULT_BUFFER_SIZE))?.also {
                if (bufferSize != it) {
                    bufferSize = it
                    listeners.forEach { listener -> listener.onSettingChanged(key, it) }
                }
            }
            "landscapeSpanCount" -> sharedPreferences?.getInt(key, Constants.DEFAULT_SPANCOUNT_LANDSCAPE)?.also {
                if (landscapeSpanCount != it) {
                    landscapeSpanCount = it
                    listeners.forEach { listener -> listener.onSettingChanged(key, it) }
                }
            }
            "lastRunVersionCode" -> sharedPreferences?.getLong(key, 0)?.also {
                if (lastVersion != it) {
                    lastVersion = it
                    listeners.forEach { listener -> listener.onSettingChanged(key, it) }
                }
            }
            "repressMode" -> sharedPreferences?.getString(key, null).also {
                val value = it?.let { SoundPlayer.RepressMode.valueOf(it) } ?: SoundPlayer.RepressMode.STOP
                if (repressMode != value) {
                    repressMode = value
                    listeners.forEach { listener -> listener.onSettingChanged(key, value) }
                }
            }
            "watchFolder" -> sharedPreferences?.getString(key, null).also {
                val value = if (it == null || it == "") null else Uri.parse(it)
                if (watchFolder != value) {
                    watchFolder = value
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
        fun onSettingChanged(key: String, value: Any?)
    }
}