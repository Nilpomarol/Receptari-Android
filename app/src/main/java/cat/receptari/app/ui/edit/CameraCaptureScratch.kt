package cat.receptari.app.ui.edit

import java.io.File

/** Owns the temporary full-size files handed to the external camera app. */
internal class CameraCaptureScratch(
    private val directory: File,
) {
    fun createFileName(nowMillis: Long): String = "capture-$nowMillis.jpg"

    fun file(fileName: String): File {
        require(CAPTURE_FILE.matches(fileName)) { "Invalid camera capture file name" }
        directory.mkdirs()
        return File(directory, fileName)
    }

    /**
     * Reads a successful capture and removes the scratch file in every outcome. The
     * permanent, downscaled copy is created by ImageStore after these bytes are returned.
     */
    fun consume(fileName: String, captured: Boolean): ByteArray? {
        val capture = file(fileName)
        return try {
            capture.takeIf { captured && it.isFile && it.length() > 0L }?.readBytes()
        } finally {
            capture.delete()
        }
    }

    fun discard(fileName: String) {
        file(fileName).delete()
    }

    /** Clears captures abandoned by cancellation, a crash, or process death. */
    fun clearAbandoned(pendingFileName: String?) {
        directory.listFiles()
            .orEmpty()
            .filter { file ->
                CAPTURE_FILE.matches(file.name) && file.name != pendingFileName
            }
            .forEach(File::delete)
    }

    private companion object {
        val CAPTURE_FILE = Regex("capture-[0-9]+\\.jpg")
    }
}
