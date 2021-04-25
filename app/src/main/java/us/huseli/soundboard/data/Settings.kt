package us.huseli.soundboard.data

import android.content.SharedPreferences
import com.google.gson.Gson
import us.huseli.soundboard.helpers.Functions

class Settings(language: String?, nightMode: String?, bufferSize: Int?) {
    private var _language: String? = language
    private var _nightMode: String? = nightMode
    private var _bufferSize: Int = bufferSize ?: -1

    val language: String
        get() = _language ?: Constants.DEFAULT_LANGUAGE

    val nightMode: String
        get() = _nightMode ?: "default"

    val bufferSize: Int
        get() =
            if (_bufferSize > -1) _bufferSize else Functions.bufferSizeToSeekbarValue(Constants.DEFAULT_BUFFER_SIZE)

    fun setLanguage(value: String?) {
        _language = value
    }

    fun setNightMode(value: String?) {
        _nightMode = value
    }

    fun setBufferSize(value: Int) {
        _bufferSize = value
    }

    fun toJson(): String = Gson().toJson(this)

    companion object {
        fun fromJson(json: String): Settings = Gson().fromJson(json, Settings::class.java)

        fun fromPrefs(prefs: SharedPreferences?) = Settings(
            prefs?.getString("language", null),
            prefs?.getString("nightMode", null),
            prefs?.getInt("bufferSize", -1)
        )
    }
}
