package cat.receptari.app.data.backup

import android.database.sqlite.SQLiteDatabase
import cat.receptari.app.domain.backup.BackupError
import java.io.File

internal object BackupDatabaseInspector {
    fun inspect(databaseFile: File, imageDirectory: File): BackupContents {
        val database = try {
            SQLiteDatabase.openDatabase(databaseFile.path, null, SQLiteDatabase.OPEN_READONLY)
        } catch (error: Exception) {
            throw BackupError.InvalidArchive(error)
        }
        return database.use { db ->
            db.rawQuery("PRAGMA integrity_check", null).use { cursor ->
                if (!cursor.moveToFirst() || cursor.getString(0) != "ok") {
                    throw BackupError.InvalidArchive()
                }
            }
            val schemaVersion = db.rawQuery("PRAGMA user_version", null).use { cursor ->
                if (!cursor.moveToFirst()) throw BackupError.InvalidArchive()
                cursor.getInt(0)
            }
            val recipeCount = try {
                db.rawQuery("SELECT COUNT(*) FROM recipes", null).use { cursor ->
                    if (!cursor.moveToFirst()) throw BackupError.InvalidArchive()
                    cursor.getInt(0)
                }
            } catch (error: Exception) {
                throw BackupError.InvalidArchive(error)
            }
            val images = mutableListOf<File>()
            db.rawQuery("SELECT imagePath FROM recipes WHERE imagePath IS NOT NULL", null).use { cursor ->
                while (cursor.moveToNext()) {
                    val path = cursor.getString(0)
                    val fileName = path.removePrefix("images/")
                    if (path != "images/$fileName" || fileName.isBlank() || fileName.contains('/')) {
                        throw BackupError.InvalidArchive()
                    }
                    val image = File(imageDirectory, fileName)
                    if (image.isFile) images += image
                }
            }
            BackupContents(schemaVersion, recipeCount, images.distinctBy { it.name })
        }
    }
}
