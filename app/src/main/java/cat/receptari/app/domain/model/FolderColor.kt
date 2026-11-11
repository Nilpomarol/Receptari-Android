package cat.receptari.app.domain.model

/**
 * The label colour a folder is filed under.
 *
 * A fixed, curated set — not a free colour picker — of muted inks that sit on aged paper, the
 * same way ADR-009 ties every other colour in the book to one hand-picked palette. Declared
 * in colour-wheel order, which is the order the picker shows them in. Stored by name, so a
 * constant is never renamed or removed.
 */
enum class FolderColor {
    CLARET,
    TERRACOTTA,
    GOLD,
    OLIVE,
    FOREST,
    TEAL,
    INDIGO,
    PLUM,
    COCOA,
    SLATE,
}
