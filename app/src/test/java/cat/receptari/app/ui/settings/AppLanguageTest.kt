package cat.receptari.app.ui.settings

import org.junit.Assert.assertEquals
import org.junit.Test

class AppLanguageTest {

    @Test
    fun `empty tag means follow the system`() {
        assertEquals(AppLanguage.System, AppLanguage.fromLanguageTag(""))
    }

    @Test
    fun `null tag means follow the system`() {
        assertEquals(AppLanguage.System, AppLanguage.fromLanguageTag(null))
    }

    @Test
    fun `plain language tags map to their language`() {
        assertEquals(AppLanguage.Catalan, AppLanguage.fromLanguageTag("ca"))
        assertEquals(AppLanguage.Spanish, AppLanguage.fromLanguageTag("es"))
        assertEquals(AppLanguage.English, AppLanguage.fromLanguageTag("en"))
    }

    @Test
    fun `region qualified tags map to their language`() {
        assertEquals(AppLanguage.Catalan, AppLanguage.fromLanguageTag("ca-ES"))
        assertEquals(AppLanguage.English, AppLanguage.fromLanguageTag("en-GB"))
    }

    @Test
    fun `an unsupported language falls back to the system`() {
        assertEquals(AppLanguage.System, AppLanguage.fromLanguageTag("de-DE"))
    }
}
