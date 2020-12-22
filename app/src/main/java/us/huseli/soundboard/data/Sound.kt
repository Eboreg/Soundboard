package us.huseli.soundboard.data

import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import android.os.Parcel
import android.os.Parcelable
import android.provider.OpenableColumns
import android.util.Log
import androidx.room.*
import us.huseli.soundboard.SoundPlayer
import java.util.*

@Entity(
        tableName = "Sound",
        foreignKeys = [ForeignKey(
                entity = Category::class, parentColumns = ["id"], childColumns = ["categoryId"],
                onDelete = ForeignKey.CASCADE, onUpdate = ForeignKey.CASCADE)],
        indices = [Index("categoryId")]
)
data class Sound(
        @PrimaryKey(autoGenerate = true) var id: Int? = null,
        var categoryId: Int?,
        var name: String,
        val uri: Uri,
        var order: Int,
        var volume: Int,
        val added: Date
) : Parcelable {
    constructor(parcel: Parcel) : this(
            parcel.readValue(Int::class.java.classLoader) as? Int,
            parcel.readValue(Int::class.java.classLoader) as? Int,
            parcel.readString() ?: "",
            parcel.readParcelable(Uri::class.java.classLoader)!!,
            parcel.readInt(),
            parcel.readInt(),
            parcel.readSerializable() as Date) {
        Log.d("SOUND", "Create Sound though Parcelable constructor: $this")
    }

    @Ignore
    constructor(uri: Uri, flags: Int, contentResolver: ContentResolver) :
            this(null, null, "", uri, 0, 100, Date()) {
        /**
         * Used in MainActivity.onActivityResult when new sounds are added
         *
         * FLAG_GRANT_READ_URI_PERMISSION is not one of the permissions we are requesting
         * here, so bitwise-AND it away
         */
        contentResolver.takePersistableUriPermission(uri, flags and Intent.FLAG_GRANT_READ_URI_PERMISSION)
        // Try to set sound name based on filename:
        when (val cursor = contentResolver.query(uri, null, null, null, null)) {
            null -> name = ""
            else -> {
                cursor.moveToFirst()
                var filename = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME))
                if (filename.contains("."))
                    filename = filename.substring(0, filename.lastIndexOf("."))
                name = filename
                cursor.close()
            }
        }
        Log.d("SOUND", "Create Sound though new sound constructor: $this")
    }

    override fun equals(other: Any?) = other is Sound && other.id == id

    override fun toString(): String {
        val hashCode = Integer.toHexString(System.identityHashCode(this))
        return "Sound $hashCode <id=$id, name=$name, categoryId=$categoryId>"
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeValue(id)
        parcel.writeValue(categoryId)
        parcel.writeString(name)
        parcel.writeParcelable(uri, flags)
        parcel.writeInt(order)
        parcel.writeInt(volume)
        parcel.writeSerializable(added)
    }

    override fun describeContents(): Int = 0

    override fun hashCode(): Int = id ?: 0


    class Comparator(private val sortBy: SortParameter, private val sortOrder: SortOrder, private val players: HashMap<Sound, SoundPlayer>) : java.util.Comparator<Sound> {
        override fun compare(o1: Sound, o2: Sound): Int {
            val s1 = if (sortOrder == SortOrder.ASCENDING) o1 else o2
            val s2 = if (sortOrder == SortOrder.ASCENDING) o2 else o1
            // Why would this be unreachable code?
            return when (sortBy) {
                SortParameter.NAME -> {
                    return when {
                        s1.name.toLowerCase(Locale.ROOT) > s2.name.toLowerCase(Locale.ROOT) -> 1
                        s1.name.equals(s2.name, ignoreCase = true) -> 0
                        else -> -1
                    }
                }
                SortParameter.DURATION -> {
                    return when {
                        players[s1]?.duration ?: 0 > players[s2]?.duration ?: 0 -> 1
                        players[s1]?.duration ?: 0 == players[s2]?.duration ?: 0 -> 0
                        else -> -1
                    }
                }
                SortParameter.TIME_ADDED -> {
                    return when {
                        s1.added > s2.added -> 1
                        s1.added == s2.added -> 0
                        else -> 1
                    }
                }
            }
        }
    }


    enum class SortOrder { ASCENDING, DESCENDING }

    enum class SortParameter { NAME, DURATION, TIME_ADDED }

    companion object CREATOR : Parcelable.Creator<Sound> {
        override fun createFromParcel(parcel: Parcel) = Sound(parcel)

        override fun newArray(size: Int): Array<Sound?> = arrayOfNulls(size)
    }
}
