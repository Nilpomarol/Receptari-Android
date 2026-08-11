package cat.receptari.app.data.remote.web

import org.jsoup.Jsoup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The parser reads documents from the open web, where every field is optional in a different
 * way. These cases are the shapes that actually occur on recipe sites — each one was a real
 * variant before it was a test.
 */
class SchemaOrgRecipeParserTest {

    private fun parse(body: String) = SchemaOrgRecipeParser.parse(Jsoup.parse(body))

    private fun jsonLd(json: String) =
        """<html><head><script type="application/ld+json">$json</script></head><body></body></html>"""

    @Test
    fun `reads a plain JSON-LD recipe`() {
        val draft = parse(
            jsonLd(
                """
                {
                  "@context": "https://schema.org",
                  "@type": "Recipe",
                  "name": "Arròs negre",
                  "recipeIngredient": ["400 g d'arròs", "2 sípies"],
                  "recipeInstructions": ["Sofregir la sípia.", "Afegir l'arròs."],
                  "prepTime": "PT20M",
                  "cookTime": "PT1H10M",
                  "recipeYield": "4 racions",
                  "inLanguage": "ca",
                  "author": {"@type": "Person", "name": "Àvia"}
                }
                """,
            ),
        )!!

        assertEquals("Arròs negre", draft.title)
        assertEquals(listOf("400 g d'arròs", "2 sípies"), draft.ingredientSections.single().lines)
        assertEquals(listOf("Sofregir la sípia.", "Afegir l'arròs."), draft.instructionSections.single().lines)
        assertEquals(20, draft.prepTimeMinutes)
        assertEquals(70, draft.cookTimeMinutes)
        assertEquals(4, draft.servings)
        assertEquals("ca", draft.originalLanguage)
        assertEquals("Àvia", draft.sourceName)
    }

    @Test
    fun `finds the recipe inside an @graph wrapper`() {
        val draft = parse(
            jsonLd(
                """
                {"@context":"https://schema.org","@graph":[
                  {"@type":"WebSite","name":"Cuina"},
                  {"@type":"Recipe","name":"Truita","recipeIngredient":["4 ous"]}
                ]}
                """,
            ),
        )!!

        assertEquals("Truita", draft.title)
        assertEquals(listOf("4 ous"), draft.ingredientSections.single().lines)
    }

    @Test
    fun `accepts @type as an array`() {
        val draft = parse(
            jsonLd("""{"@type":["Recipe","NewsArticle"],"name":"Pa","recipeIngredient":["500 g de farina"]}"""),
        )!!

        assertEquals("Pa", draft.title)
    }

    @Test
    fun `reads instructions given as HowToStep objects`() {
        val draft = parse(
            jsonLd(
                """
                {"@type":"Recipe","name":"X","recipeInstructions":[
                  {"@type":"HowToStep","text":"Escalfar el forn."},
                  {"@type":"HowToStep","text":"Coure 30 minuts."}
                ]}
                """,
            ),
        )!!

        assertEquals(
            listOf("Escalfar el forn.", "Coure 30 minuts."),
            draft.instructionSections.single().lines,
        )
    }

    @Test
    fun `keeps HowToSection grouping`() {
        val draft = parse(
            jsonLd(
                """
                {"@type":"Recipe","name":"X","recipeInstructions":[
                  {"@type":"HowToSection","name":"Massa","itemListElement":[
                     {"@type":"HowToStep","text":"Barrejar."}]},
                  {"@type":"HowToSection","name":"Farcit","itemListElement":[
                     {"@type":"HowToStep","text":"Sofregir."}]}
                ]}
                """,
            ),
        )!!

        assertEquals(listOf("Massa", "Farcit"), draft.instructionSections.map { it.name })
        assertEquals(listOf("Barrejar."), draft.instructionSections[0].lines)
    }

    @Test
    fun `splits a single instructions string on markup`() {
        val draft = parse(
            jsonLd("""{"@type":"Recipe","name":"X","recipeInstructions":"Pas u.<br>Pas dos.<br/>Pas tres."}"""),
        )!!

        assertEquals(listOf("Pas u.", "Pas dos.", "Pas tres."), draft.instructionSections.single().lines)
    }

    @Test
    fun `strips HTML out of ingredient text`() {
        val draft = parse(
            jsonLd("""{"@type":"Recipe","name":"X","recipeIngredient":["<span>200 g</span> de nata"]}"""),
        )!!

        assertEquals(listOf("200 g de nata"), draft.ingredientSections.single().lines)
    }

    @Test
    fun `accepts plain minutes where a duration was expected`() {
        val draft = parse(jsonLd("""{"@type":"Recipe","name":"X","prepTime":"15"}"""))!!

        assertEquals(15, draft.prepTimeMinutes)
    }

    @Test
    fun `ignores an unparseable duration rather than guessing`() {
        val draft = parse(jsonLd("""{"@type":"Recipe","name":"X","prepTime":"about an hour"}"""))!!

        assertNull(draft.prepTimeMinutes)
    }

    @Test
    fun `falls back to microdata when there is no JSON-LD`() {
        val draft = parse(
            """
            <div itemscope itemtype="https://schema.org/Recipe">
              <h1 itemprop="name">Croquetes</h1>
              <time itemprop="cookTime" datetime="PT25M">25 min</time>
              <span itemprop="recipeYield">6 racions</span>
              <li itemprop="recipeIngredient">100 g de pernil</li>
              <li itemprop="recipeIngredient">Sal al gust</li>
              <div itemprop="recipeInstructions">Fer la beixamel.</div>
            </div>
            """,
        )!!

        assertEquals("Croquetes", draft.title)
        assertEquals(25, draft.cookTimeMinutes)
        assertEquals(6, draft.servings)
        assertEquals(listOf("100 g de pernil", "Sal al gust"), draft.ingredientSections.single().lines)
        assertEquals(listOf("Fer la beixamel."), draft.instructionSections.single().lines)
    }

    @Test
    fun `returns null for a page with no recipe metadata`() {
        assertNull(parse("<html><body><h1>Un blog</h1><p>Hola</p></body></html>"))
    }

    @Test
    fun `returns null when JSON-LD is malformed rather than throwing`() {
        assertNull(parse(jsonLd("{not json at all")))
    }

    @Test
    fun `ignores JSON-LD that is not a Recipe`() {
        assertNull(parse(jsonLd("""{"@type":"Article","name":"Not a recipe"}""")))
    }

    /**
     * A partial page is a partial success, not a failure — the user finishes it in the
     * editor. What must never happen is an invented ingredient.
     */
    @Test
    fun `keeps a recipe that only has a title`() {
        val draft = parse(jsonLd("""{"@type":"Recipe","name":"Només el títol"}"""))!!

        assertEquals("Només el títol", draft.title)
        assertTrue(draft.ingredientSections.isEmpty())
        assertTrue(draft.instructionSections.isEmpty())
    }
}
