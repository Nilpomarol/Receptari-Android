package cat.receptari.app.core.designsystem.theme

import androidx.compose.ui.graphics.Color

/**
 * The palette of an old recipe book: aged paper, brown ink, one sealing-wax red for
 * everything you can press, and a little gold leaf for ratings.
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

/**
 * The one accent: sealing-wax red, the rubric colour of a printed cookbook. Buttons, links,
 * selected states and small-caps labels all use it, so "red" always means "you can act here".
 */
internal val Wax = Color(0xFF9A2E22)
internal val WaxOn = Color(0xFFFBF3E4)
internal val WaxTint = Color(0xFFF0D4C8)
internal val WaxTintOn = Color(0xFF3F0B06)

/** Ratings. Gold leaf, not the yellow of a warning. */
internal val Gold = Color(0xFFB98A2E)

/** Engraved botanical line work: a dried-olive sepia. */
internal val Botanical = Color(0xFF8C7A4C)
internal val BotanicalDark = Color(0xFFA39468)
internal val GoldOn = Color(0xFF2A1C00)

/** Favourites. Deep sealing-wax red — related to error, deliberately not the same. */
internal val Claret = Color(0xFF9E2B24)

/** Additional inks reserved for folder stamps. Muted enough to sit on aged paper. */
internal val Forest = Color(0xFF386447)
internal val Indigo = Color(0xFF4B547D)
internal val Cocoa = Color(0xFF73513A)
internal val Slate = Color(0xFF5C6670)

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

internal val WaxDark = Color(0xFFE38D7E)
internal val WaxDarkOn = Color(0xFF4A0F0A)
internal val WaxTintDark = Color(0xFF6B2119)
internal val WaxTintDarkOn = Color(0xFFF3D9CF)

internal val GoldDark = Color(0xFFDDB25C)
internal val GoldDarkOn = Color(0xFF3A2800)

internal val ClaretDark = Color(0xFFE08A80)

internal val ForestDark = Color(0xFF8AB69A)
internal val IndigoDark = Color(0xFFA6AED8)
internal val CocoaDark = Color(0xFFC7A68D)
internal val SlateDark = Color(0xFFAEB7C0)

internal val ErrorInkDark = Color(0xFFE79489)
internal val ErrorInkDarkOn = Color(0xFF56100A)
internal val ErrorTintDark = Color(0xFF74180F)
internal val ErrorTintDarkOn = Color(0xFFF3D9CF)
