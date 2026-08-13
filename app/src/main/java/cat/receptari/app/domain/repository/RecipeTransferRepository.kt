package cat.receptari.app.domain.repository

import cat.receptari.app.domain.transfer.RecipeTransferArchive
import cat.receptari.app.domain.transfer.RecipeTransferResult
import java.io.File

interface RecipeTransferRepository {
    /** Null means the whole library; otherwise only the requested recipe ids are exported. */
    suspend fun createTransfer(recipeIds: Set<String>? = null): Result<RecipeTransferArchive>

    /** Imports every valid recipe after package-level validation, without per-recipe review. */
    suspend fun importTransfer(packageFile: File): Result<RecipeTransferResult>
}
