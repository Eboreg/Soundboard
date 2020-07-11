package us.huseli.soundboard_kotlin

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.OpenableColumns
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import us.huseli.soundboard_kotlin.data.Sound
import us.huseli.soundboard_kotlin.data.SoundViewModel

class AddSoundActivity : AppCompatActivity(), EditSoundInterface {
    companion object {
        const val REQUEST_SOUND_GET = 1
    }

    private val viewModel by viewModels<SoundViewModel>()
    private lateinit var sound: Sound

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.fragment_edit_sound)

        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
        intent.type = "audio/*"
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true)
        if (intent.resolveActivity(packageManager) != null) {
            startActivityForResult(intent, REQUEST_SOUND_GET)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_SOUND_GET && resultCode == Activity.RESULT_OK) {
            data?.data?.let {
                val soundName: String
                when (val cursor = contentResolver.query(it, null, null, null, null)) {
                    null -> soundName = ""
                    else -> {
                        cursor.moveToFirst()
                        var filename = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME))
                        if (filename.contains("."))
                            filename = filename.substring(0, filename.lastIndexOf("."))
                        soundName = filename
                        cursor.close()
                    }
                }
                sound = Sound(soundName, it)
                showEditDialog(sound)
            }
        }
    }

    override fun onSoundDialogSave(data: Bundle) {
        sound.name = data.getString(SoundListFragment.ARG_SOUND_NAME, "")
        viewModel.insertSound(sound)
        finish()
    }

    override fun showEditDialog(sound: Sound) {
        supportFragmentManager.beginTransaction().apply {
            val fragment = EditSoundFragment.newInstance(null, sound.name)
            add(0, fragment)
            show(fragment)
            commit()
        }
    }
}