package us.huseli.soundboard.data

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.os.Parcel
import android.os.Parcelable
import android.provider.OpenableColumns
import android.util.Log
import androidx.room.*
import us.huseli.soundboard.BuildConfig
import us.huseli.soundboard.helpers.MD5
import java.io.File
import java.io.FileOutputStream
import java.util.*

@Entity(
    tableName = "Sound",
    foreignKeys = [ForeignKey(
        entity = Category::class,
        parentColumns = ["id"],
        childColumns = ["categoryId"],
        onDelete = ForeignKey.CASCADE,
        onUpdate = ForeignKey.CASCADE
    )],
    indices = [Index("categoryId")]
)
open class Sound(
    @PrimaryKey(autoGenerate = true) open val id: Int?,
    open val categoryId: Int?,
    open val name: String,
    open val path: String,
    open val order: Int,
    open val volume: Int,
    open val added: Date,
    open val duration: Long,
    open val checksum: String,
    open val trashed: Boolean,
    @Ignore val uri: Uri?
) : Parcelable {

    constructor(
        id: Int?,
        categoryId: Int?,
        name: String,
        path: String,
        order: Int,
        volume: Int,
        added: Date,
        duration: Long,
        checksum: String,
        trashed: Boolean
    ) :
            this(id, categoryId, name, path, order, volume, added, duration, checksum, trashed, null)

    constructor(parcel: Parcel) : this(
        parcel.readValue(Int::class.java.classLoader) as? Int,  // id
        parcel.readValue(Int::class.java.classLoader) as? Int,  // categoryId
        parcel.readString() ?: "",                        // name
        parcel.readString() ?: "",                         // path
        parcel.readInt(),                                       // order
        parcel.readInt(),                                       // volume
        parcel.readSerializable() as Date,                      // added
        parcel.readLong(),                                      // duration
        parcel.readString() ?: "",                     // checksum
        parcel.readBoolean(),                                   // trashed
        null                                                // uri
    ) {
        @Suppress("LeakingThis")
        if (BuildConfig.DEBUG) Log.d("SOUND", "Create Sound though Parcelable constructor: $this")
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
        parcel.writeString(path)
        parcel.writeInt(order)
        parcel.writeInt(volume)
        parcel.writeSerializable(added)
        parcel.writeLong(duration)
        parcel.writeString(checksum)
        parcel.writeBoolean(trashed)
    }

    override fun describeContents() = 0

    override fun hashCode() = id ?: 0

    fun calculateChecksum() = MD5.calculate(File(path)) ?: throw Exception("MD5.calculate returned null")


    class Comparator(private val sorting: SoundSorting) :
        java.util.Comparator<Sound> {

        override fun compare(o1: Sound, o2: Sound): Int {
            val s1 = if (sorting.order == SoundSorting.Order.ASCENDING) o1 else o2
            val s2 = if (sorting.order == SoundSorting.Order.ASCENDING) o2 else o1
            return when (sorting.parameter) {
                SoundSorting.Parameter.NAME -> {
                    when {
                        s1.name.lowercase(Locale.getDefault()) > s2.name.lowercase(Locale.getDefault()) -> 1
                        s1.name.equals(s2.name, ignoreCase = true) -> 0
                        else -> -1
                    }
                }
                SoundSorting.Parameter.DURATION -> {
                    when {
                        s1.duration > s2.duration -> 1
                        s1.duration == s2.duration -> 0
                        else -> -1
                    }
                }
                SoundSorting.Parameter.TIME_ADDED -> {
                    when {
                        s1.added > s2.added -> 1
                        s1.added == s2.added -> 0
                        else -> -1
                    }
                }
                SoundSorting.Parameter.UNDEFINED -> 0
            }
        }
    }


    @SuppressLint("ParcelCreator")
    companion object CREATOR : Parcelable.Creator<Sound> {
        override fun createFromParcel(parcel: Parcel) = Sound(parcel)

        override fun newArray(size: Int): Array<Sound?> = arrayOfNulls(size)

        fun createTemporary(uri: Uri, context: Context): Sound {
            /** Create Sound object from non-local URI, not to be saved to DB */
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: throw Exception("File provider returned null")
            val checksum = MD5.calculate(inputStream)
                ?: throw Exception("MD5.calculate returned null")
            inputStream.close()

            val cursor = context.contentResolver.query(
                uri,
                arrayOf(OpenableColumns.DISPLAY_NAME),
                null,
                null,
                null
            )

            val name = when (cursor) {
                null -> ""
                else -> {
                    cursor.moveToFirst()
                    cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)).let {
                        if (it.contains(".")) it.substring(0, it.lastIndexOf("."))
                        else it
                    }
                }
            }
            cursor?.close()

            return Sound(
                null,
                null,
                name,
                uri.path ?: "",
                -1,
                Constants.DEFAULT_VOLUME,
                Date(),
                -1,
                checksum,
                false,
                uri
            )
        }

        fun createFromTemporary(
            tempSound: Sound,
            name: String?,
            volume: Int?,
            categoryId: Int?,
            order: Int?,
            context: Context
        ): Sound {
            /** Copy data to local storage and return new Sound object to be saved to DB */
            val uri = tempSound.uri ?: throw Exception("Sound ${tempSound.name} has no URI")
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val outputFile = File(context.getDir(Constants.SOUND_DIRNAME, Context.MODE_PRIVATE), tempSound.checksum)

                FileOutputStream(outputFile).use { outputStream ->
                    val buf = ByteArray(1024)
                    var len: Int

                    while (inputStream.read(buf).also { len = it } > 0) {
                        outputStream.write(buf, 0, len)
                    }

                    return Sound(
                        null,
                        categoryId ?: tempSound.categoryId,
                        name ?: tempSound.name,
                        outputFile.path,
                        order ?: tempSound.order,
                        volume ?: tempSound.volume,
                        tempSound.added,
                        tempSound.duration,
                        tempSound.checksum,
                        false
                    )
                }
            } ?: throw Exception("File provider returned null")
        }

        fun createFromTemporary(tempSound: Sound, categoryId: Int?, order: Int?, context: Context) =
            createFromTemporary(tempSound, null, null, categoryId, order, context)

        fun createFromTemporary(tempSound: Sound, context: Context) =
            createFromTemporary(tempSound, null, null, null, null, context)
    }
}

data class SoundExtended(
    override val id: Int?,
    override val categoryId: Int?,
    override val name: String,
    override val path: String,
    override val order: Int,
    override val volume: Int,
    override val added: Date,
    override val duration: Long,
    override val checksum: String,
    override val trashed: Boolean,
    val backgroundColor: Int?
) :
    Sound(id, categoryId, name, path, order, volume, added, duration, checksum, trashed) {

    @Ignore
    var textColor: Int? = null

    override fun equals(other: Any?) = other is Sound && other.id == id

    override fun hashCode(): Int = id ?: 0

    override fun toString(): String {
        val hashCode = Integer.toHexString(System.identityHashCode(this))
        return "SoundExtended $hashCode <id=$id, name=$name, backgroundColor=$backgroundColor, categoryId=$categoryId>"
    }
}


class SoundSorting(val parameter: Parameter, val order: Order) {
    enum class Order { ASCENDING, DESCENDING }
    enum class Parameter { UNDEFINED, NAME, DURATION, TIME_ADDED }
}