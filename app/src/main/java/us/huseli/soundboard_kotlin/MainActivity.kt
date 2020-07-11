package us.huseli.soundboard_kotlin

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import us.huseli.soundboard_kotlin.data.Sound
import us.huseli.soundboard_kotlin.data.SoundViewModel

class MainActivity : AppCompatActivity(), EditSoundInterface {
    private val viewModel by viewModels<SoundViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val toolbar = findViewById<View>(R.id.toolbar) as Toolbar
        setSupportActionBar(toolbar)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.appbar_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_add_sound -> {
                val intent = Intent(this, AddSoundActivity::class.java)
                startActivity(intent)
            }
        }
        return true
    }

    override fun showEditDialog(sound: Sound) {
        supportFragmentManager.beginTransaction().apply {
            val fragment = EditSoundFragment.newInstance(sound.id, sound.name)
            add(0, fragment)
            show(fragment)
            commit()
        }
    }

    override fun onSoundDialogSave(data: Bundle) {
        val soundId = data.getInt(SoundListFragment.ARG_SOUND_ID)
        val soundName = data.getString(SoundListFragment.ARG_SOUND_NAME, "")
        viewModel.updateSoundName(soundId, soundName)
    }
}