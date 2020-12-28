package us.huseli.soundboard.data

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.os.Parcel
import android.os.Parcelable
import android.provider.OpenableColumns
import android.util.Log
import androidx.core.net.toUri
import androidx.room.*
import us.huseli.soundboard.GlobalApplication
import us.huseli.soundboard.helpers.MD5
import java.io.File
import java.io.FileOutputStream
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
        val added: Date,
        var duration: Int,
        var checksum: String?,
        @Ignore var path: String?) : Parcelable {

    constructor(parcel: Parcel) : this(
            parcel.readValue(Int::class.java.classLoader) as? Int,  // id
            parcel.readValue(Int::class.java.classLoader) as? Int,  // categoryId
            parcel.readString() ?: "",  // name
            parcel.readParcelable(Uri::class.java.classLoader)!!,  // uri
            parcel.readInt(),  // order
            parcel.readInt(),  // volume
            parcel.readSerializable() as Date,  // added
            parcel.readInt(),  // duration
            parcel.readString(),  // checksum
            parcel.readString()  // path
    ) {
        Log.d("SOUND", "Create Sound though Parcelable constructor: $this")
    }

    constructor(id: Int?, categoryId: Int?, name: String, uri: Uri, order: Int, volume: Int, added: Date, duration: Int, checksum: String?) :
            this(id, categoryId, name, uri, order, volume, added, duration, checksum, uri.path)

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
        parcel.writeInt(duration)
        parcel.writeString(checksum)
        parcel.writeString(path)
    }

    override fun describeContents(): Int = 0

    override fun hashCode(): Int = id ?: 0

    fun copyData(contentResolver: ContentResolver, context: Context) {
        val inputStream = contentResolver.openInputStream(uri)
                ?: throw Exception("File provider returned null")
        val filename = checksum ?: uri.hashCode().toString().replace("-", "_")
        val outputStream = context.openFileOutput(filename, Context.MODE_PRIVATE)
        val buf = ByteArray(1024)
        var len: Int
        while (inputStream.read(buf).also { len = it } > 0) {
            outputStream.write(buf, 0, len)
        }
        outputStream.close()
        inputStream.close()

    }


    class Comparator(private val sortBy: SortParameter, private val sortOrder: SortOrder) : java.util.Comparator<Sound> {
        override fun compare(o1: Sound, o2: Sound): Int {
            val s1 = if (sortOrder == SortOrder.ASCENDING) o1 else o2
            val s2 = if (sortOrder == SortOrder.ASCENDING) o2 else o1
            return when (sortBy) {
                SortParameter.NAME -> {
                    when {
                        s1.name.toLowerCase(Locale.ROOT) > s2.name.toLowerCase(Locale.ROOT) -> 1
                        s1.name.equals(s2.name, ignoreCase = true) -> 0
                        else -> -1
                    }
                }
                SortParameter.DURATION -> {
                    when {
                        s1.duration > s2.duration -> 1
                        s1.duration == s2.duration -> 0
                        else -> -1
                    }
                }
                SortParameter.TIME_ADDED -> {
                    when {
                        s1.added > s2.added -> 1
                        s1.added == s2.added -> 0
                        else -> -1
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

        fun createTemporary(uri: Uri): Sound {
            /** Create Sound object from non-local URI, not to be saved to DB */
            val application = GlobalApplication.application
            val inputStream = application.contentResolver.openInputStream(uri)
                    ?: throw Exception("File provider returned null")
            val checksum = MD5.calculate(inputStream)
                    ?: throw Exception("MD5.calculate returned null")
            inputStream.close()

            val name: String

            when (val cursor = application.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)) {
                null -> name = ""
                else -> {
                    cursor.moveToFirst()
                    name = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)).let {
                        if (it.contains(".")) it.substring(0, it.lastIndexOf("."))
                        else it
                    }
                    cursor.close()
                }
            }

            return Sound(null, null, name, uri, -1, 100, Date(), -1, checksum)
        }

        fun createFromTemporary(tempSound: Sound): Sound {
            /** Copy data to local storage and return new Sound object to be saved to DB */
            val application = GlobalApplication.application
            val inputStream = application.contentResolver.openInputStream(tempSound.uri)
                    ?: throw Exception("File provider returned null")

            /** Some paranoid extra measures */
            val filename = tempSound.checksum ?: MD5.calculate(inputStream)
            ?: throw Exception("MD5.calculate returned null")
            val file = File(application.soundDir, filename)
            val outputStream = FileOutputStream(file)
            //val outputStream = application.applicationContext.openFileOutput(filename, Context.MODE_PRIVATE)
            val buf = ByteArray(1024)
            var len: Int
            while (inputStream.read(buf).also { len = it } > 0) {
                outputStream.write(buf, 0, len)
            }
            outputStream.close()
            inputStream.close()

            val uri = file.toUri()

            return Sound(null, tempSound.categoryId, tempSound.name, uri, tempSound.order, tempSound.volume, tempSound.added, tempSound.duration, tempSound.checksum)
        }
    }
}
