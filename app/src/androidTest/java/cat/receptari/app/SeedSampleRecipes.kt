package cat.receptari.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import cat.receptari.app.data.local.ReceptariDatabase
import cat.receptari.app.data.local.entity.CookEventEntity
import cat.receptari.app.data.local.entity.TagEntity
import cat.receptari.app.data.local.mapper.toWriteModel
import cat.receptari.app.domain.model.Ingredient
import cat.receptari.app.domain.model.IngredientSection
import cat.receptari.app.domain.model.InstructionSection
import cat.receptari.app.domain.model.Recipe
import cat.receptari.app.domain.model.Step
import cat.receptari.app.domain.model.Tag
import cat.receptari.app.domain.parser.IngredientParser
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.time.Duration
import java.time.Instant
import java.util.UUID

/**
 * Development utility: fills the real on-device database with sample recipes so the UI can
 * be looked at with realistic content.
 *
 * Not a test — see [ManualOnly]. It writes to the app's actual database file, so it is
 * destructive to whatever is already there.
 */
@RunWith(AndroidJUnit4::class)
class SeedSampleRecipes {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val now: Instant = Instant.now()

    @Test
    @ManualOnly
    fun seed() = runBlocking {
        val database = Room.databaseBuilder(
            context,
            ReceptariDatabase::class.java,
            ReceptariDatabase.NAME,
        ).build()

        // Start clean. Re-seeding on top of existing data reuses tag rows by normalized
        // name (the unique index), so the freshly generated tag ids below would not exist
        // and the cross-references would fail their foreign key.
        database.clearAllTables()

        val recipeDao = database.recipeDao()
        val tagDao = database.tagDao()
        val cookEventDao = database.cookEventDao()

        val tags = listOf(
            "Carn", "Postres", "Ràpid", "Vegetarià", "Arròs", "Peix", "Tradicional", "Iaia",
        ).associateWith { name ->
            Tag(id = UUID.randomUUID().toString(), name = name)
        }
        tagDao.upsertAll(tags.values.map { TagEntity(it.id, it.name, it.normalizedName) })

        val recipes = sampleRecipes(tags)

        recipes.forEach { (recipe, cookedDaysAgo) ->
            val write = recipe.toWriteModel(recipe.tags)
            recipeDao.saveAggregate(
                recipe = write.recipe,
                ingredientSections = write.ingredientSections,
                ingredients = write.ingredients,
                instructionSections = write.instructionSections,
                steps = write.steps,
                tagLinks = write.tagLinks,
                searchIndex = write.searchIndex,
            )
            cookedDaysAgo.forEach { daysAgo ->
                cookEventDao.insert(
                    CookEventEntity(
                        id = UUID.randomUUID().toString(),
                        recipeId = recipe.id,
                        cookedAt = now.minus(Duration.ofDays(daysAgo)).toEpochMilli(),
                        note = null,
                    ),
                )
            }
        }

        database.close()
    }

    /** Each entry is a recipe plus the days-ago on which it was cooked. */
    private fun sampleRecipes(tags: Map<String, Tag>): List<Pair<Recipe, List<Long>>> = listOf(
        recipe(
            title = "Fricandó de vedella",
            image = imageFile("fricando", 0xFF8C4A2F.toInt(), 0xFF5A2E1C.toInt()),
            prep = 20,
            cook = 90,
            total = 110,
            servings = 4,
            rating = 5,
            tags = listOf(tags.getValue("Carn"), tags.getValue("Tradicional"), tags.getValue("Iaia")),
            notes = "La iaia hi posa un rajolí de vi ranci al final.",
            source = "Receptari de la iaia",
            ingredientSections = listOf(
                "Vedella" to listOf(
                    "800 g de vedella per fricandó",
                    "Sal i pebre al gust",
                    "Farina per enfarinar",
                ),
                "Sofregit" to listOf(
                    "2 cebes",
                    "3 tomàquets madurs",
                    "2 grans d'all",
                    "100 ml de vi blanc",
                    "1–2 fulles de llorer",
                ),
                "Bolets" to listOf(
                    "30 g de moixernons secs",
                    "1 cullerada d'oli d'oliva",
                ),
            ),
            instructionSections = listOf(
                "Prepara la carn" to listOf(
                    "Salpebra la carn i enfarina-la lleugerament.",
                    "Fregeix-la en una cassola fins que estigui daurada i reserva-la.",
                ),
                "Sofregit i cocció" to listOf(
                    "Sofregeix la ceba ben picada a foc lent durant 15 minuts.",
                    "Afegeix l'all i el tomàquet ratllat i cuina 10 minuts més.",
                    "Aboca el vi i deixa que redueixi.",
                    "Torna-hi la carn, cobreix amb aigua i cou a foc lent 1 hora.",
                    "Afegeix els moixernons remullats i cou 15 minuts més.",
                ),
            ),
        ) to listOf(4L, 40L, 120L),

        recipe(
            title = "Crema catalana",
            image = imageFile("crema", 0xFFD8C68D.toInt(), 0xFF8C7A3F.toInt()),
            prep = 25,
            cook = 15,
            total = 40,
            servings = 6,
            rating = 4,
            favorite = true,
            tags = listOf(tags.getValue("Postres"), tags.getValue("Tradicional")),
            notes = "Cal deixar-la reposar a la nevera com a mínim 4 hores.",
            ingredientSections = listOf(
                null to listOf(
                    "1 l de llet",
                    "8 rovells d'ou",
                    "200 g de sucre",
                    "40 g de midó de blat de moro",
                    "1 branca de canyella",
                    "La pell d'una llimona",
                    "Sucre per cremar",
                ),
            ),
            instructionSections = listOf(
                null to listOf(
                    "Bull la llet amb la canyella i la pell de llimona.",
                    "Barreja els rovells amb el sucre i el midó.",
                    "Aboca la llet colada sobre la barreja sense parar de remenar.",
                    "Torna-ho al foc i remena fins que espesseixi.",
                    "Reparteix en cassoletes i deixa refredar.",
                    "Just abans de servir, cobreix amb sucre i crema'l amb el ferro.",
                ),
            ),
        ) to listOf(12L),

        recipe(
            title = "Truita de patates",
            prep = 15,
            cook = 25,
            total = 40,
            servings = 4,
            rating = 4,
            tags = listOf(tags.getValue("Ràpid"), tags.getValue("Vegetarià")),
            ingredientSections = listOf(
                null to listOf(
                    "6 ous",
                    "700 g de patates",
                    "1 ceba",
                    "Oli d'oliva per fregir",
                    "Sal al gust",
                ),
            ),
            instructionSections = listOf(
                null to listOf(
                    "Pela i talla les patates ben fines.",
                    "Fregeix-les a foc mitjà amb la ceba fins que estiguin tendres.",
                    "Escorre-les i barreja-les amb els ous batuts.",
                    "Qualla la truita a la paella per les dues bandes.",
                ),
            ),
        ) to listOf(2L, 9L, 16L, 30L, 44L),

        recipe(
            title = "Pa amb tomàquet",
            servings = 2,
            tags = listOf(tags.getValue("Ràpid"), tags.getValue("Vegetarià")),
            ingredientSections = listOf(
                null to listOf(
                    "4 llesques de pa de pagès",
                    "2 tomàquets de penjar",
                    "Oli d'oliva verge extra",
                    "Sal",
                ),
            ),
            instructionSections = listOf(
                null to listOf(
                    "Torra el pa.",
                    "Frega el tomàquet partit per sobre.",
                    "Amaneix amb oli i sal.",
                ),
            ),
        ) to listOf(1L, 3L, 6L),

        recipe(
            title = "Arròs negre",
            image = imageFile("arros", 0xFF2B2B2B.toInt(), 0xFF6B5E2F.toInt()),
            prep = 20,
            cook = 25,
            total = 45,
            servings = 4,
            rating = 5,
            tags = listOf(tags.getValue("Arròs"), tags.getValue("Peix")),
            source = "Adaptada d'una recepta de l'Empordà",
            ingredientSections = listOf(
                null to listOf(
                    "320 g d'arròs bomba",
                    "400 g de sípia",
                    "1 l de fumet de peix",
                    "2 sobres de tinta de sípia",
                    "1 ceba",
                    "2 grans d'all",
                    "½ pebrot vermell",
                    "Allioli per acompanyar",
                ),
            ),
            instructionSections = listOf(
                null to listOf(
                    "Sofregeix la ceba, l'all i el pebrot.",
                    "Afegeix la sípia a daus i cuina 5 minuts.",
                    "Incorpora l'arròs i la tinta dissolta en una mica de fumet.",
                    "Aboca el fumet calent i cou 18 minuts.",
                    "Deixa reposar 5 minuts i serveix amb allioli.",
                ),
            ),
        ) to emptyList(),

        recipe(
            title = "Chicken curry",
            prep = 10,
            cook = 25,
            total = 35,
            servings = 4,
            language = "en",
            tags = listOf(tags.getValue("Carn"), tags.getValue("Ràpid")),
            ingredientSections = listOf(
                null to listOf(
                    "500 g chicken thighs",
                    "1 onion",
                    "2 garlic cloves",
                    "1 tbsp curry powder",
                    "400 ml coconut milk",
                    "1 1/2 cups basmati rice",
                    "A handful of coriander",
                    "Salt to taste",
                ),
            ),
            instructionSections = listOf(
                null to listOf(
                    "Fry the onion until soft.",
                    "Add the garlic and curry powder and cook for a minute.",
                    "Add the chicken and brown it all over.",
                    "Pour in the coconut milk and simmer for 15 minutes.",
                    "Serve with rice and coriander.",
                ),
            ),
        ) to listOf(21L),
    )

    private fun recipe(
        title: String,
        image: String? = null,
        prep: Int? = null,
        cook: Int? = null,
        total: Int? = null,
        servings: Int? = null,
        rating: Int? = null,
        favorite: Boolean = false,
        notes: String? = null,
        source: String? = null,
        language: String? = null,
        tags: List<Tag> = emptyList(),
        ingredientSections: List<Pair<String?, List<String>>> = emptyList(),
        instructionSections: List<Pair<String?, List<String>>> = emptyList(),
    ): Recipe {
        val id = UUID.randomUUID().toString()
        return Recipe(
            id = id,
            title = title,
            imagePath = image,
            prepTimeMinutes = prep,
            cookTimeMinutes = cook,
            totalTimeMinutes = total,
            baseServings = servings,
            isFavorite = favorite,
            rating = rating,
            notes = notes,
            sourceName = source,
            originalLanguage = language,
            createdAt = now.minus(Duration.ofDays((1..200L).random())),
            updatedAt = now,
            tags = tags,
            ingredientSections = ingredientSections.map { (name, lines) ->
                IngredientSection(
                    id = UUID.randomUUID().toString(),
                    name = name,
                    // Seeded through the real parser, so the sample data exercises exactly
                    // what the editor produces.
                    ingredients = lines.mapNotNull { line ->
                        IngredientParser.parse(line)?.let { parsed ->
                            Ingredient(
                                id = UUID.randomUUID().toString(),
                                quantity = parsed.quantity,
                                quantityMax = parsed.quantityMax,
                                unit = parsed.unit,
                                name = parsed.name,
                                note = parsed.note,
                                originalText = parsed.originalText,
                            )
                        }
                    },
                )
            },
            instructionSections = instructionSections.map { (name, steps) ->
                InstructionSection(
                    id = UUID.randomUUID().toString(),
                    name = name,
                    steps = steps.map { Step(id = UUID.randomUUID().toString(), text = it) },
                )
            },
        )
    }

    /** A placeholder gradient so the image layouts can be judged without real photos. */
    private fun imageFile(name: String, from: Int, to: Int): String {
        val directory = File(context.filesDir, "images").apply { mkdirs() }
        val file = File(directory, "$name.jpg")

        val bitmap = Bitmap.createBitmap(1200, 800, Bitmap.Config.ARGB_8888)
        Canvas(bitmap).drawRect(
            0f,
            0f,
            1200f,
            800f,
            Paint().apply {
                shader = LinearGradient(0f, 0f, 1200f, 800f, from, to, Shader.TileMode.CLAMP)
            },
        )
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it) }
        bitmap.recycle()

        return "images/$name.jpg"
    }
}
