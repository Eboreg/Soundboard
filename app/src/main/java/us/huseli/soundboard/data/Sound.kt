package us.huseli.soundboard.data

import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import android.os.Parcel
import android.os.Parcelable
import android.provider.OpenableColumns
import android.util.Log
import androidx.room.*

@Entity(
        tableName = "Sound",
        foreignKeys = [ForeignKey(
                entity = Category::class, parentColumns = ["id"], childColumns = ["categoryId"],
                onDelete = ForeignKey.CASCADE, onUpdate = ForeignKey.CASCADE)],
        indices = [Index("categoryId")]
)
data class Sound(
        @PrimaryKey(autoGenerate = true) override var id: Int? = null,
        override var categoryId: Int?,
        override var name: String,
        override val uri: Uri,
        override var order: Int,
        override var volume: Int
) : AbstractSound(), Parcelable {
    constructor(parcel: Parcel) : this(
            parcel.readValue(Int::class.java.classLoader) as? Int,
            parcel.readValue(Int::class.java.classLoader) as? Int,
            parcel.readString() ?: "",
            parcel.readParcelable(Uri::class.java.classLoader)!!,
            parcel.readInt(),
            parcel.readInt()) {
        Log.d("SOUND", "Create Sound though Parcelable constructor: $this")
    }

    @Ignore constructor(uri: Uri, flags: Int, contentResolver: ContentResolver): this(null, null, "", uri, 0, 100) {
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
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Sound> {
        override fun createFromParcel(parcel: Parcel) = Sound(parcel)

        override fun newArray(size: Int): Array<Sound?> = arrayOfNulls(size)
    }
}
