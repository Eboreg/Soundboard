package us.huseli.soundboard

import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

class test {
    init {
        val inputStream = GlobalApplication.application.contentResolver.openInputStream(Uri.parse("your_uri_here"))
        val outputStream: OutputStream = FileOutputStream(File("your_file_here"))
        val buf = ByteArray(1024)
        var len: Int
        while (inputStream!!.read(buf).also { len = it } > 0) {
            outputStream.write(buf, 0, len)
        }
        outputStream.close()
        inputStream.close()
    }
}