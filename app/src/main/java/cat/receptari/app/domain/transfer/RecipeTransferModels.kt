package cat.receptari.app.domain.transfer

import java.io.File

data class RecipeTransferArchive(
    val file: File,
    val suggestedFileName: String,
)

data class RecipeTransferResult(
    val importedRecipeCount: Int,
    val skippedDuplicateCount: Int,
    val importedTranslationCount: Int,
    val importedImageCount: Int,
)

sealed class RecipeTransferError(message: String, cause: Throwable? = null) :
    Exception(message, cause) {
    class EmptySelection : RecipeTransferError("There are no recipes to share")

    class InvalidPackage(cause: Throwable? = null) :
        RecipeTransferError("The file is not a valid Receptari transfer", cause)

    class NewerVersion :
        RecipeTransferError("The transfer was created by a newer Receptari version")

    class Storage(cause: Throwable? = null) :
        RecipeTransferError("The transfer could not be read or written", cause)
}
