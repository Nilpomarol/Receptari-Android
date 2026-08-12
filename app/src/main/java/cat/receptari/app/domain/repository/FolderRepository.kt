package cat.receptari.app.domain.repository

import cat.receptari.app.domain.model.Folder
import cat.receptari.app.domain.model.FolderColor
import cat.receptari.app.domain.model.FolderIcon
import cat.receptari.app.domain.model.FolderSummary
import kotlinx.coroutines.flow.Flow

interface FolderRepository {

    fun observeAll(): Flow<List<Folder>>

    fun observeAllWithCounts(): Flow<List<FolderSummary>>

    /**
     * Creates a folder, or returns the existing one if its normalized name already matches —
     * same dedupe philosophy as [TagRepository.findOrCreate], applied to a single name. The
     * colour and icon are ignored when an existing folder is reused.
     */
    suspend fun create(name: String, color: FolderColor, icon: FolderIcon): Folder

    suspend fun update(id: String, name: String, color: FolderColor, icon: FolderIcon)

    /** Recipes filed under this folder become unfiled; they are never deleted (PRD §5). */
    suspend fun delete(id: String)
}
