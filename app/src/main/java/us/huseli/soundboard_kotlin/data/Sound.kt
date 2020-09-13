package us.huseli.soundboard_kotlin.data

import android.net.Uri
import android.os.Parcel
import android.os.Parcelable
import androidx.room.*
import us.huseli.soundboard_kotlin.interfaces.OrderableItem

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
        override var order: Int,
        var volume: Int
) : OrderableItem, Parcelable {
    constructor(parcel: Parcel) : this(
            parcel.readValue(Int::class.java.classLoader) as? Int,
            parcel.readValue(Int::class.java.classLoader) as? Int,
            parcel.readString() ?: "",
            parcel.readParcelable(Uri::class.java.classLoader)!!,
            //Converters.stringToUri(parcel.readString()!!),
            parcel.readInt(),
            parcel.readInt())

    @Ignore constructor(name: String, uri: Uri): this(null, null, name, uri, 0, 100)

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeValue(id)
        parcel.writeValue(categoryId)
        parcel.writeString(name)
        //parcel.writeString(Converters.uriToString(uri))
        parcel.writeParcelable(uri, flags)
        parcel.writeInt(order)
        parcel.writeInt(volume)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Sound> {
        override fun createFromParcel(parcel: Parcel): Sound {
            return Sound(parcel)
        }

        override fun newArray(size: Int): Array<Sound?> {
            return arrayOfNulls(size)
        }
    }
}
