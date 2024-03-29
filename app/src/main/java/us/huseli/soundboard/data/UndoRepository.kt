package us.huseli.soundboard.data

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UndoRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val categoryDao: CategoryDao,
    private val soundDao: SoundDao) {

    private val scope = CoroutineScope(Job() + Dispatchers.Default)
    private val states = StateList()
    private val _isRedoPossible = MutableLiveData(false)
    private val _isUndoPossible = MutableLiveData(false)

    val isRedoPossible: LiveData<Boolean>
        get() = _isRedoPossible

    val isUndoPossible: LiveData<Boolean>
        get() = _isUndoPossible

    init {
        /** First push the initial state to position 0 */
        scope.launch { pushState() }
    }

    suspend fun pushState() {
        states.push(State(soundDao.listAll(), categoryDao.list().first()))
    }

    suspend fun replaceCurrentState() {
        states.replaceCurrent(State(soundDao.listAll(), categoryDao.list().first()))
    }

    suspend fun redo() = apply(states.getRedoState())

    suspend fun undo() = apply(states.getUndoState())

    private suspend fun apply(state: State?) {
        state?.categories?.also { categoryDao.applyState(it) }
        state?.sounds?.also { soundDao.applyState(it) }
    }


    class State(val sounds: List<Sound>?, val categories: List<Category>?)


    inner class StateList {
        /** Make sure not to update currentPos on element access, only when such access has been successful */
        private var currentPos = -1
            set(value) {
                field = value
                _isUndoPossible.postValue(value > 0)
                _isRedoPossible.postValue(value + 1 < list.size)
            }
        private val list = mutableListOf<State>()

        fun getRedoState() = list.getOrNull(currentPos + 1)?.also { currentPos++ }

        fun getUndoState() = list.getOrNull(currentPos - 1)?.also { currentPos-- }

        fun push(state: State): Boolean {
            /** On push of new state, all states after currentPos become unusable and are scrapped */
            if (currentPos < list.size - 1) {
                list.removeAll(list.subList(currentPos + 1, list.size).toSet())
                currentPos = list.size - 1
            }
            return list.add(state).also {
                if (it) {
                    currentPos++
                    /** If max undo states is reached, delete the first one */
                    if (list.size > Constants.MAX_UNDO_STATES) removeFirst()
                }
            }
        }

        fun replaceCurrent(state: State) {
            list[currentPos] = state
        }

        private fun deleteSoundFiles(removedState: State, nextState: State) {
            if (removedState.sounds != null && nextState.sounds != null)
                removedState.sounds.subtract(nextState.sounds).forEach {
                    context.getDir(Constants.SOUND_DIRNAME, Context.MODE_PRIVATE)?.listFiles()?.forEach { file ->
                        if (file.path == it.path) file.delete()
                    }
                }
        }

        private fun removeFirst(): State? {
            /**
             * When removing the first state, delete any files included in it but not in the next state (which SHOULD
             * now have the same index as the deleted state once had)
             */
            return list.removeFirstOrNull()?.also { removedState ->
                currentPos--
                list.firstOrNull()?.also { nextState -> deleteSoundFiles(removedState, nextState) }
            }
        }
    }
}