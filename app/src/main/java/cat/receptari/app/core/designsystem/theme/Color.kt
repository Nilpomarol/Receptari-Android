package cat.receptari.app.core.designsystem.theme

import androidx.compose.ui.graphics.Color

/**
 * The palette of an old recipe book: aged paper, brown ink, kitchen-garden olive, and a
 * little gold leaf.
 *
 * Nothing here is a Material You token and nothing is derived at runtime — the whole point
 * of ADR-009 is that these exact values reach the screen. Light is "paper"; dark is
 * "lamplight", the same book read at night, which is why it is warm walnut rather than the
 * neutral near-black Material would generate.
 */

// --- Light: paper -----------------------------------------------------------------

/** The page itself. Every screen background. */
internal val Parchment = Color(0xFFF1E4C9)

/** A sheet laid on the page — cards, sections, anything that should lift slightly. */
internal val ParchmentRaised = Color(0xFFF8EEDB)

/** A recess in the page — text fields, wells, pressed states. */
internal val ParchmentSunk = Color(0xFFE8D9B8)

/** Aging at the edges. Used for the vignette and the grain, never for text. */
internal val PaperEdge = Color(0xFFC3A578)

internal val Ink = Color(0xFF3A2614)
internal val InkSoft = Color(0xFF77603F)

/** Hairlines: card frames, rules, dividers. */
internal val Rule = Color(0xFFB99A66)

internal val Olive = Color(0xFF55642F)
internal val OliveOn = Color(0xFFF7F0DE)
internal val OliveTint = Color(0xFFDCE0B8)
internal val OliveTintOn = Color(0xFF2C3410)

internal val Terracotta = Color(0xFF9B4A26)
internal val TerracottaOn = Color(0xFFFBF3E4)
internal val TerracottaTint = Color(0xFFF0D5C0)
internal val TerracottaTintOn = Color(0xFF4A1B06)

/** Ratings. Gold leaf, not the yellow of a warning. */
internal val Gold = Color(0xFFB98A2E)
internal val GoldOn = Color(0xFF2A1C00)

/** Favourites. Deep sealing-wax red — related to error, deliberately not the same. */
internal val Claret = Color(0xFF9E2B24)

internal val ErrorInk = Color(0xFF8E2A20)
internal val ErrorInkOn = Color(0xFFFBF3E4)
internal val ErrorTint = Color(0xFFF3D9CF)
internal val ErrorTintOn = Color(0xFF3F0B06)

// --- Dark: lamplight --------------------------------------------------------------

internal val Leather = Color(0xFF1E1710)
internal val LeatherRaised = Color(0xFF2A2118)
internal val LeatherSunk = Color(0xFF171009)

/** Darkening at the edges, the same role [PaperEdge] plays on paper. */
internal val LeatherEdge = Color(0xFF0B0704)

internal val InkDark = Color(0xFFEADCC0)
internal val InkSoftDark = Color(0xFFB3A184)

internal val RuleDark = Color(0xFF5C4C34)

internal val OliveDark = Color(0xFFA9B878)
internal val OliveDarkOn = Color(0xFF232C0A)
internal val OliveTintDark = Color(0xFF3C4620)
internal val OliveTintDarkOn = Color(0xFFDDE7B4)

internal val TerracottaDark = Color(0xFFE09A6E)
internal val TerracottaDarkOn = Color(0xFF4A1B06)
internal val TerracottaTintDark = Color(0xFF6B2F13)
internal val TerracottaTintDarkOn = Color(0xFFF0D5C0)

internal val GoldDark = Color(0xFFDDB25C)
internal val GoldDarkOn = Color(0xFF3A2800)

internal val ClaretDark = Color(0xFFE08A80)

internal val ErrorInkDark = Color(0xFFE79489)
internal val ErrorInkDarkOn = Color(0xFF56100A)
internal val ErrorTintDark = Color(0xFF74180F)
internal val ErrorTintDarkOn = Color(0xFFF3D9CF)
