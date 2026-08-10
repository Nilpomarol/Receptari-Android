package cat.receptari.app.domain.scaling

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class QuantityFormatterTest {

    private val ca = Locale.forLanguageTag("ca")
    private val en = Locale.ENGLISH

    @Test
    fun `whole numbers lose their decimal part`() {
        assertEquals("2", QuantityFormatter.format(2.0, en))
        assertEquals("600", QuantityFormatter.format(600.0, en))
        assertEquals("1", QuantityFormatter.format(1.0, en))
    }

    @Test
    fun `floating point noise still reads as a whole number`() {
        // 0.1 + 0.2 style drift must not surface as "3.0000000000000004 eggs"
        assertEquals("3", QuantityFormatter.format(3.0000000000000004, en))
        assertEquals("2", QuantityFormatter.format(1.9999999999, en))
    }

    @Test
    fun `common fractions render as glyphs`() {
        assertEquals("½", QuantityFormatter.format(0.5, en))
        assertEquals("¼", QuantityFormatter.format(0.25, en))
        assertEquals("¾", QuantityFormatter.format(0.75, en))
        assertEquals("⅓", QuantityFormatter.format(1.0 / 3, en))
        assertEquals("⅔", QuantityFormatter.format(2.0 / 3, en))
        assertEquals("⅛", QuantityFormatter.format(0.125, en))
    }

    @Test
    fun `mixed numbers pair a whole part with a fraction`() {
        assertEquals("1 ½", QuantityFormatter.format(1.5, en))
        assertEquals("2 ¼", QuantityFormatter.format(2.25, en))
        assertEquals("1 ⅓", QuantityFormatter.format(4.0 / 3, en))
    }

    @Test
    fun `quantities that are not near a common fraction fall back to decimals`() {
        assertEquals("0.6", QuantityFormatter.format(0.6, en))
        assertEquals("1.7", QuantityFormatter.format(1.7, en))
    }

    @Test
    fun `decimal separator follows the locale`() {
        assertEquals("0,6", QuantityFormatter.format(0.6, ca))
        assertEquals("0.6", QuantityFormatter.format(0.6, en))
    }

    @Test
    fun `decimals are capped at two places`() {
        // 400 g scaled by 1÷3 must not read as "133.33333333333334"
        assertEquals("133.33", QuantityFormatter.format(400.0 / 3, en))
    }

    @Test
    fun `large quantities use decimals rather than fraction glyphs`() {
        // "133 ⅓ g" is not how anyone weighs flour; "1 ⅓ cups" is how everyone measures it.
        assertEquals("133.33", QuantityFormatter.format(133.3333, en))
        assertEquals("1 ⅓", QuantityFormatter.format(1.3333, en))
        assertEquals("9 ½", QuantityFormatter.format(9.5, en))
        assertEquals("10.5", QuantityFormatter.format(10.5, en))
    }

    @Test
    fun `ranges join both bounds with an en dash`() {
        assertEquals("1–2", QuantityFormatter.formatRange(1.0, 2.0, en))
        assertEquals("½–1", QuantityFormatter.formatRange(0.5, 1.0, en))
    }

    @Test
    fun `a range without an upper bound is a plain quantity`() {
        assertEquals("2", QuantityFormatter.formatRange(2.0, null, en))
    }

    @Test
    fun `zero and negatives do not crash`() {
        assertEquals("0", QuantityFormatter.format(0.0, en))
        assertEquals("-1", QuantityFormatter.format(-1.0, en))
    }
}
